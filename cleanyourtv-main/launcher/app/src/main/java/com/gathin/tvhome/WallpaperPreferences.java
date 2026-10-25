package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.List;

/** On-TV overrides share the launcher's existing store and change counter. */
final class WallpaperPreferences {
    static final String RANDOM = "wallpaper_random";
    static final String DIM = "wallpaper_dim";
    static final String REQUESTED = "wallpaper_requested";
    private final SharedPreferences choices;
    private final SharedPreferences playback;

    WallpaperPreferences(Context context) {
        this(context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE),
                context.getSharedPreferences("wallpaper_rotation", Context.MODE_PRIVATE));
    }

    WallpaperPreferences(SharedPreferences choices, SharedPreferences playback) {
        this.choices = choices;
        this.playback = playback;
    }

    boolean hasRandomOverride() { return choices.contains(RANDOM); }

    boolean randomEnabled(boolean theme) {
        // Older fixed/None choices keep their meaning after upgrading the app.
        return choices.getBoolean(RANDOM, !choices.contains(Config.WALLPAPER_CHOICE_KEY) && theme);
    }

    int dim(int theme) { return choices.contains(DIM) ? clampDim(choices.getInt(DIM, theme)) : theme; }

    static int clampDim(int value) { return Math.max(0, Math.min(70, value)); }

    String currentName() {
        return choices.getString(REQUESTED, playback.getString("image",
                choices.getString(Config.WALLPAPER_CHOICE_KEY, "")));
    }

    String requested() { return choices.getString(REQUESTED, ""); }

    File requestedFile(List<File> files) {
        String name = requested();
        File file = WallpaperRotation.find(files, name);
        if (file == null && !name.isEmpty()) consumed(name);
        return file;
    }

    void consumed(String name) {
        if (name.equals(requested())) choices.edit().remove(REQUESTED).apply();
    }

    void setRandom(boolean enabled, File current) {
        SharedPreferences.Editor edit = edit().putBoolean(RANDOM, enabled).remove(REQUESTED);
        if (enabled) edit.remove(Config.WALLPAPER_CHOICE_KEY);
        else edit.putString(Config.WALLPAPER_CHOICE_KEY,
                current == null ? WallpaperRotation.CHOICE_NONE : current.getName());
        edit.apply();
    }

    void select(File file, int dim) {
        edit().putString(Config.WALLPAPER_CHOICE_KEY, file.getName()).putBoolean(RANDOM, false)
                .putInt(DIM, clampDim(dim)).remove(REQUESTED).apply();
    }

    /** A one-shot request wins over the next Home shuffle, without changing the mode. */
    void shuffle(File file, boolean random) {
        SharedPreferences.Editor edit = edit();
        if (random) edit.putString(REQUESTED, file.getName());
        else edit.putString(Config.WALLPAPER_CHOICE_KEY, file.getName()).remove(REQUESTED);
        edit.apply();
    }

    void setDim(int value) { edit().putInt(DIM, clampDim(value)).apply(); }

    void none() {
        edit().putBoolean(RANDOM, false).putString(Config.WALLPAPER_CHOICE_KEY,
                WallpaperRotation.CHOICE_NONE).remove(REQUESTED).apply();
    }

    void reset() {
        edit().remove(RANDOM).remove(DIM).remove(REQUESTED).remove(Config.WALLPAPER_CHOICE_KEY).apply();
    }

    private SharedPreferences.Editor edit() {
        return choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1);
    }
}
