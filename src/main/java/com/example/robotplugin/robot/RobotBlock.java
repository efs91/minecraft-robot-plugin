package com.example.robotplugin.robot;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class RobotBlock {
    private Location location;
    private Player owner;
    private float yaw; // Direction en degrés (0-360)
    private ArmorStand nameTag; // Pour afficher le nom du joueur au-dessus du robot
    
    // Propriétés pour le tracé
    private boolean traceEnabled = false;
    private DyeColor traceColor = DyeColor.WHITE;

    // Constructeur existant - crée un robot à la position du joueur
    public RobotBlock(Player owner) {
        this.owner = owner;
        this.location = owner.getLocation().clone();
        this.yaw = owner.getLocation().getYaw();
        spawnRobot();
    }
    
    // Nouveau constructeur - crée un robot à une position spécifique
    public RobotBlock(Location location, Player owner) {
        this.location = location.clone();
        this.owner = owner;
        this.yaw = owner.getLocation().getYaw(); // Utilise l'orientation du joueur pour le robot
        spawnRobot();
    }

    private void spawnRobot() {
        // Supprime l'ancien bloc si nécessaire
        if (location.getBlock().getType() == Material.MAGENTA_GLAZED_TERRACOTTA) {
            location.getBlock().setType(Material.AIR, false); // false = ignorer la physique, pas besoin de vérifier les permissions
        }

        // Supprime l'ancien ArmorStand s'il existe
        if (nameTag != null && !nameTag.isDead()) {
            nameTag.remove();
        }

        // Place le bloc robot en contournant les vérifications de permissions
        Block block = location.getBlock();
        BlockState oldState = block.getState();
        block.setType(Material.MAGENTA_GLAZED_TERRACOTTA, false); // false = ignorer la physique
        
        // Appliquer la direction au bloc (les terres cuites émaillées sont directionnelles)
        setBlockDirection(block);
        
        // Crée un ArmorStand pour afficher le nom du joueur
        Location tagLocation = location.clone().add(0.5, 1.0, 0.5);
        nameTag = (ArmorStand) location.getWorld().spawnEntity(tagLocation, EntityType.ARMOR_STAND);
        nameTag.setCustomName("§bRobot de §e" + owner.getName());
        nameTag.setCustomNameVisible(true);
        nameTag.setVisible(false);
        nameTag.setSmall(true);
        nameTag.setGravity(false);
        nameTag.setMarker(true);
        nameTag.setInvulnerable(true);
    }

    public void moveForward(int distance) {
        // Sauvegardons la position de départ pour y placer une laine plus tard
        Location startPos = location.clone();
        
        // Supprime l'ancien bloc et le nametag
        location.getBlock().setType(Material.AIR, false);
        if (nameTag != null && !nameTag.isDead()) {
            nameTag.remove();
        }
        
        // Arrondir le yaw à un multiple de 45° le plus proche
        int roundedYaw = Math.round(yaw / 45) * 45;
        if (roundedYaw >= 360) roundedYaw -= 360;
        
        // On s'assure que distance est toujours positif pour avancer
        int steps = Math.abs(distance);
        
        // Si le tracé est activé et que le robot se déplace effectivement,
        // placer une laine au point de départ
        if (traceEnabled && steps > 0) {
            // Nous plaçons de la laine au point de départ
            Block startBlock = startPos.getBlock();
            placeLaine(startBlock);
        }
        
        // Déplacer le bloc selon la direction arrondie
        for (int i = 0; i < steps; i++) {
            int dx = 0;
            int dz = 0;
            
            // Déterminer le déplacement en fonction de l'orientation arrondie
            switch (roundedYaw) {
                case 0: // Sud
                    dz = 1;
                    break;
                case 45: // Sud-Ouest
                    dz = 1;
                    dx = -1;
                    break;
                case 90: // Ouest
                    dx = -1;
                    break;
                case 135: // Nord-Ouest
                    dz = -1;
                    dx = -1;
                    break;
                case 180: // Nord
                    dz = -1;
                    break;
                case 225: // Nord-Est
                    dz = -1;
                    dx = 1;
                    break;
                case 270: // Est
                    dx = 1;
                    break;
                case 315: // Sud-Est
                    dz = 1;
                    dx = 1;
                    break;
            }
            
            // Appliquer le déplacement
            location.add(dx, 0, dz);
            
            // Laisser une trace si activé, sauf pour le dernier pas
            if (traceEnabled && i < steps - 1) {
                leaveTrace();
            }
        }
        
        // Réafficher le robot
        spawnRobot();
    }

    public void moveBackward(int distance) {
        // Sauvegardons la position de départ pour y placer une laine plus tard
        Location startPos = location.clone();
        
        // Supprime l'ancien bloc et le nametag
        location.getBlock().setType(Material.AIR, false);
        if (nameTag != null && !nameTag.isDead()) {
            nameTag.remove();
        }
        
        // Arrondir le yaw à un multiple de 45° le plus proche
        int roundedYaw = Math.round(yaw / 45) * 45;
        if (roundedYaw >= 360) roundedYaw -= 360;
        
        // On s'assure que distance est toujours positif pour reculer
        int steps = Math.abs(distance);
        
        // Si le tracé est activé et que le robot se déplace effectivement,
        // placer une laine au point de départ
        if (traceEnabled && steps > 0) {
            // Nous plaçons de la laine au point de départ
            Block startBlock = startPos.getBlock();
            placeLaine(startBlock);
        }
        
        // Déplacer le bloc selon la direction opposée
        for (int i = 0; i < steps; i++) {
            int dx = 0;
            int dz = 0;
            
            // Déterminer le déplacement en fonction de l'orientation arrondie (inverse de moveForward)
            switch (roundedYaw) {
                case 0: // Sud → Nord
                    dz = -1;
                    break;
                case 45: // Sud-Ouest → Nord-Est
                    dz = -1;
                    dx = 1;
                    break;
                case 90: // Ouest → Est
                    dx = 1;
                    break;
                case 135: // Nord-Ouest → Sud-Est
                    dz = 1;
                    dx = 1;
                    break;
                case 180: // Nord → Sud
                    dz = 1;
                    break;
                case 225: // Nord-Est → Sud-Ouest
                    dz = 1;
                    dx = -1;
                    break;
                case 270: // Est → Ouest
                    dx = -1;
                    break;
                case 315: // Sud-Est → Nord-Ouest
                    dz = -1;
                    dx = -1;
                    break;
            }
            
            // Appliquer le déplacement
            location.add(dx, 0, dz);
            
            // Laisser une trace si activé, sauf pour le dernier pas
            if (traceEnabled && i < steps - 1) {
                leaveTrace();
            }
        }
        
        // Réafficher le robot
        spawnRobot();
    }
    
    public void rotate(int degrees) {
        // Supprime l'ancien bloc
        location.getBlock().setType(Material.AIR, false); // false = ignorer la physique, pas besoin de vérifier les permissions
        
        // Mettre à jour la direction (yaw)
        this.yaw = (this.yaw + degrees) % 360;
        if (this.yaw < 0) {
            this.yaw += 360;
        }
        
        // Recréer le bloc avec la nouvelle orientation
        spawnRobot();
    }

    /**
     * Fait monter le robot d'un certain nombre de blocs
     * @param blocks Nombre de blocs à monter
     */
    public void moveUp(int blocks) {
        // Sauvegardons la position de départ pour y placer une laine plus tard
        Location startPos = location.clone();
        
        // Supprime l'ancien bloc et le nametag
        location.getBlock().setType(Material.AIR, false);
        if (nameTag != null && !nameTag.isDead()) {
            nameTag.remove();
        }
        
        // Si le tracé est activé et que le robot se déplace effectivement,
        // placer une laine au point de départ
        if (traceEnabled && blocks > 0) {
            Block startBlock = startPos.getBlock();
            placeLaine(startBlock);
        }
        
        // Déplacer le bloc vers le haut
        for (int i = 0; i < blocks; i++) {
            // Monter d'un bloc
            location.add(0, 1, 0);
            
            // Laisser une trace si activé, sauf pour le dernier pas
            if (traceEnabled && i < blocks - 1) {
                leaveTrace();
            }
        }
        
        // Réafficher le robot
        spawnRobot();
    }
    
    /**
     * Fait descendre le robot d'un certain nombre de blocs, sans aller sous Y=-60
     * @param blocks Nombre de blocs à descendre
     * @return Vrai si le mouvement a été complètement effectué, faux s'il a été partiellement bloqué
     */
    public boolean moveDown(int blocks) {
        // Sauvegardons la position de départ pour y placer une laine plus tard
        Location startPos = location.clone();
        
        // Supprime l'ancien bloc et le nametag
        location.getBlock().setType(Material.AIR, false);
        if (nameTag != null && !nameTag.isDead()) {
            nameTag.remove();
        }
        
        // Vérifier si on peut descendre sans aller sous Y=-60
        int actualBlocksToMove = Math.min(blocks, (int)(location.getY() + blocks) - (-60));
        if (actualBlocksToMove <= 0) {
            // Impossible de descendre, on replace le robot à sa position
            spawnRobot();
            return false;
        }
        
        // Si le tracé est activé et que le robot se déplace effectivement,
        // placer une laine au point de départ
        if (traceEnabled && actualBlocksToMove > 0) {
            Block startBlock = startPos.getBlock();
            placeLaine(startBlock);
        }
        
        // Déplacer le bloc vers le bas
        for (int i = 0; i < actualBlocksToMove; i++) {
            // Descendre d'un bloc
            location.add(0, -1, 0);
            
            // Laisser une trace si activé, sauf pour le dernier pas
            if (traceEnabled && i < actualBlocksToMove - 1) {
                leaveTrace();
            }
            
            // Vérifier à nouveau qu'on n'est pas descendu trop bas
            if (location.getY() <= -60) {
                location.setY(-60);
                break;
            }
        }
        
        // Réafficher le robot
        spawnRobot();
        
        // Renvoyer vrai si on a pu descendre du nombre de blocs demandé
        return actualBlocksToMove == blocks;
    }

    // Méthode pour supprimer le robot et son nametag
    public void remove() {
        if (location.getBlock().getType() == Material.MAGENTA_GLAZED_TERRACOTTA) {
            location.getBlock().setType(Material.AIR, false); // false = ignorer la physique, pas besoin de vérifier les permissions
        }
        
        if (nameTag != null && !nameTag.isDead()) {
            nameTag.remove();
        }
    }

    private void setBlockDirection(Block block) {
        if (block.getBlockData() instanceof Directional) {
            Directional directional = (Directional) block.getBlockData();
            
            // Convertir yaw en BlockFace
            // Les blocs dans Minecraft n'ont que 4 orientations (NORTH, EAST, SOUTH, WEST)
            // On arrondit à l'orientation la plus proche
            BlockFace face;
            
            if (yaw >= 315 || yaw < 45) {
                face = BlockFace.SOUTH; // 0 degrés = Sud
            } else if (yaw >= 45 && yaw < 135) {
                face = BlockFace.WEST; // 90 degrés = Ouest
            } else if (yaw >= 135 && yaw < 225) {
                face = BlockFace.NORTH; // 180 degrés = Nord
            } else { // yaw >= 225 && yaw < 315
                face = BlockFace.EAST; // 270 degrés = Est
            }
            
            directional.setFacing(face);
            block.setBlockData(directional);
        }
    }

    public Location getLocation() {
        return location;
    }

    public Player getOwner() {
        return owner;
    }
    
    // Nouvelles méthodes pour gérer le tracé
    public void setTraceEnabled(boolean enabled) {
        this.traceEnabled = enabled;
    }
    
    public void setTraceColor(DyeColor color) {
        this.traceColor = color;
    }
    
    public boolean isTraceEnabled() {
        return traceEnabled;
    }
    
    public DyeColor getTraceColor() {
        return traceColor;
    }
    
    // Méthode pour laisser une trace de laine à l'emplacement actuel
    private void leaveTrace() {
        if (!traceEnabled) return;
        
        // Ne pas laisser de trace s'il y a déjà un bloc à cet endroit
        Block block = location.getBlock();
        placeLaine(block);
    }

    // Méthode pour placer un bloc de laine si possible
    private void placeLaine(Block block) {
        if (!traceEnabled) return;
        
        // Ne pas laisser de trace s'il y a déjà un bloc à cet endroit (sauf air ou le robot)
        if (block.getType() != Material.AIR && block.getType() != Material.MAGENTA_GLAZED_TERRACOTTA) {
            return;
        }
        
        // Convertir la couleur DyeColor en type de laine
        Material woolMaterial;
        
        switch (traceColor) {
            case WHITE: woolMaterial = Material.WHITE_WOOL; break;
            case ORANGE: woolMaterial = Material.ORANGE_WOOL; break;
            case MAGENTA: woolMaterial = Material.MAGENTA_WOOL; break;
            case LIGHT_BLUE: woolMaterial = Material.LIGHT_BLUE_WOOL; break;
            case YELLOW: woolMaterial = Material.YELLOW_WOOL; break;
            case LIME: woolMaterial = Material.LIME_WOOL; break;
            case PINK: woolMaterial = Material.PINK_WOOL; break;
            case GRAY: woolMaterial = Material.GRAY_WOOL; break;
            case LIGHT_GRAY: woolMaterial = Material.LIGHT_GRAY_WOOL; break;
            case CYAN: woolMaterial = Material.CYAN_WOOL; break;
            case PURPLE: woolMaterial = Material.PURPLE_WOOL; break;
            case BLUE: woolMaterial = Material.BLUE_WOOL; break;
            case BROWN: woolMaterial = Material.BROWN_WOOL; break;
            case GREEN: woolMaterial = Material.GREEN_WOOL; break;
            case RED: woolMaterial = Material.RED_WOOL; break;
            case BLACK: woolMaterial = Material.BLACK_WOOL; break;
            default: woolMaterial = Material.WHITE_WOOL;
        }
        
        block.setType(woolMaterial, false);
    }
}