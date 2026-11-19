package ariolmc.online.aMCEggVoid.core.events.impl;

import ariolmc.online.aMCEggVoid.core.boss.BossManager;
import ariolmc.online.aMCEggVoid.core.events.MiniEvent;
import org.bukkit.Bukkit;

/**
 * Мини-ивент: появление босса в мире с наибольшим количеством игроков (кроме Энда).
 */
public class BossMiniEvent implements MiniEvent {

    private final BossManager bossManager;

    public BossMiniEvent(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    @Override
    public String getId() {
        return "boss";
    }

    @Override
    public String getDisplayName() {
        return "§5§lПоявление Босса";
    }

    @Override
    public int getDefaultDurationSeconds() {
        return 180;
    }

    @Override
    public boolean canStart() {
        return bossManager != null && bossManager.canStart();
    }

    @Override
    public void onStart() {
        if (bossManager == null || !bossManager.spawnBoss()) {
            Bukkit.broadcastMessage("§6AMCEggVoid | §cНе удалось заспавнить босса (неподходящий мир или нет игроков).");
        }
    }

    @Override
    public void onStop() {
        if (bossManager != null && bossManager.getActiveBossEntity() != null) {
            bossManager.despawnActiveBoss();
            Bukkit.broadcastMessage("§6AMCEggVoid | §7Время босса вышло — он исчез в пустоте.");
        }
    }
}
