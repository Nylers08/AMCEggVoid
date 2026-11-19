package ariolmc.online.aMCEggVoid.core.trader;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

public class TraderBoothBuilder {

    private final TraderCfg cfg;

    public TraderBoothBuilder(TraderCfg cfg) {
        this.cfg = cfg;
    }

    public Villager spawnBooth(Player player) {
        Location pLoc = player.getLocation();
        World world = pLoc.getWorld();
        if (world == null) {
            return null;
        }

        BlockFace playerFacing = getCardinalDirection(pLoc.getYaw());
        BlockFace boothFront = playerFacing.getOppositeFace();

        // Центр кабинки — в 3 блоках перед игроком
        Block centerBlock = pLoc.getBlock().getRelative(playerFacing, 3);
        return spawnBoothAt(world, centerBlock.getX(), centerBlock.getY(), centerBlock.getZ(), boothFront);
    }

    public Villager spawnBoothAt(World world, int cx, int cy, int cz, BlockFace boothFront) {
        if (world == null) {
            return null;
        }

        // Строим 3x3 кабинку
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int bx = cx + dx;
                int bz = cz + dz;

                // Пол
                world.getBlockAt(bx, cy - 1, bz).setType(Material.SPRUCE_PLANKS, false);

                // Крыша
                if (dx == 0 && dz == 0) {
                    world.getBlockAt(bx, cy + 2, bz).setType(Material.SHROOMLIGHT, false);
                } else {
                    world.getBlockAt(bx, cy + 2, bz).setType(Material.SPRUCE_SLAB, false);
                }

                // Внутренняя клетка (центр) — воздух
                if (dx == 0 && dz == 0) {
                    world.getBlockAt(bx, cy, bz).setType(Material.AIR, false);
                    world.getBlockAt(bx, cy + 1, bz).setType(Material.AIR, false);
                    continue;
                }

                // Переднее окно к игроку (прилавок + открытый проем)
                if (dx == boothFront.getModX() && dz == boothFront.getModZ()) {
                    world.getBlockAt(bx, cy, bz).setType(Material.OAK_SLAB, false);
                    world.getBlockAt(bx, cy + 1, bz).setType(Material.AIR, false);
                    continue;
                }

                // Угловые столбы
                if (dx != 0 && dz != 0) {
                    world.getBlockAt(bx, cy, bz).setType(Material.STRIPPED_OAK_LOG, false);
                    world.getBlockAt(bx, cy + 1, bz).setType(Material.STRIPPED_OAK_LOG, false);
                } else {
                    // Боковые и задняя стены со стеклянными окнами
                    world.getBlockAt(bx, cy, bz).setType(Material.OAK_PLANKS, false);
                    world.getBlockAt(bx, cy + 1, bz).setType(Material.GLASS_PANE, false);
                }
            }
        }

        // Спавним жителя строго в центре лицом к игроку
        float villagerYaw = getYawForFace(boothFront);
        Location villagerLoc = new Location(world, cx + 0.5, cy, cz + 0.5, villagerYaw, 0.0f);

        Villager villager = (Villager) world.spawnEntity(villagerLoc, EntityType.VILLAGER);
        villager.setCustomName(cfg.getName());
        villager.setCustomNameVisible(true);
        villager.setProfession(cfg.getProfession());
        villager.setVillagerType(Villager.Type.PLAINS);
        villager.setVillagerLevel(5);
        villager.setVillagerExperience(1);
        villager.setInvulnerable(cfg.isInvulnerable());
        villager.setAI(false);
        villager.setCollidable(false);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.setRecipes(cfg.buildRecipes());

        return villager;
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

    private float getYawForFace(BlockFace face) {
        return switch (face) {
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case NORTH -> 180.0f;
            case EAST -> -90.0f;
            default -> 0.0f;
        };
    }
}
