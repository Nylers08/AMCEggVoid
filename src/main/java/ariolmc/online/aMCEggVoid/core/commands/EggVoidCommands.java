package ariolmc.online.aMCEggVoid.core.commands;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.blazeDrop.BlazeDropModule;
import ariolmc.online.aMCEggVoid.core.commands.sub.*;
import ariolmc.online.aMCEggVoid.core.itemGiver.ItemGiverModule;
import ariolmc.online.aMCEggVoid.core.nether.NetherModule;
import ariolmc.online.aMCEggVoid.core.playerDrop.PlayerDropModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EggVoidCommands implements CommandExecutor {

    private final MemberList memberList;
    private final ItemGiverModule itemGiver;
    private final PlayerDropModule playerDropModule;
    private final NetherModule netherModule;
    private final BlazeDropModule blazeDropModule;
    private final Map<String, Subcommand> subcommandMap = new HashMap<>();

    public EggVoidCommands(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule) {
        this(memberList, itemGiverModule, playerDropModule, netherModule, null);
    }

    public EggVoidCommands(MemberList memberList, ItemGiverModule itemGiverModule, PlayerDropModule playerDropModule, NetherModule netherModule, BlazeDropModule blazeDropModule) {
        this.memberList = memberList;
        this.itemGiver = itemGiverModule;
        this.playerDropModule = playerDropModule;
        this.netherModule = netherModule;
        this.blazeDropModule = blazeDropModule;

        initSubcommand();
    }

    private void initSubcommand() {
        addSubCommand(new StartCommand(itemGiver.getItemGiver()), "start");
        addSubCommand(new StopCommand(itemGiver.getItemGiver()), "stop");
        addSubCommand(new ReloadCommand(memberList, itemGiver, playerDropModule, netherModule, blazeDropModule), "reload");
        addSubCommand(new AddMemberCommand(memberList, itemGiver.getGiverBossBar()), "addmember");
        addSubCommand(new RemoveMemberCommand(memberList, itemGiver.getGiverBossBar()), "removemember");
        addSubCommand(new NetherGenCommand(netherModule), "nethergen", "gennether");
    }

    public void addSubCommand(Subcommand command, String... aliases) {
        for (String alias : aliases) {
            String commandName = alias.toLowerCase();
            subcommandMap.put(commandName, command);
        }
    }

    public Set<String> getAllAliases() {
        return subcommandMap.keySet();
    }

    public @Nullable Subcommand getSubcommand(String alias) {
        return subcommandMap.get(alias);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            actionWithoutSubcommand(sender, args);
            return true;
        }
        String subcommandName = args[0];
        executeSubCommand(subcommandName, sender, args);

        return true;
    }

    public void executeSubCommand(String alias, CommandSender sender, String[] args) {
        String commandName = alias.toLowerCase();
        Subcommand subcommand = getSubcommand(commandName);
        if (subcommand == null) {
            actionUnknownSubcommand(commandName, sender, args);
            return;
        }

        subcommand.execute(sender, args);
    }

    public void actionWithoutSubcommand(CommandSender sender, String[] args) {
        sender.sendMessage("§6AMCEggVoid | §fДоступные команды: §e/eggvoid <start|stop|reload|addmember|removemember|nethergen>");
    }

    public void actionUnknownSubcommand(String alias, CommandSender sender, String[] args) {
        sender.sendMessage("§6AMCEggVoid | §cНеизвестная подкоманда: §f" + alias);
    }

    public void messageUsage(String alias, CommandSender sender, String[] args) {

    }
}
