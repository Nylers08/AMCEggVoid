package ariolmc.online.aMCEggVoid;

import ariolmc.online.aMCEggVoid.core.EventState;
import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.blazeDrop.BlazeDropModule;
import ariolmc.online.aMCEggVoid.core.boss.BossListener;
import ariolmc.online.aMCEggVoid.core.boss.BossManager;
import ariolmc.online.aMCEggVoid.core.cfg.LootCfg;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import ariolmc.online.aMCEggVoid.core.commands.EggVoidCommands;
import ariolmc.online.aMCEggVoid.core.end.EndModule;
import ariolmc.online.aMCEggVoid.core.endportal.EndPortalModule;
import ariolmc.online.aMCEggVoid.core.events.MiniEventManager;
import ariolmc.online.aMCEggVoid.core.mail.MailCommand;
import ariolmc.online.aMCEggVoid.core.mail.MailJoinListener;
import ariolmc.online.aMCEggVoid.core.mail.MailService;
import ariolmc.online.aMCEggVoid.core.pvp.MemberPvpListener;
import ariolmc.online.aMCEggVoid.core.pvp.PvpManager;
import ariolmc.online.aMCEggVoid.core.itemGiver.ItemGiverModule;
import ariolmc.online.aMCEggVoid.core.itemGiver.listener.ConnectPlayerListener;
import ariolmc.online.aMCEggVoid.core.nether.NetherModule;
import ariolmc.online.aMCEggVoid.core.piglinBarter.PiglinBarterModule;
import ariolmc.online.aMCEggVoid.core.playerDrop.PlayerDropModule;
import ariolmc.online.aMCEggVoid.core.trader.TraderModule;
import ariolmc.online.aMCEggVoid.core.victory.VictoryListener;
import ariolmc.online.aMCEggVoid.core.victory.VictoryService;
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
    private static MailService mailService;

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

    @Getter
    private static EndPortalModule endPortalModule;

    @Getter
    private static PvpManager pvpManager;

    @Getter
    private static BossManager bossManager;

    @Getter
    private static MiniEventManager miniEventManager;

    @Getter
    private static VictoryService victoryService;

    @Override
    public void onEnable() {
        instance = this;
        EventState.stop();
        saveDefaultConfig();
        saveResource("unique_items.yml", false);
        saveResource("loot.yml", false);
        saveResource("bosses.yml", false);

        uniqueItemsCfg = new UniqueItemsCfg();
        lootCfg = new LootCfg();

        memberList = new MemberList();
        mailService = new MailService(this, memberList);

        itemGiverModule = new ItemGiverModule(memberList, uniqueItemsCfg, lootCfg);
        playerDropModule = new PlayerDropModule();
        netherModule = new NetherModule(uniqueItemsCfg, lootCfg);
        blazeDropModule = new BlazeDropModule(uniqueItemsCfg, lootCfg);
        piglinBarterModule = new PiglinBarterModule(uniqueItemsCfg, lootCfg);
        traderModule = new TraderModule(uniqueItemsCfg);
        endModule = new EndModule(this);
        endPortalModule = new EndPortalModule(this);
        pvpManager = new PvpManager(memberList);
        bossManager = new BossManager(this, memberList, uniqueItemsCfg);
        miniEventManager = new MiniEventManager(this, memberList, pvpManager, bossManager, itemGiverModule.getItemTier(), traderModule);
        victoryService = new VictoryService(this);

        Bukkit.getPluginManager().registerEvents(new ConnectPlayerListener(memberList, itemGiverModule), this);
        Bukkit.getPluginManager().registerEvents(playerDropModule.getPlayerDeathDropListener(), this);
        Bukkit.getPluginManager().registerEvents(blazeDropModule.getDeathListener(), this);
        Bukkit.getPluginManager().registerEvents(piglinBarterModule.getBarterListener(), this);
        Bukkit.getPluginManager().registerEvents(netherModule.getSpawnerProtectListener(), this);
        Bukkit.getPluginManager().registerEvents(endModule.getDragonEggVoidService(), this);
        Bukkit.getPluginManager().registerEvents(endPortalModule.getListener(), this);
        Bukkit.getPluginManager().registerEvents(new MemberPvpListener(pvpManager), this);
        Bukkit.getPluginManager().registerEvents(new BossListener(bossManager), this);
        Bukkit.getPluginManager().registerEvents(new MailJoinListener(mailService), this);
        Bukkit.getPluginManager().registerEvents(new VictoryListener(victoryService), this);
        this.getCommand("eggvoid").setExecutor(new EggVoidCommands(memberList, itemGiverModule, playerDropModule, netherModule, blazeDropModule, piglinBarterModule, traderModule, endModule, endPortalModule, pvpManager, miniEventManager));
        this.getCommand("mail").setExecutor(new MailCommand(mailService));
    }

    @Override
    public void onDisable() {
        EventState.stop();
        if (itemGiverModule != null) {
            itemGiverModule.getItemGiver().stopGiveCycle();
        }
        if (endModule != null) {
            endModule.stop();
        }
        if (endPortalModule != null) {
            endPortalModule.stop();
        }
        if (miniEventManager != null) {
            miniEventManager.shutdown();
        }
        if (bossManager != null) {
            bossManager.despawnActiveBoss();
        }
        if (mailService != null) {
            mailService.save();
        }
        if (victoryService != null) {
            victoryService.stopCelebration();
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
