package com.gathin.tvhome;

/** Coalesces Home/visibility events into one wallpaper change, without a timer or queue. */
final class WallpaperHomeRefresh {
    private final boolean enabled;
    private boolean resumed;
    private boolean pending;

    WallpaperHomeRefresh(boolean enabled) {
        this.enabled = enabled;
    }

    void onResume() {
        if (!enabled) return;
        // The constructor already selected the first picture. Later resumes include waking the TV.
        if (resumed) pending = true;
        resumed = true;
    }

    void onHomeIntent() {
        if (enabled) pending = true;
    }

    boolean consumeChange() {
        boolean change = pending;
        pending = false;
        return change;
    }

    boolean hasPendingChange() {
        return pending;
    }
}
