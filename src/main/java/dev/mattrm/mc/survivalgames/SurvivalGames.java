package dev.mattrm.mc.survivalgames;

import dev.mattrm.mc.gametools.util.ActionBarUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.dependency.Dependency;
import org.bukkit.plugin.java.annotation.dependency.SoftDependency;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.author.Author;

import java.util.HashSet;
import java.util.Set;

@Plugin(name = "SurvivalGames", version = "1.0")
@Author("ZekNikZ")
@SoftDependency("GameToolsLibrary")
@Dependency("WorldEdit")
@Dependency("WorldGuard")
public final class SurvivalGames extends JavaPlugin {
    @Override
    public void onEnable() {
        // Plugin startup logic
        Set<Material> transparentBlocks = new HashSet<>();
        transparentBlocks.add(Material.AIR);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                String str;
                Block block;
                if ((block = player.getTargetBlock(transparentBlocks, 100)) != null && block.getType() != Material.AIR) {
                    Location loc = block.getLocation();
                    str = loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " (" + block.getType() + ")";
                } else {
                    str = "N/A";
                }

                ActionBarUtils.get().sendActionBarMessage(player, ChatColor.GOLD + "Looking at: " + str);
            });
        }, 0, 1);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
