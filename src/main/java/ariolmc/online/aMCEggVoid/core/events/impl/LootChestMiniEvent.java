package ariolmc.online.aMCEggVoid.core.events.impl;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.events.MiniEvent;
import ariolmc.online.aMCEggVoid.core.itemGiver.ItemTier;
import ariolmc.online.aMCEggVoid.core.itemGiver.LevelItemTier;
import ariolmc.online.aMCEggVoid.core.itemGiver.RewardItem;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Мини-ивент: появление сундука с лутом 13-15 тира (и шансом на тотем)
 * в мире с наибольшим количеством игроков (включая Энд).
 */
public class LootChestMiniEvent implements MiniEvent {

    private final MemberList memberList;
    private final ItemTier itemTier;
    private final Random random = new Random();

    public LootChestMiniEvent(MemberList memberList, ItemTier itemTier) {
        this.memberList = memberList;
        this.itemTier = itemTier;
    }

    @Override
    public String getId() {
        return "loot_chest";
    }

    @Override
    public String getDisplayName() {
        return "§b§lСундук Оракула";
    }

    @Override
    public int getDefaultDurationSeconds() {
        return 0;
    }

    @Override
    public boolean canStart() {
        return !getEligiblePlayers().isEmpty();
    }

    @Override
    public void onStart() {
        List<Player> players = getEligiblePlayers();
        World world = selectMostPopulatedWorld(players);
        if (world == null) {
            Bukkit.broadcastMessage("§6AMCEggVoid | §cНе удалось выбрать мир для сундука с лутом.");
            return;
        }

        FileConfiguration cfg = CfgUtils.getConfig();
        int minY = cfg.getInt("mini-events.events.loot_chest.min-y", 56);
        int maxY = Math.max(minY, cfg.getInt("mini-events.events.loot_chest.max-y", 78));
        int searchRadius = Math.max(4, cfg.getInt("mini-events.events.loot_chest.search-radius", 16));
        int originSpread = Math.max(5, cfg.getInt("mini-events.events.loot_chest.origin-spread", 20));
        int minItems = Math.max(1, cfg.getInt("mini-events.events.loot_chest.min-items", 5));
        int maxItems = Math.max(minItems, cfg.getInt("mini-events.events.loot_chest.max-items", 9));
        double totemChance = cfg.getDouble("mini-events.events.loot_chest.totem-chance", 0.35);

        Location avgCenter = computeAverageXZ(world, players);
        WorldBorder border = world.getWorldBorder();

        Location originPoint = null;
        Block chestBlock = null;

        Location borderCenter = border.getCenter();
        double halfSize = Math.max(1.0, border.getSize() / 2.0 - 1.5);
        int minBX = (int) Math.ceil(borderCenter.getX() - halfSize);
        int maxBX = (int) Math.floor(borderCenter.getX() + halfSize);
        int minBZ = (int) Math.ceil(borderCenter.getZ() - halfSize);
        int maxBZ = (int) Math.floor(borderCenter.getZ() + halfSize);

        // Если в радиусе от изначальной точки не найдено ни опоры, ни свободного блока — меняем изначальную точку
        for (int originAttempt = 0; originAttempt < 12; originAttempt++) {
            int ox = avgCenter.getBlockX() + random.nextInt(originSpread * 2 + 1) - originSpread;
            int oz = avgCenter.getBlockZ() + random.nextInt(originSpread * 2 + 1) - originSpread;
            ox = Math.max(minBX, Math.min(maxBX, ox));
            oz = Math.max(minBZ, Math.min(maxBZ, oz));
            int oy = minY + random.nextInt(maxY - minY + 1);

            Location candidateOrigin = new Location(world, ox + 0.5, oy, oz + 0.5);

            Block found = findSolidWithAirAbove(world, ox, oy, oz, searchRadius, minY, maxY, border);
            if (found == null) {
                found = findFreeAirBlock(world, ox, oy, oz, searchRadius, minY, maxY, border);
            }
            if (found != null) {
                originPoint = candidateOrigin;
                chestBlock = found;
                break;
            }
        }

        if (chestBlock == null || originPoint == null) {
            Bukkit.broadcastMessage("§6AMCEggVoid | §cОракул не смог найти подходящую точку для сундука с лутом.");
            return;
        }

        chestBlock.setType(Material.CHEST, false);
        if (chestBlock.getState() instanceof Chest chest) {
            fillChest(chest.getBlockInventory(), minItems, maxItems, totemChance);
        }

        Location fxLoc = chestBlock.getLocation().add(0.5, 0.8, 0.5);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, fxLoc, 35, 0.5, 0.5, 0.5, 0.08);
        world.spawnParticle(Particle.END_ROD, fxLoc, 20, 0.4, 0.5, 0.4, 0.03);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.1f);
        }

        String worldLabel = switch (world.getEnvironment()) {
            case NETHER -> "Незер";
            case THE_END -> "Энд";
            default -> "Верхний мир";
        };
        Bukkit.broadcastMessage("§6AMCEggVoid | §b§lПоявился сундук с лутом! §eОракул предсказывает примерную точку: §fX: "
                + originPoint.getBlockX() + ", Y: " + originPoint.getBlockY() + ", Z: " + originPoint.getBlockZ()
                + " §7(радиус §e" + searchRadius + " §7бл., мир: §d" + worldLabel + "§7)");
    }

    @Override
    public void onStop() {
        // Сундук остаётся в мире, пока игроки его не найдут
    }

    private void fillChest(Inventory inv, int minItems, int maxItems, double totemChance) {
        List<RewardItem> pool = new ArrayList<>();
        for (int tierNum = 13; tierNum <= 15; tierNum++) {
            LevelItemTier tier = itemTier.getLevelByNumber(tierNum);
            if (tier != null && tier.getItemList() != null) {
                pool.addAll(tier.getItemList());
            }
        }

        int count = minItems + (maxItems > minItems ? random.nextInt(maxItems - minItems + 1) : 0);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            RewardItem reward = pool.get(random.nextInt(pool.size()));
            if (reward.itemStack() == null) {
                continue;
            }
            ItemStack item = reward.itemStack().clone();
            int min = Math.max(1, reward.minAmount());
            int max = Math.max(min, reward.maxAmount());
            int amt = min + (max > min ? random.nextInt(max - min + 1) : 0);
            item.setAmount(Math.min(item.getMaxStackSize(), amt));
            placeInRandomEmptySlot(inv, item);
        }

        if (random.nextDouble() <= totemChance) {
            placeInRandomEmptySlot(inv, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        }
    }

    private void placeInRandomEmptySlot(Inventory inv, ItemStack item) {
        List<Integer> emptySlots = new ArrayList<>();
        for (int s = 0; s < inv.getSize(); s++) {
            ItemStack cur = inv.getItem(s);
            if (cur == null || cur.getType().isAir()) {
                emptySlots.add(s);
            }
        }
        if (!emptySlots.isEmpty()) {
            int slot = emptySlots.get(random.nextInt(emptySlots.size()));
            inv.setItem(slot, item);
        }
    }

    private Block findSolidWithAirAbove(World world, int ox, int oy, int oz, int radius, int minY, int maxY, WorldBorder border) {
        List<Block> candidates = new ArrayList<>();
        int yLow = Math.max(world.getMinHeight() + 1, Math.max(minY, oy - radius));
        int yHigh = Math.min(world.getMaxHeight() - 2, Math.min(maxY, oy + radius));

        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                int x = ox + dx;
                int z = oz + dz;
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = yLow; y <= yHigh; y++) {
                    Block ground = world.getBlockAt(x, y - 1, z);
                    Block above = world.getBlockAt(x, y, z);
                    if (ground.getType().isSolid() && !ground.isLiquid()
                            && above.getType().isAir()
                            && border.isInside(above.getLocation())) {
                        candidates.add(above);
                    }
                }
            }
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private Block findFreeAirBlock(World world, int ox, int oy, int oz, int radius, int minY, int maxY, WorldBorder border) {
        for (int attempt = 0; attempt < 30; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            int x = ox + dx;
            int z = oz + dz;
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int yLow = Math.max(world.getMinHeight() + 1, minY);
            int yHigh = Math.min(world.getMaxHeight() - 2, maxY);
            int y = yLow + random.nextInt(Math.max(1, yHigh - yLow + 1));

            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isAir() && border.isInside(block.getLocation())) {
                return block;
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

    private World selectMostPopulatedWorld(List<Player> players) {
        Map<World, Integer> counts = new HashMap<>();
        for (Player p : players) {
            counts.merge(p.getWorld(), 1, Integer::sum);
        }
        World best = null;
        int bestCount = 0;
        for (Map.Entry<World, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    private Location computeAverageXZ(World world, List<Player> players) {
        double sumX = 0.0;
        double sumZ = 0.0;
        int count = 0;
        for (Player p : players) {
            if (p.getWorld().equals(world)) {
                sumX += p.getLocation().getX();
                sumZ += p.getLocation().getZ();
                count++;
            }
        }
        if (count == 0) {
            return world.getSpawnLocation();
        }
        return new Location(world, sumX / count, 64, sumZ / count);
    }
}
