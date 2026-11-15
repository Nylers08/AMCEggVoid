package ariolmc.online.aMCEggVoid.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemStackBuilder {

    private String name = "";
    private String materialName = "STONE";
    private int amount = 1;
    private List<String> stringLore = new ArrayList<>();

    public ItemStackBuilder name(String name){
        this.name = name;
        return this;
    }

    public ItemStackBuilder materialName(String materialName){
        this.materialName = materialName;
        return this;
    }

    public ItemStackBuilder amount(int amount){
        if(amount < 1){
            amount = 1;
        } else if (amount > 64) {
            amount = 64;
        }

        this.amount = amount;
        return this;
    }

    public ItemStackBuilder lore(List<String> lore){
        this.stringLore = lore;
        return this;
    }

    public ItemStack build(){
        Material material = buildMaterial();
        ItemStack itemStack = new ItemStack(material, amount);
        setMeta(itemStack);
        return itemStack;
    }


    private Material buildMaterial(){
        Material material = Material.getMaterial(materialName);
        if(material == null){
            material = Material.STONE;
        }
        return material;
    }

    private void setMeta(ItemStack itemStack){
        ItemMeta meta = itemStack.getItemMeta();
        setName(meta);
        setLore(meta);
        itemStack.setItemMeta(meta);
    }

    private void setName(ItemMeta meta){
        if(!name.isEmpty()){
            Component title = Component.text(name);
            meta.displayName(title);
        }
    }

    private void setLore(ItemMeta meta){
        if(stringLore != null && !stringLore.isEmpty()){
            List<Component> lore = new ArrayList<>();
            stringLore.forEach(str->lore.add(Component.text(str)));
            meta.lore(lore);
        }
    }
}
