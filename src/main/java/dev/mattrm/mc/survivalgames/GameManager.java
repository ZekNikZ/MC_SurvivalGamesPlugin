package dev.mattrm.mc.survivalgames;

import dev.mattrm.mc.gametools.Service;
import dev.mattrm.mc.gametools.data.IDataManager;
import dev.mattrm.mc.gametools.data.SharedReference;
import dev.mattrm.mc.gametools.scoreboards.GameScoreboard;
import dev.mattrm.mc.gametools.scoreboards.ScoreboardService;
import dev.mattrm.mc.gametools.scoreboards.ValueEntry;
import dev.mattrm.mc.gametools.scoreboards.impl.SharedReferenceEntry;
import dev.mattrm.mc.gametools.scoreboards.impl.TimerEntry;
import dev.mattrm.mc.gametools.teams.GameTeam;
import dev.mattrm.mc.gametools.teams.TeamService;
import dev.mattrm.mc.gametools.timer.GameStopwatch;
import dev.mattrm.mc.gametools.util.ISB;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.util.BlockVector;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GameManager extends Service implements IDataManager {
    private static final GameManager INSTANCE = new GameManager();

    private final GameStopwatch timer = new GameStopwatch(this.plugin, 20);
    private final SharedReference<Integer> alivePlayerCount =
        new SharedReference<>();

    private GameTeam aliveTeam;
    private GameTeam spectatorTeam;

    private Set<BlockVector> openedChests = new HashSet<>();

    @Override
    protected void setupService() {
        TeamService.getInstance().clearTeams();
        TeamService.getInstance().newTeam("alive", "Alive", "");
        TeamService.getInstance().newTeam("spectator", "Spectator", "SPEC");
        this.aliveTeam = TeamService.getInstance().getTeam("alive");
        this.spectatorTeam = TeamService.getInstance().getTeam("spectator");
        this.spectatorTeam.setFormatCode(ChatColor.DARK_GRAY);
    }

    public static GameManager getInstance() {
        return INSTANCE;
    }

    public void setupScoreboard() {
        GameScoreboard scoreboard =
            ScoreboardService.getInstance().createNewScoreboard("" + ChatColor.YELLOW + ChatColor.BOLD + "Survival Games");
        scoreboard.addEntry(new TimerEntry(scoreboard, timer, "Time: ",
            ValueEntry.ValuePos.SUFFIX, null));
        scoreboard.addEntry(new SharedReferenceEntry<>(scoreboard,
            "Alive: ", ValueEntry.ValuePos.SUFFIX, alivePlayerCount));
    }

    private void updatePlayers() {
        int aliveCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            GameTeam team = TeamService.getInstance().getPlayerTeam(player);
            if (team == null) {
                TeamService.getInstance().joinTeam(player, aliveTeam);
            } else if (aliveTeam.equals(team)) {
                player.setGameMode(GameMode.SURVIVAL);
                ++aliveCount;
            } else if (spectatorTeam.equals(team)) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }

        this.alivePlayerCount.setAndNotify(aliveCount);
    }

    public void startGame() {

    }

    @EventHandler
    private void onOpenChest(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Chest) {
            Chest chest = (Chest) holder;
            Location loc = chest.getLocation();
            BlockVector vec = new BlockVector(loc.getBlockX(),
                loc.getBlockY(), loc.getBlockZ());
            if (!this.openedChests.contains(vec)) {
                this.setupChest(chest, vec);
            }
        }
    }

    private void setupChest(Chest chest, BlockVector vec) {
        int itemCount = Utils.getRandomNumber(4, 7);
        List<Material> mats;
        if (this.isTier2Chest(chest, vec)) {
            mats = new ArrayList<>(ChestContent.getInstance().tier2Materials);
        } else {
            mats = new ArrayList<>(ChestContent.getInstance().tier1Materials);
        }
        Collections.shuffle(mats);
        List<Material> chosenMaterials =
            mats.stream().limit(itemCount).collect(Collectors.toList());

        List<Integer> positions =
            IntStream.range(0, 27).boxed().collect(Collectors.toList());
        Collections.shuffle(positions);
        List<Integer> chosenPositions =
            positions.stream().limit(itemCount).collect(Collectors.toList());

        for (int i = 0; i < itemCount; i++) {
            chest.getInventory().setItem(chosenPositions.get(i),
                ISB.stack(chosenMaterials.get(i)));
        }

        this.openedChests.add(vec);
    }

    private boolean isTier2Chest(Chest chest, BlockVector vec) {
        return false;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        TeamService.getInstance().joinTeam(player, this.spectatorTeam);
        updatePlayers();
    }

    @Override
    public String getDataFileName() {
        return "config";
    }

    @Override
    public void onLoad(ConfigurationSection config) {

    }

    @Override
    public void onSave(ConfigurationSection configurationSection) {

    }
}
