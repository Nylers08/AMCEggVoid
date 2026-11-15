package ariolmc.online.aMCEggVoid;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.commands.EggVoidCommands;
import ariolmc.online.aMCEggVoid.core.itemGiver.ItemGiverModule;
import ariolmc.online.aMCEggVoid.core.itemGiver.listener.ConnectPlayerListener;
import ariolmc.online.aMCEggVoid.core.nether.NetherModule;
import ariolmc.online.aMCEggVoid.core.playerDrop.PlayerDropModule;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AMCEggVoid extends JavaPlugin {

    @Getter
    private static AMCEggVoid instance;

    @Getter
    private static MemberList memberList;

    @Getter
    private static ItemGiverModule itemGiverModule;

    @Getter
    private static PlayerDropModule playerDropModule;

    @Getter
    private static NetherModule netherModule;


    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        memberList = new MemberList();

        itemGiverModule = new ItemGiverModule(memberList);
        playerDropModule = new PlayerDropModule();
        netherModule = new NetherModule();


        Bukkit.getPluginManager().registerEvents(new ConnectPlayerListener(memberList, itemGiverModule), this);
        Bukkit.getPluginManager().registerEvents(playerDropModule.getPlayerDeathDropListener(), this);
        this.getCommand("eggvoid").setExecutor(new EggVoidCommands(memberList, itemGiverModule, playerDropModule, netherModule));
    }

    @Override
    public void onDisable() {
        if (itemGiverModule != null) {
            itemGiverModule.getItemGiver().stopGiveCycle();
        }
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        if (netherModule != null) {
            return netherModule.getChunkGenerator();
        }
        return super.getDefaultWorldGenerator(worldName, id);
    }
}
