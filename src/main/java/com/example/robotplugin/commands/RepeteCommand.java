package com.example.robotplugin.commands;

import com.example.robotplugin.RobotPlugin;
import com.example.robotplugin.listeners.RobotPlaceListener;
import com.example.robotplugin.robot.RobotBlock;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
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
        List<Instruction> instructions = parseInstructions(instructionsStr);
        if (instructions.isEmpty()) {
            player.sendMessage("§cAucune instruction valide trouvée.");
            return true;
        }

        // Exécuter les instructions de façon asynchrone
        player.sendMessage("§aExécution de la séquence d'instructions " + repetitions + " fois...");
        
        BukkitRunnable runnable = new BukkitRunnable() {
            private int currentRepetition = 0;
            private int currentInstruction = 0;

            @Override
            public void run() {
                // Vérifier si le robot existe toujours
                if (!RobotPlaceListener.playerRobots.containsKey(playerId) || 
                    RobotPlaceListener.playerRobots.get(playerId) != robot) {
                    player.sendMessage("§cRobot non trouvé. Séquence interrompue.");
                    this.cancel();
                    runningTasks.remove(playerId);
                    return;
                }

                // Exécuter l'instruction courante
                Instruction instruction = instructions.get(currentInstruction);
                executeInstruction(robot, instruction, player);

                // Passer à l'instruction suivante
                currentInstruction++;

                // Si toutes les instructions de cette répétition sont terminées
                if (currentInstruction >= instructions.size()) {
                    currentInstruction = 0;
                    currentRepetition++;
                    if (currentRepetition >= repetitions) {
                        player.sendMessage("§aFin de la séquence d'instructions.");
                        this.cancel();
                        runningTasks.remove(playerId);
                    }
                }
            }
        };
        
        // Enregistrer la tâche et la démarrer
        BukkitTask task = runnable.runTaskTimer(plugin, 0L, 5L); // 5 ticks (0.25 secondes) entre chaque instruction
        runningTasks.put(playerId, task);

        return true;
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
        private final String type;      // "avance", "recule", "tourne", "trace", "monte", "descends"
        private final int value;        // La valeur associée à l'instruction (pour avance, recule, tourne, monte, descends)
        private final String param;     // Paramètre supplémentaire (pour trace: couleur)
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
    }

    // Analyse la chaîne d'instructions et renvoie une liste d'objets Instruction
    private List<Instruction> parseInstructions(String instructionsStr) {
        List<Instruction> instructions = new ArrayList<>();
        
        // Utiliser une expression régulière pour extraire les paires "commande valeur"
        Pattern patternMove = Pattern.compile("(avance|recule|tourne|monte|descends)\\s+(\\d+)");
        Pattern patternTrace = Pattern.compile("trace\\s+(on|off)(?:\\s+([a-zA-Z_]+))?");
        
        // Utiliser StringTokenizer pour découper la chaîne en "tokens"
        StringTokenizer tokenizer = new StringTokenizer(instructionsStr, " \t\n\r\f", false);
        
        StringBuilder currentCommand = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            currentCommand.append(token).append(" ");
            
            // Vérifier si on a une commande complète
            String cmd = currentCommand.toString().trim();
            Matcher matcherMove = patternMove.matcher(cmd);
            Matcher matcherTrace = patternTrace.matcher(cmd);
            
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