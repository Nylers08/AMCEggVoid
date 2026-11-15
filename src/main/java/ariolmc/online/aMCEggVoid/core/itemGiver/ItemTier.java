package ariolmc.online.aMCEggVoid.core.itemGiver;

import ariolmc.online.aMCEggVoid.core.cfg.LevelItemTierCfg;

import java.util.List;

public class ItemTier {

    private LevelItemTierCfg cfg = new LevelItemTierCfg();
    private List<LevelItemTier> levels;

    public ItemTier(){
        reload();
    }

    public void reload(){
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
