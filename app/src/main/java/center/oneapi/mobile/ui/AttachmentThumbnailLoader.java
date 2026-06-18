package center.oneapi.mobile.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AttachmentThumbnailLoader {
    public interface Callback {
        void onResult(boolean loaded);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final LruCache<String, Bitmap> BITMAP_CACHE = new LruCache<>(cacheBytes()) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value == null ? 0 : value.getByteCount();
        }
    };
    private static final LruCache<String, int[]> BOUNDS_CACHE = new LruCache<>(256);

    private AttachmentThumbnailLoader() {
    }

    public static void load(Context context, String source, ImageView target, int widthPx, int heightPx) {
        load(context, source, target, widthPx, heightPx, null);
    }

    public static void load(Context context, String source, ImageView target, int widthPx, int heightPx, Callback callback) {
        String value = clean(source);
        String requestKey = cacheKey(value, Math.max(1, widthPx), Math.max(1, heightPx));
        target.setTag(requestKey);
        Bitmap cached = BITMAP_CACHE.get(requestKey);
        if (cached != null && !cached.isRecycled()) {
            target.setImageBitmap(cached);
            if (callback != null) callback.onResult(true);
            return;
        }
        target.setImageDrawable(null);
        EXECUTOR.execute(() -> {
            Bitmap bitmap = decodeSampled(context.getApplicationContext(), value, widthPx, heightPx);
            if (bitmap != null) {
                BITMAP_CACHE.put(requestKey, bitmap);
            }
            MAIN.post(() -> {
                if (requestKey.equals(target.getTag())) {
                    boolean loaded = bitmap != null && !bitmap.isRecycled();
                    if (loaded) {
                        target.setImageBitmap(bitmap);
                    }
                    if (callback != null) callback.onResult(loaded);
                }
            });
        });
    }

    public static int[] imageSize(Context context, String source) {
        String value = clean(source);
        if (value.isEmpty()) return new int[]{0, 0};
        String key = boundsKey(value);
        int[] cached = BOUNDS_CACHE.get(key);
        if (cached != null) return new int[]{cached[0], cached[1]};
        int[] size = readBounds(context.getApplicationContext(), value);
        BOUNDS_CACHE.put(key, size);
        return new int[]{size[0], size[1]};
    }

    private static Bitmap decodeSampled(Context context, String value, int widthPx, int heightPx) {
        if (value.startsWith("data:image/")) {
            byte[] bytes = imageDataBytes(value);
            if (bytes == null) return null;
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            BitmapFactory.Options options = decodeOptions(bounds.outWidth, bounds.outHeight, widthPx, heightPx);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        }
        if (value.startsWith("content:") || value.startsWith("file:") || value.startsWith("http://") || value.startsWith("https://")) {
            int[] bounds = imageSize(context, value);
            BitmapFactory.Options options = decodeOptions(bounds[0], bounds[1], widthPx, heightPx);
            try (InputStream input = openStream(context, value)) {
                return input == null ? null : BitmapFactory.decodeStream(input, null, options);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static BitmapFactory.Options decodeOptions(int sourceWidth, int sourceHeight, int widthPx, int heightPx) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = ImageSampleSize.calculate(sourceWidth, sourceHeight, Math.max(1, widthPx), Math.max(1, heightPx));
        if (widthPx <= 320 && heightPx <= 320) {
            options.inPreferredConfig = Bitmap.Config.RGB_565;
        }
        return options;
    }

    private static int[] readBounds(Context context, String value) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            if (value.startsWith("data:image/")) {
                byte[] bytes = imageDataBytes(value);
                if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            } else {
                try (InputStream input = openStream(context, value)) {
                    if (input != null) BitmapFactory.decodeStream(input, null, options);
                }
            }
            return new int[]{Math.max(0, options.outWidth), Math.max(0, options.outHeight)};
        } catch (Exception ignored) {
            return new int[]{0, 0};
        }
    }

    private static InputStream openStream(Context context, String value) throws Exception {
        if (value.startsWith("content:") || value.startsWith("file:")) {
            return context.getContentResolver().openInputStream(Uri.parse(value));
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return new URL(value).openStream();
        }
        return null;
    }

    private static byte[] imageDataBytes(String value) {
        int comma = value.indexOf(',');
        if (comma <= 0) return null;
        try {
            return Base64.decode(value.substring(comma + 1), Base64.DEFAULT);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String clean(String source) {
        return source == null ? "" : source.trim();
    }

    private static String cacheKey(String value, int widthPx, int heightPx) {
        return boundsKey(value) + ":" + widthPx + "x" + heightPx;
    }

    private static String boundsKey(String value) {
        return value.length() + ":" + Integer.toHexString(value.hashCode());
    }

    private static int cacheBytes() {
        long max = Runtime.getRuntime().maxMemory() / 8L;
        return (int) Math.max(4L * 1024L * 1024L, Math.min(max, 24L * 1024L * 1024L));
    }
}
