package com.shieldstun;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class Messages {
    /** Shows an action-bar message. force=true ignores the showMessages config. */
    public static void show(MinecraftClient mc, String msg, boolean force) {
        if (mc.player == null) return;
        if (!force && !ConfigManager.cfg.showMessages) return;
        mc.player.sendMessage(Text.literal(msg), true);
    }

    public static void show(MinecraftClient mc, String msg) {
        show(mc, msg, false);
    }
}
