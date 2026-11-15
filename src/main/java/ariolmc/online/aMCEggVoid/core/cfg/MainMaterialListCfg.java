package ariolmc.online.aMCEggVoid.core.cfg;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class MainMaterialListCfg {

    @Getter
    private List<Material> nonDropMaterials = new ArrayList<>();

    public void load() {
        FileConfiguration cfg = CfgUtils.getConfig();
        List<String> materialNames = cfg.getStringList("non-drop-items");
        List<Material> loaded = new ArrayList<>();

        for (String name : materialNames) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                loaded.add(material);
            }
        }

        this.nonDropMaterials = loaded;
    }
}
