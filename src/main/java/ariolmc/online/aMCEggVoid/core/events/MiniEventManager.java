package ariolmc.online.aMCEggVoid.core.events;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import ariolmc.online.aMCEggVoid.core.EventState;
import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.boss.BossManager;
import ariolmc.online.aMCEggVoid.core.events.impl.AnvilRainListener;
import ariolmc.online.aMCEggVoid.core.events.impl.AnvilRainMiniEvent;
import ariolmc.online.aMCEggVoid.core.events.impl.BossMiniEvent;
import ariolmc.online.aMCEggVoid.core.events.impl.HangoverMiniEvent;
import ariolmc.online.aMCEggVoid.core.events.impl.JumpBoostMiniEvent;
import ariolmc.online.aMCEggVoid.core.events.impl.LootChestMiniEvent;
import ariolmc.online.aMCEggVoid.core.events.impl.MeatballRainMiniEvent;
import ariolmc.online.aMCEggVoid.core.events.impl.MobWaveMiniEvent;
import ariolmc.online.aMCEggVoid.core.events.impl.PvpMiniEvent;
import ariolmc.online.aMCEggVoid.core.events.impl.TraderMiniEvent;
import ariolmc.online.aMCEggVoid.core.events.impl.VoidDropListener;
import ariolmc.online.aMCEggVoid.core.events.impl.VoidDropMiniEvent;
import ariolmc.online.aMCEggVoid.core.itemGiver.ItemTier;
import ariolmc.online.aMCEggVoid.core.pvp.PvpManager;
import ariolmc.online.aMCEggVoid.core.trader.TraderModule;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

/**
 * Управляет жизненным циклом дополнительных ивентов:
 * случайным запуском, обратным отсчётом до старта, обратным отсчётом до конца и остановкой.
 */
public class MiniEventManager {

    private final Plugin plugin;
    private final BossManager bossManager;
    @Getter
    private final MiniEventCfg cfg;
    @Getter
    private final MiniEventRegistry registry;
    private final Random random = new Random();

    private BukkitTask randomSchedulerTask;
    private BukkitTask activeTickTask;
    private BossBar bossBar;

    @Getter
    private MiniEvent activeEvent;
    @Getter
    private MiniEvent pendingEvent;

    public MiniEventManager(Plugin plugin, MemberList memberList, PvpManager pvpManager, BossManager bossManager, ItemTier itemTier, TraderModule traderModule) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.cfg = new MiniEventCfg();
        this.cfg.load();
        this.registry = new MiniEventRegistry();

