package ariolmc.online.aMCEggVoid.core.events.impl;

import ariolmc.online.aMCEggVoid.core.MemberList;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Мини-ивент «Прыгучесть»: накладывает на игроков эффект прыгучести на время ивента.
 */
public class JumpBoostMiniEvent extends PeriodicEffectMiniEvent {

    public JumpBoostMiniEvent(Plugin plugin, MemberList memberList) {
        super(plugin, memberList);
    }

    @Override
    public String getId() {
        return "jump_boost";
    }

    @Override
    public String getDisplayName() {
        return "§a§lСупер-Прыжок";
    }

    @Override
    public int getDefaultDurationSeconds() {
        return 45;
    }

    @Override
    protected List<EffectSpec> resolveEffects(FileConfiguration cfg) {
        int amplifier = Math.max(0, cfg.getInt("mini-events.events.jump_boost.amplifier", 2));
        return List.of(new EffectSpec(PotionEffectType.JUMP_BOOST, amplifier));
    }

    @Override
    protected String getStartMessage() {
        return "§6AMCEggVoid | §a§lВключён режим «Супер-Прыжок»! §eПрыгайте выше облаков!";
    }

    @Override
    protected String getStopMessage() {
        return "§6AMCEggVoid | §7Эффект прыгучести закончился.";
    }

    @Override
    protected Sound getStartSound() {
        return Sound.ENTITY_RABBIT_JUMP;
    }

    @Override
    protected float getStartSoundPitch() {
        return 1.15f;
    }
}
