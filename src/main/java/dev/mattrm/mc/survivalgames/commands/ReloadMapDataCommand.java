package dev.mattrm.mc.survivalgames.commands;

import dev.mattrm.mc.survivalgames.ChestContent;
import dev.mattrm.mc.survivalgames.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.annotation.command.Commands;

@Commands(@org.bukkit.plugin.java.annotation.command.Command(
    name = "reloadmapdata",
    desc = "Reloads the map data",
    usage = "/reloadmapdata"
))
public class ReloadMapDataCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        GameManager.getInstance().loadData();
        ChestContent.getInstance().loadData();

        sender.sendMessage(ChatColor.GRAY + "Reload successful");

        return true;
    }
}
