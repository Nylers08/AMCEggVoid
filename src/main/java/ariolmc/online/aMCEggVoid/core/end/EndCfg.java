package ariolmc.online.aMCEggVoid.core.end;

import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public class EndCfg {

    private int gridY = 60;
    private double scatterChance = 0.08;
    private boolean generatePillars = true;
    private boolean spawnDragon = true;

    public void load() {
        FileConfiguration cfg = CfgUtils.getConfig();

        this.gridY = cfg.getInt("end.grid-y", 60);
        double rawChance = cfg.getDouble("end.scatter-chance", 0.08);
        this.scatterChance = rawChance > 1.0 ? rawChance / 100.0 : Math.max(0.01, rawChance);
        this.generatePillars = cfg.getBoolean("end.generate-pillars", true);
        this.spawnDragon = cfg.getBoolean("end.spawn-dragon", true);
    }
}
