package ariolmc.online.aMCEggVoid.core.commands.sub;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.blazeDrop.BlazeDropModule;
import ariolmc.online.aMCEggVoid.core.commands.Subcommand;
import ariolmc.online.aMCEggVoid.core.itemGiver.ItemGiverModule;
import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.nether.NetherModule;
import ariolmc.online.aMCEggVoid.core.piglinBarter.PiglinBarterModule;
import ariolmc.online.aMCEggVoid.core.playerDrop.PlayerDropModule;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements Subcommand {

    private final MemberList memberList;
    private final ItemGiverModule itemGiverModule;
    private final PlayerDropModule playerDropModule;
    private final NetherModule netherModule;
    private final BlazeDropModule blazeDropModule;
    private final PiglinBarterModule piglinBarterModule;

    public ReloadCommand(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule) {
        this(memberList, itemGiverModule, playerDropModule, netherModule, null, null);
    }

    public ReloadCommand(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule, BlazeDropModule blazeDropModule) {
        this(memberList, itemGiverModule, playerDropModule, netherModule, blazeDropModule, null);
    }

    public ReloadCommand(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule, BlazeDropModule blazeDropModule, PiglinBarterModule piglinBarterModule) {
        this.memberList = memberList;
        this.itemGiverModule = itemGiverModule;
        this.playerDropModule = playerDropModule;
        this.netherModule = netherModule;
        this.blazeDropModule = blazeDropModule;
        this.piglinBarterModule = piglinBarterModule;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        CfgUtils.reloadConfig();
        itemGiverModule.getItemTier().reload();
        itemGiverModule.getGiverBossBar().reloadCfg();
        itemGiverModule.getItemGiver().reloadCfg();
        playerDropModule.getMainMaterialList().reload();
        if (netherModule != null) {
            netherModule.reloadCfg();
        }
        if (blazeDropModule != null) {
            blazeDropModule.reloadCfg();
        }
        if (piglinBarterModule != null) {
            piglinBarterModule.reloadCfg();
        }
        memberList.reload();

        sender.sendMessage("§6AMCEggVoid | §aконфиг перезагружен");
    }
}
