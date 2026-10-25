package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.Random;

/** Local wallpaper controls, reachable with only the TV remote. No services or system changes. */
public final class WallpaperSettingsActivity extends Activity {
    private static final int MENU = 0, GALLERY = 1, PREVIEW = 2, COLUMNS = 4;
    private WallpaperPreferences preferences;
    private Config config;
    private List<File> files;
    private WallpaperImageLoader loader;
    private FrameLayout root;
    private ImageView background;
    private View shade;
    private View content;
    private TextView randomChoice;
    private TextView applyChoice;
    private RecyclerView gallery;
    private TextView galleryBack;
    private int screen = MENU;
    private int selected;
    private int dim;
    private int draftDim;
    private File current;
    private boolean previewReady;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = new WallpaperPreferences(this);
        reload();
        if (state != null) {
            screen = state.getInt("screen", MENU);
            selected = Math.min(Math.max(0, state.getInt("selected")), Math.max(0, files.size() - 1));
            draftDim = state.getInt("draft_dim", dim);
            if (files.isEmpty()) screen = MENU;
        }
        root = new FrameLayout(this);
        root.setLayoutDirection(getResources().getBoolean(R.bool.rtl_app_grid)
                ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setBackgroundColor(config.backgroundColor);
        background = new ImageView(this);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        background.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(background, match());
        shade = new View(this);
        shade.setBackgroundColor(Color.BLACK);
        shade.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(shade, match());
        setContentView(root);
    }

    private void reload() {
        config = Config.load(this);
        files = WallpaperRotation.available(Config.dir(this));
        selected = Math.min(Math.max(0, selected), Math.max(0, files.size() - 1));
        if (files.isEmpty()) screen = MENU;
        dim = preferences.dim(config.wallpaperDim);
        current = WallpaperRotation.find(files, preferences.currentName());
        if (current == null && !config.wallpapers.isEmpty()) current = config.wallpapers.get(0);
        if (current == null && !files.isEmpty()) current = files.get(0);
    }

    @Override protected void onStart() {
        super.onStart();
        loader = new WallpaperImageLoader();
        reload();
        render();
    }

    @Override protected void onStop() {
        background.setImageDrawable(null);
        if (gallery != null) gallery.setAdapter(null);
        loader.close();
        loader = null;
        super.onStop();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("screen", screen);
        state.putInt("selected", selected);
        state.putInt("draft_dim", draftDim);
        super.onSaveInstanceState(state);
    }

    private void render() {
        if (content != null) root.removeView(content);
        gallery = null;
        applyChoice = null;
        if (screen == GALLERY) showGallery();
        else if (screen == PREVIEW) showPreview();
        else showMenu();
    }

