package ariolmc.online.aMCEggVoid.core.commands.sub;

import ariolmc.online.aMCEggVoid.core.commands.Subcommand;
import ariolmc.online.aMCEggVoid.core.nether.BlazeZone;
import ariolmc.online.aMCEggVoid.core.nether.NetherModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class NetherGenCommand implements Subcommand {

    private final NetherModule netherModule;

    public NetherGenCommand(NetherModule netherModule) {
        this.netherModule = netherModule;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        World world = null;
        if (sender instanceof Player player) {
            world = player.getWorld();
        }

        // Если игрок не в Незере или команда из консоли — ищем мир с Environment.NETHER
        if (world == null || world.getEnvironment() != World.Environment.NETHER) {
            for (World w : Bukkit.getWorlds()) {
                if (w.getEnvironment() == World.Environment.NETHER) {
                    world = w;
                    break;
                }
            }
        }

        if (world == null) {
            world = Bukkit.getWorld("world_nether");
        }

        if (world == null) {
            sender.sendMessage("§6AMCEggVoid | §cМир Незера (world_nether) не найден на сервере!");
            return;
        }

        int gridRadius = 80;
        int clearRadius = 80;

        if (args.length > 1) {
            try {
                gridRadius = Integer.parseInt(args[1]);
                if (gridRadius < 10) gridRadius = 10;
                if (gridRadius > 500) gridRadius = 500;
                clearRadius = gridRadius;
            } catch (NumberFormatException e) {
                sender.sendMessage("§6AMCEggVoid | §eИспользование: /eggvoid nethergen [радиус сетки] [радиус очистки]");
                return;
            }
        }

        if (args.length > 2) {
            try {
                clearRadius = Integer.parseInt(args[2]);
                if (clearRadius < 0) clearRadius = 0;
                if (clearRadius > 500) clearRadius = 500;
            } catch (NumberFormatException e) {
                sender.sendMessage("§6AMCEggVoid | §eИспользование: /eggvoid nethergen [радиус сетки] [радиус очистки]");
                return;
            }
        }

        sender.sendMessage("§6AMCEggVoid | §eПодготовка генерации Незера в мире " + world.getName() + "...");

        if (clearRadius > 0) {
            sender.sendMessage("§6AMCEggVoid | §eОчистка старых блоков через FAWE в радиусе " + clearRadius + "...");
            int cleared = netherModule.clearNether(world, clearRadius);
            sender.sendMessage("§6AMCEggVoid | §aОчищено старых блоков: §e" + cleared + "§a.");
        }

        sender.sendMessage("§6AMCEggVoid | §eГенерация сетки Незера (радиус: " + gridRadius + ")...");
        int placed = netherModule.generateNether(world, gridRadius);

        sender.sendMessage("§6AMCEggVoid | §fУстановлено блоков сетки: §e" + placed + "§f на высоте Y=" + netherModule.getCfg().getGridY() + ".");

        List<BlazeZone> zones = netherModule.getActiveZones();
        sender.sendMessage("§6AMCEggVoid | §fСгенерировано зон с ифритами: §e" + zones.size() + "§f шт.");
        for (int i = 0; i < zones.size(); i++) {
            BlazeZone z = zones.get(i);
            sender.sendMessage("§6AMCEggVoid | §7• Зона #" + (i + 1) + ": грань=" + z.getBlocksOnEdge() + " блоков");
        }
        sender.sendMessage("§6AMCEggVoid | §fСгенерировано сундуков с лутом: §e" + netherModule.getLastGeneratedChestsCount() + "§f шт.");
    }
}
