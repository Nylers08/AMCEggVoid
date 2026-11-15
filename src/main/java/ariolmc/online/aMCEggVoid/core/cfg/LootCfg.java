package ariolmc.online.aMCEggVoid.core.cfg;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LootCfg {

    private File file;

    @Getter
    private FileConfiguration config;

    public LootCfg() {
        load();
    }

    public void load() {
        AMCEggVoid plugin = AMCEggVoid.getInstance();
        if (plugin != null) {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            this.file = new File(plugin.getDataFolder(), "loot.yml");
            if (!this.file.exists()) {
                plugin.saveResource("loot.yml", false);
            }

            this.config = YamlConfiguration.loadConfiguration(this.file);

            InputStream defConfigStream = plugin.getResource("loot.yml");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
                this.config.setDefaults(defConfig);
            }
        } else {
            this.config = new YamlConfiguration();
        }
    }
}
