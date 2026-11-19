package ariolmc.online.aMCEggVoid.core.events.impl;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.events.MiniEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

/**
 * Мини-ивент: волна ночных монстров в радиусе 8-24 блоков от участников.
 * Днём на монстров надевается золотой шлем, чтобы они не сгорали на солнце.
 */
public class MobWaveMiniEvent implements MiniEvent {

    private static final List<EntityType> NIGHT_MONSTERS = List.of(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.CREEPER,
            EntityType.SPIDER,
            EntityType.WITCH,
            EntityType.ENDERMAN
    );

    private final Plugin plugin;
    private final MemberList memberList;
    private final Random random = new Random();
    private BukkitTask waveTask;

    public MobWaveMiniEvent(Plugin plugin, MemberList memberList) {
        this.plugin = plugin;
        this.memberList = memberList;
    }

    @Override
    public String getId() {
        return "mob_wave";
    }

    @Override
    public String getDisplayName() {
        return "§2§lВолна монстров";
    }

    @Override
    public int getDefaultDurationSeconds() {
        return 90;
    }

    @Override
    public void onStart() {
        Bukkit.broadcastMessage("§6AMCEggVoid | §2§lВолна монстров началась! Вокруг участников появляются враги!");

        FileConfiguration cfg = CfgUtils.getConfig();
        int intervalSec = Math.max(3, cfg.getInt("mini-events.events.mob_wave.spawn-interval-seconds", 12));

        stopWaveTask();
        waveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnWaveAroundPlayers, 0L, intervalSec * 20L);
    }

    @Override
    public void onStop() {
        stopWaveTask();
        Bukkit.broadcastMessage("§6AMCEggVoid | §aВолна монстров отступила!");
    }

    private void stopWaveTask() {
        if (waveTask != null) {
            waveTask.cancel();
            waveTask = null;
        }
    }

    private void spawnWaveAroundPlayers() {
        FileConfiguration cfg = CfgUtils.getConfig();
        int minRadius = Math.max(4, cfg.getInt("mini-events.events.mob_wave.min-radius", 8));
        int maxRadius = Math.max(minRadius, cfg.getInt("mini-events.events.mob_wave.max-radius", 24));
        int mobsPerPlayer = Math.max(1, cfg.getInt("mini-events.events.mob_wave.mobs-per-player-per-wave", 3));

        List<Player> targets = memberList.buildRecipientsPlayers();
        for (Player player : targets) {
            if (!player.isOnline() || player.isDead() || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            World world = player.getWorld();
            boolean daytime = isDaytime(world);

            for (int i = 0; i < mobsPerPlayer; i++) {
                Location spawnLoc = findSafeLocation(player.getLocation(), minRadius, maxRadius);
                if (spawnLoc == null) {
                    continue;
                }
                EntityType mobType = NIGHT_MONSTERS.get(random.nextInt(NIGHT_MONSTERS.size()));
                Entity entity = world.spawnEntity(spawnLoc, mobType);
                if (daytime && entity instanceof LivingEntity living) {
                    EntityEquipment equip = living.getEquipment();
                    if (equip != null && (equip.getHelmet() == null || equip.getHelmet().getType() == Material.AIR)) {
                        equip.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                        equip.setHelmetDropChance(0.0f);
                    }
                }
            }
        }
    }

    private boolean isDaytime(World world) {
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return false;
        }
        long time = world.getTime() % 24000L;
        return time < 12500L || time > 23500L;
    }

    private Location findSafeLocation(Location center, int minRadius, int maxRadius) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        int baseY = center.getBlockY();

        for (int attempt = 0; attempt < 25; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = minRadius + random.nextDouble() * (maxRadius - minRadius);
            int x = (int) Math.round(center.getX() + Math.cos(angle) * dist);
            int z = (int) Math.round(center.getZ() + Math.sin(angle) * dist);

            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            int minY = Math.max(world.getMinHeight() + 1, baseY - 8);
            int maxY = Math.min(world.getMaxHeight() - 2, baseY + 6);

            for (int y = maxY; y >= minY; y--) {
                Block ground = world.getBlockAt(x, y - 1, z);
                Block feet = world.getBlockAt(x, y, z);
                Block head = world.getBlockAt(x, y + 1, z);

                if (isSafeGround(ground) && feet.isPassable() && !feet.isLiquid()
                        && head.isPassable() && !head.isLiquid()) {
                    return new Location(world, x + 0.5, y, z + 0.5, random.nextFloat() * 360f, 0f);
                }
            }
        }
        return null;
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
