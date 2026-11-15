package ariolmc.online.aMCEggVoid.core;

import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MemberList {

    @Getter
    private List<String> memberNames = new ArrayList<>();

    public MemberList(){
        load();
    }


    public void addAll(Collection<String> playerNames){
        for (String name : playerNames){
            String lower = name.toLowerCase();
            if(!memberNames.contains(lower)){
                memberNames.add(lower);
            }
        }
        save();
    }

    public void add(String playerName){
        String lower = playerName.toLowerCase();
        if(!memberNames.contains(lower)){
            memberNames.add(lower);
            save();
        }
    }

    public void remove(String playerName){
        if(memberNames.remove(playerName.toLowerCase())){
            save();
        }
    }

    public boolean contains(String playerName){
        return memberNames.contains(playerName.toLowerCase());
    }

    public List<Player> buildRecipientsPlayers(){
        List<Player> players = new ArrayList<>();
        for (String name : memberNames){
            Player player = Bukkit.getPlayer(name);
            if(player != null){
                players.add(player);
            }
        }

        return players;
    }


    public void reload(){
        load();
    }

    public void save(){
        CfgUtils.getConfig().set("members", memberNames);
        CfgUtils.saveConfig();

    }

    public void load(){
        List<String> cfgMemberName = CfgUtils.getConfig().getStringList("members");
        memberNames = new ArrayList<>();
        addAll(cfgMemberName);
    }
}
