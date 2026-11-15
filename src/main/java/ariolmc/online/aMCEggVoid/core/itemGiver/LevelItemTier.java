package ariolmc.online.aMCEggVoid.core.itemGiver;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LevelItemTier {

    @Getter
    private final String name;

    @Getter
    private final long tickStart;

    @Getter
    private final int giveTickDelay;

    @Getter
    private final List<RewardItem> itemList;

    public LevelItemTier(String name, int tickStart, int giveTickDelay, List<RewardItem> itemList){
        this.name = name;
        this.tickStart = tickStart;
        this.giveTickDelay = giveTickDelay;
        this.itemList = itemList;
    }
}
