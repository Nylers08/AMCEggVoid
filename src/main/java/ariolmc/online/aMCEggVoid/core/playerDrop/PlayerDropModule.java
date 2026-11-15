package ariolmc.online.aMCEggVoid.core.playerDrop;

import ariolmc.online.aMCEggVoid.core.playerDrop.listener.PlayerDeathDropListener;
import lombok.Getter;

public class PlayerDropModule {

    @Getter
    private final MainMaterialList mainMaterialList;

    @Getter
    private final PlayerDropCfg playerDropCfg;

    @Getter
    private final PlayerDeathDropListener playerDeathDropListener;

    public PlayerDropModule() {
        this.mainMaterialList = new MainMaterialList();
        this.playerDropCfg = new PlayerDropCfg();
        this.playerDropCfg.load();
        this.playerDeathDropListener = new PlayerDeathDropListener(mainMaterialList, playerDropCfg);
    }

    public void reload() {
        mainMaterialList.reload();
        playerDropCfg.load();
    }
}
