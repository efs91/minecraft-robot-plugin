package com.example.robotplugin.listeners;

import com.example.robotplugin.robot.RobotBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import java.util.UUID;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Vérifie si le joueur avait un robot
        if (RobotPlaceListener.playerRobots.containsKey(playerId)) {
            // Récupère et supprime le robot
            RobotBlock robot = RobotPlaceListener.playerRobots.get(playerId);
            robot.remove(); // Supprime le bloc et le nom flottant
            
            // Retire l'entrée de la map
            RobotPlaceListener.playerRobots.remove(playerId);
        }
        
        // Vider l'inventaire du joueur
        player.getInventory().clear();
    }
}