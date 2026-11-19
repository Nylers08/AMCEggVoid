package ariolmc.online.aMCEggVoid.core.pvp;

import ariolmc.online.aMCEggVoid.core.MemberList;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Отдельный менеджер управления состоянием PvP между участниками ивента.
 */
public class PvpManager {

    private final MemberList memberList;
    @Getter
    private final PvpCfg cfg;
    @Getter
    private boolean pvpEnabled;
    private final Map<UUID, Long> notifyCooldowns = new ConcurrentHashMap<>();

    public PvpManager(MemberList memberList) {
        this.memberList = memberList;
        this.cfg = new PvpCfg();
        this.cfg.load();
        this.pvpEnabled = cfg.isPvpEnabled();
    }

    public void reloadCfg() {
        cfg.load();
        this.pvpEnabled = cfg.isPvpEnabled();
    }

    public void enablePvp() {
        this.pvpEnabled = true;
    }

    public void disablePvp() {
        this.pvpEnabled = false;
    }

    public void setPvpEnabled(boolean enabled) {
        this.pvpEnabled = enabled;
    }

    public boolean isAttackAllowed(Player attacker, Player victim) {
        if (pvpEnabled || attacker.equals(victim)) {
            return true;
        }
        return !(memberList.contains(attacker.getName()) && memberList.contains(victim.getName()));
    }

    public void notifyDenied(Player attacker) {
        String msg = cfg.getDenyMessage();
        if (msg == null || msg.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = cfg.getMessageCooldownMs();
        Long last = notifyCooldowns.get(attacker.getUniqueId());
        if (last == null || now - last >= cooldown) {
            notifyCooldowns.put(attacker.getUniqueId(), now);
            attacker.sendMessage(msg);
        }
    }
}
