package ariolmc.online.aMCEggVoid.core.cfg;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

public class ItemGiverCfg {

    @Getter
    private Sound sound = Sound.BLOCK_NOTE_BLOCK_BELL;

    @Getter
    private float volume = 1f;

    @Getter
    private float pitch = 1f;

    public void load(){
        FileConfiguration cfg = AMCEggVoid.getInstance().getConfig();
        volume = (float) cfg.getDouble("item-giver.volume", 1.0);
        pitch = (float) cfg.getDouble("item-giver.pitch", 1.0);
        sound = CfgUtils.loadSound("item-giver.sound", Sound.BLOCK_NOTE_BLOCK_BELL);
    }
}
