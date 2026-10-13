package com.fredplenter.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server-side effects of the wand spells.
 * <p>
 * {@link #apply} returns {@code false} when the spell had nothing to act on
 * (for example no creature in front of the caster). In that case the wand is
 * not charged.
 */
public final class SpellEffects {

    private static final double TARGET_RANGE = 12.0;
    private static final double TARGET_ALIGNMENT = 0.93;

    private SpellEffects() {
    }

    public static boolean apply(Spell spell, ServerPlayer player) {
        return switch (spell) {
            case GUST -> gust(player);
            case PYRE -> pyre(player);
            case TIDE -> tide(player);
            case RADIANCE -> radiance(player);
            case VERDURE -> verdure(player);
            case DREAD -> dread(player);
        };
    }

    /** Knocks back every creature in a wide cone in front of the caster. */
    private static boolean gust(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        List<LivingEntity> targets = inCone(player, 5.0, 0.5);
        if (targets.isEmpty()) {
            return false;
        }
        for (LivingEntity target : targets) {
            target.push(look.x * 1.1, 0.45, look.z * 1.1);
            level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(),
                    6, 0.3, 0.3, 0.3, 0.05);
        }
        return true;
    }

    /** Sets the nearest creature in the aim line on fire. */
    private static boolean pyre(ServerPlayer player) {
        LivingEntity target = findTarget(player);
        if (target == null) {
            return false;
        }
        target.igniteForSeconds(5.0F);
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.FLAME,
                target.getX(), target.getY() + 1.0, target.getZ(), 14, 0.3, 0.5, 0.3, 0.02);
        return true;
    }

    /** Slows the aimed creature and puts out its fire. */
    private static boolean tide(ServerPlayer player) {
        LivingEntity target = findTarget(player);
        if (target == null) {
            return false;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
        target.setRemainingFireTicks(0);
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.SPLASH,
                target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
        return true;
    }

    /** Heals the caster and leaves a short regeneration trail. Always castable. */
    private static boolean radiance(ServerPlayer player) {
        player.heal(4.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.4, 0.6, 0.4, 0.02);
        return true;
    }

    /**
     * Speeds up up to twelve nearby plants by running their random-tick growth
     * a few extra times, which is how crops and saplings grow naturally.
     */
    private static boolean verdure(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 1, 3))) {
            BlockPos target = pos.immutable();
            BlockState state = level.getBlockState(target);
            if (state.getBlock() instanceof BonemealableBlock && state.isRandomlyTicking()) {
                for (int tick = 0; tick < 3; tick++) {
                    state.randomTick(level, target, level.random);
                }
                grown++;
                if (grown >= 12) {
                    break;
                }
            }
        }
        if (grown == 0) {
            return false;
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(),
                20, 2.0, 0.5, 2.0, 0.0);
        return true;
    }

    /** Weakens and blinds the aimed creature. */
    private static boolean dread(ServerPlayer player) {
        LivingEntity target = findTarget(player);
        if (target == null) {
            return false;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.SMOKE,
                target.getX(), target.getY() + 1.0, target.getZ(), 18, 0.3, 0.5, 0.3, 0.02);
        return true;
    }

    /** The living creature closest to the caster's aim line, or {@code null}. */
    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : inCone(player, TARGET_RANGE, TARGET_ALIGNMENT)) {
            double distance = entity.getBoundingBox().getCenter().distanceTo(eye);
            if (distance < bestDistance) {
                best = entity;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static List<LivingEntity> inCone(ServerPlayer player, double range, double minAlignment) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox().inflate(range);
        return player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive()
                        && isInCone(eye, look, entity, range, minAlignment));
    }

    private static boolean isInCone(Vec3 eye, Vec3 look, LivingEntity entity, double range, double minAlignment) {
        Vec3 toTarget = entity.getBoundingBox().getCenter().subtract(eye);
        double distance = toTarget.length();
        if (distance < 0.1 || distance > range) {
            return false;
        }
        return toTarget.normalize().dot(look) >= minAlignment;
    }
}
