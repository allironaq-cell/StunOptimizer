package com.shieldstun;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;

public final class HotbarScanner {
    public record Result(int sword, int axe) {}

    public static Result scan(PlayerInventory inv) {
        int bestSword = -1, bestSwordRank = -1;
        int bestAxe = -1, bestAxeRank = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            int rank = tierRank(s);
            if (s.isIn(ItemTags.SWORDS) && rank > bestSwordRank) {
                bestSword = i; bestSwordRank = rank;
            }
            if (s.isIn(ItemTags.AXES) && rank > bestAxeRank) {
                bestAxe = i; bestAxeRank = rank;
            }
        }
        return new Result(bestSword, bestAxe);
    }

    /** Ranks by material using the item id (compile-safe across mapping changes). */
    private static int tierRank(ItemStack s) {
        String id = Registries.ITEM.getId(s.getItem()).getPath();
        if (id.startsWith("netherite")) return 50;
        if (id.startsWith("diamond")) return 40;
        if (id.startsWith("iron")) return 30;
        if (id.startsWith("copper")) return 25;
        if (id.startsWith("stone")) return 20;
        if (id.startsWith("golden")) return 5;
        if (id.startsWith("wooden")) return 4;
        return 1;
    }
}
