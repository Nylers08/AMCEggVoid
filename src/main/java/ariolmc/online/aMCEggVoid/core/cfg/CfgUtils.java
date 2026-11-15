package ariolmc.online.aMCEggVoid.core.cfg;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

public class CfgUtils {

    public static void saveConfig(){
        AMCEggVoid.getInstance().saveConfig();
    }

    public static void reloadConfig(){
        AMCEggVoid.getInstance().reloadConfig();
    }

    public static FileConfiguration getConfig(){
        return AMCEggVoid.getInstance().getConfig();
    }

    public static Sound loadSound(String path, Sound defaultSound){
        String soundName = getConfig().getString(path);
        if(soundName == null || soundName.isBlank()){
            return defaultSound;
        }
        NamespacedKey key = NamespacedKey.fromString(soundName);
        if(key == null){
            return defaultSound;
        }
        Sound sound = Registry.SOUNDS.get(key);
        return sound == null ? defaultSound : sound;
    }
}
