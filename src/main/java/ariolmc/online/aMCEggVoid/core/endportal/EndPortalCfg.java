package ariolmc.online.aMCEggVoid.core.endportal;

import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class EndPortalCfg {

    private final Map<Material, Integer> requirements = new LinkedHashMap<>();
    private final Map<Material, String> customNames = new HashMap<>();

    private double lineSpacing = 0.28;
    private double baseYOffset = 1.3;
    private String requirementLine = "§f{name}: §e{collected}§7/§e{required} §7(осталось §c{left}§7)";
    private String requirementDoneLine = "§a✔ {name}: §a{collected}/{required}";
    private List<String> progressLines = new ArrayList<>();
    private List<String> openedLines = new ArrayList<>();

    private static final Map<Material, String> RU_NAMES = Map.ofEntries(
            Map.entry(Material.BLAZE_ROD, "Стержень ифрита"),
            Map.entry(Material.BLAZE_POWDER, "Огненный порошок"),
            Map.entry(Material.ENDER_PEARL, "Жемчуг Эндера"),
            Map.entry(Material.ENDER_EYE, "Око Эндера"),
            Map.entry(Material.OBSIDIAN, "Обсидиан"),
            Map.entry(Material.CRYING_OBSIDIAN, "Плачущий обсидиан"),
            Map.entry(Material.DIAMOND, "Алмаз"),
            Map.entry(Material.DIAMOND_BLOCK, "Алмазный блок"),
            Map.entry(Material.IRON_INGOT, "Железный слиток"),
            Map.entry(Material.IRON_BLOCK, "Железный блок"),
            Map.entry(Material.GOLD_INGOT, "Золотой слиток"),
            Map.entry(Material.GOLD_BLOCK, "Золотой блок"),
            Map.entry(Material.EMERALD, "Изумруд"),
            Map.entry(Material.NETHERITE_INGOT, "Незеритовый слиток"),
            Map.entry(Material.NETHERITE_SCRAP, "Незеритовый лом"),
            Map.entry(Material.ANCIENT_DEBRIS, "Древние обломки"),
            Map.entry(Material.GHAST_TEAR, "Слеза гаста"),
            Map.entry(Material.NETHER_STAR, "Звезда Незера")
    );

    public void load() {
        FileConfiguration cfg = CfgUtils.getConfig();

        lineSpacing = cfg.getDouble("end-portal.hologram.line-spacing", 0.28);
        baseYOffset = cfg.getDouble("end-portal.hologram.base-y-offset", 1.3);
        requirementLine = color(cfg.getString(
                "end-portal.hologram.requirement-line",
                "§f{name}: §e{collected}§7/§e{required} §7(осталось §c{left}§7)"
        ));
        requirementDoneLine = color(cfg.getString(
                "end-portal.hologram.requirement-done-line",
                "§a✔ {name}: §a{collected}/{required}"
        ));

        progressLines = new ArrayList<>();
        List<String> cfgProgress = cfg.getStringList("end-portal.hologram.progress-lines");
        if (cfgProgress.isEmpty()) {
            progressLines.add("§5§lАЛТАРЬ ЭНДА §7(§d{eyes}§7/§d12 §7очей • §e{percent}%§7)");
            progressLines.add("{requirements}");
            progressLines.add("§eБросайте нужные ресурсы внутрь портала!");
        } else {
            for (String line : cfgProgress) {
                progressLines.add(color(line));
            }
        }

        openedLines = new ArrayList<>();
        List<String> cfgOpened = cfg.getStringList("end-portal.hologram.opened-lines");
        if (cfgOpened.isEmpty()) {
            openedLines.add("§5§lАЛТАРЬ ЭНДА §a§lОТКРЫТ!");
            openedLines.add("§7Прогресс: §a12§7/§a12 §7очей (§a100%§7)");
            openedLines.add("§dПрыгайте в портал!");
        } else {
            for (String line : cfgOpened) {
                openedLines.add(color(line));
            }
        }

        customNames.clear();
        ConfigurationSection namesSection = cfg.getConfigurationSection("end-portal.item-names");
        if (namesSection != null) {
            for (String key : namesSection.getKeys(false)) {
                Material mat = Material.matchMaterial(key.trim().toUpperCase());
                String name = namesSection.getString(key);
                if (mat != null && name != null) {
                    customNames.put(mat, color(name));
                }
            }
        }

        requirements.clear();
        ConfigurationSection reqSection = cfg.getConfigurationSection("end-portal.requirements");
        if (reqSection != null) {
            for (String key : reqSection.getKeys(false)) {
                Material mat = Material.matchMaterial(key.trim().toUpperCase());
                int amount = reqSection.getInt(key, 0);
                if (mat != null && mat.isItem() && amount > 0) {
                    requirements.put(mat, amount);
                }
            }
        }

        if (requirements.isEmpty()) {
            requirements.put(Material.BLAZE_ROD, 40);
            requirements.put(Material.BLAZE_POWDER, 40);
            requirements.put(Material.ENDER_PEARL, 40);
            requirements.put(Material.OBSIDIAN, 80);
            requirements.put(Material.GOLD_INGOT, 64);
        }
    }

    public Map<Material, Integer> getRequirements() {
        return Collections.unmodifiableMap(requirements);
    }

    public String getDisplayName(Material material) {
        String custom = customNames.get(material);
        if (custom != null) {
            return custom;
        }
        return RU_NAMES.getOrDefault(material, material.name());
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
