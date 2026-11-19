package ariolmc.online.aMCEggVoid.core.commands.sub;

import ariolmc.online.aMCEggVoid.core.commands.Subcommand;
import ariolmc.online.aMCEggVoid.core.events.MiniEvent;
import ariolmc.online.aMCEggVoid.core.events.MiniEventManager;
import org.bukkit.command.CommandSender;

import java.util.stream.Collectors;

public class EventCommand implements Subcommand {

    private final MiniEventManager manager;

    public EventCommand(MiniEventManager manager) {
        this.manager = manager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("amceggvoid.admin")) {
            sender.sendMessage("§cУ вас нет прав.");
            return;
        }

        if (manager == null) {
            sender.sendMessage("§6AMCEggVoid | §cМенеджер доп. ивентов не инициализирован.");
            return;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "list" -> {
                String list = manager.getRegistry().getAll().stream()
                        .map(e -> "§e" + e.getId() + " §7(" + e.getDisplayName() + "§7)")
                        .collect(Collectors.joining("§f, "));
                sender.sendMessage("§6AMCEggVoid | §fДоступные доп. ивенты: " + list);
            }
            case "stop" -> {
                if (manager.stopActive()) {
                    sender.sendMessage("§6AMCEggVoid | §aТекущий доп. ивент/отсчёт остановлен.");
                } else {
                    sender.sendMessage("§6AMCEggVoid | §eСейчас нет активного доп. ивента или отсчёта.");
                }
            }
            case "start" -> {
                if (args.length < 3) {
                    sender.sendMessage("§6AMCEggVoid | §eИспользование: /eggvoid event start <id> [длительность_сек]");
                    return;
                }
                MiniEvent event = manager.getRegistry().get(args[2]);
                if (event == null) {
                    sender.sendMessage("§6AMCEggVoid | §cИвент §f" + args[2] + " §cне найден. Список: §e/eggvoid event list");
                    return;
                }
                int duration = manager.getCfg().getDurationSeconds(event);
                if (args.length >= 4) {
                    try {
                        duration = Math.max(0, Integer.parseInt(args[3]));
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§6AMCEggVoid | §cНекорректная длительность: " + args[3]);
                        return;
                    }
                }
                if (manager.startNow(event, duration)) {
                    sender.sendMessage("§6AMCEggVoid | §aИвент §f" + event.getId() + " §aзапущен сразу!");
                } else {
                    sender.sendMessage("§6AMCEggVoid | §cИвент §f" + event.getId() + " §cсейчас не может быть запущен (большинство игроков в Энде или нет игроков онлайн).");
                }
            }
            case "countdown" -> {
                if (args.length < 3) {
                    sender.sendMessage("§6AMCEggVoid | §eИспользование: /eggvoid event countdown <id> [секунды]");
                    return;
                }
                MiniEvent event = manager.getRegistry().get(args[2]);
                if (event == null) {
                    sender.sendMessage("§6AMCEggVoid | §cИвент §f" + args[2] + " §cне найден. Список: §e/eggvoid event list");
                    return;
                }
                int seconds = manager.getCfg().getCountdownSeconds();
                if (args.length >= 4) {
                    try {
                        seconds = Math.max(1, Integer.parseInt(args[3]));
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§6AMCEggVoid | §cНекорректное число секунд: " + args[3]);
                        return;
                    }
                }
                if (manager.startCountdown(event, seconds)) {
                    sender.sendMessage("§6AMCEggVoid | §aЗапущен отсчёт (" + seconds + "с) перед ивентом §f" + event.getId() + "§a!");
                } else {
                    sender.sendMessage("§6AMCEggVoid | §cИвент §f" + event.getId() + " §cсейчас не может начать отсчёт (большинство игроков в Энде или нет игроков онлайн).");
                }
            }
            case "endcountdown" -> {
                if (args.length < 3) {
                    sender.sendMessage("§6AMCEggVoid | §eИспользование: /eggvoid event endcountdown <секунды>");
                    return;
                }
                int seconds;
                try {
                    seconds = Math.max(1, Integer.parseInt(args[2]));
                } catch (NumberFormatException e) {
                    sender.sendMessage("§6AMCEggVoid | §cНекорректное число секунд: " + args[2]);
                    return;
                }
                if (manager.startEndCountdown(seconds)) {
                    sender.sendMessage("§6AMCEggVoid | §aОбратный отсчёт до конца активного ивента установлен на §f" + seconds + "с§a.");
                } else {
                    sender.sendMessage("§6AMCEggVoid | §cСейчас нет активного доп. ивента.");
                }
            }
            default -> sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6AMCEggVoid | §fУправление доп. ивентами:");
        sender.sendMessage("§e/eggvoid event list §7— список ивентов");
        sender.sendMessage("§e/eggvoid event start <id> [сек] §7— запустить сразу");
        sender.sendMessage("§e/eggvoid event countdown <id> [сек] §7— отсчёт до старта");
        sender.sendMessage("§e/eggvoid event endcountdown <сек> §7— отсчёт до конца активного ивента");
        sender.sendMessage("§e/eggvoid event stop §7— остановить текущий ивент/отсчёт");
    }
}
