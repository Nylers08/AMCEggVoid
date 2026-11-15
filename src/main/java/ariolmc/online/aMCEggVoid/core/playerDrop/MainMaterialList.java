package ariolmc.online.aMCEggVoid.core.playerDrop;

import ariolmc.online.aMCEggVoid.core.cfg.MainMaterialListCfg;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class MainMaterialList {

    private final MainMaterialListCfg cfg = new MainMaterialListCfg();
    private List<Material> materials = new ArrayList<>();

    public MainMaterialList() {
        reload();
    }

    public void reload() {
        cfg.load();
        this.materials = new ArrayList<>(cfg.getNonDropMaterials());
    }

    public void add(Material material) {
        materials.add(material);
    }

    public void remove(Material material) {
        materials.remove(material);
    }

    public boolean contains(Material material) {
        return materials.contains(material);
    }

    public List<Material> getMaterials() {
        return new ArrayList<>(materials);
    }

    public void clear() {
        materials.clear();
    }
}
