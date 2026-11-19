package ariolmc.online.aMCEggVoid.core.events.impl;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.events.MiniEvent;
import ariolmc.online.aMCEggVoid.core.trader.TraderModule;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Мини-ивент: появление Торговца Бездны в случайной точке верхнего мира (Y: 54-78) на 3 минуты.
 */
public class TraderMiniEvent implements MiniEvent {

    private static final int TOTAL_BOOTH_BLOCKS = 3 * 4 * 3; // 3x4x3 (dx -1..1, dy -1..2, dz -1..1)

    private final MemberList memberList;
    private final TraderModule traderModule;
    private final Random random = new Random();

    private Villager spawnedVillager;
    private Location boothCenter;

    public TraderMiniEvent(MemberList memberList, TraderModule traderModule) {
        this.memberList = memberList;
        this.traderModule = traderModule;
    }

    @Override
    public String getId() {
        return "trader";
    }

    @Override
    public String getDisplayName() {
        return "§6§lТорговец Бездны";
    }

    @Override
    public int getDefaultDurationSeconds() {
        return 180;
    }

    @Override
    public boolean canStart() {
        if (traderModule == null || getOverworld() == null) {
            return false;
        }
        List<Player> players = getEligiblePlayers();
        if (players.isEmpty()) {
            return false;
        }
        int endCount = 0;
        for (Player p : players) {
            if (p.getWorld().getEnvironment() == World.Environment.THE_END) {
                endCount++;
            }
        }
        return endCount * 2 < players.size();
    }

    @Override
    public void onStart() {
        World overworld = getOverworld();
        if (overworld == null || traderModule == null) {
            Bukkit.broadcastMessage("§6AMCEggVoid | §cНе удалось заспавнить Торговца Бездны.");
            return;
        }

        FileConfiguration cfg = CfgUtils.getConfig();
        int minY = cfg.getInt("mini-events.events.trader.min-y", 54);
        int maxY = Math.max(minY, cfg.getInt("mini-events.events.trader.max-y", 78));
        int attempts = Math.max(5, cfg.getInt("mini-events.events.trader.search-attempts", 30));

        WorldBorder border = overworld.getWorldBorder();
        Location borderCenter = border.getCenter();
        double halfSize = Math.max(3.0, border.getSize() / 2.0 - 2.5);

        int minX = (int) Math.ceil(borderCenter.getX() - halfSize);
        int maxX = (int) Math.floor(borderCenter.getX() + halfSize);
        int minZ = (int) Math.ceil(borderCenter.getZ() - halfSize);
        int maxZ = (int) Math.floor(borderCenter.getZ() + halfSize);

        Location bestLocation = null;
        int bestFreeBlocks = -1;

        for (int i = 0; i < attempts; i++) {
            int cx = minX + random.nextInt(Math.max(1, maxX - minX + 1));
            int cz = minZ + random.nextInt(Math.max(1, maxZ - minZ + 1));
            int cy = minY + random.nextInt(Math.max(1, maxY - minY + 1));

            if (!overworld.isChunkLoaded(cx >> 4, cz >> 4)) {
                overworld.getChunkAt(cx >> 4, cz >> 4);
            }

            int freeCount = countFreeBoothBlocks(overworld, cx, cy, cz);
            if (freeCount > bestFreeBlocks) {
                bestFreeBlocks = freeCount;
                bestLocation = new Location(overworld, cx, cy, cz);
            }
            if (freeCount >= TOTAL_BOOTH_BLOCKS) {
                break;
            }
        }

        if (bestLocation == null) {
            Bukkit.broadcastMessage("§6AMCEggVoid | §cНе найдено место для Торговца Бездны.");
            return;
        }

        int cx = bestLocation.getBlockX();
        int cy = bestLocation.getBlockY();
        int cz = bestLocation.getBlockZ();

        this.boothCenter = bestLocation;
        this.spawnedVillager = traderModule.spawnTraderBoothAt(overworld, cx, cy, cz, BlockFace.SOUTH);

        overworld.spawnParticle(Particle.HAPPY_VILLAGER, bestLocation.clone().add(0.5, 1.5, 0.5), 30, 1.2, 1.0, 1.2, 0.05);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.8f, 1.0f);
        }

        Bukkit.broadcastMessage("§6AMCEggVoid | §6§lПоявился Торговец Бездны! §eКоординаты: §fX: "
                + cx + ", Y: " + cy + ", Z: " + cz + " §7(исчезнет через §c3 мин.§7)");
    }

    @Override
    public void onStop() {
        if (spawnedVillager != null && !spawnedVillager.isDead()) {
            spawnedVillager.remove();
        }
        spawnedVillager = null;

        if (boothCenter != null && boothCenter.getWorld() != null) {
            World world = boothCenter.getWorld();
            int cx = boothCenter.getBlockX();
            int cy = boothCenter.getBlockY();
            int cz = boothCenter.getBlockZ();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        world.getBlockAt(cx + dx, cy + dy, cz + dz).setType(Material.AIR, false);
                    }
                }
            }
            world.spawnParticle(Particle.SMOKE, boothCenter.clone().add(0.5, 1.0, 0.5), 25, 0.8, 0.8, 0.8, 0.04);
        }
        boothCenter = null;

        Bukkit.broadcastMessage("§6AMCEggVoid | §7Торговец Бездны исчез!");
    }

    private int countFreeBoothBlocks(World world, int cx, int cy, int cz) {
        int free = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    Block block = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    if (block.getType().isAir() || (block.isPassable() && !block.isLiquid())) {
                        free++;
                    }
                }
            }
        }
        return free;
    }

    private World getOverworld() {
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NORMAL) {
                return w;
            }
        }
        return null;
    }

    private List<Player> getEligiblePlayers() {
        List<Player> players = new ArrayList<>();
        for (Player p : memberList.buildRecipientsPlayers()) {
            if (p.isOnline() && !p.isDead() && p.getGameMode() != GameMode.SPECTATOR) {
                players.add(p);
            }
        }
        if (players.isEmpty()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOnline() && !p.isDead() && p.getGameMode() != GameMode.SPECTATOR) {
                    players.add(p);
                }
            }
        }
        return players;
    }
}
