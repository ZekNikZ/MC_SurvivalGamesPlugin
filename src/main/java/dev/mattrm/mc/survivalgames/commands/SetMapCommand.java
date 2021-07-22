package dev.mattrm.mc.survivalgames.commands;

import dev.mattrm.mc.survivalgames.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.annotation.command.Commands;

@Commands(@org.bukkit.plugin.java.annotation.command.Command(
    name = "setmap",
    desc = "Sets the map data to use",
    usage = "/setmap <map>"
))
public class SetMapCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return false;
        }

        GameManager.getInstance().setMap(args[0]);

        return true;
    }
}
