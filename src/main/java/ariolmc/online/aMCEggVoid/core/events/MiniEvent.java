package ariolmc.online.aMCEggVoid.core.events;

/**
 * Контракт дополнительного мини-ивента внутри основного конкурса.
 */
public interface MiniEvent {

    String getId();

    String getDisplayName();

    int getDefaultDurationSeconds();

    default boolean isRevealInCountdownByDefault() {
        return true;
    }

    /**
     * Проверяет, может ли ивент начать обратный отсчёт и запуститься прямо сейчас.
     */
    default boolean canStart() {
        return true;
    }

    void onStart();

    /**
     * Вызывается каждую секунду обратного отсчёта до конца активного ивента.
     */
    default void onEndCountdownTick(int secondsLeft) {
    }

    void onStop();
}
