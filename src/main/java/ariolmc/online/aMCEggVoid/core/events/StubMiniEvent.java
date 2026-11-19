package ariolmc.online.aMCEggVoid.core.events;

import org.bukkit.Bukkit;

/**
 * Заготовка под будущий дополнительный ивент.
 */
public class StubMiniEvent implements MiniEvent {

    private final String id;
    private final String displayName;
    private final int defaultDurationSeconds;

    public StubMiniEvent(String id, String displayName, int defaultDurationSeconds) {
        this.id = id.toLowerCase();
        this.displayName = displayName;
        this.defaultDurationSeconds = defaultDurationSeconds;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int getDefaultDurationSeconds() {
        return defaultDurationSeconds;
    }

    @Override
    public void onStart() {
        Bukkit.broadcastMessage("§6AMCEggVoid | §e[Заготовка] Ивент " + displayName + " §eзапущен!");
    }

    @Override
    public void onStop() {
        Bukkit.broadcastMessage("§6AMCEggVoid | §7[Заготовка] Ивент " + displayName + " §7завершён.");
    }
}
