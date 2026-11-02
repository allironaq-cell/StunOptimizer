# ShieldStun Combo (Fabric, MC 1.21.11, client-only)

Press the combo key near a target: sword hit -> axe hit (breaks shield) -> back to original slot.

## Keybinds (Options > Controls > Key Binds > "ShieldStun Combo")
- Toggle Mod: M (ON/OFF, shows action-bar message)
- Run Combo: F (only works while ON)

## Config: config/shieldstun.json
stepDelayTicks, waitForCooldown, cooldownThreshold, forceFastMode, restoreSlot,
sprintReset, criticalHit, onlyWhenTargetBlocking, repeatWhileHeld, showMessages, allowAxeOnly

## Build
Local: install Java 21 + Gradle 8.14+, run `gradle wrapper` once, then `./gradlew build`.
GitHub: push the repo; Actions builds it and uploads the jar as an artifact.
Output: build/libs/shieldstun-combo-1.0.0.jar (not the -sources jar). Needs Fabric API.

## WARNING
This automates combat actions. Only use it on servers/worlds where macros are allowed;
many servers ban it.
