package ariolmc.online.aMCEggVoid.core.itemGiver;

import ariolmc.online.aMCEggVoid.core.MemberList;
import lombok.Getter;

public class ItemGiverModule {

    @Getter private  ItemTier itemTier;
    @Getter private  GiverBossBar giverBossBar;
    @Getter private  ItemGiver itemGiver;

    public ItemGiverModule(MemberList memberList){
        itemTier = new ItemTier();
        giverBossBar = new GiverBossBar();
        itemGiver = new ItemGiver(memberList, itemTier, giverBossBar);
    }
}
