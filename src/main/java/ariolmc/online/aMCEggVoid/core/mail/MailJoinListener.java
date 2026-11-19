package ariolmc.online.aMCEggVoid.core.mail;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class MailJoinListener implements Listener {

    private final MailService mailService;

    public MailJoinListener(MailService mailService) {
        this.mailService = mailService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerMail mail = mailService.getMail(event.getPlayer().getName());
        if (mail != null && !mail.isEmpty()) {
            event.getPlayer().sendMessage("§6AMCEggVoid | §eНа вашей почте есть неполученные предметы (§f"
                    + mail.size() + " шт.§e)! Введите §a/mail§e, чтобы забрать.");
        }
    }
}
