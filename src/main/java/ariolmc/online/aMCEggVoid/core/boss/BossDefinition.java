package ariolmc.online.aMCEggVoid.core.boss;

import ariolmc.online.aMCEggVoid.core.cfg.LootEntry;
import lombok.Getter;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.List;

@Getter
public class BossDefinition {

    private final String id;
    private final EntityType entityType;
    private final String name;
    private final double maxHealth;
    private final double scale;
    private final int platformRadius;
    private final BarColor barColor;
    private final List<String> abilities;
    private final List<LootEntry> loot;

    public BossDefinition(
            String id,
            EntityType entityType,
            String name,
            double maxHealth,
            double scale,
            int platformRadius,
            BarColor barColor,
            List<String> abilities,
            List<LootEntry> loot
    ) {
        this.id = id;
        this.entityType = entityType;
        this.name = name;
        this.maxHealth = maxHealth;
        this.scale = scale;
        this.platformRadius = platformRadius;
        this.barColor = barColor;
        this.abilities = abilities == null ? List.of() : Collections.unmodifiableList(abilities);
        this.loot = loot == null ? List.of() : Collections.unmodifiableList(loot);
    }
}
