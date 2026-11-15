package ariolmc.online.aMCEggVoid.core;

/**
 * Централизованный класс состояния ивента.
 * Все игровые механики плагина проверяют EventState.isRunning() перед срабатыванием.
 */
public final class EventState {

    private static volatile boolean running = false;

    private EventState() {
    }

    public static boolean isRunning() {
        return running;
    }

    public static void setRunning(boolean running) {
        EventState.running = running;
    }

    public static void start() {
        running = true;
    }

    public static void stop() {
        running = false;
    }
}
