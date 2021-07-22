package dev.mattrm.mc.survivalgames.commands;

import dev.mattrm.mc.survivalgames.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.annotation.command.Commands;

@Commands(@org.bukkit.plugin.java.annotation.command.Command(
    name = "startgame",
    desc = "Starts the game",
    usage = "/startgame"
))
public class StartGameCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        GameManager.getInstance().startGame();
        return true;
    }
}
