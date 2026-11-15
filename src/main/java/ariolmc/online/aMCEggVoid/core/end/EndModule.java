package ariolmc.online.aMCEggVoid.core.end;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Модуль управления миром Энда и защитой Яйца Дракона от падения в пустоту.
 */
@Getter
public class EndModule {

    private final EndCfg endCfg;
    private final EndGridService gridService;
    private final EndGridChunkGenerator chunkGenerator;
    private final DragonEggVoidService dragonEggVoidService;

    public EndModule(Plugin plugin) {
        this.endCfg = new EndCfg();
        this.endCfg.load();
        this.gridService = new EndGridService(endCfg, plugin.getLogger());
        this.chunkGenerator = new EndGridChunkGenerator(endCfg);
        this.dragonEggVoidService = new DragonEggVoidService(plugin, plugin.getLogger());
        this.dragonEggVoidService.start();
    }

    public void reloadCfg() {
        endCfg.load();
    }

    public void stop() {
        dragonEggVoidService.stop();
    }

    public World getEndWorld() {
        World world = Bukkit.getWorld("world_the_end");
        if (world != null) return world;
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.THE_END) {
                return w;
            }
        }
        return null;
    }

    public int generateEnd(int genRadius, int clearRadius, Player notifyPlayer) {
        World endWorld = getEndWorld();
        if (endWorld == null) return -1;
        return gridService.prepareAndGenerate(endWorld, genRadius, clearRadius, notifyPlayer);
    }

    public int clearEnd(int clearRadius) {
        World endWorld = getEndWorld();
        if (endWorld == null) return -1;
        return gridService.clearEndArea(endWorld, clearRadius);
    }
}
