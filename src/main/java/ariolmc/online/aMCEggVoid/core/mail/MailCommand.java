package ariolmc.online.aMCEggVoid.core.mail;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MailCommand implements CommandExecutor {

    private final MailService mailService;

    public MailCommand(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§6AMCEggVoid | §cКоманда доступна только игрокам.");
            return true;
        }

        PlayerMail mail = mailService.getOrCreateMail(player.getName());
        if (mail == null || mail.isEmpty()) {
            player.sendMessage("§6AMCEggVoid | §7Ваша почта пуста.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            player.sendMessage("§6AMCEggVoid | §fНа вашей почте сейчас §e" + mail.size() + " §fпредмет(ов). Введите §a/mail§f, чтобы забрать.");
            return true;
        }

        int before = mail.size();
        int claimed = mailService.claimAll(player);
        int remaining = mail.size();

        if (claimed > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.9f, 1.1f);
            if (remaining == 0) {
                player.sendMessage("§6AMCEggVoid | §aВы забрали все предметы с почты (§f" + before + "§a)!");
            } else {
                player.sendMessage("§6AMCEggVoid | §aЗабрано §f" + claimed + " §aпредметов. Осталось на почте: §e" + remaining + " §7(освободите место в инвентаре).");
            }
        } else {
            player.sendMessage("§6AMCEggVoid | §cВ инвентаре нет места! На почте ждёт §e" + before + " §cпредмет(ов).");
        }
        return true;
    }
}
