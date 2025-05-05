package com.example.robotplugin.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerJoinListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Créer un bloc de terre cuite émaillée magenta
        ItemStack robotBlock = new ItemStack(Material.MAGENTA_GLAZED_TERRACOTTA, 1);
        ItemMeta meta = robotBlock.getItemMeta();
        meta.setDisplayName("§dRobot");
        robotBlock.setItemMeta(meta);
        
        // Donner le bloc au joueur
        event.getPlayer().getInventory().addItem(robotBlock);
        
        // Message de bienvenue complet avec toutes les fonctionnalités
        event.getPlayer().sendMessage("§a§l=== Votre Robot Minecraft ===");
        event.getPlayer().sendMessage("§aVous avez reçu un robot ! Posez le bloc magenta pour créer votre robot.");
        event.getPlayer().sendMessage("§aCommandes disponibles :");
        event.getPlayer().sendMessage("§e/avance <nombre> §7- Avance le robot");
        event.getPlayer().sendMessage("§e/recule <nombre> §7- Recule le robot");
        event.getPlayer().sendMessage("§e/tourne <degrés> §7- Tourne le robot (45° ou 90° recommandés)");
        event.getPlayer().sendMessage("§e/monte <nombre> §7- Monte le robot verticalement");
        event.getPlayer().sendMessage("§e/descends <nombre> §7- Descend le robot verticalement");
        event.getPlayer().sendMessage("§e/trace on <couleur> §7- Active le tracé en laine colorée");
        event.getPlayer().sendMessage("§e/trace off §7- Désactive le tracé");
        event.getPlayer().sendMessage("§e/repete <nombre> (instructions) §7- Répète une séquence d'instructions");
        event.getPlayer().sendMessage("§e/robot stop §7- Interrompt l'exécution d'une séquence de commandes");
    }
}