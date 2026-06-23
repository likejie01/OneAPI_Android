package center.oneapi.mobile.features.attachments;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Base64;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AttachmentContentReader {
    private final ContentResolver resolver;

    public AttachmentContentReader(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public String displayName(Uri uri) {
        String fallback = uri == null ? "附件" : String.valueOf(uri.getLastPathSegment());
        try (Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String value = cursor.getString(0);
                if (value != null && !value.trim().isEmpty()) return value.trim();
            }
        } catch (Exception ignored) {
        }
        return fallback == null || fallback.trim().isEmpty() ? "附件" : fallback;
    }

    public String readText(Uri uri, int limit) {
        String type = resolver.getType(uri);
        String name = displayName(uri).toLowerCase(Locale.ROOT);
        boolean textLike = (type != null && (type.startsWith("text/") || type.contains("json") || type.contains("xml")))
                || name.endsWith(".txt") || name.endsWith(".json") || name.endsWith(".md") || name.endsWith(".markdown")
                || name.endsWith(".csv") || name.endsWith(".log") || name.endsWith(".xml") || name.endsWith(".yaml") || name.endsWith(".yml");
        if (!textLike) return null;
        try (InputStream input = resolver.openInputStream(uri);
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            if (input == null) return null;
            byte[] buffer = new byte[4096];
            int read;
            int total = 0;
            while ((read = input.read(buffer)) != -1 && total < limit) {
                int allowed = Math.min(read, limit - total);
                output.write(buffer, 0, allowed);
                total += allowed;
            }
            return output.toString(java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            return null;
        }
    }

    public String readableContent(Uri uri, int limit) {
        String name = displayName(uri).toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || name.endsWith(".xlsm") || name.endsWith(".xls")) {
            String sheet = readXlsx(uri, 200);
            if (sheet != null && !sheet.trim().isEmpty()) return sheet.trim();
        }
        String text = readText(uri, limit);
        if (text != null && !text.trim().isEmpty()) return text.trim();
        return null;
    }

    public boolean isImage(Uri uri) {
        String type = resolver.getType(uri);
        String name = displayName(uri).toLowerCase(Locale.ROOT);
        return (type != null && type.startsWith("image/"))
                || name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".webp")
                || name.endsWith(".gif")
                || name.endsWith(".heic")
                || name.endsWith(".heif");
    }

    public boolean isPdf(Uri uri) {
        String type = resolver.getType(uri);
        String name = displayName(uri).toLowerCase(Locale.ROOT);
        return "application/pdf".equals(type) || name.endsWith(".pdf");
    }

    public String imageDataUrl(Uri uri) throws Exception {
        String direct = uri == null ? "" : uri.toString().trim();
        if (direct.startsWith("data:image/")) return direct;
        String type = resolver.getType(uri);
        String name = displayName(uri).toLowerCase(Locale.ROOT);
        if (type == null || !type.startsWith("image/")) {
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) type = "image/jpeg";
            else if (name.endsWith(".webp")) type = "image/webp";
            else if (name.endsWith(".gif")) type = "image/gif";
            else type = "image/png";
        }
        type = canonicalImageMime(type, name);
        String converted = jpegDataUrl(uri);
        if (!converted.isEmpty()) return converted;
        if (!isModelImageMime(type)) return "";
        byte[] bytes = readBytes(uri, 12 * 1024 * 1024);
        if (bytes.length == 0) return "";
        return "data:" + type + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public String pdfFirstPageDataUrl(Uri uri) {
        ParcelFileDescriptor fd = null;
        PdfRenderer renderer = null;
        PdfRenderer.Page page = null;
        try {
            fd = resolver.openFileDescriptor(uri, "r");
            if (fd == null) return "";
            renderer = new PdfRenderer(fd);
            if (renderer.getPageCount() <= 0) return "";
            page = renderer.openPage(0);
            int maxWidth = 1280;
            float ratio = page.getWidth() / (float) Math.max(1, page.getHeight());
            int width = Math.max(480, Math.min(maxWidth, page.getWidth() * 2));
            int height = Math.max(480, (int) (width / Math.max(0.25f, ratio)));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 92, output);
            bitmap.recycle();
            return "data:image/png;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
        } catch (Exception ignored) {
            return "";
        } finally {
            try {
                if (page != null) page.close();
            } catch (Exception ignored) {
            }
            try {
                if (renderer != null) renderer.close();
            } catch (Exception ignored) {
            }
            try {
                if (fd != null) fd.close();
            } catch (Exception ignored) {
            }
        }
    }

    public byte[] readBytes(Uri uri, int limit) throws Exception {
        try (InputStream input = resolver.openInputStream(uri);
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            if (input == null) return new byte[0];
            byte[] buffer = new byte[8192];
            int read;
            int total = 0;
            while ((read = input.read(buffer)) != -1 && total < limit) {
                int allowed = Math.min(read, limit - total);
                output.write(buffer, 0, allowed);
                total += allowed;
            }
            return output.toByteArray();
        }
    }

    public String readXlsx(Uri uri, int maxRows) {
        List<String> shared = new ArrayList<>();
        Map<String, String> entries = new LinkedHashMap<>();
        try (InputStream input = resolver.openInputStream(uri);
             ZipInputStream zip = new ZipInputStream(input)) {
            if (input == null) return null;
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if ("xl/sharedStrings.xml".equals(name) || name.startsWith("xl/worksheets/sheet")) {
                    java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = zip.read(buffer)) != -1 && output.size() < 800000) {
                        output.write(buffer, 0, read);
                    }
                    entries.put(name, output.toString(java.nio.charset.StandardCharsets.UTF_8.name()));
                }
                zip.closeEntry();
            }
        } catch (Exception ignored) {
            return null;
        }
        String sharedXml = entries.get("xl/sharedStrings.xml");
        if (sharedXml != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("<t[^>]*>(.*?)</t>", java.util.regex.Pattern.DOTALL).matcher(sharedXml);
            while (matcher.find()) {
                shared.add(xmlText(matcher.group(1)));
            }
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (!entry.getKey().startsWith("xl/worksheets/sheet")) continue;
            out.append("## ").append(entry.getKey().replace("xl/worksheets/", "")).append('\n');
            java.util.regex.Matcher rowMatcher = java.util.regex.Pattern.compile("<row[^>]*>(.*?)</row>", java.util.regex.Pattern.DOTALL).matcher(entry.getValue());
            int rows = 0;
            while (rowMatcher.find() && rows < maxRows) {
                List<String> cells = new ArrayList<>();
                java.util.regex.Matcher cellMatcher = java.util.regex.Pattern.compile("<c([^>]*)>(.*?)</c>", java.util.regex.Pattern.DOTALL).matcher(rowMatcher.group(1));
                while (cellMatcher.find()) {
                    String attrs = cellMatcher.group(1);
                    String body = cellMatcher.group(2);
                    java.util.regex.Matcher valueMatcher = java.util.regex.Pattern.compile("<v>(.*?)</v>", java.util.regex.Pattern.DOTALL).matcher(body);
                    String value = "";
                    if (valueMatcher.find()) value = xmlText(valueMatcher.group(1));
                    if (attrs != null && attrs.contains("t=\"s\"")) {
                        try {
                            int index = Integer.parseInt(value.trim());
                            value = index >= 0 && index < shared.size() ? shared.get(index) : value;
                        } catch (Exception ignored) {
                        }
                    }
                    cells.add(value);
                }
                if (!cells.isEmpty()) {
                    out.append("| ").append(TextUtils.join(" | ", cells)).append(" |\n");
                    rows++;
                }
            }
            if (rows >= maxRows) out.append("\n... 已截断预览\n");
            break;
        }
        return out.length() == 0 ? null : out.toString();
    }

    private String canonicalImageMime(String type, String name) {
        String clean = type == null ? "" : type.toLowerCase(Locale.ROOT).trim();
        String lowerName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(clean) || "image/pjpeg".equals(clean) || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if ("image/x-png".equals(clean) || lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".webp")) return "image/webp";
        if (lowerName.endsWith(".gif")) return "image/gif";
        return clean.isEmpty() ? "image/png" : clean;
    }

    private boolean isModelImageMime(String type) {
        String clean = type == null ? "" : type.toLowerCase(Locale.ROOT);
        return "image/png".equals(clean)
                || "image/jpeg".equals(clean)
                || "image/webp".equals(clean)
                || "image/gif".equals(clean);
    }

    private String jpegDataUrl(Uri uri) {
        Bitmap bitmap = null;
        Bitmap scaled = null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream probe = resolver.openInputStream(uri)) {
                if (probe == null) return "";
                BitmapFactory.decodeStream(probe, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return "";
            BitmapFactory.Options options = new BitmapFactory.Options();
            int longest = Math.max(bounds.outWidth, bounds.outHeight);
            int sample = 1;
            while (longest / sample > 2400) sample *= 2;
            options.inSampleSize = sample;
            try (InputStream input = resolver.openInputStream(uri)) {
                if (input == null) return "";
                bitmap = BitmapFactory.decodeStream(input, null, options);
            }
            if (bitmap == null) return "";
            int maxSide = 1600;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (Math.max(width, height) > maxSide) {
                float scale = maxSide / (float) Math.max(width, height);
                int nextWidth = Math.max(1, Math.round(width * scale));
                int nextHeight = Math.max(1, Math.round(height * scale));
                scaled = Bitmap.createScaledBitmap(bitmap, nextWidth, nextHeight, true);
            }
            Bitmap outputBitmap = scaled == null ? bitmap : scaled;
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            int quality = 88;
            do {
                output.reset();
                outputBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output);
                quality -= 8;
            } while (output.size() > 3 * 1024 * 1024 && quality >= 60);
            return "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
        } catch (Exception ignored) {
            return "";
        } finally {
            if (scaled != null && scaled != bitmap) scaled.recycle();
            if (bitmap != null) bitmap.recycle();
        }
    }

    private String xmlText(String value) {
        return (value == null ? "" : value)
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replaceAll("<[^>]+>", "")
                .trim();
    }
}
