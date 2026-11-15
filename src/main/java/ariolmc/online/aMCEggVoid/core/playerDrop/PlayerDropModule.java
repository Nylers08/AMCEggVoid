package ariolmc.online.aMCEggVoid.core.playerDrop;

import ariolmc.online.aMCEggVoid.core.playerDrop.listener.PlayerDeathDropListener;
import lombok.Getter;

public class  PlayerDropModule {

    @Getter
    private final MainMaterialList mainMaterialList;

    @Getter
    private final PlayerDeathDropListener playerDeathDropListener;

    public PlayerDropModule() {
        this.mainMaterialList = new MainMaterialList();
        this.playerDeathDropListener = new PlayerDeathDropListener(mainMaterialList);
    }
}
