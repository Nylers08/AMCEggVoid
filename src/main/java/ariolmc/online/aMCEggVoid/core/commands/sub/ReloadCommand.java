package ariolmc.online.aMCEggVoid.core.commands.sub;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.blazeDrop.BlazeDropModule;
import ariolmc.online.aMCEggVoid.core.commands.Subcommand;
import ariolmc.online.aMCEggVoid.core.end.EndModule;
import ariolmc.online.aMCEggVoid.core.endportal.EndPortalModule;
import ariolmc.online.aMCEggVoid.core.events.MiniEventManager;
import ariolmc.online.aMCEggVoid.core.pvp.PvpManager;
import ariolmc.online.aMCEggVoid.core.itemGiver.ItemGiverModule;
import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.nether.NetherModule;
import ariolmc.online.aMCEggVoid.core.piglinBarter.PiglinBarterModule;
import ariolmc.online.aMCEggVoid.core.playerDrop.PlayerDropModule;
import ariolmc.online.aMCEggVoid.core.trader.TraderModule;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements Subcommand {

    private final MemberList memberList;
    private final ItemGiverModule itemGiverModule;
    private final PlayerDropModule playerDropModule;
    private final NetherModule netherModule;
    private final BlazeDropModule blazeDropModule;
    private final PiglinBarterModule piglinBarterModule;
    private final TraderModule traderModule;
    private final EndModule endModule;
    private final EndPortalModule endPortalModule;
    private final PvpManager pvpManager;
    private final MiniEventManager miniEventManager;

    public ReloadCommand(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule) {
        this(memberList, itemGiverModule, playerDropModule, netherModule, null, null, null, null, null, null, null);
    }

    public ReloadCommand(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule, BlazeDropModule blazeDropModule) {
        this(memberList, itemGiverModule, playerDropModule, netherModule, blazeDropModule, null, null, null, null, null, null);
    }

    public ReloadCommand(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule, BlazeDropModule blazeDropModule, PiglinBarterModule piglinBarterModule) {
        this(memberList, itemGiverModule, playerDropModule, netherModule, blazeDropModule, piglinBarterModule, null, null, null, null, null);
    }

    public ReloadCommand(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule, BlazeDropModule blazeDropModule, PiglinBarterModule piglinBarterModule, TraderModule traderModule) {
        this(memberList, itemGiverModule, playerDropModule, netherModule, blazeDropModule, piglinBarterModule, traderModule, null, null, null, null);
    }

    public ReloadCommand(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule, BlazeDropModule blazeDropModule, PiglinBarterModule piglinBarterModule, TraderModule traderModule, EndModule endModule) {
        this(memberList, itemGiverModule, playerDropModule, netherModule, blazeDropModule, piglinBarterModule, traderModule, endModule, null, null, null);
    }

    public ReloadCommand(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule, BlazeDropModule blazeDropModule, PiglinBarterModule piglinBarterModule, TraderModule traderModule, EndModule endModule, EndPortalModule endPortalModule) {
        this(memberList, itemGiverModule, playerDropModule, netherModule, blazeDropModule, piglinBarterModule, traderModule, endModule, endPortalModule, null, null);
    }

    public ReloadCommand(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule, BlazeDropModule blazeDropModule, PiglinBarterModule piglinBarterModule, TraderModule traderModule, EndModule endModule, EndPortalModule endPortalModule, PvpManager pvpManager) {
        this(memberList, itemGiverModule, playerDropModule, netherModule, blazeDropModule, piglinBarterModule, traderModule, endModule, endPortalModule, pvpManager, null);
    }

    public ReloadCommand(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule, BlazeDropModule blazeDropModule, PiglinBarterModule piglinBarterModule, TraderModule traderModule, EndModule endModule, EndPortalModule endPortalModule, PvpManager pvpManager, MiniEventManager miniEventManager) {
        this.memberList = memberList;
        this.itemGiverModule = itemGiverModule;
        this.playerDropModule = playerDropModule;
        this.netherModule = netherModule;
        this.blazeDropModule = blazeDropModule;
        this.piglinBarterModule = piglinBarterModule;
        this.traderModule = traderModule;
        this.endModule = endModule;
        this.endPortalModule = endPortalModule;
        this.pvpManager = pvpManager;
        this.miniEventManager = miniEventManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        CfgUtils.reloadConfig();
        itemGiverModule.getItemTier().reload();
        itemGiverModule.getGiverBossBar().reloadCfg();
        itemGiverModule.getItemGiver().reloadCfg();
        playerDropModule.reload();
        if (netherModule != null) {
            netherModule.reloadCfg();
        }
        if (blazeDropModule != null) {
            blazeDropModule.reloadCfg();
        }
        if (piglinBarterModule != null) {
            piglinBarterModule.reloadCfg();
        }
        if (traderModule != null) {
            traderModule.reloadCfg();
        }
        if (endModule != null) {
            endModule.reloadCfg();
        }
        if (endPortalModule != null) {
            endPortalModule.reloadCfg();
        }
        if (pvpManager != null) {
            pvpManager.reloadCfg();
        }
        if (miniEventManager != null) {
            miniEventManager.reloadCfg();
        }
        memberList.reload();
        if (ariolmc.online.aMCEggVoid.AMCEggVoid.getMailService() != null) {
            ariolmc.online.aMCEggVoid.AMCEggVoid.getMailService().ensureRegisteredMembers(memberList);
        }

        sender.sendMessage("§6AMCEggVoid | §aконфиг перезагружен");
    }
}
