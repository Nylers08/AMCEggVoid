package ariolmc.online.aMCEggVoid.core.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Реестр доступных дополнительных ивентов.
 */
public class MiniEventRegistry {

    private final Map<String, MiniEvent> events = new LinkedHashMap<>();
    private final Random random = new Random();

    public void register(MiniEvent event) {
        if (event != null) {
            events.put(event.getId().toLowerCase(), event);
        }
    }

    public MiniEvent get(String id) {
        return id == null ? null : events.get(id.toLowerCase());
    }

    public Collection<MiniEvent> getAll() {
        return Collections.unmodifiableCollection(events.values());
    }

    public MiniEvent getRandom() {
        if (events.isEmpty()) {
            return null;
        }
        List<MiniEvent> list = new ArrayList<>(events.values());
        return list.get(random.nextInt(list.size()));
    }
}
