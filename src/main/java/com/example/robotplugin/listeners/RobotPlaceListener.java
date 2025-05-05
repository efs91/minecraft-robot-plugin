package com.example.robotplugin.listeners;

import com.example.robotplugin.robot.RobotBlock;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class RobotPlaceListener implements Listener {
    // Map statique pour stocker la position du robot de chaque joueur
    public static final Map<UUID, RobotBlock> playerRobots = new HashMap<>();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (event.getBlockPlaced().getType() == Material.MAGENTA_GLAZED_TERRACOTTA) {
            UUID playerId = player.getUniqueId();
            
            // Vérifier si le joueur a déjà un robot actif
            if (playerRobots.containsKey(playerId)) {
                // Supprimer l'ancien robot
                RobotBlock oldRobot = playerRobots.get(playerId);
                oldRobot.remove();
                player.sendMessage("§eVotre ancien robot a été détruit.");
            }
            
            // Enregistrer le nouveau robot
            RobotBlock robot = new RobotBlock(event.getBlockPlaced().getLocation(), player);
            playerRobots.put(playerId, robot);
            
            // Message complet avec toutes les commandes disponibles
            player.sendMessage("§a§l=== Robot Activé ===");
            player.sendMessage("§aRobot placé ! Utilisez les commandes suivantes pour le contrôler :");
            player.sendMessage("§e• /avance <nombre> §7- Déplace le robot vers l'avant");
            player.sendMessage("§e• /recule <nombre> §7- Déplace le robot vers l'arrière");
            player.sendMessage("§e• /tourne <degrés> §7- Fait pivoter le robot");
            player.sendMessage("§e• /monte <nombre> §7- Fait monter le robot verticalement");
            player.sendMessage("§e• /descends <nombre> §7- Fait descendre le robot verticalement");
            player.sendMessage("§e• /trace on <couleur> §7- Laisse une trace en laine colorée");
            player.sendMessage("§e• /trace off §7- Arrête de laisser une trace");
            player.sendMessage("§e• /repete <n> (instructions) §7- Ex: /repete 4 (avance 5 tourne 90)");
            player.sendMessage("§e• /robot stop §7- Interrompt l'exécution d'une séquence");
        }
    }
}
