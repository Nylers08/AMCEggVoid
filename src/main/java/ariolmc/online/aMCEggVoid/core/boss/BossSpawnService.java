package ariolmc.online.aMCEggVoid.core.boss;

import ariolmc.online.aMCEggVoid.core.MemberList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Отвечает за выбор мира, расчёт средней точки игроков (с ограничением Y)
 * и поиск/создание безопасной платформы для спавна босса.
 */
public class BossSpawnService {

    private final MemberList memberList;
    private final BossCfg cfg;

    public BossSpawnService(MemberList memberList, BossCfg cfg) {
        this.memberList = memberList;
        this.cfg = cfg;
    }

    public List<Player> getEligiblePlayers() {
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

    /**
     * Проверяет, можно ли запускать босс-ивент (нельзя, если большинство игроков в Энде или никого нет).
     */
    public boolean canStartBossEvent() {
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
        if (endCount * 2 >= players.size()) {
            return false;
        }
        return selectTargetWorld(players) != null;
    }

    public World selectTargetWorld(List<Player> players) {
        Map<World, Integer> counts = new HashMap<>();
        for (Player p : players) {
            World world = p.getWorld();
            if (world.getEnvironment() == World.Environment.THE_END) {
                continue;
            }
            if (cfg.getBossFor(world.getEnvironment()) == null) {
                continue;
            }
            counts.merge(world, 1, Integer::sum);
        }

        World bestWorld = null;
        int bestCount = 0;
        for (Map.Entry<World, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestWorld = entry.getKey();
            }
        }
        return bestWorld;
    }

    public Location computeClampedAveragePoint(World world, List<Player> players) {
        double sumX = 0.0;
        double sumY = 0.0;
        double sumZ = 0.0;
        int count = 0;

        for (Player p : players) {
            if (!p.getWorld().equals(world)) {
                continue;
            }
            Location loc = p.getLocation();
            double clampedY = Math.max(cfg.getMinY(), Math.min(cfg.getMaxY(), loc.getY()));
            sumX += loc.getX();
            sumY += clampedY;
            sumZ += loc.getZ();
            count++;
        }

        if (count == 0) {
            return null;
        }
        return new Location(world, sumX / count, sumY / count, sumZ / count);
    }

    public Location findOrBuildSpawnLocation(Location center, int platformRadius) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }

        int searchRadius = cfg.getSearchRadius();
        int cx = center.getBlockX();
        int cy = (int) Math.round(center.getY());
        int cz = center.getBlockZ();

        // Поиск ближайшей безопасной платформы в радиусе searchRadius
        Location best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int r = 0; r <= searchRadius; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    if (dx * dx + dz * dz > searchRadius * searchRadius) {
                        continue;
                    }
                    int x = cx + dx;
                    int z = cz + dz;
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }

                    int minY = Math.max(world.getMinHeight() + 1, cy - 10);
                    int maxY = Math.min(world.getMaxHeight() - 4, cy + 10);
                    for (int y = maxY; y >= minY; y--) {
                        if (isSafePlatformAt(world, x, y, z, platformRadius)) {
                            double distSq = dx * dx + (y - cy) * (y - cy) + dz * dz;
                            if (distSq < bestDistSq) {
                                bestDistSq = distSq;
                                best = new Location(world, x + 0.5, y, z + 0.5);
                            }
                            break;
                        }
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }

        // Если безопасной позиции нет — строим платформу нужного размера в средней точке
        int buildY = Math.max(world.getMinHeight() + 1, Math.min(world.getMaxHeight() - 4, cy));
        buildPlatform(world, cx, buildY, cz, platformRadius);
        return new Location(world, cx + 0.5, buildY, cz + 0.5);
    }

    public void ensurePlatform(Location loc, int radius) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        if (!isSafePlatformAt(world, cx, cy, cz, radius)) {
            buildPlatform(world, cx, cy, cz, radius);
        }
    }

    public boolean isSafePlatformAt(World world, int cx, int y, int cz, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int bx = cx + dx;
                int bz = cz + dz;
                Block ground = world.getBlockAt(bx, y - 1, bz);
                if (!isSafeGround(ground)) {
                    return false;
                }
                for (int dy = 0; dy <= 3; dy++) {
                    Block space = world.getBlockAt(bx, y + dy, bz);
                    if (!space.isPassable() || space.isLiquid()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void buildPlatform(World world, int cx, int y, int cz, int radius) {
        Material mat = cfg.getPlatformMaterial();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int bx = cx + dx;
                int bz = cz + dz;
                world.getBlockAt(bx, y - 1, bz).setType(mat, false);
                for (int dy = 0; dy <= 3; dy++) {
                    world.getBlockAt(bx, y + dy, bz).setType(Material.AIR, false);
                }
            }
        }
    }

    private boolean isSafeGround(Block block) {
        Material type = block.getType();
        return type.isSolid()
                && !block.isLiquid()
                && type != Material.MAGMA_BLOCK
                && type != Material.CACTUS
                && type != Material.CAMPFIRE
                && type != Material.SOUL_CAMPFIRE
                && type != Material.FIRE
                && type != Material.SOUL_FIRE
                && type != Material.BARRIER;
    }
}
