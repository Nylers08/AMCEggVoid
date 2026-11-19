package ariolmc.online.aMCEggVoid.core.boss;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import ariolmc.online.aMCEggVoid.core.cfg.LootConfigHelper;
import ariolmc.online.aMCEggVoid.core.cfg.LootEntry;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Getter
public class BossCfg {

    private int searchRadius = 24;
    private int minY = 0;
    private int maxY = 124;
    private Material platformMaterial = Material.POLISHED_BLACKSTONE_BRICKS;
    private final Map<World.Environment, BossDefinition> worldBosses = new EnumMap<>(World.Environment.class);
    private List<LootEntry> addLoot = List.of();

    public BossCfg() {
        load();
    }

    public void load() {
        AMCEggVoid plugin = AMCEggVoid.getInstance();
        FileConfiguration config;
        if (plugin != null) {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            File file = new File(plugin.getDataFolder(), "bosses.yml");
            if (!file.exists()) {
                plugin.saveResource("bosses.yml", false);
            }
            config = YamlConfiguration.loadConfiguration(file);
            InputStream defStream = plugin.getResource("bosses.yml");
            if (defStream != null) {
                YamlConfiguration defCfg = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defStream, StandardCharsets.UTF_8)
                );
                config.setDefaults(defCfg);
            }
        } else {
            config = new YamlConfiguration();
        }

        this.searchRadius = Math.max(4, config.getInt("boss-event.search-radius", 24));
        this.minY = config.getInt("boss-event.min-y", 0);
        this.maxY = Math.max(this.minY, config.getInt("boss-event.max-y", 124));

        Material platMat = Material.matchMaterial(config.getString("boss-event.platform-material", "POLISHED_BLACKSTONE_BRICKS"));
        this.platformMaterial = (platMat != null && platMat.isBlock()) ? platMat : Material.POLISHED_BLACKSTONE_BRICKS;

        worldBosses.clear();
        ConfigurationSection worldsSec = config.getConfigurationSection("worlds");
        if (worldsSec != null) {
            for (String envKey : worldsSec.getKeys(false)) {
                World.Environment env;
                try {
                    env = World.Environment.valueOf(envKey.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                ConfigurationSection sec = worldsSec.getConfigurationSection(envKey);
                if (sec == null) {
                    continue;
                }
                BossDefinition def = parseBoss(envKey, sec);
                if (def != null) {
                    worldBosses.put(env, def);
                }
            }
        }

        this.addLoot = LootConfigHelper.loadLootList(config.getConfigurationSection("adds"), "loot", AMCEggVoid.getUniqueItemsCfg());
    }

    public BossDefinition getBossFor(World.Environment environment) {
        return worldBosses.get(environment);
    }

    private BossDefinition parseBoss(String fallbackId, ConfigurationSection sec) {
        String id = sec.getString("id", fallbackId.toLowerCase());
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(sec.getString("entity-type", "RAVAGER").trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            entityType = EntityType.RAVAGER;
        }
        String name = ChatColor.translateAlternateColorCodes('&', sec.getString("name", "§4§lБосс"));
        double maxHealth = Math.max(20.0, sec.getDouble("max-health", 250.0));
        double scale = Math.max(0.5, Math.min(4.0, sec.getDouble("scale", 1.0)));
        int platformRadius = Math.max(1, Math.min(6, sec.getInt("platform-radius", 2)));

        BarColor barColor;
        try {
            barColor = BarColor.valueOf(sec.getString("bossbar-color", "RED").trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            barColor = BarColor.RED;
        }

        List<String> abilities = sec.getStringList("abilities");
        List<LootEntry> loot = LootConfigHelper.loadLootList(sec, "loot", AMCEggVoid.getUniqueItemsCfg());
        return new BossDefinition(id, entityType, name, maxHealth, scale, platformRadius, barColor, abilities, loot);
    }
}
