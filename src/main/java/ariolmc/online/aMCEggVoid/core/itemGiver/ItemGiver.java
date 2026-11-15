package ariolmc.online.aMCEggVoid.core.itemGiver;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.cfg.ItemGiverCfg;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ItemGiver {

    @Delegate
    private ItemGiverCfg cfg;

    private final MemberList memberList;

    private ItemTier itemTier;
    private LevelItemTier currentLevel;

    private BukkitTask currentRun;
    private boolean hasStartCycle = false;
    private long currentTick = 0;

    @Getter
    private GiverBossBar bossBar;


    public ItemGiver(MemberList memberList, ItemTier itemTier, GiverBossBar bossBar){
        this.memberList = memberList;
        this.itemTier = itemTier;
        this.bossBar = bossBar;

        this.cfg = new ItemGiverCfg();
        this.cfg.load();
    }

    public void startGiveCycle(){
        hasStartCycle = true;
        updateLevel();
        if(currentLevel == null){
            return;
        }

        int giveTickDelay = currentLevel.getGiveTickDelay();
        startCycle(giveTickDelay);
        bossBar.start(giveTickDelay, memberList.buildRecipientsPlayers());
    }

    public void stopGiveCycle(){
        hasStartCycle = false;
        currentTick = 0;
        if(currentRun != null){
            currentRun.cancel();
            currentRun = null;
        }
        bossBar.stop();
    }

    public void reloadCfg(){
        cfg.load();
    }

    private void updateLevel(){
        currentLevel = itemTier.getLevel(currentTick);
    }


    private void startCycle(int tickDelay){
        if(currentRun != null){
            currentRun.cancel();
        }
        currentRun = Bukkit.getScheduler().runTaskLater(
                AMCEggVoid.getInstance(), ()->giveCycleBody(tickDelay), tickDelay);
    }

    private void giveCycleBody(int tickDelay){
        List<Player> players = memberList.buildRecipientsPlayers();

        giveItems(players);

        currentTick+=tickDelay;
        if(hasStartCycle){
            startGiveCycle();
        }
    }


    private void giveItems(Collection<? extends Player> players){
        List<RewardItem> items = currentLevel.getItemList();
        if(items == null || items.isEmpty()){
            return;
        }
        Random random = new Random();

        for (Player player : players){
            int randInd = random.nextInt(items.size());
            RewardItem randItem = items.get(randInd);
            int min = randItem.minAmount();
            int max = Math.max(min, randItem.maxAmount());
            int randAmount = random.nextInt(min, max + 1);

            ItemStack item = randItem.itemStack().clone();
            item.setAmount(randAmount);
            player.getInventory().addItem(item);
            player.playSound(player.getLocation(), getSound(), getVolume(), getPitch());
        }
    }

}
