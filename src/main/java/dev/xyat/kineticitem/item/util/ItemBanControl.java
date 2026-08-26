package dev.xyat.kineticitem.item.util;

import java.util.function.Supplier;

public class ItemBanControl {
    // 默认关闭，直到 WorldLoadEventHandler 将其开启
    private static volatile boolean replacementEnabled = false;

    private static final ThreadLocal<Boolean> SKIP_REPLACEMENT = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> BUILDING_TAB = ThreadLocal.withInitial(() -> false);

    public static boolean isReplacementEnabled() {
        return replacementEnabled;
    }

    public static void setReplacementEnabled(boolean enabled) {
        replacementEnabled = enabled;
    }

    public static boolean shouldSkip() {
        // 如果全局替换未开启，或者处于局部跳过状态，则全部跳过
        return !replacementEnabled || SKIP_REPLACEMENT.get() || BUILDING_TAB.get();
    }

    public static <T> void withSkip(Supplier<T> action) {
        boolean old = SKIP_REPLACEMENT.get();
        SKIP_REPLACEMENT.set(true);
        try {
            action.get();
        } finally {
            SKIP_REPLACEMENT.set(old);
        }
    }

    public static void beginBuildingTab() { BUILDING_TAB.set(true); }
    public static void endBuildingTab() { BUILDING_TAB.set(false); }
}