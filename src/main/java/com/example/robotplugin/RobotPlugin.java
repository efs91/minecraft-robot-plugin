package com.example.robotplugin;

import org.bukkit.plugin.java.JavaPlugin;
import com.example.robotplugin.commands.RobotCommand;
import com.example.robotplugin.commands.RepeteCommand;
import com.example.robotplugin.commands.TraceCommand;
import com.example.robotplugin.commands.MonteCommand;
import com.example.robotplugin.commands.DescendsCommand;
import com.example.robotplugin.commands.RobotStopCommand;
import com.example.robotplugin.listeners.PlayerJoinListener;
import com.example.robotplugin.listeners.RobotPlaceListener;
import com.example.robotplugin.listeners.RobotBreakListener;
import com.example.robotplugin.listeners.PlayerQuitListener;

public class RobotPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("RobotPlugin has been enabled!");
        
        // Enregistrement des commandes
        getCommand("avance").setExecutor(new RobotCommand(this));
        getCommand("recule").setExecutor(new RobotCommand(this));
        getCommand("tourne").setExecutor(new RobotCommand(this));
        getCommand("monte").setExecutor(new MonteCommand(this));
        getCommand("descends").setExecutor(new DescendsCommand(this));
        getCommand("repete").setExecutor(new RepeteCommand(this));
        getCommand("trace").setExecutor(new TraceCommand(this));
        getCommand("robot").setExecutor(new RobotStopCommand(this));
        
        // Enregistrement des listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new RobotPlaceListener(), this);
        getServer().getPluginManager().registerEvents(new RobotBreakListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("RobotPlugin has been disabled!");
    }
}