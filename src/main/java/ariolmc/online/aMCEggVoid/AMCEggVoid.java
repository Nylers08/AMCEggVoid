package ariolmc.online.aMCEggVoid;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.blazeDrop.BlazeDropModule;
import ariolmc.online.aMCEggVoid.core.cfg.LootCfg;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import ariolmc.online.aMCEggVoid.core.commands.EggVoidCommands;
import ariolmc.online.aMCEggVoid.core.end.EndModule;
import ariolmc.online.aMCEggVoid.core.itemGiver.ItemGiverModule;
import ariolmc.online.aMCEggVoid.core.itemGiver.listener.ConnectPlayerListener;
import ariolmc.online.aMCEggVoid.core.nether.NetherModule;
import ariolmc.online.aMCEggVoid.core.piglinBarter.PiglinBarterModule;
import ariolmc.online.aMCEggVoid.core.playerDrop.PlayerDropModule;
import ariolmc.online.aMCEggVoid.core.trader.TraderModule;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AMCEggVoid extends JavaPlugin {

    @Getter
    private static AMCEggVoid instance;

    @Getter
    private static UniqueItemsCfg uniqueItemsCfg;

    @Getter
    private static LootCfg lootCfg;

    @Getter
    private static MemberList memberList;

    @Getter
    private static ItemGiverModule itemGiverModule;

    @Getter
    private static PlayerDropModule playerDropModule;

    @Getter
    private static NetherModule netherModule;

    @Getter
    private static BlazeDropModule blazeDropModule;

    @Getter
    private static PiglinBarterModule piglinBarterModule;

    @Getter
    private static TraderModule traderModule;

    @Getter
    private static EndModule endModule;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("unique_items.yml", false);
        saveResource("loot.yml", false);

        uniqueItemsCfg = new UniqueItemsCfg();
        lootCfg = new LootCfg();

        memberList = new MemberList();

        itemGiverModule = new ItemGiverModule(memberList, uniqueItemsCfg, lootCfg);
        playerDropModule = new PlayerDropModule();
        netherModule = new NetherModule(uniqueItemsCfg, lootCfg);
        blazeDropModule = new BlazeDropModule(uniqueItemsCfg, lootCfg);
        piglinBarterModule = new PiglinBarterModule(uniqueItemsCfg, lootCfg);
        traderModule = new TraderModule(uniqueItemsCfg);
        endModule = new EndModule(this);

        Bukkit.getPluginManager().registerEvents(new ConnectPlayerListener(memberList, itemGiverModule), this);
        Bukkit.getPluginManager().registerEvents(playerDropModule.getPlayerDeathDropListener(), this);
        Bukkit.getPluginManager().registerEvents(blazeDropModule.getDeathListener(), this);
        Bukkit.getPluginManager().registerEvents(piglinBarterModule.getBarterListener(), this);
        Bukkit.getPluginManager().registerEvents(netherModule.getSpawnerProtectListener(), this);
        Bukkit.getPluginManager().registerEvents(endModule.getDragonEggVoidService(), this);
        this.getCommand("eggvoid").setExecutor(new EggVoidCommands(memberList, itemGiverModule, playerDropModule, netherModule, blazeDropModule, piglinBarterModule, traderModule, endModule));
    }

    @Override
    public void onDisable() {
        if (itemGiverModule != null) {
            itemGiverModule.getItemGiver().stopGiveCycle();
        }
        if (endModule != null) {
            endModule.stop();
        }
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        if (worldName.toLowerCase().contains("end") && endModule != null) {
            return endModule.getChunkGenerator();
        }
        if (netherModule != null) {
            return netherModule.getChunkGenerator();
        }
        return super.getDefaultWorldGenerator(worldName, id);
    }
}
