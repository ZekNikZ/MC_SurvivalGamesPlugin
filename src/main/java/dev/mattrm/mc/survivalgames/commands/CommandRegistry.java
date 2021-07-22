package dev.mattrm.mc.survivalgames.commands;

import dev.mattrm.mc.gametools.CommandGroup;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.command.Commands;

public class CommandRegistry implements CommandGroup {
    @Override
    public void registerCommands(JavaPlugin plugin) {
        plugin.getCommand("setmap").setExecutor(new SetMapCommand());
        plugin.getCommand("reloadmapdata").setExecutor(new ReloadMapDataCommand());
        plugin.getCommand("startgame").setExecutor(new StartGameCommand());
        plugin.getCommand("startdeathmatch").setExecutor(new StartDeathmatchCommand());
    }
}
