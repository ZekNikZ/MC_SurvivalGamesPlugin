package dev.mattrm.mc.survivalgames;

import dev.mattrm.mc.gametools.Service;
import dev.mattrm.mc.gametools.data.IDataManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ChestContent extends Service implements IDataManager {
    private static final ChestContent INSTANCE = new ChestContent();

    public static ChestContent getInstance() {
        return INSTANCE;
    }

    public List<Material> tier1Materials = new ArrayList<>();
    public List<Material> tier2Materials = new ArrayList<>();

    @Override
    public String getDataFileName() {
        return "chests";
    }

    @Override
    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public void onLoad(ConfigurationSection configurationSection) {
        List<String> tier1Loot = configurationSection.getStringList("tier1");
        this.tier1Materials =
            (tier1Loot.stream().map(Material::getMaterial).collect(Collectors.toList()));

        List<String> tier2Loot = configurationSection.getStringList("tier2");
        this.tier2Materials =
            (tier2Loot.stream().map(Material::getMaterial).collect(Collectors.toList()));
    }

    @Override
    public void onSave(ConfigurationSection configurationSection) {
        configurationSection.set("tier1",
            this.tier1Materials.stream().map(Enum::toString).collect(Collectors.toList()));
        configurationSection.set("tier2",
            this.tier2Materials.stream().map(Enum::toString).collect(Collectors.toList()));
    }
}
