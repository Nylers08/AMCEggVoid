package ariolmc.online.aMCEggVoid.core.commands.sub;

import ariolmc.online.aMCEggVoid.core.commands.Subcommand;
import ariolmc.online.aMCEggVoid.core.endportal.EndPortalModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PortalCommand implements Subcommand {

    private final EndPortalModule endPortalModule;

    public PortalCommand(EndPortalModule endPortalModule) {
        this.endPortalModule = endPortalModule;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("amceggvoid.admin")) {
            sender.sendMessage("§cУ вас нет прав.");
            return;
        }

        if (endPortalModule == null) {
            sender.sendMessage("§6AMCEggVoid | §cМодуль портала в Энд не инициализирован.");
            return;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("reset")) {
            if (!endPortalModule.getAltar().isActive()) {
                sender.sendMessage("§6AMCEggVoid | §cАлтарь портала ещё не заспавнен.");
                return;
            }
            endPortalModule.resetPortal();
            sender.sendMessage("§6AMCEggVoid | §aПрогресс алтаря портала сброшен до 0/12.");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§6AMCEggVoid | §cСпавн портала доступен только игрокам.");
            return;
        }

        if (endPortalModule.spawnPortal(player)) {
            player.sendMessage("§6AMCEggVoid | §aАлтарь Портала в Энд создан перед вами! §7(Сброс: §e/eggvoid portal reset§7)");
        } else {
            player.sendMessage("§6AMCEggVoid | §cНе удалось создать Алтарь Портала.");
        }
    }
}
