package ariolmc.online.aMCEggVoid.core.mail.gui;

import java.util.UUID;

/**
 * Контекст открытия меню почты: владелец и номер страницы (0-based).
 */
public record MailMenuContext(UUID playerId, int page) {
}
