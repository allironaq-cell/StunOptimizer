package com.shieldstun;

import com.shieldstun.ConfigManager.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

/**
 * IDLE -> SWORD_SWITCH -> SWORD_HIT -> AXE_SWITCH -> AXE_HIT -> RESTORE -> IDLE
 *
 * COOLDOWN NOTES: in vanilla 1.9+ combat, changing the held item resets the attack
 * cooldown, so a hit right after a switch is a weak hit (less damage, no crit/sweep).
 *  - waitForCooldown=true : each hit waits until getAttackCooldownProgress >= threshold
 *                           (strong hits, but the combo is slower).
 *  - forceFastMode=true   : ignores cooldown, hits at the stepDelayTicks pace.
 */
public final class ComboStateMachine {
    public static final double REACH = 3.0;
    private static final double REACH_LENIENCY = 0.6; // crosshair hits box edge, we measure to center
    private static final int MAX_WAIT_TICKS = 40;

    private enum State { IDLE, SWORD_SWITCH, SWORD_HIT, AXE_SWITCH, AXE_HIT, RESTORE }

    private static State state = State.IDLE;
    private static int delay, waited;
    private static int swordSlot = -1, axeSlot = -1, originalSlot = -1;
    private static LivingEntity target;
    private static boolean wasSprinting;

    public static boolean isRunning() { return state != State.IDLE; }

    public static void start(MinecraftClient mc) {
        if (isRunning()) return;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        Config c = ConfigManager.cfg;

        HotbarScanner.Result r = HotbarScanner.scan(p.getInventory());
        boolean noSword = r.sword() < 0;
        if (r.axe() < 0 || (noSword && !c.allowAxeOnly)) {
            Messages.show(mc, "No sword/axe in hotbar", true);
            return;
        }

        LivingEntity t = TargetSelector.pick(mc, p);
        if (t == null) { Messages.show(mc, "No target"); return; }
        if (c.onlyWhenTargetBlocking && !t.isBlocking()) {
            Messages.show(mc, "Target is not blocking");
            return;
        }

        target = t;
        swordSlot = r.sword();
        axeSlot = r.axe();
        originalSlot = p.getInventory().getSelectedSlot();
        delay = 0;
        waited = 0;
        wasSprinting = false;
        state = noSword ? State.AXE_SWITCH : State.SWORD_SWITCH;
    }

    public static void cancel(MinecraftClient mc) {
        if (isRunning()) abort(mc, null);
    }

    public static void tick(MinecraftClient mc) {
        if (state == State.IDLE) return;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) { reset(); return; }
        if (!p.isAlive()) { reset(); return; }
        if (mc.currentScreen != null) { abort(mc, "Combo aborted: screen opened"); return; }
        if (target == null || !target.isAlive() || target.isRemoved()) { abort(mc, "Target gone"); return; }
        if (p.distanceTo(target) > REACH + REACH_LENIENCY) { abort(mc, "Target out of reach"); return; }
        if (delay > 0) { delay--; return; }

        Config c = ConfigManager.cfg;
        switch (state) {
            case SWORD_SWITCH -> {
                selectSlot(p, swordSlot);
                // Sprint reset: drop sprint now, restore it right before the hit so the
                // server sees a fresh sprint-hit (extra knockback). Best with stepDelayTicks >= 1.
                if (c.sprintReset && p.isSprinting()) {
                    p.setSprinting(false);
                    wasSprinting = true;
                }
                state = State.SWORD_HIT;
                delay = c.stepDelayTicks;
            }
            case SWORD_HIT -> {
                if (!cooldownReady(p)) { if (++waited > MAX_WAIT_TICKS) abort(mc, "Cooldown timeout"); return; }
                if (c.criticalHit && !canCrit(p)) { if (++waited > MAX_WAIT_TICKS) abort(mc, "Not falling - no crit"); return; }
                if (wasSprinting && mc.options.forwardKey.isPressed()) p.setSprinting(true);
                hit(mc, p);
                waited = 0;
                state = State.AXE_SWITCH;
                delay = c.stepDelayTicks;
            }
            case AXE_SWITCH -> {
                selectSlot(p, axeSlot);
                state = State.AXE_HIT;
                delay = c.stepDelayTicks;
            }
            case AXE_HIT -> {
                if (!cooldownReady(p)) { if (++waited > MAX_WAIT_TICKS) abort(mc, "Cooldown timeout"); return; }
                hit(mc, p);
                waited = 0;
                state = State.RESTORE;
                delay = c.stepDelayTicks;
            }
            case RESTORE -> {
                if (c.restoreSlot) selectSlot(p, originalSlot);
                reset();
            }
            default -> { }
        }
    }

    private static boolean cooldownReady(ClientPlayerEntity p) {
        Config c = ConfigManager.cfg;
        if (c.forceFastMode || !c.waitForCooldown) return true;
        return p.getAttackCooldownProgress(0.5f) >= c.cooldownThreshold;
    }

    private static boolean canCrit(ClientPlayerEntity p) {
        return !p.isOnGround() && p.getVelocity().y < 0 && !p.isClimbing() && !p.isTouchingWater();
    }

    /** Real attack + visible swing. */
    private static void hit(MinecraftClient mc, ClientPlayerEntity p) {
        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
    }

    /** Change slot client-side (first-person hand swaps) and tell the server the same tick. */
    private static void selectSlot(ClientPlayerEntity p, int slot) {
        if (slot < 0 || slot > 8) return;
        p.getInventory().setSelectedSlot(slot);
        if (p.networkHandler != null) {
            p.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private static void abort(MinecraftClient mc, String msg) {
        ClientPlayerEntity p = mc.player;
        if (p != null && ConfigManager.cfg.restoreSlot && originalSlot >= 0) selectSlot(p, originalSlot);
        if (msg != null) Messages.show(mc, msg);
        reset();
    }

    private static void reset() {
        state = State.IDLE;
        target = null;
        delay = 0;
        waited = 0;
        wasSprinting = false;
    }
}
