package com.example.robotplugin.commands;

import com.example.robotplugin.RobotPlugin;
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
        }        // Aplatir récursivement les instructions et créer la queue finale
        List<Instruction> queue = new ArrayList<>();
        for (int i = 0; i < repetitions; i++) {
            List<Instruction> flattened = flattenInstructions(playerId, parsed);
            player.sendMessage("§7[DEBUG] Itération " + (i+1) + ": " + flattened.size() + " instructions aplaties");
            queue.addAll(flattened);
        }

        // Exécuter les instructions séquentiellement
        player.sendMessage("§aExécution de la séquence d'instructions " + repetitions + " fois...");
        player.sendMessage("§7[DEBUG] Queue totale: " + queue.size() + " instructions");

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
     */    private List<Instruction> flattenInstructions(UUID playerId, List<Instruction> instructions) {
        List<Instruction> result = new ArrayList<>();
        
        for (Instruction instr : instructions) {
            System.out.println("[DEBUG] Traitement instruction: " + instr.type + " - " + instr.param);
            switch (instr.type) {
                case "repete":
                    // Répéter N fois le flatten du corps
                    List<Instruction> inner = parseInstructions(instr.param);
                    System.out.println("[DEBUG] Repete " + instr.value + " fois, instructions internes: " + inner.size());
                    for (int i = 0; i < instr.value; i++) {
                        result.addAll(flattenInstructions(playerId, inner));
                    }
                    break;
                case "fonction":
                    // Extraire nom et args de la fonction
                    String functionExpr = instr.param; // ex: "read carre(10,20)"
                    System.out.println("[DEBUG] Appel fonction: " + functionExpr);
                    
                    // S'assurer que l'expression commence par "read "
                    if (!functionExpr.startsWith("read ")) {
                        functionExpr = "read " + functionExpr;
                    }
                    
                    // Extraire le nom de la fonction et ses arguments
                    Pattern funcPattern = Pattern.compile("read\\s+([a-zA-Z0-9_]+)(?:\\(([^)]*)\\))?");
                    Matcher m = funcPattern.matcher(functionExpr);
                    
                    if (m.find()) {
                        String fnName = m.group(1);
                        String argsStr = m.group(2);                        // Récupérer la définition de la fonction
                        FunctionDefinition def = FonctionCommand.getFunctionDefinition(playerId, fnName);
                        
                        if (def != null) {
                            System.out.println("[DEBUG] Définition trouvée: " + def.getBody());
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
                            System.out.println("[DEBUG] Corps après substitution: " + body);
                            
                            // Parser et aplatir récursivement le corps de la fonction
                            List<Instruction> bodyInstructions = parseInstructions(body);
                            System.out.println("[DEBUG] Instructions du corps: " + bodyInstructions.size());
                            result.addAll(flattenInstructions(playerId, bodyInstructions));
                        } else {
                            System.err.println("[DEBUG] Fonction non trouvée: " + fnName);
                        }
                    }
                    break;                default:
                    // Instructions standard (avance, recule, tourne, etc.)
                    System.out.println("[DEBUG] Instruction standard: " + instr.type + " " + instr.value);
                    result.add(instr);
                    break;
            }
        }
        
        System.out.println("[DEBUG] Flatten résultat: " + result.size() + " instructions");
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
    }    // Analyse la chaîne d'instructions et renvoie une liste d'objets Instruction
    private List<Instruction> parseInstructions(String instructionsStr) {
        List<Instruction> instructions = new ArrayList<>();
        
        // Patterns pour identifier les différents types d'instructions
        Pattern patternMove = Pattern.compile("(avance|recule|tourne|monte|descends)\\s+(\\d+)");
        Pattern patternTrace = Pattern.compile("trace\\s+(on|off)(?:\\s+([a-zA-Z_]+))?");
        Pattern patternFunction = Pattern.compile("(?:fonction\\s+)?read\\s+([a-zA-Z0-9_]+)(?:\\(([^)]*)\\))?");
        Pattern patternRepete = Pattern.compile("repete\\s+(\\d+)\\s*\\(([^)]*)\\)");
        
        // Diviser les instructions par des espaces, mais en préservant les parenthèses
        String[] tokens = splitInstructions(instructionsStr);
        
        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty()) continue;
            
            // Vérifier les patterns dans l'ordre de priorité
            Matcher matcherRepete = patternRepete.matcher(token);
            Matcher matcherMove = patternMove.matcher(token);
            Matcher matcherTrace = patternTrace.matcher(token);
            Matcher matcherFunction = patternFunction.matcher(token);
            
            if (matcherRepete.matches()) {
                // C'est une commande repete imbriquée
                int count = Integer.parseInt(matcherRepete.group(1));
                String innerInstructions = matcherRepete.group(2);
                instructions.add(new Instruction("repete", count, innerInstructions));
            } else if (matcherMove.matches()) {
                // C'est une commande de mouvement
                String type = matcherMove.group(1);
                int value = Integer.parseInt(matcherMove.group(2));
                instructions.add(new Instruction(type, value));
            } else if (matcherTrace.matches()) {
                // C'est une commande trace
                boolean traceOn = matcherTrace.group(1).equalsIgnoreCase("on");
                String color = matcherTrace.group(2);
                instructions.add(new Instruction("trace", traceOn, color != null ? color : "blanc"));
            } else if (matcherFunction.matches()) {
                // C'est un appel de fonction
                String functionName = matcherFunction.group(1);
                String functionArgs = matcherFunction.group(2);
                instructions.add(new Instruction("fonction", functionName, functionArgs));
            }
        }
        
        return instructions;
    }
    
    /**
     * Divise une chaîne d'instructions en respectant les parenthèses et la structure
     */
    private String[] splitInstructions(String instructionsStr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        int parenLevel = 0;
        boolean inFunctionCall = false;
        
        String[] words = instructionsStr.split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            
            // Détecter le début d'un appel de fonction
            if (word.equals("read") || (word.equals("fonction") && i + 1 < words.length && words[i + 1].equals("read"))) {
                // Si on avait déjà un token en cours, le finaliser
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString().trim());
                    currentToken = new StringBuilder();
                }
                inFunctionCall = true;
            }
            
            // Détecter le début d'une répétition
            if (word.equals("repete")) {
                // Si on avait déjà un token en cours, le finaliser
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString().trim());
                    currentToken = new StringBuilder();
                }
            }
            
            // Ajouter le mot au token actuel
            if (currentToken.length() > 0) {
                currentToken.append(" ");
            }
            currentToken.append(word);
            
            // Compter les parenthèses
            for (char c : word.toCharArray()) {
                if (c == '(') parenLevel++;
                if (c == ')') parenLevel--;
            }
            
            // Vérifier si on a terminé une instruction complète
            boolean isComplete = false;
            
            if (inFunctionCall) {
                // Pour les fonctions, on termine quand on a fermé toutes les parenthèses
                // ou quand on n'a pas de parenthèses du tout
                if (parenLevel == 0 && (word.contains(")") || !instructionsStr.contains("("))) {
                    isComplete = true;
                    inFunctionCall = false;
                }
            } else if (word.equals("repete")) {
                // Pour repete, on continue
                isComplete = false;
            } else if (currentToken.toString().startsWith("repete")) {
                // Pour repete, on termine quand toutes les parenthèses sont fermées
                if (parenLevel == 0 && word.contains(")")) {
                    isComplete = true;
                }
            } else {
                // Pour les autres commandes simples (avance, tourne, etc.)
                String currentStr = currentToken.toString().trim();
                if (currentStr.matches("(avance|recule|tourne|monte|descends)\\s+\\d+") ||
                    currentStr.matches("trace\\s+(on|off)(?:\\s+[a-zA-Z_]+)?")) {
                    isComplete = true;
                }
            }
            
            if (isComplete) {
                tokens.add(currentToken.toString().trim());
                currentToken = new StringBuilder();
                parenLevel = 0;
            }
        }
        
        // Ajouter le dernier token s'il en reste un
        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString().trim());
        }
        
        return tokens.toArray(new String[0]);
    }// Exécute une instruction sur le robot
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
            default:
                player.sendMessage("§cInstruction inconnue: " + instruction.type);
                break;
        }
    }
}