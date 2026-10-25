package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import java.io.File;

/** The same bounded, sampled decode for Home, thumbnails and full-screen previews. */
final class WallpaperDecoder {
    private WallpaperDecoder() { }

    static Bitmap decode(File file, int targetWidth, int targetHeight, Bitmap.Config format,
                         int dim, boolean hardware) {
        Bitmap decoded = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), options);
            int width = options.outWidth, height = options.outHeight;
            if (width <= 0 || height <= 0) return null;
            int sample = WallpaperSize.sampleSize(width, height, targetWidth, targetHeight);
            if (!WallpaperSize.accepted(width / sample, height / sample)) return null;
            options.inJustDecodeBounds = false;
            options.inSampleSize = sample;
            int[] scale = WallpaperSize.coverScale(width / sample, height / sample, targetWidth, targetHeight);
            if (scale != null) {
                options.inScaled = true;
                options.inDensity = scale[0];
                options.inTargetDensity = scale[1];
            }
            options.inPreferredConfig = format;
            options.inMutable = dim > 0;
            decoded = BitmapFactory.decodeFile(file.getPath(), options);
            if (decoded == null) return null;
            if (dim > 0) new Canvas(decoded).drawColor((dim * 255 / 100) << 24);
            if (!hardware) return decoded;
            Bitmap gpu = decoded.copy(Bitmap.Config.HARDWARE, false);
            if (gpu == null) return decoded;
            decoded.recycle();
            return gpu;
        } catch (Exception | OutOfMemoryError failure) {
            if (decoded != null) decoded.recycle();
            return null;
        }
    }
}