        registerEvents(memberList, pvpManager, itemTier, traderModule);
        startRandomScheduler();
    }

    private void registerEvents(MemberList memberList, PvpManager pvpManager, ItemTier itemTier, TraderModule traderModule) {
        registry.register(new PvpMiniEvent(pvpManager));
        registry.register(new MobWaveMiniEvent(plugin, memberList));
        registry.register(new BossMiniEvent(bossManager));
        registry.register(new LootChestMiniEvent(memberList, itemTier));
        registry.register(new TraderMiniEvent(memberList, traderModule));
        VoidDropMiniEvent voidDropMiniEvent = new VoidDropMiniEvent(plugin, AMCEggVoid.getUniqueItemsCfg());
        registry.register(voidDropMiniEvent);
        Bukkit.getPluginManager().registerEvents(new VoidDropListener(voidDropMiniEvent), plugin);

        AnvilRainMiniEvent anvilRainMiniEvent = new AnvilRainMiniEvent(plugin, memberList);
        registry.register(anvilRainMiniEvent);
        Bukkit.getPluginManager().registerEvents(new AnvilRainListener(anvilRainMiniEvent), plugin);

        registry.register(new HangoverMiniEvent(plugin, memberList));
        registry.register(new JumpBoostMiniEvent(plugin, memberList));
        registry.register(new MeatballRainMiniEvent(plugin, memberList, AMCEggVoid.getUniqueItemsCfg()));
    }

    public void reloadCfg() {
        cfg.load();
        if (bossManager != null) {
            bossManager.reloadCfg();
        }
        startRandomScheduler();
    }

    public void startRandomScheduler() {
        stopRandomScheduler();
        if (!cfg.isAutoEnabled()) {
            return;
        }
        int min = cfg.getMinIntervalSeconds();
        int max = cfg.getMaxIntervalSeconds();
        int delaySeconds = min + (max > min ? random.nextInt(max - min + 1) : 0);

        randomSchedulerTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if ((!cfg.isOnlyWhenMainEventRunning() || EventState.isRunning())
                    && activeEvent == null
                    && pendingEvent == null) {
                MiniEvent next = registry.getRandom();
                if (next != null && next.canStart()) {
                    startCountdown(next, cfg.getCountdownSeconds());
                }
            }
            startRandomScheduler();
        }, delaySeconds * 20L);
    }

    public void stopRandomScheduler() {
        if (randomSchedulerTask != null) {
            randomSchedulerTask.cancel();
            randomSchedulerTask = null;
        }
    }

    public boolean startCountdown(MiniEvent event, int seconds) {
        if (event == null || !event.canStart()) {
            return false;
        }
        stopActive();

        int total = Math.max(1, seconds);
        this.pendingEvent = event;
        showBossBar(BarColor.YELLOW);

        String shownName = cfg.isRevealInCountdown(event) ? event.getDisplayName() : cfg.getSecretEventName();
        Bukkit.broadcastMessage("§6AMCEggVoid | §eЧерез §c" + total + " сек. §eначнётся доп. ивент: "
                + shownName + "§e!");

        final int[] left = {total};
        activeTickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (left[0] <= 0) {
                startNow(event);
                return;
            }
            updateBossBar(
                    cfg.getCountdownBossBarText()
                            .replace("%event%", shownName)
                            .replace("%time%", String.valueOf(left[0])),
                    Math.max(0.0, Math.min(1.0, (double) left[0] / total))
            );
            if (left[0] <= 5) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.2f);
                }
            }
            left[0]--;
        }, 0L, 20L);
        return true;
    }

    public boolean startNow(MiniEvent event) {
        return startNow(event, cfg.getDurationSeconds(event));
    }

    public boolean startNow(MiniEvent event, int durationSeconds) {
        if (event == null || !event.canStart()) {
            return false;
        }
        stopActive();

        this.activeEvent = event;
        event.onStart();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 0.8f, 1.0f);
        }

        if (durationSeconds > 0) {
            startEndCountdown(durationSeconds);
        }
        return true;
    }

    /**
     * Запускает или обновляет обратный отсчёт до завершения текущего активного ивента.
     */
    public boolean startEndCountdown(int durationSeconds) {
        if (activeEvent == null) {
            return false;
        }
        if (activeTickTask != null) {
            activeTickTask.cancel();
            activeTickTask = null;
        }

        int total = Math.max(1, durationSeconds);
        MiniEvent current = this.activeEvent;
        showBossBar(BarColor.GREEN);

        final int[] left = {total};
        activeTickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (left[0] <= 0) {
                stopActive();
                return;
            }
            current.onEndCountdownTick(left[0]);
            updateBossBar(
                    cfg.getActiveBossBarText()
                            .replace("%event%", current.getDisplayName())
                            .replace("%time%", String.valueOf(left[0])),
                    Math.max(0.0, Math.min(1.0, (double) left[0] / total))
            );
            if (left[0] <= 5) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1.0f);
                }
            }
            left[0]--;
        }, 0L, 20L);
        return true;
    }

    public boolean stopActive() {
        boolean hadSomething = activeTickTask != null || activeEvent != null || pendingEvent != null;
        if (activeTickTask != null) {
            activeTickTask.cancel();
            activeTickTask = null;
        }
        hideBossBar();
        this.pendingEvent = null;
        if (this.activeEvent != null) {
            MiniEvent finished = this.activeEvent;
            this.activeEvent = null;
            finished.onStop();
        }
        return hadSomething;
    }

    public void shutdown() {
        stopRandomScheduler();
        stopActive();
    }

    private void showBossBar(BarColor color) {
        hideBossBar();
        bossBar = Bukkit.createBossBar("", color, BarStyle.SOLID);
        for (Player p : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(p);
        }
    }

    private void updateBossBar(String title, double progress) {
        if (bossBar == null) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!bossBar.getPlayers().contains(p)) {
                bossBar.addPlayer(p);
            }
        }
        bossBar.setTitle(title);
        bossBar.setProgress(progress);
    }

    private void hideBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }
}
