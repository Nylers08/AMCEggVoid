package ariolmc.online.aMCEggVoid.core.playerDrop;

import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Конфигурация процентов выпадения вещей при смерти игрока.
 */
@Getter
public class PlayerDropCfg {

    public enum DeathCauseType {
        SELF,
        MOB,
        PLAYER
    }

    private double selfMinPercent = 0.0;
    private double selfMaxPercent = 50.0;

    private double mobMinPercent = 0.0;
    private double mobMaxPercent = 30.0;

    private double playerMinPercent = 0.0;
    private double playerMaxPercent = 10.0;

    private long combatTagMillis = 15_000L;

    public void load() {
        FileConfiguration cfg = CfgUtils.getConfig();

        this.selfMinPercent = clampPercent(cfg.getDouble("player-drop.self-death.min-percent", 0.0));
        this.selfMaxPercent = Math.max(this.selfMinPercent, clampPercent(cfg.getDouble("player-drop.self-death.max-percent", 50.0)));

        this.mobMinPercent = clampPercent(cfg.getDouble("player-drop.mob-death.min-percent", 0.0));
        this.mobMaxPercent = Math.max(this.mobMinPercent, clampPercent(cfg.getDouble("player-drop.mob-death.max-percent", 30.0)));

        this.playerMinPercent = clampPercent(cfg.getDouble("player-drop.player-death.min-percent", 0.0));
        this.playerMaxPercent = Math.max(this.playerMinPercent, clampPercent(cfg.getDouble("player-drop.player-death.max-percent", 10.0)));

        int combatSeconds = Math.max(1, cfg.getInt("player-drop.combat-tag-seconds", 15));
        this.combatTagMillis = combatSeconds * 1000L;
    }

    public double rollDropFraction(DeathCauseType cause) {
        double min;
        double max;
        switch (cause) {
            case PLAYER -> {
                min = playerMinPercent;
                max = playerMaxPercent;
            }
            case MOB -> {
                min = mobMinPercent;
                max = mobMaxPercent;
            }
            default -> {
                min = selfMinPercent;
                max = selfMaxPercent;
            }
        }
        double percent = (max <= min) ? min : ThreadLocalRandom.current().nextDouble(min, max);
        return percent / 100.0;
    }

    private double clampPercent(double val) {
        return Math.max(0.0, Math.min(100.0, val));
    }
}
