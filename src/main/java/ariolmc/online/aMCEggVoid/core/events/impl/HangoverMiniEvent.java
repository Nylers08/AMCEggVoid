package ariolmc.online.aMCEggVoid.core.events.impl;

import ariolmc.online.aMCEggVoid.core.MemberList;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Мини-ивент «Похмелье»: накладывает на игроков эффекты тошноты и слепоты на время ивента.
 */
public class HangoverMiniEvent extends PeriodicEffectMiniEvent {

    public HangoverMiniEvent(Plugin plugin, MemberList memberList) {
        super(plugin, memberList);
    }

    @Override
    public String getId() {
        return "hangover";
    }

    @Override
    public String getDisplayName() {
        return "§2§lПохмелье";
    }

    @Override
    public int getDefaultDurationSeconds() {
        return 45;
    }

    @Override
    protected List<EffectSpec> resolveEffects(FileConfiguration cfg) {
        int nauseaAmp = Math.max(0, cfg.getInt("mini-events.events.hangover.nausea-amplifier", 0));
        int blindnessAmp = Math.max(0, cfg.getInt("mini-events.events.hangover.blindness-amplifier", 0));
        return List.of(
                new EffectSpec(PotionEffectType.NAUSEA, nauseaAmp),
                new EffectSpec(PotionEffectType.BLINDNESS, blindnessAmp)
        );
    }

    @Override
    protected String getStartMessage() {
        return "§6AMCEggVoid | §2§lНаступило Похмелье! §eГолова кружится, в глазах темнеет...";
    }

    @Override
    protected String getStopMessage() {
        return "§6AMCEggVoid | §aПохмелье прошло! §7Голова наконец прояснилась.";
    }

    @Override
    protected Sound getStartSound() {
        return Sound.ENTITY_PLAYER_BURP;
    }

    @Override
    protected float getStartSoundPitch() {
        return 0.75f;
    }
}
