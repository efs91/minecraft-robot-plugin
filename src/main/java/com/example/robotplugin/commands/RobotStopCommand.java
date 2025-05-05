package com.example.robotplugin.commands;

import com.example.robotplugin.RobotPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RobotStopCommand implements CommandExecutor {
    private final RobotPlugin plugin;
    
    public RobotStopCommand(RobotPlugin plugin) {
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
        
        // Vérifier si une tâche de répétition est en cours pour ce joueur
        boolean taskFound = RepeteCommand.stopRunningTask(playerId);
        
        if (taskFound) {
            player.sendMessage("§aExécution des commandes interrompue avec succès.");
        } else {
            player.sendMessage("§eAucune séquence d'instructions en cours d'exécution.");
        }
        
        return true;
    }
}