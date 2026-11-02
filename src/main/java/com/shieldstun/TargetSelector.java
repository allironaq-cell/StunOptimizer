package com.shieldstun;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

public final class TargetSelector {
    private static final double FOV_DOT = 0.75; // ~41 degree half-angle

    public static LivingEntity pick(MinecraftClient mc, ClientPlayerEntity p) {
        // 1) Crosshair target
        if (mc.crosshairTarget instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity le
                && valid(p, le)) {
            return le;
        }
        if (mc.world == null) return null;

        // 2) Closest player in reach and in front of us; blocking players preferred
        Vec3d look = p.getRotationVec(1.0f).normalize();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (PlayerEntity other : mc.world.getPlayers()) {
            if (!valid(p, other)) continue;
            double dist = p.distanceTo(other);
            if (dist > ComboStateMachine.REACH) continue;
            Vec3d to = other.getBoundingBox().getCenter().subtract(p.getEyePos()).normalize();
            if (look.dotProduct(to) < FOV_DOT) continue;
            double score = dist + (other.isBlocking() ? 0 : 100);
            if (score < bestScore) { bestScore = score; best = other; }
        }
        return best;
    }

    private static boolean valid(ClientPlayerEntity self, LivingEntity e) {
        return e != self && e.isAlive() && !e.isSpectator();
    }
}
