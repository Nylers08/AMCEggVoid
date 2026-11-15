package ariolmc.online.aMCEggVoid.core.itemGiver;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.cfg.LootCfg;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import lombok.Getter;

public class ItemGiverModule {

    @Getter private ItemTier itemTier;
    @Getter private GiverBossBar giverBossBar;
    @Getter private ItemGiver itemGiver;

    public ItemGiverModule(MemberList memberList) {
        this(memberList, new UniqueItemsCfg(), new LootCfg());
    }

    public ItemGiverModule(MemberList memberList, UniqueItemsCfg uniqueItemsCfg, LootCfg lootCfg) {
        itemTier = new ItemTier(uniqueItemsCfg, lootCfg);
        giverBossBar = new GiverBossBar();
        itemGiver = new ItemGiver(memberList, itemTier, giverBossBar);
    }
}
