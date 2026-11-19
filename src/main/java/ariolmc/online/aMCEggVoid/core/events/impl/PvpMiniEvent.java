package ariolmc.online.aMCEggVoid.core.events.impl;

import ariolmc.online.aMCEggVoid.core.events.MiniEvent;
import ariolmc.online.aMCEggVoid.core.pvp.PvpManager;
import org.bukkit.Bukkit;

/**
 * Мини-ивент: временное включение PvP между участниками.
 */
public class PvpMiniEvent implements MiniEvent {

    private final PvpManager pvpManager;

    public PvpMiniEvent(PvpManager pvpManager) {
        this.pvpManager = pvpManager;
    }

    @Override
    public String getId() {
        return "pvp";
    }

    @Override
    public String getDisplayName() {
        return "§c§lPvP-режим";
    }

    @Override
    public int getDefaultDurationSeconds() {
        return 120;
    }

    @Override
    public void onStart() {
        if (pvpManager != null) {
            pvpManager.enablePvp();
        }
        Bukkit.broadcastMessage("§6AMCEggVoid | §c§lPvP-режим ВКЛЮЧЁН! Урон между участниками открыт!");
    }

    @Override
    public void onEndCountdownTick(int secondsLeft) {
        if (secondsLeft == 10 || (secondsLeft >= 1 && secondsLeft <= 3)) {
            Bukkit.broadcastMessage("§6AMCEggVoid | §ePvP-режим закончится через §c" + secondsLeft + " сек.");
        }
    }

    @Override
    public void onStop() {
        if (pvpManager != null) {
            pvpManager.disablePvp();
        }
        Bukkit.broadcastMessage("§6AMCEggVoid | §aPvP-режим завершён! Урон между участниками снова отключён.");
    }
}