    private void showMenu() {
        screen = MENU;
        showImage(current, dim);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xF0101419);
        LinearLayout panel = column();
        panel.setPadding(dp(24), dp(20), dp(24), dp(20));
        panel.addView(title(R.string.wallpaper_personalize));
        randomChoice = button(panel, R.string.wallpaper_label, () -> {
            boolean enabled = !randomEnabled();
            preferences.setRandom(enabled, current);
            setResult(RESULT_OK);
            updateRandomLabel();
        });
        updateRandomLabel();
        button(panel, R.string.wallpaper_gallery, () -> {
            if (files.isEmpty()) empty();
            else { screen = GALLERY; render(); }
        });
        button(panel, R.string.wallpaper_change_now, () -> {
            File next = WallpaperRotation.pick(files, preferences.currentName(), new Random());
            if (next == null) empty();
            else {
                preferences.shuffle(next, randomEnabled());
                applied();
            }
        });
        dimControl(panel, WallpaperPreferences.clampDim(dim), false);
        button(panel, R.string.wallpaper_none, () -> { preferences.none(); applied(); });
        button(panel, R.string.wallpaper_theme_default, () -> {
            preferences.reset();
            applied();
        });
        button(panel, R.string.launcher_settings, () ->
                startActivity(new Intent(this, LauncherSettingsActivity.class)));
        button(panel, R.string.tv_settings, () -> {
            try { startActivity(SystemSettings.intent(this, Settings.ACTION_SETTINGS)); }
            catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
            }
        });
        scroll.addView(panel);
        FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(dp(390), -1, Gravity.END);
        root.addView(scroll, layout);
        content = scroll;
        randomChoice.requestFocus();
    }

    private boolean randomEnabled() {
        return preferences.randomEnabled(config.wallpaperChangeOnHome || config.wallpaperRotationMinutes > 0);
    }

    private void updateRandomLabel() {
        randomChoice.setText(getString(R.string.wallpaper_random_state,
                getString(randomEnabled() ? R.string.on : R.string.off)));
        randomChoice.setSelected(randomEnabled());
    }

    private void showGallery() {
        showImage(current, dim);
        LinearLayout panel = column();
        panel.setPadding(dp(36), dp(18), dp(36), dp(18));
        panel.setBackgroundColor(0xED101419);
        galleryBack = button(panel, R.string.wallpaper_personalize, () -> { screen = MENU; render(); });
        galleryBack.setOnKeyListener((v, key, event) -> {
            if (key == KeyEvent.KEYCODE_DPAD_DOWN && event.getAction() == KeyEvent.ACTION_DOWN) {
                focusImage(selected);
                return true;
            }
            return false;
        });
        gallery = new RecyclerView(this);
        gallery.setLayoutManager(new GridLayoutManager(this, COLUMNS));
        gallery.setItemAnimator(null);
        gallery.setHasFixedSize(true);
        gallery.setAdapter(new GalleryAdapter());
        panel.addView(gallery, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(panel, match());
        content = panel;
        focusImage(selected);
    }

    private void focusImage(int index) {
        if (gallery == null) return;
        selected = Math.max(0, Math.min(files.size() - 1, index));
        gallery.scrollToPosition(selected);
        gallery.post(() -> {
            if (gallery == null) return;
            RecyclerView.ViewHolder holder = gallery.findViewHolderForAdapterPosition(selected);
            if (holder != null) holder.itemView.requestFocus();
        });
    }

    private void showPreview() {
        previewReady = false;
        showImage(files.get(selected), draftDim);
        LinearLayout panel = column();
        panel.setPadding(dp(36), dp(12), dp(36), dp(12));
        panel.setBackgroundColor(0xF0101419);
        dimControl(panel, WallpaperPreferences.clampDim(draftDim), true);
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        applyChoice = button(actions, android.R.string.ok, () -> {
            if (!previewReady) return;
            preferences.select(files.get(selected), draftDim);
            applied();
        });
        applyChoice.setEnabled(previewReady);
        TextView cancel = button(actions, android.R.string.cancel, this::cancelPreview);
        panel.addView(actions);
        root.addView(panel, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        content = panel;
        cancel.requestFocus();
    }

    private void cancelPreview() { screen = GALLERY; render(); }

    private void dimControl(LinearLayout panel, int value, boolean draft) {
        TextView label = title(R.string.wallpaper_label);
        label.setText(getString(R.string.wallpaper_dimming, value));
        label.setTextSize(16);
        panel.addView(label);
        SeekBar seek = new SeekBar(this);
        seek.setContentDescription(label.getText());
        seek.setMax(70);
        seek.setKeyProgressIncrement(5);
        seek.setProgress(value);
        seek.setMinimumHeight(dp(42));
        seek.setBackgroundResource(R.drawable.settings_choice_bg);
        panel.addView(seek, new LinearLayout.LayoutParams(-1, dp(42)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                label.setText(getString(R.string.wallpaper_dimming, progress));
                bar.setContentDescription(label.getText());
                shade.setAlpha(progress / 100f);
                if (draft) draftDim = progress;
                else {
                    dim = progress;
                    preferences.setDim(progress);
                    setResult(RESULT_OK);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
    }

    private void showImage(File file, int value) {
        shade.setAlpha(value / 100f);
        if (file == null) { background.setImageDrawable(null); return; }
        loader.preview(file, getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels, config.wallpaperTrueColor, bitmap -> {
                    background.setImageBitmap(bitmap);
                    if (screen == PREVIEW) {
                        previewReady = bitmap != null;
                        if (applyChoice != null) applyChoice.setEnabled(previewReady);
                    }
                    if (bitmap == null) Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
                });
    }

    private void applied() {
        setResult(RESULT_OK);
        Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void empty() { Toast.makeText(this, R.string.wallpaper_empty, Toast.LENGTH_SHORT).show(); }

    @Override public void onBackPressed() {
        if (screen == PREVIEW) cancelPreview();
        else if (screen == GALLERY) { screen = MENU; render(); }
        else super.onBackPressed();
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView title(int resource) {
        TextView view = new TextView(this);
        view.setText(resource);
        view.setTextColor(Color.WHITE);
        view.setTextSize(22);
        view.setPadding(0, dp(4), 0, dp(8));
        return view;
    }

    private TextView button(LinearLayout panel, int resource, Runnable action) {
        TextView view = new TextView(this);
        view.setId(View.generateViewId());
        view.setText(resource);
        view.setTextColor(Color.WHITE);
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(14), dp(6), dp(14), dp(6));
        view.setFocusable(true);
        view.setBackgroundResource(R.drawable.settings_choice_bg);
        view.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                panel.getOrientation() == LinearLayout.VERTICAL ? -1 : dp(190), dp(44));
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        panel.addView(view, params);
        return view;
    }

    private FrameLayout.LayoutParams match() { return new FrameLayout.LayoutParams(-1, -1); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final class GalleryAdapter extends RecyclerView.Adapter<GalleryHolder> {
        @Override public GalleryHolder onCreateViewHolder(ViewGroup parent, int type) {
            FrameLayout tile = new FrameLayout(WallpaperSettingsActivity.this);
            tile.setPadding(dp(4), dp(4), dp(4), dp(4));
            tile.setBackgroundResource(R.drawable.settings_choice_bg);
            tile.setFocusable(true);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, dp(120));
            params.setMargins(dp(5), dp(5), dp(5), dp(5));
            tile.setLayoutParams(params);
            ImageView image = new ImageView(WallpaperSettingsActivity.this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.addView(image, match());
            return new GalleryHolder(tile, image);
        }

        @Override public void onBindViewHolder(GalleryHolder holder, int position) {
            File file = files.get(position);
            holder.itemView.setContentDescription(file.getName());
            holder.itemView.setOnFocusChangeListener((v, focused) -> {
                if (focused) selected = holder.getBindingAdapterPosition();
            });
            holder.itemView.setOnClickListener(v -> {
                selected = holder.getBindingAdapterPosition();
                if (selected == RecyclerView.NO_POSITION) return;
                draftDim = dim;
                screen = PREVIEW;
                render();
            });
            holder.itemView.setOnKeyListener((v, key, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;
                int target = GridNavigation.target(pos, files.size(), COLUMNS,
                        gallery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL, key);
                if (target == -1) { galleryBack.requestFocus(); return true; }
                if (target == -2) return false;
                focusImage(target);
                return true;
            });
            int width = Math.max(1, (getResources().getDisplayMetrics().widthPixels - dp(112)) / COLUMNS);
            loader.thumbnail(file, holder.image, width, dp(112));
        }

        @Override public int getItemCount() { return files.size(); }
        @Override public void onViewRecycled(GalleryHolder holder) {
            if (loader != null) loader.cancel(holder.image);
        }
    }

    private static final class GalleryHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        GalleryHolder(View view, ImageView image) { super(view); this.image = image; }
    }
}
