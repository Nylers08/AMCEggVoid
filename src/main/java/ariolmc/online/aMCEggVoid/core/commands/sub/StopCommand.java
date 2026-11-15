package ariolmc.online.aMCEggVoid.core.commands.sub;

import ariolmc.online.aMCEggVoid.core.commands.Subcommand;
import ariolmc.online.aMCEggVoid.core.itemGiver.ItemGiver;
import org.bukkit.command.CommandSender;

public class StopCommand implements Subcommand {

    private final ItemGiver itemGiver;

    public StopCommand(ItemGiver itemGiver) {
        this.itemGiver = itemGiver;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        itemGiver.stopGiveCycle();
        sender.sendMessage("§6AMCEggVoid | §aивент закончился");
    }
}
