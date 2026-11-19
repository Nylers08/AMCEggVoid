package ariolmc.online.aMCEggVoid.core.commands.sub;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.commands.Subcommand;
import ariolmc.online.aMCEggVoid.core.itemGiver.GiverBossBar;
import ariolmc.online.aMCEggVoid.core.mail.MailService;
import org.bukkit.command.CommandSender;

public class AddMemberCommand implements Subcommand {

    private final MemberList memberList;
    private final GiverBossBar giverBossBar;

    public AddMemberCommand(MemberList memberList, GiverBossBar giverBossBar) {
        this.memberList = memberList;
        this.giverBossBar = giverBossBar;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length < 2){
            sender.sendMessage("§6AMCEggVoid | §cУкажите имя игрока: /eggvoid addMember <ник>");
            return;
        }

        String playerName = args[1];

        if(memberList.contains(playerName)){
            sender.sendMessage("§6AMCEggVoid | §f" + playerName + " §cуже участвует");
            return;
        }

        memberList.add(playerName);
        giverBossBar.addPlayer(playerName);
        MailService mailService = AMCEggVoid.getMailService();
        if (mailService != null) {
            mailService.getOrCreateMail(playerName);
        }
        sender.sendMessage("§6AMCEggVoid |§f " + playerName+ "§a теперь участвует");
    }
}
