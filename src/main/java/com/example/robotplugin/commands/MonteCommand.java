package com.example.robotplugin.commands;

import com.example.robotplugin.RobotPlugin;
import com.example.robotplugin.listeners.RobotPlaceListener;
import com.example.robotplugin.robot.RobotBlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MonteCommand implements CommandExecutor {
    private final RobotPlugin plugin;
    
    public MonteCommand(RobotPlugin plugin) {
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
        
        // Vérifier si le joueur a un robot
        RobotBlock robot = RobotPlaceListener.playerRobots.get(playerId);
        if (robot == null) {
            player.sendMessage("§cVous devez d'abord placer votre robot pour l'utiliser.");
            return true;
        }
        
        // Vérifier si un nombre de blocs est spécifié
        if (args.length < 1) {
            player.sendMessage("§cUtilisation: /monte <nombre>");
            return true;
        }
        
        // Essayer de convertir l'argument en nombre
        int blocks;
        try {
            blocks = Integer.parseInt(args[0]);
            if (blocks <= 0) {
                player.sendMessage("§cLe nombre de blocs doit être positif.");
                return true;
            }
            if (blocks > 100) {
                player.sendMessage("§cVous ne pouvez pas monter de plus de 100 blocs à la fois.");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cVeuillez spécifier un nombre valide.");
            return true;
        }
        
        // Faire monter le robot
        robot.moveUp(blocks);
        player.sendMessage("§aRobot monté de " + blocks + " blocs.");
        
        return true;
    }
}