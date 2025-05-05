package com.example.robotplugin.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.entity.Player;
import com.example.robotplugin.robot.RobotBlock;

import java.util.Collection;
import java.util.UUID;

public class RobotBreakListener implements Listener {
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.MAGENTA_GLAZED_TERRACOTTA) {
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();
            Location blockLoc = event.getBlock().getLocation();
            
            // Recherche du robot dans la map des joueurs
            if (RobotPlaceListener.playerRobots.containsKey(playerId)) {
                RobotBlock robot = RobotPlaceListener.playerRobots.get(playerId);
                if (robot.getLocation().equals(blockLoc)) {
                    // Utilise la méthode remove pour supprimer le bloc et l'ArmorStand
                    robot.remove();
                    RobotPlaceListener.playerRobots.remove(playerId);
                    player.sendMessage("§cVotre robot a été détruit. Posez-en un nouveau pour le contrôler.");
                }
            }
            
            // Nettoyage supplémentaire: supprimer tous les ArmorStands proches du bloc détruit
            // même s'ils n'étaient pas détectés par la méthode standard
            World world = blockLoc.getWorld();
            if (world != null) {
                // Recherche d'ArmorStands dans un rayon de 1 bloc
                Collection<Entity> nearbyEntities = world.getNearbyEntities(
                    blockLoc.clone().add(0.5, 1.0, 0.5), 1.0, 1.0, 1.0, 
                    entity -> entity instanceof ArmorStand
                );
                
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof ArmorStand) {
                        ArmorStand stand = (ArmorStand) entity;
                        if (stand.isCustomNameVisible() && stand.getCustomName() != null && 
                            stand.getCustomName().contains("Robot de")) {
                            stand.remove();
                        }
                    }
                }
            }
        }
    }
}
