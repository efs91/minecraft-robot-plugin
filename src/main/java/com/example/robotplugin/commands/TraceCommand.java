package com.example.robotplugin.commands;

import com.example.robotplugin.RobotPlugin;
import com.example.robotplugin.listeners.RobotPlaceListener;
import com.example.robotplugin.robot.RobotBlock;
import org.bukkit.DyeColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TraceCommand implements CommandExecutor {
    private final RobotPlugin plugin;

    public TraceCommand(RobotPlugin plugin) {
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
            player.sendMessage("§cVous devez d'abord placer votre robot pour utiliser cette commande.");
            return true;
        }

        // Activer ou désactiver le tracé
        if (args.length == 0 || (args.length >= 1 && args[0].equalsIgnoreCase("off"))) {
            // Désactiver le tracé
            robot.setTraceEnabled(false);
            player.sendMessage("§aTracé désactivé.");
            return true;
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("on")) {
            // Activer le tracé
            DyeColor color = DyeColor.WHITE; // Couleur par défaut

            // Si une couleur est spécifiée
            if (args.length >= 2) {
                try {
                    color = getColorFromString(args[1]);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cCouleur invalide. Couleurs disponibles : blanc, noir, rouge, vert, bleu, cyan, gris, rose, jaune, violet, orange, etc.");
                    return true;
                }
            }

            robot.setTraceEnabled(true);
            robot.setTraceColor(color);
            player.sendMessage("§aTracé activé avec la couleur §" + getColorCode(color) + args[1] + "§a.");
            return true;
        } else {
            player.sendMessage("§cUtilisation: /trace <on|off> [couleur]");
            return true;
        }
    }

    /**
     * Convertit une chaîne en couleur
     */
    public static DyeColor getColorFromString(String colorName) {
        switch (colorName.toLowerCase()) {
            case "blanc": return DyeColor.WHITE;
            case "orange": return DyeColor.ORANGE;
            case "magenta": return DyeColor.MAGENTA;
            case "bleu_clair": case "bleuclair": return DyeColor.LIGHT_BLUE;
            case "jaune": return DyeColor.YELLOW;
            case "citron": case "vert_clair": case "vertclair": return DyeColor.LIME;
            case "rose": return DyeColor.PINK;
            case "gris": return DyeColor.GRAY;
            case "gris_clair": case "grisclair": return DyeColor.LIGHT_GRAY;
            case "cyan": return DyeColor.CYAN;
            case "violet": return DyeColor.PURPLE;
            case "bleu": return DyeColor.BLUE;
            case "marron": case "brun": return DyeColor.BROWN;
            case "vert": return DyeColor.GREEN;
            case "rouge": return DyeColor.RED;
            case "noir": return DyeColor.BLACK;
            default:
                try {
                    return DyeColor.valueOf(colorName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Couleur inconnue: " + colorName);
                }
        }
    }

    /**
     * Obtient le code de couleur pour l'affichage dans le chat
     */
    private String getColorCode(DyeColor color) {
        switch (color) {
            case WHITE: return "f";
            case ORANGE: return "6";
            case MAGENTA: return "d";
            case LIGHT_BLUE: return "b";
            case YELLOW: return "e";
            case LIME: return "a";
            case PINK: return "d";
            case GRAY: return "8";
            case LIGHT_GRAY: return "7";
            case CYAN: return "3";
            case PURPLE: return "5";
            case BLUE: return "9";
            case BROWN: return "6";
            case GREEN: return "2";
            case RED: return "c";
            case BLACK: return "0";
            default: return "f";
        }
    }
}