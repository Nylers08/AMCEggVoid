package ariolmc.online.aMCEggVoid.core.pvp;

import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public class PvpCfg {

    private boolean pvpEnabled = false;
    private String denyMessage = "§6AMCEggVoid | §cPvP между участниками сейчас отключено!";
    private long messageCooldownMs = 1500L;

    public void load() {
        FileConfiguration cfg = CfgUtils.getConfig();
        this.pvpEnabled = cfg.getBoolean("pvp.enabled", false);
        String rawMsg = cfg.getString("pvp.deny-message", "§6AMCEggVoid | §cPvP между участниками сейчас отключено!");
        this.denyMessage = ChatColor.translateAlternateColorCodes('&', rawMsg);
        this.messageCooldownMs = Math.max(0L, cfg.getLong("pvp.message-cooldown-ms", 1500L));
    }
}
