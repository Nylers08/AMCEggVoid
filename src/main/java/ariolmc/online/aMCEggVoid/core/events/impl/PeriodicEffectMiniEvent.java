package ariolmc.online.aMCEggVoid.core.events.impl;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.events.MiniEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс мини-ивентов, периодически накладывающих набор эффектов зелий на время действия ивента.
 */
public abstract class PeriodicEffectMiniEvent implements MiniEvent {

    protected record EffectSpec(PotionEffectType type, int amplifier) {
    }

    private final Plugin plugin;
    private final MemberList memberList;
    private BukkitTask refreshTask;
    private volatile boolean active = false;
    private List<EffectSpec> activeEffects = List.of();

    protected PeriodicEffectMiniEvent(Plugin plugin, MemberList memberList) {
        this.plugin = plugin;
        this.memberList = memberList;
    }

    protected abstract List<EffectSpec> resolveEffects(FileConfiguration cfg);

    protected abstract String getStartMessage();

    protected abstract String getStopMessage();

    protected abstract Sound getStartSound();

    protected float getStartSoundPitch() {
        return 1.0f;
    }

    @Override
    public boolean canStart() {
        return !getEligiblePlayers().isEmpty();
    }

    @Override
    public void onStart() {
        active = true;
        FileConfiguration cfg = CfgUtils.getConfig();
        this.activeEffects = resolveEffects(cfg);
        long refreshTicks = Math.max(20L, cfg.getLong("mini-events.events." + getId() + ".refresh-interval-ticks", 40L));

        Bukkit.broadcastMessage(getStartMessage());
        for (Player player : getEligiblePlayers()) {
            player.playSound(player.getLocation(), getStartSound(), 0.9f, getStartSoundPitch());
        }

        applyEffects();
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (active) {
                applyEffects();
            }
        }, refreshTicks, refreshTicks);
    }

    @Override
    public void onStop() {
        if (!active) {
            return;
        }
        active = false;
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (EffectSpec spec : activeEffects) {
                player.removePotionEffect(spec.type());
            }
        }
        Bukkit.broadcastMessage(getStopMessage());
    }

    private void applyEffects() {
        List<PotionEffect> effects = activeEffects.stream()
                .map(spec -> new PotionEffect(spec.type(), 100, spec.amplifier(), true, false, true))
                .toList();
        for (Player player : getEligiblePlayers()) {
            for (PotionEffect effect : effects) {
                player.addPotionEffect(effect);
            }
        }
    }

    protected List<Player> getEligiblePlayers() {
        List<Player> players = new ArrayList<>();
        for (Player p : memberList.buildRecipientsPlayers()) {
            if (p.isOnline() && !p.isDead() && p.getGameMode() != GameMode.SPECTATOR) {
                players.add(p);
            }
        }
        if (players.isEmpty()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOnline() && !p.isDead() && p.getGameMode() != GameMode.SPECTATOR) {
                    players.add(p);
                }
            }
        }
        return players;
    }
}
