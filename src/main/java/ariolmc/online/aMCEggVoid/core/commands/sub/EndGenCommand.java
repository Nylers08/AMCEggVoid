package ariolmc.online.aMCEggVoid.core.commands.sub;

import ariolmc.online.aMCEggVoid.core.commands.Subcommand;
import ariolmc.online.aMCEggVoid.core.end.EndModule;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Команда /eggvoid endgen [radius] [clearRadius]
 * Очищает зону в Энде, генерирует одиночный слой раскиданного эндерняка,
 * обсидиановые столбы с кристаллами, рамку портала и Дракона Края.
 */
public class EndGenCommand implements Subcommand {

    private final EndModule endModule;

    public EndGenCommand(EndModule endModule) {
        this.endModule = endModule;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("amceggvoid.admin")) {
            sender.sendMessage("§cУ вас нет прав.");
            return;
        }

        World endWorld = endModule.getEndWorld();
        if (endWorld == null) {
            sender.sendMessage("§cМир Энда (world_the_end / THE_END) не найден.");
            return;
        }

        int genRadius = 150;
        if (args.length > 1) {
            try {
                genRadius = Math.max(60, Math.min(500, Integer.parseInt(args[1])));
            } catch (NumberFormatException e) {
                sender.sendMessage("§6AMCEggVoid | §eИспользование: /eggvoid endgen [радиус] [радиус очистки]");
                return;
            }
        }

        int clearRadius = Math.max(genRadius, 200);
        if (args.length > 2) {
            try {
                clearRadius = Math.max(genRadius, Math.min(1000, Integer.parseInt(args[2])));
            } catch (NumberFormatException e) {
                sender.sendMessage("§6AMCEggVoid | §eИспользование: /eggvoid endgen [радиус] [радиус очистки]");
                return;
            }
        }

        Player notifyPlayer = sender instanceof Player p ? p : null;
        if (notifyPlayer == null) {
            sender.sendMessage("§6[Энд] §eОчистка зоны радиусом " + clearRadius + " и генерация Энда радиусом " + genRadius + "...");
        }

        int placed = endModule.generateEnd(genRadius, clearRadius, notifyPlayer);
        if (placed < 0) {
            sender.sendMessage("§cНе удалось сгенерировать Энд.");
        } else if (notifyPlayer == null) {
            sender.sendMessage("§6[Энд] §aГотово! Установлено блоков эндерняка: " + placed);
        }
    }
}
