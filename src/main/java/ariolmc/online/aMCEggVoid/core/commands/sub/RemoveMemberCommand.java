package ariolmc.online.aMCEggVoid.core.commands.sub;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.commands.Subcommand;
import ariolmc.online.aMCEggVoid.core.itemGiver.GiverBossBar;
import org.bukkit.command.CommandSender;

public class RemoveMemberCommand implements Subcommand {

    private final MemberList memberList;
    private final GiverBossBar giverBossBar;

    public RemoveMemberCommand(MemberList memberList, GiverBossBar giverBossBar) {
        this.memberList = memberList;
        this.giverBossBar = giverBossBar;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length < 2){
            sender.sendMessage("§6AMCEggVoid |§cУкажите имя игрока: /eggvoid removeMember <ник>");
            return;
        }

        String playerName = args[1];

        if(!memberList.contains(playerName)){
            sender.sendMessage("§6AMCEggVoid |§f" + playerName+ " §cне являлся участником");
            return;
        }

        memberList.remove(playerName);
        giverBossBar.removePlayer(playerName);
        sender.sendMessage("§6AMCEggVoid | §f" + playerName+ " §aбольше не участвует");
    }
}
