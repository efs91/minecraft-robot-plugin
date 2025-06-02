package com.example.robotplugin.commands;

import com.example.robotplugin.RobotPlugin;
import com.example.robotplugin.listeners.RobotPlaceListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FonctionCommand implements CommandExecutor {
    private final RobotPlugin plugin;
    
    // Structure pour stocker les fonctions définies par chaque joueur
    // Map<UUID du joueur, Map<nom de fonction, FunctionDefinition>>
    private static final Map<UUID, Map<String, FunctionDefinition>> playerFunctions = new ConcurrentHashMap<>();
    
    // Pour suivre les fonctions en cours d'exécution pour éviter les appels récursifs infinis
    private static final Map<UUID, Set<String>> executingFunctions = new ConcurrentHashMap<>();
    
    // Profondeur maximale d'appels imbriqués
    private static final int MAX_CALL_DEPTH = 10;
    
    // Dossier où seront stockés les fichiers de fonctions
    private static final String FUNCTIONS_DIR = "plugins/RobotPlugin/functions/";
    
    // Pattern pour détecter les déclarations de fonction avec paramètres: fonction create nom(param1,param2) corps
    private static final Pattern FUNCTION_DECLARE_PATTERN = Pattern.compile("^create\\s+([a-zA-Z0-9_]+)\\(([^)]*?)\\)\\s+(.+)$");
    
    // Pattern pour détecter les appels de fonction avec paramètres: read nomFonction(10,20)
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile("^read\\s+([a-zA-Z0-9_]+)\\(([^)]*)\\)$");
    
    // Classe interne pour stocker la définition d'une fonction avec ses paramètres
    public static class FunctionDefinition {
        private final List<String> parameters;
        private final String body;
        
        public FunctionDefinition(List<String> parameters, String body) {
            this.parameters = parameters;
            this.body = body;
        }
        
        public List<String> getParameters() {
            return parameters;
        }
        
        public String getBody() {
            return body;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(parameters.get(i));
            }
            sb.append(") ").append(body);
            return sb.toString();
        }
    }
    
    public FonctionCommand(RobotPlugin plugin) {
        this.plugin = plugin;
        
        // Créer le dossier des fonctions s'il n'existe pas
        File functionsDir = new File(FUNCTIONS_DIR);
        if (!functionsDir.exists()) {
            functionsDir.mkdirs();
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSeuls les joueurs peuvent utiliser cette commande.");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        
        // Vérifier que le joueur a placé un robot
        if (!RobotPlaceListener.playerRobots.containsKey(playerId)) {
            player.sendMessage("§cVous devez d'abord placer votre robot avant de pouvoir utiliser cette commande.");
            return true;
        }
        
        // Si pas d'arguments, afficher l'aide
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        // Reconstruire la commande complète pour traiter les cas avec parenthèses
        StringBuilder commandBuilder = new StringBuilder();
        for (String arg : args) {
            commandBuilder.append(arg).append(" ");
        }
        String fullCommand = commandBuilder.toString().trim();
        
        // Vérifier si c'est une création de fonction (avec le préfixe "create")
        if (args.length >= 1 && args[0].equalsIgnoreCase("create")) {
            // Retirer le préfixe "create" pour analyser le reste de la commande
            String createCommand = fullCommand.substring(6).trim();
            
            // Vérifier si la commande est une définition de fonction avec paramètres
            // Utiliser createCommand qui ne contient plus le préfixe "create"
            Matcher declareMatcher = Pattern.compile("^([a-zA-Z0-9_]+)\\(([^)]*?)\\)\\s+(.+)$").matcher(createCommand);
            if (declareMatcher.find()) {
                String functionName = declareMatcher.group(1);
                String paramsString = declareMatcher.group(2);
                String functionBody = declareMatcher.group(3);
                
                // Vérifier si le nom de la fonction est valide
                if (!functionName.matches("^[a-zA-Z0-9_]+$")) {
                    player.sendMessage("§cLe nom de la fonction ne doit contenir que des lettres, des chiffres et des underscores.");
                    return true;
                }
                
                // Traiter les paramètres
                List<String> parameters = new ArrayList<>();
                if (!paramsString.isEmpty()) {
                    String[] params = paramsString.split(",");
                    for (String param : params) {
                        String trimmedParam = param.trim();
                        if (!trimmedParam.isEmpty()) {
                            parameters.add(trimmedParam);
                        }
                    }
                }
                
                // Créer la définition de fonction
                FunctionDefinition functionDef = new FunctionDefinition(parameters, functionBody);
                
                // Obtenir ou créer la map des fonctions du joueur
                Map<String, FunctionDefinition> functions = playerFunctions.computeIfAbsent(playerId, k -> new HashMap<>());
                
                // Enregistrer la fonction
                functions.put(functionName, functionDef);
                player.sendMessage("§aFonction '" + functionName + "' définie avec succès.");
                
                // Sauvegarder la fonction dans un fichier
                saveFunctionsToFile(playerId);
                
                return true;
            }
            
            // Si le format n'est pas correct pour une création avec paramètres
            player.sendMessage("§cFormat incorrect. Utilisez: /fonction create nomFonction(param1,param2) instructions");
            return true;
        }
          // Vérifier si c'est un appel de fonction (avec le préfixe "read")
        if (args.length >= 1 && args[0].equalsIgnoreCase("read")) {
            // Retirer le préfixe "read" pour analyser le reste de la commande
            String readCommand = fullCommand.substring(4).trim();
            
            // Vérifier si c'est un appel de fonction avec paramètres: read carre(10)
            Matcher callMatcher = FUNCTION_CALL_PATTERN.matcher(fullCommand);
            if (callMatcher.find()) {
                String functionName = callMatcher.group(1);
                String argsString = callMatcher.group(2);
                
                // Traiter les arguments
                List<String> functionArgs = new ArrayList<>();
                if (!argsString.isEmpty()) {
                    String[] argArray = argsString.split(",");
                    for (String arg : argArray) {
                        functionArgs.add(arg.trim());
                    }
                }
                
                // Exécuter la fonction avec les arguments
                return executeFunction(player, functionName, functionArgs, 0);
            }
            
            // Vérifier si c'est un appel à une fonction sans paramètres: read carre
            if (args.length >= 2) {
                String functionName = args[1];
                return executeFunction(player, functionName, new ArrayList<>(), 0);
            }
            
            // Si le format n'est pas correct pour un appel
            player.sendMessage("§cFormat incorrect. Utilisez: /fonction read nomFonction(arg1,arg2)");
            return true;
        }
        
        // Vérifier si c'est une suppression de fonction (avec le préfixe "delete")
        if (args.length >= 1 && args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                player.sendMessage("§cVeuillez spécifier le nom de la fonction à supprimer. Utilisez: /fonction delete nomFonction");
                return true;
            }
            
            String functionName = args[1];
            
            // Vérifier si le nom de la fonction est valide
            if (!functionName.matches("^[a-zA-Z0-9_]+$")) {
                player.sendMessage("§cLe nom de la fonction ne doit contenir que des lettres, des chiffres et des underscores.");
                return true;
            }
            
            // Obtenir la map des fonctions du joueur
            Map<String, FunctionDefinition> functions = playerFunctions.get(playerId);
            
            if (functions == null || !functions.containsKey(functionName)) {
                player.sendMessage("§cLa fonction '" + functionName + "' n'existe pas.");
                return true;
            }
            
            // Supprimer la fonction
            functions.remove(functionName);
            player.sendMessage("§aFonction '" + functionName + "' supprimée avec succès.");
            
            // Sauvegarder les modifications dans le fichier
            saveFunctionsToFile(playerId);
            
            return true;
        }
        
        // Si aucun préfixe reconnu
        player.sendMessage("§cVeuillez utiliser 'create' pour définir une fonction, 'read' pour exécuter une fonction, ou 'delete' pour supprimer une fonction.");
        showHelp(player);
        return true;
    }
    
    /**
     * Exécute une fonction définie par le joueur avec les arguments fournis
     * @param player Le joueur qui exécute la fonction
     * @param functionName Le nom de la fonction à exécuter
     * @param args Les arguments passés à la fonction
     * @param callDepth Profondeur d'appel actuelle pour détecter les appels récursifs
     * @return true si la commande s'est exécutée correctement
     */
    private boolean executeFunction(Player player, String functionName, List<String> args, int callDepth) {
        UUID playerId = player.getUniqueId();
        
        // Vérifier si le joueur a des fonctions définies
        Map<String, FunctionDefinition> functions = playerFunctions.get(playerId);
        if (functions == null || !functions.containsKey(functionName)) {
            player.sendMessage("§cFonction '" + functionName + "' non trouvée. Utilisez /fonction create nom(params) instructions pour définir une fonction.");
            return true;
        }
        
        // Vérifier la profondeur d'appel pour éviter une récursion infinie
        if (callDepth >= MAX_CALL_DEPTH) {
            player.sendMessage("§cErreur: Trop d'appels imbriqués de fonctions (max " + MAX_CALL_DEPTH + ").");
            return true;
        }
        
        // Vérifier qu'on n'appelle pas une fonction déjà en cours d'exécution (éviter les boucles infinies)
        Set<String> executing = executingFunctions.computeIfAbsent(playerId, k -> new HashSet<>());
        if (executing.contains(functionName)) {
            player.sendMessage("§cErreur: Appel récursif détecté dans la fonction '" + functionName + "'.");
            return true;
        }
        
        // Marquer cette fonction comme en cours d'exécution
        executing.add(functionName);
        
        try {
            // Récupérer la définition de la fonction
            FunctionDefinition functionDef = functions.get(functionName);
            
            // Vérifier le nombre de paramètres
            if (functionDef.getParameters().size() != args.size()) {
                player.sendMessage("§cErreur: La fonction '" + functionName + "' attend " + 
                                  functionDef.getParameters().size() + " paramètre(s), mais " + 
                                  args.size() + " ont été fournis.");
                return true;
            }
            
            // Exécuter les instructions comme si le joueur les avait tapées
            if (callDepth == 0) {
                player.sendMessage("§aExécution de la fonction '" + functionName + "'...");
            }
            
            // Copier les instructions pour les modifier
            String instructions = functionDef.getBody();
            
            // Remplacer les paramètres par leurs valeurs
            for (int i = 0; i < functionDef.getParameters().size(); i++) {
                String paramName = functionDef.getParameters().get(i);
                String paramValue = args.get(i);
                instructions = instructions.replaceAll("\\b" + paramName + "\\b", paramValue);
            }            // Si les instructions commencent par "repete", on les exécute directement sans passer par RepeteCommand
            if (instructions.startsWith("repete")) {
                // Utiliser RepeteCommand mais en passant par dispatchCommand pour conserver le contexte
                plugin.getServer().dispatchCommand(player, instructions);
            } else {
                // Sinon, c'est une séquence d'instructions individuelles
                // Vérifier d'abord si c'est séparé par ; ou par des espaces
                String[] individualCommands;
                if (instructions.contains(";")) {
                    individualCommands = instructions.split(";");
                } else {
                    individualCommands = parseCommandsFromString(instructions);
                }
                executeCommandsSequentially(player, individualCommands, 0, callDepth);
            }
            return true;
        } finally {
            // Toujours retirer la fonction de la liste des fonctions en cours d'exécution
            executing.remove(functionName);
            if (executing.isEmpty()) {
                executingFunctions.remove(playerId);
            }
        }
    }
    
    /**
     * Exécute une liste de commandes séquentiellement avec un délai entre chaque commande
     * @param player Le joueur qui exécute les commandes
     * @param commands La liste des commandes à exécuter
     * @param index L'index de la commande actuelle
     * @param callDepth La profondeur d'appel actuelle
     */
    private void executeCommandsSequentially(Player player, String[] commands, int index, int callDepth) {
        if (index >= commands.length) {
            return;  // Toutes les commandes ont été exécutées
        }
        
        String cmd = commands[index].trim();
        if (!cmd.isEmpty()) {
            // Exécuter cette commande avant de passer à la suivante
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Vérifier si c'est un appel à une autre fonction
                if (cmd.startsWith("read ") && cmd.length() > 5) {
                    String readPart = cmd.substring(5).trim();
                    
                    // Vérifier s'il s'agit d'un appel de fonction avec paramètres
                    Matcher callMatcher = FUNCTION_CALL_PATTERN.matcher(cmd);
                    if (callMatcher.find()) {
                        String calledFunction = callMatcher.group(1);
                        String argsString = callMatcher.group(2);
                        
                        // Traiter les arguments
                        List<String> functionArgs = new ArrayList<>();
                        if (!argsString.isEmpty()) {
                            String[] argArray = argsString.split(",");
                            for (String arg : argArray) {
                                functionArgs.add(arg.trim());
                            }
                        }
                        
                        // Exécuter la fonction avec les arguments
                        executeFunction(player, calledFunction, functionArgs, callDepth + 1);
                    } else {
                        // Appel sans paramètres
                        executeFunction(player, readPart, new ArrayList<>(), callDepth + 1);
                    }
                } else {
                    // Sinon, c'est une commande normale
                    // Vérifier si la commande a besoin d'un préfixe "/"
                    String command = cmd;
                    if (cmd.startsWith("avance") || cmd.startsWith("recule") || 
                        cmd.startsWith("tourne") || cmd.startsWith("monte") || 
                        cmd.startsWith("descends") || cmd.startsWith("trace")) {
                        command = "/" + cmd;
                    }
                    plugin.getServer().dispatchCommand(player, command);
                }
                
                // Programmer l'exécution de la prochaine commande après un délai
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    executeCommandsSequentially(player, commands, index + 1, callDepth);
                }, 10L); // Délai de 10 ticks (1 seconde) entre les commandes
            });
        } else {
            // Si la commande est vide, passer directement à la suivante
            executeCommandsSequentially(player, commands, index + 1, callDepth);
        }
    }
    
    /**
     * Traite et exécute les appels de fonction dans une chaîne d'instructions
     * @param instructions Les instructions à traiter
     * @param player Le joueur qui exécute les instructions
     * @param callDepth Profondeur d'appel actuelle
     * @return Les instructions modifiées après traitement des appels de fonction
     */
    private String processAndExecuteFunctionCalls(String instructions, Player player, int callDepth) {
        // Nouveau pattern pour détecter les appels de fonction avec le préfixe "read"
        Pattern readCallPattern = Pattern.compile("read\\s+([a-zA-Z0-9_]+)\\(([^)]*)\\)");
        Matcher matcher = readCallPattern.matcher(instructions);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String functionName = matcher.group(1);
            String argsString = matcher.group(2);
            
            // Traiter les arguments
            List<String> args = new ArrayList<>();
            if (!argsString.isEmpty()) {
                String[] argArray = argsString.split(",");
                for (String arg : argArray) {
                    args.add(arg.trim());
                }
            }
            
            // Exécuter la fonction appelée
            executeFunction(player, functionName, args, callDepth + 1);
            
            // Remplacer l'appel de fonction par une chaîne vide dans le résultat
            matcher.appendReplacement(result, "");
        }
        matcher.appendTail(result);
        
        return result.toString().trim();
    }
      /**
     * Affiche l'aide pour la commande fonction
     * @param player Le joueur à qui afficher l'aide
     */
    private void showHelp(Player player) {
        player.sendMessage("§a=== Commande Fonction ===");
        player.sendMessage("§e/fonction create nom(param1,param2) instructions §7- Définit une fonction avec paramètres");
        player.sendMessage("§e/fonction read nom(param1,param2) §7- Exécute une fonction déjà définie avec des paramètres");
        player.sendMessage("§e/fonction delete nom §7- Supprime une fonction déjà définie");
        player.sendMessage("§7Exemples:");
        player.sendMessage("§7• §e/fonction create carre(size) repete 4( avance size tourne 90)");
        player.sendMessage("§7• §e/fonction create rectangle(x,y) repete 2 (avance x tourne 90 avance y tourne 90)");
        player.sendMessage("§7• §e/fonction create escalier(hauteur,largeur) repete hauteur (avance largeur monte 1)");
        player.sendMessage("§7Appel: §e/fonction read carre(5) §7ou dans une autre fonction: §eread carre(5)");
        player.sendMessage("§7Suppression: §e/fonction delete carre");
        player.sendMessage("§7Utilisez §e; §7pour séparer plusieurs commandes individuelles");
        player.sendMessage("§7Vos fonctions sont sauvegardées et disponibles à votre prochaine connexion.");
        
        // Afficher les fonctions actuellement définies par le joueur
        UUID playerId = player.getUniqueId();
        Map<String, FunctionDefinition> functions = playerFunctions.get(playerId);
        
        if (functions != null && !functions.isEmpty()) {
            player.sendMessage("§a=== Fonctions définies ===");
            for (Map.Entry<String, FunctionDefinition> entry : functions.entrySet()) {
                player.sendMessage("§e" + entry.getKey() + entry.getValue().toString());
            }
        }
    }
    
    /**
     * Charge les fonctions d'un joueur depuis le fichier
     * @param playerId UUID du joueur
     */
    public static void loadFunctionsFromFile(UUID playerId) {
        File playerFile = new File(FUNCTIONS_DIR + playerId.toString() + ".dat");
        if (!playerFile.exists()) {
            return;  // Pas de fichier de fonctions pour ce joueur
        }
        
        Map<String, FunctionDefinition> functions = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(playerFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // Format: nomFonction=params=body
                    String[] parts = line.split("=", 3);
                    if (parts.length >= 2) {
                        String functionName = parts[0];
                        
                        if (parts.length == 2) {
                            // Ancienne version sans paramètres
                            FunctionDefinition functionDef = new FunctionDefinition(
                                new ArrayList<>(), parts[1]
                            );
                            functions.put(functionName, functionDef);
                        } else if (parts.length == 3) {
                            // Nouvelle version avec paramètres
                            String paramsStr = parts[1];
                            String body = parts[2];
                            
                            List<String> params = new ArrayList<>();
                            if (!paramsStr.isEmpty()) {
                                String[] paramArray = paramsStr.split(",");
                                for (String param : paramArray) {
                                    params.add(param.trim());
                                }
                            }
                            
                            FunctionDefinition functionDef = new FunctionDefinition(params, body);
                            functions.put(functionName, functionDef);
                        }
                    }
                } catch (Exception e) {
                    // Ignorer les lignes malformées
                    System.err.println("Erreur lors du chargement d'une fonction: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (!functions.isEmpty()) {
            playerFunctions.put(playerId, functions);
        }
    }
    
    /**
     * Sauvegarde les fonctions d'un joueur dans un fichier
     * @param playerId UUID du joueur
     */
    public static void saveFunctionsToFile(UUID playerId) {
        Map<String, FunctionDefinition> functions = playerFunctions.get(playerId);
        if (functions == null || functions.isEmpty()) {
            return;  // Pas de fonctions à sauvegarder
        }
        
        File playerFile = new File(FUNCTIONS_DIR + playerId.toString() + ".dat");
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(playerFile))) {
            for (Map.Entry<String, FunctionDefinition> entry : functions.entrySet()) {
                // Format: nomFonction=param1,param2,...=instructions
                writer.write(entry.getKey() + "=");
                
                // Écrire les paramètres
                FunctionDefinition def = entry.getValue();
                List<String> params = def.getParameters();
                for (int i = 0; i < params.size(); i++) {
                    if (i > 0) {
                        writer.write(",");
                    }
                    writer.write(params.get(i));
                }
                
                // Écrire le corps de la fonction
                writer.write("=" + def.getBody());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Supprime le fichier de fonctions d'un joueur
     * @param playerId UUID du joueur
     */
    public static void deletePlayerFunctionsFile(UUID playerId) {
        File playerFile = new File(FUNCTIONS_DIR + playerId.toString() + ".dat");
        if (playerFile.exists()) {
            playerFile.delete();
        }
    }
    
    /**
     * Efface les fonctions d'un joueur de la mémoire
     * @param playerId UUID du joueur
     */
    public static void clearPlayerFunctions(UUID playerId) {
        playerFunctions.remove(playerId);
    }
    
    /**
     * Récupère la définition d'une fonction pour un joueur donné
     * @param playerId UUID du joueur
     * @param functionName Nom de la fonction
     * @return La définition de la fonction ou null si non trouvée
     */
    public static FunctionDefinition getFunctionDefinition(UUID playerId, String functionName) {
        Map<String, FunctionDefinition> functions = playerFunctions.get(playerId);
        if (functions == null) {
            return null;
        }
        return functions.get(functionName);
    }
    
    /**
     * Parse une chaîne d'instructions séparées par des espaces en tenant compte des appels de fonction
     * @param instructionsStr La chaîne d'instructions à parser
     * @return Un tableau de commandes individuelles
     */
    private String[] parseCommandsFromString(String instructionsStr) {
        List<String> commands = new ArrayList<>();
        String[] words = instructionsStr.trim().split("\\s+");
        
        int i = 0;
        while (i < words.length) {
            String word = words[i];
            
            // Si c'est une commande de mouvement standard avec un paramètre
            if (word.matches("(avance|recule|tourne|monte|descends)") && i + 1 < words.length) {
                commands.add(word + " " + words[i + 1]);
                i += 2;
            }
            // Si c'est une commande trace
            else if (word.equals("trace") && i + 1 < words.length) {
                if (i + 2 < words.length && words[i + 1].equals("on")) {
                    // trace on couleur
                    commands.add(word + " " + words[i + 1] + " " + words[i + 2]);
                    i += 3;
                } else {
                    // trace off
                    commands.add(word + " " + words[i + 1]);
                    i += 2;
                }
            }
            // Si c'est un appel de fonction
            else if (word.equals("read") && i + 1 < words.length) {
                StringBuilder functionCall = new StringBuilder();
                functionCall.append(word).append(" ");
                i++;
                
                // Ajouter le nom de la fonction et ses paramètres
                while (i < words.length) {
                    functionCall.append(words[i]);
                    if (words[i].contains(")")) {
                        // Fin de l'appel de fonction
                        i++;
                        break;
                    }
                    if (i + 1 < words.length) {
                        functionCall.append(" ");
                    }
                    i++;
                }
                commands.add(functionCall.toString());
            }
            // Autres commandes simples
            else {
                commands.add(word);
                i++;
            }
        }
        
        return commands.toArray(new String[0]);
    }
}