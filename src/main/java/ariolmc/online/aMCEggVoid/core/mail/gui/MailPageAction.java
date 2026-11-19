package ariolmc.online.aMCEggVoid.core.mail.gui;

import io.neris.NGui.core.gui.button.ClickEventContext;
import io.neris.NGui.core.gui.button.MainItemEventContext;
import io.neris.NGui.core.gui.button.controller.actions.ButtonAction;
import io.neris.NGui.core.services.MenuServiceController;
import io.neris.NGui.core.services.StaticMenuServiceController;
import org.bukkit.entity.Player;

/**
 * Действие NGui для переключения страниц в меню почты.
 */
public class MailPageAction implements ButtonAction {

    public static final String KEY = "amceggvoid_mail_page";

    private final MailMenuFactory menuFactory;
    private final int targetPage;

    public MailPageAction(MailMenuFactory menuFactory, int targetPage) {
        this.menuFactory = menuFactory;
        this.targetPage = targetPage;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String value() {
        return String.valueOf(targetPage);
    }

    @Override
    public void execute(ClickEventContext click) {
        if (!(click.whoClicked instanceof Player player)) {
            return;
        }
        if (!(click instanceof MainItemEventContext main) || main.getMainItem() == null) {
            return;
        }

        MenuServiceController ngui = StaticMenuServiceController.getServiceController();
        String rawPage = ngui.getNbtTagger().getValueStringNBT(main.getMainItem(), KEY);
        if (rawPage == null) {
            return;
        }

        int page;
        try {
            page = Integer.parseInt(rawPage);
        } catch (NumberFormatException ignored) {
            return;
        }

        ngui.getPlayerMenuOpener().open(player, menuFactory.create(new MailMenuContext(player.getUniqueId(), page)));
    }
}
