package com.gathin.tvhome;

import android.content.SharedPreferences;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class WallpaperPreferencesTest {
    private final MemoryPreferences choices = new MemoryPreferences();
    private final MemoryPreferences playback = new MemoryPreferences();
    private final WallpaperPreferences preferences = new WallpaperPreferences(choices, playback);

    @Test public void upgradeInheritsThemeWithoutWritingPreferences() {
        assertTrue(preferences.randomEnabled(true));
        assertFalse(preferences.randomEnabled(false));
        assertEquals(30, preferences.dim(30));
        assertEquals(90, preferences.dim(90)); // Existing themes are not changed by opening settings.
        assertFalse(preferences.hasRandomOverride());
        assertTrue(choices.getAll().isEmpty());
    }

    @Test public void legacyFixedAndNoneChoicesSurviveUpgrade() {
        for (String name : new String[]{"8.jpg", "none"}) {
            choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, name).apply();
            assertFalse(preferences.randomEnabled(true));
            assertEquals(name, choices.getString(Config.WALLPAPER_CHOICE_KEY, null));
        }
        assertFalse(preferences.hasRandomOverride());
    }

    @Test public void disablingRandomLocksTheDisplayedPictureAcrossInstances() {
        playback.edit().putString("image", "12.jpg").apply();
        preferences.setRandom(false, new File(preferences.currentName()));
        WallpaperPreferences restored = new WallpaperPreferences(choices, playback);
        assertFalse(restored.randomEnabled(true));
        assertEquals("12.jpg", choices.getString(Config.WALLPAPER_CHOICE_KEY, null));
        assertEquals(1L, choices.getLong(Config.SETTINGS_REVISION_KEY, 0));
    }

    @Test public void enablingRandomRemovesFixedAndOneShotChoices() {
        preferences.select(new File("2.jpg"), 30);
        choices.edit().putString(WallpaperPreferences.REQUESTED, "3.jpg").apply();
        preferences.setRandom(true, new File("2.jpg"));
        assertTrue(preferences.randomEnabled(false));
        assertFalse(choices.contains(Config.WALLPAPER_CHOICE_KEY));
        assertEquals("", preferences.requested());
    }

    @Test public void confirmationAtomicallyPersistsFixedModeImageAndDim() {
        preferences.setRandom(true, null);
        preferences.select(new File("23.jpg"), 65);
        WallpaperPreferences restored = new WallpaperPreferences(choices, playback);
        assertFalse(restored.randomEnabled(true));
        assertEquals("23.jpg", choices.getString(Config.WALLPAPER_CHOICE_KEY, null));
        assertEquals(65, restored.dim(30));
    }

    @Test public void oneShotShuffleKeepsRandomEnabledAndWinsUntilShown() {
        playback.edit().putString("image", "2.jpg").apply();
        preferences.setRandom(true, null);
        preferences.shuffle(new File("3.jpg"), true);
        assertTrue(preferences.randomEnabled(false));
        assertEquals("3.jpg", preferences.currentName());
        preferences.consumed("2.jpg");
        assertEquals("3.jpg", preferences.requested());
        preferences.consumed("3.jpg");
        assertEquals("", preferences.requested());
    }

    @Test public void manualShuffleDoesNotCreateAModeOverrideForExistingTheme() {
        preferences.shuffle(new File("3.jpg"), true);
        assertFalse(preferences.hasRandomOverride());
        assertTrue(preferences.randomEnabled(true));
    }

    @Test public void missingOneShotIsDiscardedWithoutChangingModeOrDim() {
        preferences.setRandom(true, null);
        preferences.setDim(40);
        preferences.shuffle(new File("removed.jpg"), true);
        assertNull(preferences.requestedFile(java.util.Collections.singletonList(new File("2.jpg"))));
        assertEquals("", preferences.requested());
        assertTrue(preferences.randomEnabled(false));
        assertEquals(40, preferences.dim(30));
    }

    @Test public void ordinaryStartupDoesNotWriteARequestOrRevision() {
        assertNull(preferences.requestedFile(java.util.Collections.singletonList(new File("2.jpg"))));
        assertTrue(choices.getAll().isEmpty());
    }

    @Test public void manualShuffleInFixedModePersistsNewFixedImage() {
        preferences.select(new File("2.jpg"), 25);
        preferences.shuffle(new File("3.jpg"), false);
        assertFalse(preferences.randomEnabled(true));
        assertEquals("3.jpg", choices.getString(Config.WALLPAPER_CHOICE_KEY, null));
        assertEquals(25, preferences.dim(0));
        assertEquals("", preferences.requested());
    }

    @Test public void dimClampsAtZeroAndSeventyWithoutChangingModeOrImage() {
        preferences.select(new File("2.jpg"), 30);
        preferences.setDim(-10);
        assertEquals(0, preferences.dim(30));
        preferences.setDim(90);
        assertEquals(70, preferences.dim(30));
        assertFalse(preferences.randomEnabled(true));
        assertEquals("2.jpg", choices.getString(Config.WALLPAPER_CHOICE_KEY, null));
    }

    @Test public void missingCurrentOnDisableKeepsTheNoneOption() {
        preferences.setRandom(false, null);
        assertEquals("none", choices.getString(Config.WALLPAPER_CHOICE_KEY, null));
        assertFalse(preferences.randomEnabled(true));
    }

    @Test public void themeResetRemovesOnlyWallpaperOverrides() {
        choices.edit().putInt("columns", 5).putString("dock", "keep").apply();
        preferences.select(new File("2.jpg"), 55);
        preferences.shuffle(new File("3.jpg"), true);
        preferences.reset();
        assertFalse(preferences.hasRandomOverride());
        assertTrue(preferences.randomEnabled(true));
        assertEquals(30, preferences.dim(30));
        assertEquals("", preferences.requested());
        assertEquals(5, choices.getInt("columns", 0));
        assertEquals("keep", choices.getString("dock", ""));
    }

    @Test public void noneDoesNotDiscardDimmingOrOtherLauncherChoices() {
        preferences.setDim(45);
        preferences.none();
        assertFalse(preferences.randomEnabled(true));
        assertEquals(45, preferences.dim(30));
        assertEquals("none", choices.getString(Config.WALLPAPER_CHOICE_KEY, null));
    }

    /** Isolated Android preference contract; no device, framework calls or production data. */
    private static final class MemoryPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();
        @Override public Map<String, ?> getAll() { return new HashMap<>(values); }
        @Override public String getString(String key, String fallback) { return (String) values.getOrDefault(key, fallback); }
        @SuppressWarnings("unchecked")
        @Override public Set<String> getStringSet(String key, Set<String> fallback) {
            return (Set<String>) values.getOrDefault(key, fallback);
        }
        @Override public int getInt(String key, int fallback) { return (Integer) values.getOrDefault(key, fallback); }
        @Override public long getLong(String key, long fallback) { return (Long) values.getOrDefault(key, fallback); }
        @Override public float getFloat(String key, float fallback) { return (Float) values.getOrDefault(key, fallback); }
        @Override public boolean getBoolean(String key, boolean fallback) { return (Boolean) values.getOrDefault(key, fallback); }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }
        @Override public Editor edit() {
            return new Editor() {
                private final Map<String, Object> pending = new HashMap<>();
                private boolean clear;
                @Override public Editor putString(String key, String value) { pending.put(key, value); return this; }
                @Override public Editor putStringSet(String key, Set<String> value) {
                    pending.put(key, value == null ? null : new HashSet<>(value)); return this;
                }
                @Override public Editor putInt(String key, int value) { pending.put(key, value); return this; }
                @Override public Editor putLong(String key, long value) { pending.put(key, value); return this; }
                @Override public Editor putFloat(String key, float value) { pending.put(key, value); return this; }
                @Override public Editor putBoolean(String key, boolean value) { pending.put(key, value); return this; }
                @Override public Editor remove(String key) { pending.put(key, null); return this; }
                @Override public Editor clear() { clear = true; return this; }
                @Override public boolean commit() { apply(); return true; }
                @Override public void apply() {
                    if (clear) values.clear();
                    pending.forEach((key, value) -> {
                        if (value == null) values.remove(key); else values.put(key, value);
                    });
                }
            };
        }
    }
}
