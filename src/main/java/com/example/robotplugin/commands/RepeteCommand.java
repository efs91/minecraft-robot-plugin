package com.example.robotplugin.commands;

import com.example.robotplugin.RobotPlugin;
import com.example.robotplugin.commands.FonctionCommand;
import com.example.robotplugin.commands.FonctionCommand.FunctionDefinition;
import com.example.robotplugin.listeners.RobotPlaceListener;
import com.example.robotplugin.robot.RobotBlock;
import org.bukkit.DyeColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepeteCommand implements CommandExecutor {
    private final RobotPlugin plugin;
    
    // Stockage des tâches en cours d'exécution par UUID de joueur
    private static final Map<UUID, BukkitTask> runningTasks = new HashMap<>();

    public RepeteCommand(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSeuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        // Vérifier que le joueur a au moins spécifié un nombre de répétitions et des instructions
        if (args.length < 2) {
            player.sendMessage("§cUtilisation: /repete <nombre> (<instruction1> <instruction2> ... <instructionN>)");
            player.sendMessage("§cExemple: /repete 3 (avance 2 tourne 90 recule 1)");
            return true;
        }

        // Vérifier si le joueur a déjà une tâche en cours
        if (runningTasks.containsKey(playerId)) {
            player.sendMessage("§cVous avez déjà une séquence d'instructions en cours d'exécution.");
            player.sendMessage("§cUtilisez /robot stop pour l'interrompre avant d'en démarrer une nouvelle.");
            return true;
        }

        // Récupérer le nombre de répétitions
        int repetitions;
        try {
            repetitions = Integer.parseInt(args[0]);
            if (repetitions <= 0 || repetitions > 100) {
                player.sendMessage("§cLe nombre de répétitions doit être entre 1 et 100.");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cLe nombre de répétitions doit être un nombre entier.");
            return true;
        }

        // Récupérer le robot du joueur
        RobotBlock robot = RobotPlaceListener.playerRobots.get(playerId);
        if (robot == null) {
            player.sendMessage("§cVous devez d'abord poser votre robot avant de pouvoir utiliser cette commande.");
            return true;
        }

        // Reconstituer la chaîne d'instructions en joignant les arguments restants
        StringBuilder instructionsBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            instructionsBuilder.append(args[i]).append(" ");
        }
        String instructionsStr = instructionsBuilder.toString().trim();

        // Vérifier si les instructions sont entre parenthèses
        if (!instructionsStr.startsWith("(") || !instructionsStr.endsWith(")")) {
            player.sendMessage("§cLes instructions doivent être entre parenthèses.");
            player.sendMessage("§cExemple: /repete 3 (avance 2 tourne 90 recule 1)");
            return true;
        }

        // Enlever les parenthèses
        instructionsStr = instructionsStr.substring(1, instructionsStr.length() - 1).trim();

        // Analyser les instructions
        List<Instruction> parsed = parseInstructions(instructionsStr);
        if (parsed.isEmpty()) {
            player.sendMessage("§cAucune instruction valide trouvée.");
            return true;
        }

        // Aplatir récursivement les instructions et créer la queue finale
        List<Instruction> queue = new ArrayList<>();
        for (int i = 0; i < repetitions; i++) {
            queue.addAll(flattenInstructions(playerId, parsed));
        }

        // Exécuter les instructions séquentiellement
        player.sendMessage("§aExécution de la séquence d'instructions " + repetitions + " fois...");

        BukkitRunnable runnable = new BukkitRunnable() {
            Iterator<Instruction> it = queue.iterator();

            @Override
            public void run() {
                if (!it.hasNext()) {
                    player.sendMessage("§aFin de la séquence d'instructions.");
                    runningTasks.remove(playerId);
                    this.cancel();
                    return;
                }

                // Vérifier si le robot existe toujours
                if (!RobotPlaceListener.playerRobots.containsKey(playerId) || 
                    RobotPlaceListener.playerRobots.get(playerId) != robot) {
                    player.sendMessage("§cRobot non trouvé. Séquence interrompue.");
                    this.cancel();
                    runningTasks.remove(playerId);
                    return;
                }

                // Exécuter l'instruction courante
                executeInstruction(robot, it.next(), player);
            }
        };

        // Stocker et démarrer la tâche
        BukkitTask task = runnable.runTaskTimer(plugin, 0L, 10L); // 10 ticks (1 seconde) entre chaque instruction
        runningTasks.put(playerId, task);

        return true;
    }

    /**
     * Aplatit récursivement une liste d'instructions en traitant les repete et fonction
     * @param playerId UUID du joueur pour accéder à ses fonctions définies
     * @param instructions Liste des instructions à aplatir
     * @return Liste aplatie d'instructions (sans instructions répétées ou fonctions)
     */
    private List<Instruction> flattenInstructions(UUID playerId, List<Instruction> instructions) {
        List<Instruction> result = new ArrayList<>();
        
        for (Instruction instr : instructions) {
            switch (instr.type) {
                case "repete":
                    // Répéter N fois le flatten du corps
                    List<Instruction> inner = parseInstructions(instr.param);
                    for (int i = 0; i < instr.value; i++) {
                        result.addAll(flattenInstructions(playerId, inner));
                    }
                    break;
                case "fonction":
                    // Extraire nom et args de la fonction
                    String functionExpr = instr.param; // ex: "read carre(10,20)"
                    
                    // S'assurer que l'expression commence par "read "
                    if (!functionExpr.startsWith("read ")) {
                        functionExpr = "read " + functionExpr;
                    }
                    
                    // Extraire le nom de la fonction et ses arguments
                    Pattern funcPattern = Pattern.compile("read\\s+([a-zA-Z0-9_]+)(?:\\(([^)]*)\\))?");
                    Matcher m = funcPattern.matcher(functionExpr);
                    
                    if (m.find()) {
                        String fnName = m.group(1);
                        String argsStr = m.group(2);
                        
                        // Récupérer la définition de la fonction
                        FunctionDefinition def = FonctionCommand.getFunctionDefinition(playerId, fnName);
                        
                        if (def != null) {
                            // Traiter les arguments
                            List<String> args = new ArrayList<>();
                            if (argsStr != null && !argsStr.isEmpty()) {
                                String[] argsArray = argsStr.split(",");
                                for (String arg : argsArray) {
                                    args.add(arg.trim());
                                }
                            }
                            
                            // Vérifier le nombre de paramètres
                            if (def.getParameters().size() != args.size()) {
                                // Gérer l'erreur silencieusement (ou log)
                                System.err.println("Erreur: La fonction '" + fnName + "' attend " + 
                                                 def.getParameters().size() + " paramètre(s), mais " + 
                                                 args.size() + " ont été fournis.");
                                continue;
                            }
                            
                            // Copier le corps et substituer les paramètres
                            String body = def.getBody();
                            for (int i = 0; i < def.getParameters().size(); i++) {
                                body = body.replaceAll("\\b" + def.getParameters().get(i) + "\\b", args.get(i));
                            }
                            
                            // Parser et aplatir récursivement
                            if (body.startsWith("repete")) {
                                // Le corps est une répétition, traiter séparément
                                Pattern repetePattern = Pattern.compile("repete\\s+(\\d+)\\s*\\((.+)\\)");
                                Matcher repMatcher = repetePattern.matcher(body);
                                if (repMatcher.find()) {
                                    int count = Integer.parseInt(repMatcher.group(1));
                                    String repInstructions = repMatcher.group(2);
                                    
                                    List<Instruction> repInnerInstr = parseInstructions(repInstructions);
                                    for (int i = 0; i < count; i++) {
                                        result.addAll(flattenInstructions(playerId, repInnerInstr));
                                    }
                                }
                            } else {
                                // Sinon, parser les instructions individuelles
                                String[] individualCommands = body.split(";");
                                for (String cmd : individualCommands) {
                                    cmd = cmd.trim();
                                    if (!cmd.isEmpty()) {
                                        if (cmd.startsWith("avance") || cmd.startsWith("recule") || 
                                            cmd.startsWith("tourne") || cmd.startsWith("monte") || 
                                            cmd.startsWith("descends")) {
                                            // Instructions de mouvement
                                            String[] parts = cmd.split("\\s+");
                                            if (parts.length >= 2) {
                                                try {
                                                    int value = Integer.parseInt(parts[1]);
                                                    result.add(new Instruction(parts[0], value));
                                                } catch (NumberFormatException e) {
                                                    // Ignorer les instructions malformées
                                                }
                                            }
                                        } else if (cmd.startsWith("trace")) {
                                            // Instructions trace
                                            Pattern tracePattern = Pattern.compile("trace\\s+(on|off)(?:\\s+([a-zA-Z_]+))?");
                                            Matcher traceMatcher = tracePattern.matcher(cmd);
                                            if (traceMatcher.find()) {
                                                boolean traceOn = traceMatcher.group(1).equalsIgnoreCase("on");
                                                String color = traceMatcher.group(2); // Peut être null
                                                result.add(new Instruction("trace", traceOn, color != null ? color : "blanc"));
                                            }
                                        } else if (cmd.contains("read")) {
                                            // Appel récursif à une autre fonction
                                            Pattern readPattern = Pattern.compile("read\\s+([a-zA-Z0-9_]+)(?:\\(([^)]*)\\))?");
                                            Matcher readMatcher = readPattern.matcher(cmd);
                                            if (readMatcher.find()) {
                                                String subFnName = readMatcher.group(1);
                                                String subArgsStr = readMatcher.group(2);
                                                result.add(new Instruction("fonction", subFnName, subArgsStr));
                                            }
                                        }
                                        // Les autres types d'instructions sont ignorés
                                    }
                                }
                            }
                        }
                    }
                    break;
                default:
                    // Instructions standard (avance, recule, tourne, etc.)
                    result.add(instr);
                    break;
            }
        }
        
        return result;
    }

    /**
     * Arrête la tâche en cours pour un joueur donné
     * @param playerId UUID du joueur
     * @return true si une tâche a été trouvée et arrêtée, false sinon
     */
    public static boolean stopRunningTask(UUID playerId) {
        BukkitTask task = runningTasks.get(playerId);
        if (task != null) {
            task.cancel();
            runningTasks.remove(playerId);
            return true;
        }
        return false;
    }

    // Classe représentant une instruction à exécuter
    private static class Instruction {
        private final String type;      // "avance", "recule", "tourne", "trace", "monte", "descends", "fonction", "repete"
        private final int value;        // La valeur associée à l'instruction (pour avance, recule, tourne, monte, descends, repete)
        private final String param;     // Paramètre supplémentaire (pour trace: couleur, pour fonction: arguments, pour repete: instructions imbriquées)
        private final boolean traceOn;  // Pour trace: true = on, false = off

        public Instruction(String type, int value) {
            this.type = type;
            this.value = value;
            this.param = null;
            this.traceOn = false;
        }
        
        // Constructeur pour l'instruction trace
        public Instruction(String type, boolean traceOn, String color) {
            this.type = type;
            this.value = 0;
            this.param = color;
            this.traceOn = traceOn;
        }
        
        // Constructeur pour l'instruction fonction ou repete
        public Instruction(String type, String name, String args) {
            this.type = type;
            this.value = 0;
            this.param = name + (args != null ? "(" + args + ")" : "");
            this.traceOn = false;
        }
        
        // Constructeur spécial pour repete avec nombre de répétitions
        public Instruction(String type, int count, String innerInstructions) {
            this.type = type;
            this.value = count;
            this.param = innerInstructions;
            this.traceOn = false;
        }
    }

    // Analyse la chaîne d'instructions et renvoie une liste d'objets Instruction
    private List<Instruction> parseInstructions(String instructionsStr) {
        List<Instruction> instructions = new ArrayList<>();
        
        // Utiliser une expression régulière pour extraire les paires "commande valeur"
        Pattern patternMove = Pattern.compile("(avance|recule|tourne|monte|descends)\\s+(\\d+)");
        Pattern patternTrace = Pattern.compile("trace\\s+(on|off)(?:\\s+([a-zA-Z_]+))?");
        // Pattern amélioré pour capturer tous les formats de commande fonction possibles
        Pattern patternFunction = Pattern.compile("(?:fonction\\s+)?read\\s+([a-zA-Z0-9_]+)(?:\\(([^)]*)\\))?");
        // Pattern pour capturer les commandes repete imbriquées
        Pattern patternRepete = Pattern.compile("repete\\s+(\\d+)\\s*\\(([^)]*)\\)");
        
        // Utiliser StringTokenizer pour découper la chaîne en "tokens"
        StringTokenizer tokenizer = new StringTokenizer(instructionsStr, " \t\n\r\f", false);
        
        StringBuilder currentCommand = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            currentCommand.append(token).append(" ");
            
            // Vérifier si on a une commande complète
            String cmd = currentCommand.toString().trim();
            
            // Vérifier si c'est un repete imbriqué qui contient des parenthèses
            if (cmd.startsWith("repete") && cmd.contains("(")) {
                // Compter les parenthèses pour s'assurer que nous avons la commande complète
                int openParenCount = 0;
                int closeParenCount = 0;
                for (char c : cmd.toCharArray()) {
                    if (c == '(') openParenCount++;
                    if (c == ')') closeParenCount++;
                }
                
                // Si nous avons un nombre égal de parenthèses ouvrantes et fermantes, c'est une commande complète
                if (openParenCount > 0 && openParenCount == closeParenCount) {
                    Matcher matcherRepete = patternRepete.matcher(cmd);
                    if (matcherRepete.find()) {
                        int count = Integer.parseInt(matcherRepete.group(1));
                        String innerInstructions = matcherRepete.group(2);
                        
                        // Ajouter une instruction de type "repete"
                        instructions.add(new Instruction("repete", count, innerInstructions));
                        currentCommand = new StringBuilder();
                        continue;
                    }
                }
            }
            
            // Vérifier les autres types d'instructions
            Matcher matcherMove = patternMove.matcher(cmd);
            Matcher matcherTrace = patternTrace.matcher(cmd);
            Matcher matcherFunction = patternFunction.matcher(cmd);
            
            if (matcherMove.matches()) {
                // C'est une commande de mouvement
                String type = matcherMove.group(1);
                int value = Integer.parseInt(matcherMove.group(2));
                instructions.add(new Instruction(type, value));
                currentCommand = new StringBuilder();
            } else if (matcherTrace.matches()) {
                // C'est une commande trace
                boolean traceOn = matcherTrace.group(1).equalsIgnoreCase("on");
                String color = matcherTrace.group(2); // Peut être null si non spécifié
                instructions.add(new Instruction("trace", traceOn, color != null ? color : "blanc"));
                currentCommand = new StringBuilder();
            } else if (matcherFunction.matches()) {
                // C'est un appel de fonction
                String functionName = matcherFunction.group(1);
                String functionArgs = matcherFunction.group(2); // Peut être null si pas d'arguments
                instructions.add(new Instruction("fonction", functionName, functionArgs));
                currentCommand = new StringBuilder();
            }
        }
        
        return instructions;
    }

    // Exécute une instruction sur le robot
    private void executeInstruction(RobotBlock robot, Instruction instruction, Player player) {
        switch (instruction.type) {
            case "avance":
                robot.moveForward(instruction.value);
                player.sendMessage("§7Robot avancé de " + instruction.value + " blocs.");
                break;
            case "recule":
                robot.moveBackward(instruction.value);
                player.sendMessage("§7Robot reculé de " + instruction.value + " blocs.");
                break;
            case "tourne":
                robot.rotate(instruction.value);
                player.sendMessage("§7Robot tourné de " + instruction.value + " degrés.");
                break;
            case "monte":
                robot.moveUp(instruction.value);
                player.sendMessage("§7Robot monté de " + instruction.value + " blocs.");
                break;
            case "descends":
                boolean fullyMoved = robot.moveDown(instruction.value);
                if (fullyMoved) {
                    player.sendMessage("§7Robot descendu de " + instruction.value + " blocs.");
                } else {
                    player.sendMessage("§7Robot descendu partiellement. Vous ne pouvez pas descendre sous Y=-60.");
                }
                break;
            case "trace":
                if (instruction.traceOn) {
                    try {
                        DyeColor color = TraceCommand.getColorFromString(instruction.param);
                        robot.setTraceEnabled(true);
                        robot.setTraceColor(color);
                        player.sendMessage("§7Tracé activé avec la couleur " + instruction.param + ".");
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cCouleur invalide: " + instruction.param + ", utilisation de la couleur blanche par défaut.");
                        robot.setTraceEnabled(true);
                        robot.setTraceColor(DyeColor.WHITE);
                    }
                } else {
                    robot.setTraceEnabled(false);
                    player.sendMessage("§7Tracé désactivé.");
                }
                break;
        }
    }
}