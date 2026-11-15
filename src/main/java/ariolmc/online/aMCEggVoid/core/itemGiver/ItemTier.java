package ariolmc.online.aMCEggVoid.core.itemGiver;

import ariolmc.online.aMCEggVoid.core.cfg.LevelItemTierCfg;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import lombok.Getter;

import java.util.List;

public class ItemTier {

    @Getter
    private final UniqueItemsCfg uniqueItemsCfg;
    private final LevelItemTierCfg cfg;
    private List<LevelItemTier> levels;

    public ItemTier() {
        this.uniqueItemsCfg = new UniqueItemsCfg();
        this.cfg = new LevelItemTierCfg(this.uniqueItemsCfg);
        reload();
    }

    public void reload() {
        uniqueItemsCfg.load();
        levels = cfg.loadLevels();
    }

    public void addLevel(LevelItemTier levelItemTier) {
        levels.add(levelItemTier);
    }

    public LevelItemTier getLevel(long currentTick) {
        if (levels == null || levels.isEmpty()) {
            return null;
        }
        LevelItemTier currentLevel = levels.getFirst();
        long tickOffset = Long.MAX_VALUE;

        for (LevelItemTier level : levels) {
            long tickStart = level.getTickStart();
            long currentTickOffset = currentTick - tickStart;
            if (currentTickOffset < 0) {
                continue;
            }

            if (currentTickOffset < tickOffset) {
                tickOffset = currentTickOffset;
                currentLevel = level;
            }
        }

        return currentLevel;
    }
}
