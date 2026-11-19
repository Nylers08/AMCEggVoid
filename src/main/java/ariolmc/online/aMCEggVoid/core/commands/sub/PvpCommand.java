package ariolmc.online.aMCEggVoid.core.commands.sub;

import ariolmc.online.aMCEggVoid.core.commands.Subcommand;
import ariolmc.online.aMCEggVoid.core.pvp.PvpManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class PvpCommand implements Subcommand {

    private final PvpManager pvpManager;

    public PvpCommand(PvpManager pvpManager) {
        this.pvpManager = pvpManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("amceggvoid.admin")) {
            sender.sendMessage("§cУ вас нет прав.");
            return;
        }

        if (pvpManager == null) {
            sender.sendMessage("§6AMCEggVoid | §cМодуль PvP не инициализирован.");
            return;
        }

        if (args.length < 2) {
            String state = pvpManager.isPvpEnabled() ? "§aвключено" : "§cвыключено";
            sender.sendMessage("§6AMCEggVoid | §fPvP между участниками сейчас " + state + "§f. Используйте: §e/eggvoid pvp <on|off>");
            return;
        }

        String arg = args[1].toLowerCase();
        if (arg.equals("on") || arg.equals("enable") || arg.equals("true")) {
            pvpManager.enablePvp();
            Bukkit.broadcastMessage("§6AMCEggVoid | §c§lPvP между участниками ВКЛЮЧЕНО!");
        } else if (arg.equals("off") || arg.equals("disable") || arg.equals("false")) {
            pvpManager.disablePvp();
            Bukkit.broadcastMessage("§6AMCEggVoid | §aPvP между участниками ОТКЛЮЧЕНО.");
        } else {
            sender.sendMessage("§6AMCEggVoid | §eИспользование: /eggvoid pvp <on|off>");
        }
    }
}
