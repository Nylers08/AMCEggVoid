package ariolmc.online.aMCEggVoid.core.itemGiver;

import ariolmc.online.aMCEggVoid.core.cfg.LevelItemTierCfg;
import ariolmc.online.aMCEggVoid.core.cfg.LootCfg;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import lombok.Getter;

import java.util.List;

public class ItemTier {

    @Getter
    private final UniqueItemsCfg uniqueItemsCfg;
    @Getter
    private final LootCfg lootCfg;
    private final LevelItemTierCfg cfg;
    private List<LevelItemTier> levels;

    public ItemTier() {
        this(new UniqueItemsCfg(), new LootCfg());
    }

    public ItemTier(UniqueItemsCfg uniqueItemsCfg, LootCfg lootCfg) {
        this.uniqueItemsCfg = uniqueItemsCfg;
        this.lootCfg = lootCfg;
        this.cfg = new LevelItemTierCfg(this.uniqueItemsCfg, this.lootCfg);
        reload();
    }

    public void reload() {
        uniqueItemsCfg.load();
        if (lootCfg != null) {
            lootCfg.load();
        }
        levels = cfg.loadLevels();
    }

    public void addLevel(LevelItemTier levelItemTier) {
        levels.add(levelItemTier);
    }

    public LevelItemTier getLevelByNumber(int levelNumber) {
        if (levels == null || levelNumber < 1 || levelNumber > levels.size()) {
            return null;
        }
        return levels.get(levelNumber - 1);
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
