package ariolmc.online.aMCEggVoid.core.itemGiver.listener;

import ariolmc.online.aMCEggVoid.core.EventState;
import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.itemGiver.ItemGiverModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ConnectPlayerListener implements Listener {

    private final MemberList memberList;
    private final ItemGiverModule itemGiverModule;

    public ConnectPlayerListener(MemberList memberList, ItemGiverModule itemGiverModule) {
        this.memberList = memberList;
        this.itemGiverModule = itemGiverModule;
    }

    @EventHandler
    public void joinPlayer(PlayerJoinEvent event){
        if(!EventState.isRunning()){
            return;
        }
        String playerName = event.getPlayer().getName();
        if(memberList.contains(playerName)){
            itemGiverModule.getGiverBossBar().addPlayer(playerName);
        }
    }
}
