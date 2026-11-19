package ariolmc.online.aMCEggVoid.core.endportal;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Getter
public class EndPortalModule {

    private final EndPortalCfg cfg;
    private final EndPortalAltar altar;
    private final EndPortalListener listener;

    public EndPortalModule(Plugin plugin) {
        this.cfg = new EndPortalCfg();
        this.cfg.load();
        this.altar = new EndPortalAltar(plugin, cfg);
        this.altar.load();
        this.altar.startItemCollector();
        this.listener = new EndPortalListener(altar);
    }

    public void reloadCfg() {
        cfg.load();
        if (altar.isActive()) {
            altar.syncPortalBlocks(altar.getFilledEyesCount());
        }
    }

    public void stop() {
        altar.stopItemCollector();
    }

    public boolean spawnPortal(Player player) {
        return altar.spawnAt(player);
    }

    public void resetPortal() {
        altar.resetProgress();
    }
}
