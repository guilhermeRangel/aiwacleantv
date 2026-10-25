package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Screen-scoped decoding: a bounded thumbnail cache and just one full-size preview. */
final class WallpaperImageLoader implements AutoCloseable {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(0, 1, 5, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());
    private final LruCache<String, Bitmap> thumbnails = new LruCache<String, Bitmap>(2 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getAllocationByteCount(); }
    };
    private final Map<ImageView, Request> requests = new IdentityHashMap<>();
    private Request previewRequest;
    private String previewKey;
    private Bitmap preview;
    private volatile boolean closed;

    private static final class Request { volatile boolean cancelled; }

    void thumbnail(File file, ImageView view, int width, int height) {
        cancel(view);
        String key = key(file) + ":" + width + "x" + height;
        Bitmap cached = thumbnails.get(key);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        Request request = new Request();
        requests.put(view, request);
        worker.execute(() -> {
            if (request.cancelled || closed) return;
            Bitmap bitmap = WallpaperDecoder.decode(file, width, height, Bitmap.Config.RGB_565, 0, false);
            main.post(() -> {
                if (closed) {
                    if (bitmap != null) bitmap.recycle();
                    return;
                }
                if (bitmap != null) thumbnails.put(key, bitmap);
                if (requests.get(view) == request) {
                    requests.remove(view);
                    view.setImageBitmap(bitmap);
                }
            });
        });
    }

    void cancel(ImageView view) {
        Request request = requests.remove(view);
        if (request != null) request.cancelled = true;
        view.setImageDrawable(null);
    }

    void preview(File file, int width, int height, boolean trueColor, Consumer<Bitmap> callback) {
        if (previewRequest != null) previewRequest.cancelled = true;
        Request request = new Request();
        previewRequest = request;
        String key = key(file);
        if (key.equals(previewKey) && preview != null) {
            callback.accept(preview);
            return;
        }
        worker.execute(() -> {
            if (request.cancelled || closed) return;
            Bitmap bitmap = WallpaperDecoder.decode(file, width, height,
                    trueColor ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565, 0, true);
            main.post(() -> {
                if (closed || request.cancelled) {
                    if (bitmap != null) bitmap.recycle();
                    return;
                }
                previewKey = key;
                preview = bitmap;
                callback.accept(bitmap);
            });
        });
    }

    private static String key(File file) {
        return file.getPath() + ":" + file.length() + ":" + file.lastModified();
    }

    @Override public void close() {
        closed = true;
        if (previewRequest != null) previewRequest.cancelled = true;
        for (Request request : requests.values()) request.cancelled = true;
        requests.clear();
        worker.shutdownNow();
        thumbnails.evictAll();
        // Visible/cached pictures may still be referenced by RenderThread: let Android free those buffers.
        preview = null;
    }
}
