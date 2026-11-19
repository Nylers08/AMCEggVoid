package ariolmc.online.aMCEggVoid.core.endportal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EndPortalAltar {

    /**
     * 12 рамок портала строго по часовой стрелке (если смотреть сверху):
     * Север (Z-2) -> Восток (X+2) -> Юг (Z+2) -> Запад (X-2).
     */
    private static final int[][] CLOCKWISE_OFFSETS = {
            {-1, -2}, {0, -2}, {1, -2},   // Север -> смотрят на юг
            {2, -1}, {2, 0}, {2, 1},      // Восток -> смотрят на запад
            {1, 2}, {0, 2}, {-1, 2},      // Юг -> смотрят на север
            {-2, 1}, {-2, 0}, {-2, -1}    // Запад -> смотрят на восток
    };

    private static final BlockFace[] INWARD_FACES = {
            BlockFace.SOUTH, BlockFace.SOUTH, BlockFace.SOUTH,
            BlockFace.WEST, BlockFace.WEST, BlockFace.WEST,
            BlockFace.NORTH, BlockFace.NORTH, BlockFace.NORTH,
            BlockFace.EAST, BlockFace.EAST, BlockFace.EAST
    };

    private final Plugin plugin;
    private final EndPortalCfg cfg;
    private final File dataFile;

    private boolean active = false;
    private String worldName;
    private int centerX;
    private int centerY;
    private int centerZ;
    private final List<UUID> hologramUuids = new ArrayList<>();
    private final Map<Material, Integer> collected = new EnumMap<>(Material.class);
    private BukkitTask collectorTask;

    public EndPortalAltar(Plugin plugin, EndPortalCfg cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.dataFile = new File(plugin.getDataFolder(), "portal-data.yml");
    }

    public boolean isActive() {
        return active && getWorld() != null;
    }

    public World getWorld() {
        return worldName == null ? null : Bukkit.getWorld(worldName);
    }

    public int getCollected(Material material) {
        return collected.getOrDefault(material, 0);
    }

    public void startItemCollector() {
        stopItemCollector();
        collectorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickItemCollector, 5L, 5L);
    }

    public void stopItemCollector() {
        if (collectorTask != null) {
            collectorTask.cancel();
            collectorTask = null;
        }
    }

    private void tickItemCollector() {
        if (!isActive() || getFilledEyesCount() >= 12) {
            return;
        }
        World world = getWorld();
        if (world == null) {
            return;
        }

        Location centerLoc = new Location(world, centerX + 0.5, centerY + 0.5, centerZ + 0.5);
        if (!centerLoc.getChunk().isLoaded()) {
            return;
        }

        Map<Material, Integer> reqs = cfg.getRequirements();
        boolean changed = false;
        int prevEyes = getFilledEyesCount();

        for (Entity entity : world.getNearbyEntities(centerLoc, 2.0, 2.0, 2.0)) {
            if (!(entity instanceof Item itemEntity) || itemEntity.isDead()) {
                continue;
            }
            Location itemLoc = itemEntity.getLocation();
            if (Math.abs(itemLoc.getX() - (centerX + 0.5)) > 1.6
                    || Math.abs(itemLoc.getZ() - (centerZ + 0.5)) > 1.6
                    || Math.abs(itemLoc.getY() - centerY) > 1.5) {
                continue;
            }

            ItemStack stack = itemEntity.getItemStack();
            Material mat = stack.getType();
            Integer req = reqs.get(mat);
            if (req == null || req <= 0) {
                continue;
            }

            int current = getCollected(mat);
            int needed = req - current;
            if (needed <= 0) {
                continue;
            }

            int take = Math.min(needed, stack.getAmount());
            if (take <= 0) {
                continue;
            }

            collected.put(mat, current + take);
            if (take >= stack.getAmount()) {
                itemEntity.remove();
            } else {
                stack.setAmount(stack.getAmount() - take);
                itemEntity.setItemStack(stack);
            }

            world.playSound(itemLoc, Sound.ENTITY_ITEM_PICKUP, 0.9f, 1.2f);
            world.spawnParticle(Particle.HAPPY_VILLAGER, itemLoc.clone().add(0, 0.25, 0), 8, 0.2, 0.2, 0.2, 0.02);
            changed = true;
        }

        if (changed) {
            syncPortalBlocks(prevEyes);
            save();
            int newEyes = getFilledEyesCount();
            if (newEyes > prevEyes && newEyes < 12) {
                Bukkit.broadcastMessage("§6AMCEggVoid | §dАлтарь Энда заряжен на §f" + newEyes + "/12 §dочей!");
            }
        }
    }

    public int getFilledEyesCount() {
        Map<Material, Integer> reqs = cfg.getRequirements();
        if (reqs.isEmpty()) {
            return 0;
        }
        boolean allComplete = true;
        double sumRatio = 0.0;
        for (Map.Entry<Material, Integer> entry : reqs.entrySet()) {
            int req = Math.max(1, entry.getValue());
            int cur = Math.min(req, getCollected(entry.getKey()));
            if (cur < req) {
                allComplete = false;
            }
            sumRatio += (double) cur / req;
        }
        if (allComplete) {
            return 12;
        }
        double avg = sumRatio / reqs.size();
        return Math.min(11, Math.max(0, (int) Math.floor(avg * 12.0 + 1e-9)));
    }

    public double getProgressPercent() {
        Map<Material, Integer> reqs = cfg.getRequirements();
        if (reqs.isEmpty()) {
            return 0.0;
        }
        double sumRatio = 0.0;
        for (Map.Entry<Material, Integer> entry : reqs.entrySet()) {
            int req = Math.max(1, entry.getValue());
            int cur = Math.min(req, getCollected(entry.getKey()));
            sumRatio += (double) cur / req;
        }
        return (sumRatio / reqs.size()) * 100.0;
    }

    public boolean spawnAt(Player player) {
        Location pLoc = player.getLocation();
        World world = pLoc.getWorld();
        if (world == null) {
            return false;
        }

        removeHolograms();

        BlockFace facing = getCardinalDirection(pLoc.getYaw());
        Block center = pLoc.getBlock().getRelative(facing, 4);

        this.active = true;
        this.worldName = world.getName();
        this.centerX = center.getX();
        this.centerY = center.getY();
        this.centerZ = center.getZ();
        this.collected.clear();

        buildStructure(world);
        syncPortalBlocks(0);
        save();
        return true;
    }

    public void resetProgress() {
        if (!isActive()) {
            return;
        }
        int prev = getFilledEyesCount();
        collected.clear();
        syncPortalBlocks(prev);
        save();
    }

    private void buildStructure(World world) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int bx = centerX + dx;
                int bz = centerZ + dz;
                boolean isInner = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                boolean isCorner = Math.abs(dx) == 2 && Math.abs(dz) == 2;

                world.getBlockAt(bx, centerY - 1, bz).setType(
                        isInner ? Material.CRYING_OBSIDIAN : Material.END_STONE_BRICKS, false
                );

                world.getBlockAt(bx, centerY, bz).setType(Material.AIR, false);
                world.getBlockAt(bx, centerY + 1, bz).setType(Material.AIR, false);

                if (isCorner) {
                    world.getBlockAt(bx, centerY, bz).setType(Material.OBSIDIAN, false);
                }
            }
        }

        for (int i = 0; i < 12; i++) {
            int bx = centerX + CLOCKWISE_OFFSETS[i][0];
            int bz = centerZ + CLOCKWISE_OFFSETS[i][1];
            Block frameBlock = world.getBlockAt(bx, centerY, bz);
            frameBlock.setType(Material.END_PORTAL_FRAME, false);
            if (frameBlock.getBlockData() instanceof EndPortalFrame frameData) {
                frameData.setFacing(INWARD_FACES[i]);
                frameData.setEye(false);
                frameBlock.setBlockData(frameData, false);
            }
        }
    }

    public void syncPortalBlocks(int previousEyes) {
        World world = getWorld();
        if (world == null || !active) {
            return;
        }

        int currentEyes = getFilledEyesCount();

        for (int i = 0; i < 12; i++) {
            int bx = centerX + CLOCKWISE_OFFSETS[i][0];
            int bz = centerZ + CLOCKWISE_OFFSETS[i][1];
            Block block = world.getBlockAt(bx, centerY, bz);
            if (block.getType() != Material.END_PORTAL_FRAME) {
                block.setType(Material.END_PORTAL_FRAME, false);
            }
            if (block.getBlockData() instanceof EndPortalFrame frameData) {
                frameData.setFacing(INWARD_FACES[i]);
                boolean shouldHaveEye = i < currentEyes;
                frameData.setEye(shouldHaveEye);
                block.setBlockData(frameData, false);
            }

            if (i >= previousEyes && i < currentEyes) {
                Location fxLoc = block.getLocation().add(0.5, 1.0, 0.5);
                world.playSound(fxLoc, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 1.0f + (i * 0.04f));
                world.spawnParticle(Particle.END_ROD, fxLoc, 14, 0.2, 0.2, 0.2, 0.03);
            }
        }

        boolean portalOpen = currentEyes >= 12;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block inner = world.getBlockAt(centerX + dx, centerY, centerZ + dz);
                if (portalOpen) {
                    if (inner.getType() != Material.END_PORTAL) {
                        inner.setType(Material.END_PORTAL, false);
                    }
                } else if (inner.getType() == Material.END_PORTAL) {
                    inner.setType(Material.AIR, false);
                }
            }
        }

        if (previousEyes < 12 && portalOpen) {
            Location centerLoc = new Location(world, centerX + 0.5, centerY + 1.0, centerZ + 0.5);
            world.spawnParticle(Particle.PORTAL, centerLoc, 80, 1.2, 0.6, 1.2, 0.2);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.9f, 1.0f);
            }
            Bukkit.broadcastMessage("§6AMCEggVoid | §5§lПортал в Энд полностью заряжен и открыт!");
        }

        updateHolograms(world, currentEyes);
    }

    public boolean isFrameBlock(Block block) {
        if (!active || block == null || worldName == null) {
            return false;
        }
        if (!block.getWorld().getName().equals(worldName) || block.getY() != centerY) {
            return false;
        }
        int dx = block.getX() - centerX;
        int dz = block.getZ() - centerZ;
        for (int[] offset : CLOCKWISE_OFFSETS) {
            if (offset[0] == dx && offset[1] == dz) {
                return true;
            }
        }
        return false;
    }

    public boolean isPortalBlock(Block block) {
        if (!active || block == null || worldName == null) {
            return false;
        }
        if (!block.getWorld().getName().equals(worldName)) {
            return false;
        }
        int dx = Math.abs(block.getX() - centerX);
        int dz = Math.abs(block.getZ() - centerZ);
        int dy = block.getY() - centerY;
        return dx <= 2 && dz <= 2 && (dy == 0 || dy == -1);
    }

    private List<String> buildHologramLines(int eyes) {
        List<String> lines = new ArrayList<>();
        int pct = (int) Math.round(getProgressPercent());
        List<String> template = eyes >= 12 ? cfg.getOpenedLines() : cfg.getProgressLines();

        for (String rawLine : template) {
            if (rawLine.contains("{requirements}")) {
                for (Map.Entry<Material, Integer> entry : cfg.getRequirements().entrySet()) {
                    Material mat = entry.getKey();
                    int req = entry.getValue();
                    int cur = Math.min(req, getCollected(mat));
                    int left = Math.max(0, req - cur);
                    String name = cfg.getDisplayName(mat);
                    String reqTemplate = left == 0 ? cfg.getRequirementDoneLine() : cfg.getRequirementLine();
                    lines.add(reqTemplate
                            .replace("{name}", name)
                            .replace("{collected}", String.valueOf(cur))
                            .replace("{required}", String.valueOf(req))
                            .replace("{left}", String.valueOf(left)));
                }
            } else {
                lines.add(rawLine
                        .replace("{eyes}", String.valueOf(eyes))
                        .replace("{percent}", String.valueOf(pct)));
            }
        }
        return lines;
    }

    private void updateHolograms(World world, int eyes) {
        removeHolograms();

        List<String> lines = buildHologramLines(eyes);
        double lineSpacing = cfg.getLineSpacing();
        double baseBottomY = centerY + cfg.getBaseYOffset();
        double topY = baseBottomY + (lines.size() - 1) * lineSpacing;

        for (int i = 0; i < lines.size(); i++) {
            double y = topY - i * lineSpacing;
            Location loc = new Location(world, centerX + 0.5, y, centerZ + 0.5);
            String text = lines.get(i);
            ArmorStand stand = world.spawn(loc, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setMarker(true);
                as.setSmall(true);
                as.setInvulnerable(true);
                as.setBasePlate(false);
                as.setArms(false);
                as.setCanPickupItems(false);
                as.setPersistent(true);
                as.setCustomName(text);
                as.setCustomNameVisible(true);
            });
            hologramUuids.add(stand.getUniqueId());
        }
    }

    private void removeHolograms() {
        for (UUID uuid : hologramUuids) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
        hologramUuids.clear();

        World world = getWorld();
        if (world != null) {
            Location centerLoc = new Location(world, centerX + 0.5, centerY + 2.0, centerZ + 0.5);
            for (Entity nearby : world.getNearbyEntities(centerLoc, 1.5, 4.0, 1.5)) {
                if (nearby instanceof ArmorStand as && as.isMarker() && !as.isVisible()) {
                    as.remove();
                }
            }
        }
    }

    public void load() {
        if (!dataFile.exists()) {
            active = false;
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(dataFile);
        this.active = yml.getBoolean("active", false);
        this.worldName = yml.getString("world");
        this.centerX = yml.getInt("x");
        this.centerY = yml.getInt("y");
        this.centerZ = yml.getInt("z");

        this.hologramUuids.clear();
        for (String uuidStr : yml.getStringList("hologram-uuids")) {
            try {
                this.hologramUuids.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {
            }
        }

        this.collected.clear();
        ConfigurationSection sec = yml.getConfigurationSection("collected");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat != null) {
                    collected.put(mat, sec.getInt(key, 0));
                }
            }
        }

        if (isActive()) {
            syncPortalBlocks(getFilledEyesCount());
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("active", active);
        yml.set("world", worldName);
        yml.set("x", centerX);
        yml.set("y", centerY);
        yml.set("z", centerZ);

        List<String> uuidStrings = new ArrayList<>();
        for (UUID uuid : hologramUuids) {
            uuidStrings.add(uuid.toString());
        }
        yml.set("hologram-uuids", uuidStrings);

        for (Map.Entry<Material, Integer> entry : collected.entrySet()) {
            yml.set("collected." + entry.getKey().name(), entry.getValue());
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить portal-data.yml: " + e.getMessage());
        }
    }

    private BlockFace getCardinalDirection(float yaw) {
        double rotation = (yaw % 360 + 360) % 360;
        if (rotation >= 315 || rotation < 45) {
            return BlockFace.SOUTH;
        } else if (rotation < 135) {
            return BlockFace.WEST;
        } else if (rotation < 225) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }
}
