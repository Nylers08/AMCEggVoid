package ariolmc.online.aMCEggVoid.core.itemGiver;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import ariolmc.online.aMCEggVoid.core.cfg.GiverBossBarCfg;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GiverBossBar {

    @Getter
    private BossBar bossBar;

    @Delegate
    private GiverBossBarCfg cfgData;

    private long tickToGive;
    private long tickLeft;
    private int secToGive;

    private BukkitTask taskUpdate;
    private boolean hasStart = false;


    public GiverBossBar(){
        cfgData = new GiverBossBarCfg();
        cfgData.load();
    }


    public void addPlayer(String name){
        Player player = Bukkit.getPlayer(name);
        if(bossBar == null || player == null){
            return;
        }
        bossBar.addPlayer(player);
    }

    public void removePlayer(String name){
        Player player = Bukkit.getPlayer(name);
        if(bossBar == null || player == null){
            return;
        }
        bossBar.removePlayer(player);
    }

    public void clearPlayer(){
        if(bossBar == null){
            return;
        }
        List<Player> players = new ArrayList<>(bossBar.getPlayers());
        players.forEach(p->bossBar.removePlayer(p));
    }


    public void start(long tickToGive, Collection<? extends Player> players){
        hasStart = true;

        this.tickToGive = tickToGive;
        this.tickLeft = tickToGive;

        buildBossBarIsNull(players);
        startUpdateTask();
    }

    public void stop(){
        hasStart = false;
        if(taskUpdate != null){
            taskUpdate.cancel();
            taskUpdate = null;
        }
        clearPlayer();
    }

    public void reloadCfg(){
        cfgData.load();
    }


    private void buildBossBarIsNull(Collection<? extends Player> players){
        if(bossBar == null){
            buildBossBar(players);
        } else {
            players.forEach(bossBar::addPlayer);
        }
    }

    private void buildBossBar(Collection<? extends Player> players){
        String title = buildTitle();
        this.bossBar = Bukkit.createBossBar(title, BarColor.YELLOW, BarStyle.SEGMENTED_20);
        players.forEach(bossBar::addPlayer);
    }

    private String buildTitle(){
        if(secToGive > 0){
            return String.format(getTextTimeInfo(), secToGive);
        } else {
            return getTextGiveItem();
        }
    }


    private void startUpdateTask(){
        if(taskUpdate != null){
            taskUpdate.cancel();
        }
        taskUpdate = Bukkit.getScheduler().runTaskLater(
                AMCEggVoid.getInstance(), ()->update(getDelayTickUpdate()), getDelayTickUpdate());
    }

    private void update(long tickPassed){
        tickLeft -= tickPassed;
        secToGive = (int) tickLeft /20;

        String title = buildTitle();
        double progress = calcProgress();

        bossBar.setTitle(title);
        bossBar.setProgress(progress);

        playSound();

        if(progress != 0 && hasStart){
            startUpdateTask();
        }
    }

    private double calcProgress(){
        if(tickLeft <= 0 || tickToGive <= 0){
            return 0;
        }

        return Math.min(1.0, Math.max(0.0, (double) tickLeft / tickToGive));
    }

    private void playSound(){
        if(secToGive > getSecToPlaySound()){
            return;
        }

        bossBar.getPlayers().forEach(p->p.playSound(p.getLocation(), getSound(), getVolume(), getPitch()));
    }
}
