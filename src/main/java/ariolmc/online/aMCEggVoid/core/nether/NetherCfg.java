package ariolmc.online.aMCEggVoid.core.nether;

import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public class NetherCfg {

    private int gridY = 64;
    private int mainGridStep = 3;  // Дистанция между блоками 2 блока воздуха -> шаг 3
    private int blazeGridStep = 2; // Дистанция между блоками 1 блок воздуха -> шаг 2

    private int maxBlocksOnEdge = 15;
    private int minBlocksOnEdge = 5;
    private int maxZones = 3;
    private int zonePadding = 20;

    private int piglinGridStep = 2;
    private int piglinMaxBlocksOnEdge = 15;
    private int piglinMinBlocksOnEdge = 5;
    private int piglinMaxZones = 3;
    private int piglinZonePadding = 20;

    public void load() {
        FileConfiguration cfg = CfgUtils.getConfig();

        this.gridY = cfg.getInt("nether.grid-y", 64);
        this.mainGridStep = cfg.getInt("nether.netherrack-step", 3);
        this.blazeGridStep = cfg.getInt("nether.blaze-zone.brick-step", 2);

        this.maxBlocksOnEdge = cfg.getInt("nether.blaze-zone.max-blocks-on-edge", 15);
        this.minBlocksOnEdge = cfg.getInt("nether.blaze-zone.min-blocks-on-edge", 5);
        this.maxZones = cfg.getInt("nether.blaze-zone.max-zones", 3);
        this.zonePadding = cfg.getInt("nether.blaze-zone.padding", 20);

        this.piglinGridStep = cfg.getInt("nether.piglin-zone.block-step", this.blazeGridStep);
        this.piglinMaxBlocksOnEdge = cfg.getInt("nether.piglin-zone.max-blocks-on-edge", this.maxBlocksOnEdge);
        this.piglinMinBlocksOnEdge = cfg.getInt("nether.piglin-zone.min-blocks-on-edge", this.minBlocksOnEdge);
        this.piglinMaxZones = cfg.getInt("nether.piglin-zone.max-zones", this.maxZones);
        this.piglinZonePadding = cfg.getInt("nether.piglin-zone.padding", this.zonePadding);
    }
}
