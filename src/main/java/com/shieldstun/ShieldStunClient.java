package com.shieldstun;

import net.fabricmc.api.ClientModInitializer;

public class ShieldStunClient implements ClientModInitializer {
    public static final String MOD_ID = "shieldstun";

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        KeybindHandler.register();
    }
}
