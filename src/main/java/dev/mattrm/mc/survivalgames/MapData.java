package dev.mattrm.mc.survivalgames;

import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapData {
    private final Set<BlockVector> tier2Chests = new HashSet<>();
    private final List<BlockVector> spawnLocations = new ArrayList<>();
    private BlockVector cornMin;
    private BlockVector cornMax;

    public void addTier2Chest(BlockVector vec) {
        this.tier2Chests.add(vec);
    }

    public void addSpawnLocation(BlockVector vec) {
        this.spawnLocations.add(vec);
    }

    public boolean isTier2Chest(BlockVector vec) {
        return this.tier2Chests.contains(vec);
    }

    public void setCornMin(BlockVector cornMin) {
        this.cornMin = cornMin;
    }

    public void setCornMax(BlockVector cornMax) {
        this.cornMax = cornMax;
    }

    public BlockVector getCornMin() {
        return this.cornMin;
    }

    public BlockVector getCornMax() {
        return this.cornMax;
    }

    public Set<BlockVector> getTier2Chests() {
        return this.tier2Chests;
    }

    public List<BlockVector> getSpawnLocations() {
        return this.spawnLocations;
    }
}
