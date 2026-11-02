package com.shieldstun;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeybindHandler {
    private static KeyBinding toggleKey;
    private static KeyBinding comboKey;
    private static boolean enabled = false;

    public static void register() {
        // 1.21.9+: categories are objects. Translation key = key.category.shieldstun.main
        KeyBinding.Category category =
                KeyBinding.Category.create(Identifier.of(ShieldStunClient.MOD_ID, "main"));

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shieldstun.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, category));
        comboKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shieldstun.combo", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, category));

        ClientTickEvents.END_CLIENT_TICK.register(KeybindHandler::onTick);
    }

    private static void onTick(MinecraftClient mc) {
        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            if (!enabled) ComboStateMachine.cancel(mc);
            Messages.show(mc, "ShieldStun: " + (enabled ? "ON" : "OFF"), true);
        }

        if (enabled) {
            if (ConfigManager.cfg.repeatWhileHeld) {
                while (comboKey.wasPressed()) { /* drain queued presses */ }
                if (comboKey.isPressed() && !ComboStateMachine.isRunning()) {
                    ComboStateMachine.start(mc);
                }
            } else {
                while (comboKey.wasPressed()) {
                    ComboStateMachine.start(mc);
                }
            }
        } else {
            while (comboKey.wasPressed()) { /* ignore while OFF */ }
        }

        ComboStateMachine.tick(mc);
    }
}
