package dev.mattrm.mc.survivalgames;

import dev.mattrm.mc.gametools.Service;
import dev.mattrm.mc.gametools.data.DataService;
import dev.mattrm.mc.gametools.readyup.ReadyUpService;
import dev.mattrm.mc.gametools.scoreboards.ScoreboardService;
import dev.mattrm.mc.gametools.settings.GameSettingsService;
import dev.mattrm.mc.gametools.teams.TeamService;
import dev.mattrm.mc.gametools.util.ActionBarUtils;
import dev.mattrm.mc.survivalgames.commands.CommandRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.dependency.Dependency;
import org.bukkit.plugin.java.annotation.dependency.SoftDependency;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.author.Author;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Plugin(name = "SurvivalGames", version = "1.0")
@Author("ZekNikZ")
@SoftDependency("GameToolsLibrary")
@Dependency("WorldEdit")
@Dependency("WorldGuard")
public final class SurvivalGames extends JavaPlugin {
    @Override
    public void onEnable() {
        // Plugin startup logic

        Service[] services = new Service[] {
            GameManager.getInstance(),
            ChestContent.getInstance()
        };

        PluginManager pluginManager = this.getServer().getPluginManager();
        for (Service service : services) {
            service.setup(this);
            pluginManager.registerEvents(service, this);
        }

        // Setup data service
        DataService.getInstance().registerDataManager(GameManager.getInstance());
        DataService.getInstance().registerDataManager(ChestContent.getInstance());

        DataService.getInstance().loadAll();

        new CommandRegistry().registerCommands(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
