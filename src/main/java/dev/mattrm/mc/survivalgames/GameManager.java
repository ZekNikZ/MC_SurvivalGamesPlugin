package dev.mattrm.mc.survivalgames;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.mattrm.mc.gametools.Service;
import dev.mattrm.mc.gametools.data.IDataManager;
import dev.mattrm.mc.gametools.data.SharedReference;
import dev.mattrm.mc.gametools.event.TeamChangeEvent;
import dev.mattrm.mc.gametools.scoreboards.GameScoreboard;
import dev.mattrm.mc.gametools.scoreboards.ScoreboardService;
import dev.mattrm.mc.gametools.scoreboards.ValueEntry;
import dev.mattrm.mc.gametools.scoreboards.impl.SharedReferenceEntry;
import dev.mattrm.mc.gametools.scoreboards.impl.TimerEntry;
import dev.mattrm.mc.gametools.teams.GameTeam;
import dev.mattrm.mc.gametools.teams.TeamService;
import dev.mattrm.mc.gametools.timer.GameStopwatch;
import dev.mattrm.mc.gametools.util.ActionBarUtils;
import dev.mattrm.mc.gametools.util.ISB;
import dev.mattrm.mc.gametools.util.ListUtils;
import dev.mattrm.mc.gametools.util.Sounds;
import dev.mattrm.mc.gametools.world.WorldSyncService;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GameManager extends Service implements IDataManager {
    private static final int NO_KILL_TIME = 10;
    private static final int CHEST_REFILL_TIME = 15; // 15
    private static final int DEATHMATCH_TIME = 25; // 25

    private static final GameManager INSTANCE = new GameManager();

    private GameStopwatch timer;
    private SharedReference<Integer> alivePlayerCount;

    private GameTeam aliveTeam;
    private GameTeam spectatorTeam;

    private Set<BlockVector> openedChests = new HashSet<>();
    private String map = null;
    private Map<String, MapData> maps = new HashMap<>();
    private Map<UUID, Integer> alivePlayers = new HashMap<>();
    private Map<UUID, Integer> kills = new HashMap<>();

    private int gameState = -1; // 0 = pregame, 1 = ingame, 2 = deathmatch, 3 = deathmatch2, 4 = postgame
    private int currentStartPos = 0;
    private boolean setup = false;

    private void setupTeams() {
        if (this.setup) {
            return;
        }

        TeamService.getInstance().removeTeam("alive");
        TeamService.getInstance().removeTeam("spectator");
        TeamService.getInstance().newTeam("alive", "Alive", "ALIVE");
        TeamService.getInstance().newTeam("spectator", "Spectator", "SPEC");
        this.aliveTeam = TeamService.getInstance().getTeam("alive");
        this.spectatorTeam = TeamService.getInstance().getTeam("spectator");
        this.spectatorTeam.setFormatCode(ChatColor.WHITE);
        this.spectatorTeam.setFormatCode(ChatColor.DARK_GRAY);
        this.spectatorTeam.setColor(java.awt.Color.WHITE);
        this.spectatorTeam.setColor(java.awt.Color.DARK_GRAY);
        this.plugin.getLogger().info("Done loading teams");

        this.setup = true;
    }

    @Override
    protected void setupService() {
        WorldSyncService.getInstance()
            .setGameRuleValue("doMobSpawning", "false")
            .setGameRuleValue("doWeatherCycle", "false")
            .setGameRuleValue("doDaylightCycle", "false")
            .setWeatherClear()
            .setTime(6000)
            .setDifficulty(Difficulty.NORMAL);
        this.timer = new GameStopwatch(this.plugin, 20);
        this.timer.addHook(() -> {
            if (gameState == 0) {
                return;
            }

            if (this.alivePlayers.size() <= 1) {
                endGame();
            }
        });
        this.alivePlayerCount = new SharedReference<>(0);
        this.setupScoreboard();

        // Util
        Set<Material> transparentBlocks = Arrays.stream(Material.values()).filter(Material::isBlock).collect(Collectors.toCollection(HashSet::new));
        transparentBlocks.remove(Material.CHEST);
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    String str;
                    Block block;
                    if ((block = player.getTargetBlock(transparentBlocks, 100)) != null && block.getType() == Material.CHEST) {
                        Location loc = block.getLocation();
                        str = loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " (" + block.getType() + ")";
                    } else {
                        str = "N/A";
                    }

                    ActionBarUtils.get().sendActionBarMessage(player, ChatColor.GOLD + "Looking at: " + str);
                });

                if (gameState != -1) {
                    this.cancel();
                }
            }
        }.runTaskTimer(this.plugin, 0, 1);

        this.plugin.getLogger().info("setup game");
    }

    private void endGame() {
        this.gameState = 4;
        Bukkit.getOnlinePlayers().forEach(player -> player.setAllowFlight(true));
        this.timer.stop();

        Bukkit.broadcastMessage(ChatColor.GOLD + "========================");
        Bukkit.broadcastMessage(ChatColor.GOLD + "           Game over!");
        Bukkit.broadcastMessage(ChatColor.GOLD + "========================");

        Bukkit.broadcastMessage("Player Kills: ");
        this.kills.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(entry -> {
                Bukkit.broadcastMessage("  " + Bukkit.getOfflinePlayer(entry.getKey()).getName() + ": " + entry.getValue());
            });
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

        ScoreboardService.getInstance().setGlobalScoreboard(scoreboard);
    }

    private void updatePlayers() {
        int aliveCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            GameTeam team = TeamService.getInstance().getPlayerTeam(player);
            if (team == null) {
                TeamService.getInstance().joinTeam(player, aliveTeam);
                team = TeamService.getInstance().getPlayerTeam(player);
            }
            if (aliveTeam.equals(team)) {
                player.setGameMode(GameMode.SURVIVAL);
                ++aliveCount;
            } else if (spectatorTeam.equals(team)) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }

        this.alivePlayerCount.setAndNotify(aliveCount);
    }

    public void startGame() {
        if (this.map == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "Select map data first before starting game");
            return;
        }

        this.updatePlayers();

        new BukkitRunnable() {
            int seconds = 10;

            @Override
            public void run() {
                if (seconds > 0) {
                    Bukkit.broadcastMessage(ChatColor.GRAY + "Game starting in " + seconds + " second(s).");
                    Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sounds.get().notePling(), 1, 1));
                    seconds--;
                } else {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "Good luck! " + NO_KILL_TIME + " second grace period active.");
                    this.cancel();
                    actuallyStartGame();
                }
            }
        }.runTaskTimer(this.plugin, 0, 20);
    }

    private void actuallyStartGame() {
        this.timer.start();
        this.openedChests.clear();
        this.gameState = 1;

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 15, 10));
        });

        // Remove DM region
        RegionManager regionManager = WorldGuardPlugin.inst().getRegionManager(Bukkit.getWorlds().get(0));
        if (regionManager.hasRegion("deathmatch")) {
            regionManager.removeRegion("deathmatch");
        }

        // No kill timer
        regionManager.getRegion("__global__").setFlag(DefaultFlag.PVP, StateFlag.State.DENY);
        regionManager.getRegion("__global__").setFlag(DefaultFlag.CHEST_ACCESS, StateFlag.State.ALLOW);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            regionManager.getRegion("__global__").setFlag(DefaultFlag.PVP, StateFlag.State.ALLOW);
            Bukkit.broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + "Grace period has ended! PVP enabled.");
        }, NO_KILL_TIME * 20);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, this::startDeathmatch, DEATHMATCH_TIME * 60 * 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            Bukkit.broadcastMessage(ChatColor.GOLD + "Chests have refilled.");
            this.openedChests.clear();
        }, CHEST_REFILL_TIME * 60 * 20);
    }

    public void startDeathmatch() {
        if (this.gameState != 1) return;

        Bukkit.broadcastMessage(ChatColor.DARK_RED + "Deathmatch will start in 10 seconds.");
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, this::actuallyStartDeathmatch, 10 * 20);
    }

    private void actuallyStartDeathmatch() {
        this.gameState = 2;
        MapData mapData = this.maps.get(map);
        RegionManager regionManager = WorldGuardPlugin.inst().getRegionManager(Bukkit.getWorlds().get(0));
        ProtectedRegion region = new ProtectedCuboidRegion("deathmatch", Utils.toWorldEditBlockVector(mapData.getCornMin()), Utils.toWorldEditBlockVector(mapData.getCornMax()));
        region.setFlag(DefaultFlag.EXIT, StateFlag.State.DENY);
        regionManager.addRegion(region);

        Bukkit.getOnlinePlayers().stream()
            .filter(player -> this.alivePlayers.containsKey(player.getUniqueId()))
            .forEach(player -> {
                player.teleport(this.maps.get(this.map).getSpawnLocations().get(this.alivePlayers.get(player.getUniqueId())).toLocation(Bukkit.getWorlds().get(0)).add(0.5, 0, 0.5));
            });

        new BukkitRunnable() {
            int seconds = 10;

            @Override
            public void run() {
                if (seconds > 0) {
                    Bukkit.broadcastMessage(ChatColor.GRAY + "Deathmatch starting in " + seconds + " second(s).");
                    Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sounds.get().notePling(), 1, 1));
                    seconds--;
                } else {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "Good luck!");
                    this.cancel();
                    gameState = 3;
                }
            }
        }.runTaskTimer(this.plugin, 0, 20);
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
        } else if (holder instanceof DoubleChest) {
            DoubleChest chest = (DoubleChest) holder;
            Location loc = chest.getLocation();
            BlockVector vec = new BlockVector(loc.getBlockX(),
                loc.getBlockY(), loc.getBlockZ());
            if (!this.openedChests.contains(vec)) {
                this.setupChest(chest, vec);
            }
        }
    }

    private void setupChest(Chest chest, BlockVector vec) {
        chest.getInventory().clear();

        int itemCount = Utils.getRandomNumber(4, 6);
        List<Material> mats;
        String chestType;
        if (this.isTier2Chest(vec)) {
            mats = new ArrayList<>(ChestContent.getInstance().tier2Materials);
            chestType = "tier 2";
        } else {
            mats = new ArrayList<>(ChestContent.getInstance().tier1Materials);
            chestType = "tier 1";
        }
        Collections.shuffle(mats);
        List<Material> chosenMaterials =
            mats.stream().limit(itemCount).collect(Collectors.toList());

        List<Integer> positions =
            IntStream.range(0, chest.getInventory().getSize()).boxed().collect(Collectors.toList());
        Collections.shuffle(positions);
        List<Integer> chosenPositions =
            positions.stream().limit(itemCount).collect(Collectors.toList());

        for (int i = 0; i < itemCount; i++) {
            chest.getInventory().setItem(chosenPositions.get(i),
                ISB.stack(chosenMaterials.get(i)));
        }

        this.openedChests.add(vec);
        this.plugin.getLogger().info("Filled " + chestType + " chest at " + vec.getBlockX() + " " + vec.getBlockY() + " " + vec.getBlockZ());
    }

    private void setupChest(DoubleChest chest, BlockVector vec) {
        chest.getInventory().clear();

        int itemCount = Utils.getRandomNumber(4, 6) * 2;
        List<Material> mats;
        String chestType;
        if (this.isTier2Chest(vec)) {
            mats = new ArrayList<>(ChestContent.getInstance().tier2Materials);
            chestType = "tier 2";
        } else {
            mats = new ArrayList<>(ChestContent.getInstance().tier1Materials);
            chestType = "tier 1";
        }
        Collections.shuffle(mats);
        List<Material> chosenMaterials =
            mats.stream().limit(itemCount).collect(Collectors.toList());

        List<Integer> positions =
            IntStream.range(0, chest.getInventory().getSize()).boxed().collect(Collectors.toList());
        Collections.shuffle(positions);
        List<Integer> chosenPositions =
            positions.stream().limit(itemCount).collect(Collectors.toList());

        for (int i = 0; i < itemCount; i++) {
            chest.getInventory().setItem(chosenPositions.get(i),
                ISB.stack(chosenMaterials.get(i)));
        }

        this.openedChests.add(vec);
        this.plugin.getLogger().info("Filled " + chestType + " double chest at " + vec.getBlockX() + " " + vec.getBlockY() + " " + vec.getBlockZ());
    }

    private boolean isTier2Chest(BlockVector vec) {
        return this.map != null && this.maps.get(map).isTier2Chest(vec);
    }

    public void setMap(String map) {
        if (!this.maps.containsKey(map)) {
            Bukkit.broadcastMessage(ChatColor.RED + "Invalid map id: " + map);
            return;
        }

        this.map = map;
        Bukkit.broadcastMessage(ChatColor.GRAY + "Set map to " + map);

        this.setupTeams();
        this.gameState = 0;
        this.currentStartPos = 0;
        Bukkit.getOnlinePlayers().forEach(this::setupPlayer);
    }

    private void setupPlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[]{ null, null, null, null });
        player.setTotalExperience(0);
        player.setAllowFlight(false);
        TeamService.getInstance().joinTeam(player, aliveTeam);

        if (!this.alivePlayers.containsKey(player.getUniqueId())) {
            this.alivePlayers.put(player.getUniqueId(), currentStartPos++);
        }

        player.teleport(this.maps.get(this.map).getSpawnLocations().get(this.alivePlayers.get(player.getUniqueId())).toLocation(Bukkit.getWorlds().get(0)).add(0.5, 0, 0.5));

        this.updatePlayers();
    }

    @Override
    public String getDataFileName() {
        return "maps";
    }

    @Override
    public void onLoad(ConfigurationSection config) {
        Set<String> mapKeys = config.getKeys(false);
        this.maps.clear();
        for (String map : mapKeys) {
            ConfigurationSection mapInfo = config.getConfigurationSection(map);
            MapData mapData = new MapData();
            mapInfo.getStringList("tier2Chests").stream()
                .map(Utils::stringToBlockVector)
                .forEach(mapData::addTier2Chest);
            mapInfo.getStringList("spawnLocations").stream()
                .map(Utils::stringToBlockVector)
                .forEach(mapData::addSpawnLocation);
            mapData.setCornMin(Utils.stringToBlockVector(mapInfo.getString("cornMin")));
            mapData.setCornMax(Utils.stringToBlockVector(mapInfo.getString("cornMax")));
            this.maps.put(map, mapData);
        }
    }

    @Override
    public void onSave(ConfigurationSection config) {
        for (String map : this.maps.keySet()) {
            ConfigurationSection section = config.createSection(map);
            MapData data = this.maps.get(map);

            section.set("cornMin", Utils.blockVectorToString(data.getCornMin()));
            section.set("cornMax", Utils.blockVectorToString(data.getCornMax()));
            section.set("tier2Chests", data.getTier2Chests().stream().map(Utils::blockVectorToString).collect(Collectors.toList()));
            section.set("spawnLocations", data.getSpawnLocations().stream().map(Utils::blockVectorToString).collect(Collectors.toList()));
        }
    }

    @EventHandler
    private void onTeamChange(TeamChangeEvent event) {
//        this.updatePlayers();
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        if (killer != null) {
            this.kills.compute(killer.getUniqueId(), (key, val) -> val == null ? 1 : val + 1);
        }
        TeamService.getInstance().joinTeam(player, this.spectatorTeam);
        this.alivePlayers.remove(player.getUniqueId());
        updatePlayers();
    }

    private static final Set<Material> LEAVES = new HashSet<>(ListUtils.of(
        Material.LEAVES,
        Material.LEAVES_2
    ));

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        if (!LEAVES.contains(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (gameState == 0) {
//            this.setupTeams();
            this.setupPlayer(event.getPlayer());
        } else if (gameState != -1){
            if (!this.alivePlayers.containsKey(event.getPlayer().getUniqueId())) {
                TeamService.getInstance().joinTeam(event.getPlayer(), spectatorTeam);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getFrom();
        if ((gameState == 0 || gameState == 2) && !spectatorTeam.equals(TeamService.getInstance().getPlayerTeam(event.getPlayer()))) {
            to.setY(event.getTo().getY());
            to.setPitch(event.getTo().getPitch());
            to.setYaw(event.getTo().getYaw());
            event.setTo(to);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (gameState == 4 || gameState == 0) {
            event.setCancelled(true);
        }
    }
}
