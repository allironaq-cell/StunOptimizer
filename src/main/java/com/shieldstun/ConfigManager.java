package com.shieldstun;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    public static final class Config {
        public int stepDelayTicks = 1;            // ticks between steps (min 0)
        public boolean waitForCooldown = true;    // wait for attack cooldown before each hit
        public double cooldownThreshold = 0.9;    // 0..1 cooldown progress needed
        public boolean forceFastMode = false;     // ignore cooldown, hit at stepDelayTicks pace
        public boolean restoreSlot = true;        // go back to original slot at end
        public boolean sprintReset = true;        // stop+restart sprint around sword hit
        public boolean criticalHit = false;       // only sword-hit while falling
        public boolean onlyWhenTargetBlocking = false;
        public boolean repeatWhileHeld = false;
        public boolean showMessages = true;
        public boolean allowAxeOnly = false;      // allow combo with only an axe
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("shieldstun.json");

    public static Config cfg = new Config();

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                Config loaded = GSON.fromJson(Files.readString(PATH), Config.class);
                if (loaded != null) cfg = loaded;
            }
        } catch (Exception e) {
            System.err.println("[ShieldStun] Failed to read config, using defaults: " + e);
            cfg = new Config();
        }
        cfg.stepDelayTicks = Math.max(0, cfg.stepDelayTicks);
        cfg.cooldownThreshold = Math.min(1.0, Math.max(0.0, cfg.cooldownThreshold));
        save();
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(cfg));
        } catch (IOException e) {
            System.err.println("[ShieldStun] Failed to save config: " + e);
        }
    }
}
