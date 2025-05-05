package com.example.robotplugin.commands;

import com.example.robotplugin.RobotPlugin;
import com.example.robotplugin.robot.RobotBlock;
import com.example.robotplugin.listeners.RobotPlaceListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RobotCommand implements CommandExecutor {
    private final RobotPlugin plugin;
    
    public RobotCommand(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can control the robot.");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        
        if (args.length < 1) {
            player.sendMessage("§cUtilisation: /" + label + " <valeur>");
            return true;
        }
        
        // Récupérer le robot placé par le joueur
        RobotBlock robot = RobotPlaceListener.playerRobots.get(playerId);
        if (robot == null) {
            player.sendMessage("§cVous devez d'abord poser votre robot (bloc magenta) avant de le contrôler.");
            return true;
        }
        
        int value;
        try {
            value = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cLa valeur doit être un nombre.");
            return true;
        }
        
        switch (label.toLowerCase()) {
            case "avance":
                robot.moveForward(value);
                player.sendMessage("§aRobot avancé de " + value + " blocs.");
                break;
            case "recule":
                robot.moveBackward(value);
                player.sendMessage("§aRobot reculé de " + value + " blocs.");
                break;
            case "tourne":
                robot.rotate(value);
                player.sendMessage("§aRobot tourné de " + value + " degrés.");
                break;
            default:
                player.sendMessage("§cCommande inconnue.");
                break;
        }
        
        return true;
    }
}