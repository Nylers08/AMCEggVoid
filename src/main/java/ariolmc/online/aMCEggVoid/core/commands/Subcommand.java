package ariolmc.online.aMCEggVoid.core.commands;

import org.bukkit.command.CommandSender;

public interface Subcommand {

    void execute(CommandSender sender, String[] args);
}
