package ariolmc.online.aMCEggVoid.core.commands.sub;

import ariolmc.online.aMCEggVoid.core.commands.Subcommand;
import ariolmc.online.aMCEggVoid.core.trader.TraderModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

public class TraderCommand implements Subcommand {

    private final TraderModule traderModule;

    public TraderCommand(TraderModule traderModule) {
        this.traderModule = traderModule;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§6AMCEggVoid | §cКоманда доступна только игрокам.");
            return;
        }

        if (traderModule == null) {
            sender.sendMessage("§6AMCEggVoid | §cМодуль торговца не инициализирован.");
            return;
        }

        Villager villager = traderModule.spawnTraderBooth(player);
        if (villager != null) {
            int tradesCount = villager.getRecipeCount();
            player.sendMessage("§6AMCEggVoid | §aКабинка с торговцем создана! Доступно торгов: §e" + tradesCount + "§a.");
        } else {
            player.sendMessage("§6AMCEggVoid | §cНе удалось создать кабинку торговца.");
        }
    }
}
