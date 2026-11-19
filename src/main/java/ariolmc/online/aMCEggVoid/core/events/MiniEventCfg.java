package ariolmc.online.aMCEggVoid.core.events;

import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public class MiniEventCfg {

    private boolean autoEnabled = true;
    private boolean onlyWhenMainEventRunning = true;
    private int minIntervalSeconds = 600;
    private int maxIntervalSeconds = 1200;
    private int countdownSeconds = 60;
    private boolean revealNextEvent = true;
    private String secretEventName = "§5§lСекретный ивент";
    private String countdownBossBarText = "§eДоп. ивент %event% §eначнётся через §c%time%с";
    private String activeBossBarText = "§aИдёт доп. ивент: %event% §7(осталось §e%time%с§7)";

    public void load() {
        FileConfiguration cfg = CfgUtils.getConfig();
        this.autoEnabled = cfg.getBoolean("mini-events.auto-enabled", true);
        this.onlyWhenMainEventRunning = cfg.getBoolean("mini-events.only-when-main-event-running", true);
        this.minIntervalSeconds = Math.max(30, cfg.getInt("mini-events.min-interval-seconds", 600));
        this.maxIntervalSeconds = Math.max(this.minIntervalSeconds, cfg.getInt("mini-events.max-interval-seconds", 1200));
        this.countdownSeconds = Math.max(5, cfg.getInt("mini-events.countdown-seconds", 60));
        this.revealNextEvent = cfg.getBoolean("mini-events.reveal-next-event", true);
        this.secretEventName = color(cfg.getString("mini-events.secret-event-name", "§5§lСекретный ивент"));
        this.countdownBossBarText = color(cfg.getString(
                "mini-events.countdown-bossbar-text",
                "§eДоп. ивент %event% §eначнётся через §c%time%с"
        ));
        this.activeBossBarText = color(cfg.getString(
                "mini-events.active-bossbar-text",
                "§aИдёт доп. ивент: %event% §7(осталось §e%time%с§7)"
        ));
    }

    public boolean isRevealInCountdown(MiniEvent event) {
        if (event == null) {
            return revealNextEvent;
        }
        FileConfiguration cfg = CfgUtils.getConfig();
        String path = "mini-events.events." + event.getId() + ".reveal-in-countdown";
        if (cfg.contains(path)) {
            return cfg.getBoolean(path);
        }
        return revealNextEvent && event.isRevealInCountdownByDefault();
    }

    public int getDurationSeconds(MiniEvent event) {
        if (event == null) {
            return 0;
        }
        FileConfiguration cfg = CfgUtils.getConfig();
        String path = "mini-events.events." + event.getId() + ".duration-seconds";
        if (cfg.contains(path)) {
            return Math.max(0, cfg.getInt(path));
        }
        return event.getDefaultDurationSeconds();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
