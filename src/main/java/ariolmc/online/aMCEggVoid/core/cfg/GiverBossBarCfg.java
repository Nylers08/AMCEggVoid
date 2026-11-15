package ariolmc.online.aMCEggVoid.core.cfg;

import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

public class GiverBossBarCfg {

    @Getter
    private Sound sound = Sound.BLOCK_NOTE_BLOCK_BIT;

    @Getter
    private float volume = 1;

    @Getter
    private float pitch = 0.5f;

    @Getter
    private int secToPlaySound = 3;

    @Getter
    private long delayTickUpdate = 20;

    @Getter
    private String textTimeInfo = "Время до новых предметов: %sс";

    @Getter
    private String textGiveItem = "Предметы выданы!";


    public void load(){
        FileConfiguration cfg = CfgUtils.getConfig();

        secToPlaySound = cfg.getInt("boss-bar.sec-to-sound");
        volume = (float) cfg.getDouble("boss-bar.volume");
        pitch = (float) cfg.getDouble("boss-bar.pitch");
        delayTickUpdate = cfg.getLong("boss-bar.delay-tick-update");

        sound = CfgUtils.loadSound("boss-bar.sound", Sound.BLOCK_NOTE_BLOCK_BIT);

        textTimeInfo = cfg.getString("boss-bar.text-time-info");
        textGiveItem = cfg.getString("boss-bar.text-give-item");
    }
}
