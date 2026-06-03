package center.oneapi.mobile.ui.markdown;

import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebSettings;

import center.oneapi.mobile.R;
import center.oneapi.mobile.ui.UiKit;

import java.io.OutputStream;
import java.util.List;

public class MarkdownViews {
    private final Context context;
    private final MarkdownParser parser = new MarkdownParser();

    public MarkdownViews(Context context) {
        this.context = context;
    }

    public void renderInto(LinearLayout parent, String markdown, Runnable clickAction) {
        for (MarkdownBlock block : parser.parse(markdown)) {
            switch (block.type) {
                case MarkdownBlock.THINKING:
                    if (!block.text.trim().isEmpty()) {
                        parent.addView(thinking(block.text, clickAction));
                    }
                    break;
                case MarkdownBlock.HEADING:
                    parent.addView(text(styled(block.text), UiKit.INK, true, Math.max(15, 21 - block.level), clickAction));
                    break;
                case MarkdownBlock.BULLET:
                    parent.addView(text(styled("• " + block.text), UiKit.INK, false, 14, clickAction));
                    break;
                case MarkdownBlock.CODE:
                    parent.addView("mermaid".equals(block.language) ? mermaid(block.text, clickAction) : code(block.language, block.text, clickAction));
                    break;
                case MarkdownBlock.QUOTE:
                    parent.addView(quote(block.text, clickAction));
                    break;
                case MarkdownBlock.TABLE:
                    parent.addView(table(block.tableRows, clickAction));
                    break;
                default:
                    parent.addView(text(styled(block.text), UiKit.INK, false, 14, clickAction));
                    break;
            }
        }
    }

    private View thinking(String value, Runnable clickAction) {
        LinearLayout box = UiKit.vertical(context);
        box.setBackground(UiKit.round(UiKit.thinkingFill(context), UiKit.dp(context, 12), UiKit.thinkingStroke(context)));
        box.setPadding(UiKit.dp(context, 10), UiKit.dp(context, 7), UiKit.dp(context, 10), UiKit.dp(context, 7));
        LinearLayout head = UiKit.horizontal(context);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = UiKit.text(context, "Thinking", UiKit.thinkingText(context), 13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, new LinearLayout.LayoutParams(-2, UiKit.dp(context, 28)));
        TextView marker = UiKit.text(context, "▶", UiKit.thinkingText(context), 7);
        marker.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams markerLp = new LinearLayout.LayoutParams(UiKit.dp(context, 14), UiKit.dp(context, 28));
        markerLp.setMargins(UiKit.dp(context, 8), 0, 0, 0);
        head.addView(marker, markerLp);
        View spacer = new View(context);
        head.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView body = UiKit.text(context, value, UiKit.thinkingText(context), 13);
        body.setTextIsSelectable(true);
        body.setVisibility(View.GONE);
        box.addView(head);
        box.addView(body);
        head.setOnClickListener(v -> {
            boolean open = body.getVisibility() == View.VISIBLE;
            body.setVisibility(open ? View.GONE : View.VISIBLE);
            marker.setText(open ? "▶" : "▼");
        });
        if (clickAction != null) box.setOnClickListener(v -> clickAction.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, UiKit.dp(context, 4), 0, UiKit.dp(context, 8));
        box.setLayoutParams(lp);
        return box;
    }

    private View text(CharSequence value, int color, boolean bold, int size, Runnable clickAction) {
        TextView view = UiKit.text(context, "", color, size);
        view.setText(value);
        view.setTextIsSelectable(true);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        if (clickAction != null) view.setOnClickListener(v -> clickAction.run());
        return view;
    }

    private View code(String language, String value, Runnable clickAction) {
        LinearLayout box = UiKit.vertical(context);
        box.setBackground(UiKit.round(UiKit.codeFill(context), UiKit.dp(context, 10), UiKit.line(context)));
        LinearLayout top = UiKit.horizontal(context);
        top.setPadding(UiKit.dp(context, 10), UiKit.dp(context, 5), UiKit.dp(context, 6), 0);
        TextView lang = UiKit.text(context, language == null || language.isEmpty() ? "code" : language, UiKit.MUTED, 12);
        lang.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(lang, new LinearLayout.LayoutParams(0, UiKit.dp(context, 28), 1f));
        ImageButton copy = UiKit.imageButton(context, R.drawable.ic_bubble_copy, "复制代码");
        copy.setColorFilter(UiKit.muted(context));
        copy.setOnClickListener(v -> copy(value));
        top.addView(copy, new LinearLayout.LayoutParams(UiKit.dp(context, 30), UiKit.dp(context, 30)));
        box.addView(top);
        TextView view = UiKit.text(context, value, UiKit.codeText(context), 12);
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextIsSelectable(true);
        view.setPadding(UiKit.dp(context, 10), 0, UiKit.dp(context, 10), UiKit.dp(context, 8));
        if (clickAction != null) view.setOnClickListener(v -> clickAction.run());
        box.addView(view);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, UiKit.dp(context, 5), 0, UiKit.dp(context, 7));
        box.setLayoutParams(lp);
        return box;
    }

    private View mermaid(String value, Runnable clickAction) {
        LinearLayout box = UiKit.vertical(context);
        box.setBackground(UiKit.round(UiKit.codeFill(context), UiKit.dp(context, 10), UiKit.line(context)));
        WebView web = new WebView(context);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.loadDataWithBaseURL("https://cdn.jsdelivr.net/", mermaidHtml(value), "text/html", "UTF-8", null);
        box.addView(web, new LinearLayout.LayoutParams(-1, UiKit.dp(context, 260)));
        LinearLayout actions = UiKit.horizontal(context);
        actions.setGravity(Gravity.RIGHT);
        actions.setPadding(UiKit.dp(context, 8), UiKit.dp(context, 4), UiKit.dp(context, 6), UiKit.dp(context, 6));
        TextView label = UiKit.text(context, "mermaid", UiKit.MUTED, 12);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        actions.addView(label, new LinearLayout.LayoutParams(0, UiKit.dp(context, 30), 1f));
        ImageButton copy = UiKit.imageButton(context, R.drawable.ic_bubble_copy, "复制 Mermaid");
        copy.setColorFilter(UiKit.muted(context));
        copy.setOnClickListener(v -> copy(value));
        ImageButton svg = UiKit.imageButton(context, R.drawable.ic_bubble_download, "下载 SVG");
        svg.setColorFilter(UiKit.muted(context));
        svg.setOnClickListener(v -> saveRenderedSvg(web, value));
        ImageButton png = UiKit.imageButton(context, R.drawable.ic_bubble_download, "下载 PNG");
        png.setColorFilter(UiKit.blue(context));
        png.setOnClickListener(v -> saveWebViewPng(web, value));
        actions.addView(copy, new LinearLayout.LayoutParams(UiKit.dp(context, 30), UiKit.dp(context, 30)));
        actions.addView(svg, new LinearLayout.LayoutParams(UiKit.dp(context, 30), UiKit.dp(context, 30)));
        actions.addView(png, new LinearLayout.LayoutParams(UiKit.dp(context, 30), UiKit.dp(context, 30)));
        box.addView(actions);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, UiKit.dp(context, 5), 0, UiKit.dp(context, 7));
        box.setLayoutParams(lp);
        return box;
    }

    private View quote(String value, Runnable clickAction) {
        TextView view = UiKit.text(context, value, UiKit.MUTED, 14);
        view.setText(styled(value));
        view.setTextIsSelectable(true);
        view.setBackground(UiKit.round(UiKit.chipFill(context, false), UiKit.dp(context, 10), UiKit.line(context)));
        view.setPadding(UiKit.dp(context, 10), UiKit.dp(context, 7), UiKit.dp(context, 10), UiKit.dp(context, 7));
        if (clickAction != null) view.setOnClickListener(v -> clickAction.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, UiKit.dp(context, 4), 0, UiKit.dp(context, 6));
        view.setLayoutParams(lp);
        return view;
    }

    private View table(List<List<String>> rows, Runnable clickAction) {
        HorizontalScrollView scroller = new HorizontalScrollView(context);
        scroller.setHorizontalScrollBarEnabled(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setFillViewport(false);
        TableLayout table = new TableLayout(context);
        table.setShrinkAllColumns(false);
        table.setStretchAllColumns(false);
        for (int i = 0; i < rows.size(); i++) {
            table.addView(tableRow(rows.get(i), i == 0, clickAction));
        }
        scroller.addView(table, new HorizontalScrollView.LayoutParams(-2, -2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, UiKit.dp(context, 6), 0, UiKit.dp(context, 8));
        scroller.setLayoutParams(lp);
        return scroller;
    }

    private View tableRow(List<String> cells, boolean header, Runnable clickAction) {
        TableRow row = new TableRow(context);
        row.setGravity(Gravity.TOP);
        for (String cell : cells) {
            TextView view = UiKit.text(context, "", UiKit.INK, 12);
            view.setText(styled(cell));
            view.setTextIsSelectable(true);
            view.setMaxWidth(tableMaxCellWidth());
            view.setMinWidth(UiKit.dp(context, 72));
            view.setPadding(UiKit.dp(context, 8), UiKit.dp(context, 7), UiKit.dp(context, 8), UiKit.dp(context, 7));
            view.setBackground(UiKit.round(header ? UiKit.chipFill(context, true) : UiKit.surface(context), 0, UiKit.line(context)));
            if (header) view.setTypeface(Typeface.DEFAULT_BOLD);
            if (clickAction != null) view.setOnClickListener(v -> clickAction.run());
            TableRow.LayoutParams lp = new TableRow.LayoutParams(-2, -2);
            row.addView(view, lp);
        }
        return row;
    }

    private int tableMaxCellWidth() {
        int width = context.getResources().getDisplayMetrics().widthPixels;
        int bubble = Math.max(UiKit.dp(context, 220), width - UiKit.dp(context, 72));
        return Math.max(UiKit.dp(context, 120), (int) (bubble * 0.9f));
    }

    private CharSequence styled(String source) {
        String value = source == null ? "" : source;
        StringBuilder out = new StringBuilder();
        java.util.List<int[]> boldSpans = new java.util.ArrayList<>();
        java.util.List<int[]> italicSpans = new java.util.ArrayList<>();
        java.util.List<int[]> codeSpans = new java.util.ArrayList<>();
        int index = 0;
        while (index < value.length()) {
            if (value.charAt(index) == '`') {
                int end = value.indexOf("`", index + 1);
                if (end > index + 1) {
                    int spanStart = out.length();
                    out.append(value, index + 1, end);
                    codeSpans.add(new int[]{spanStart, out.length()});
                    index = end + 1;
                    continue;
                }
            }
            if (value.startsWith("**", index)) {
                int end = value.indexOf("**", index + 2);
                if (end > index) {
                    int spanStart = out.length();
                    out.append(value, index + 2, end);
                    boldSpans.add(new int[]{spanStart, out.length()});
                    index = end + 2;
                    continue;
                }
            }
            if (value.charAt(index) == '*' && (index + 1 >= value.length() || value.charAt(index + 1) != '*')) {
                int end = value.indexOf("*", index + 1);
                if (end > index + 1) {
                    int spanStart = out.length();
                    out.append(value, index + 1, end);
                    italicSpans.add(new int[]{spanStart, out.length()});
                    index = end + 1;
                    continue;
                }
            }
            out.append(value.charAt(index));
            index++;
        }
        SpannableString styled = new SpannableString(out.toString());
        for (int[] span : boldSpans) {
            if (span[1] > span[0]) {
                styled.setSpan(new StyleSpan(Typeface.BOLD), span[0], span[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        for (int[] span : italicSpans) {
            if (span[1] > span[0]) {
                styled.setSpan(new StyleSpan(Typeface.ITALIC), span[0], span[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        for (int[] span : codeSpans) {
            if (span[1] > span[0]) {
                styled.setSpan(new TypefaceSpan("monospace"), span[0], span[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                styled.setSpan(new BackgroundColorSpan(UiKit.codeFill(context)), span[0], span[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                styled.setSpan(new ForegroundColorSpan(UiKit.codeText(context)), span[0], span[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return styled;
    }

    private void copy(String text) {
        try {
            ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager != null) manager.setPrimaryClip(ClipData.newPlainText("OneAPI", text == null ? "" : text));
        } catch (Exception ignored) {
        }
    }

    private void saveMermaidSvg(String source) {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"800\"><foreignObject width=\"100%\" height=\"100%\"><pre xmlns=\"http://www.w3.org/1999/xhtml\" style=\"font-family:monospace;white-space:pre-wrap;\">" + escapeXml(source) + "</pre></foreignObject></svg>";
        saveBytes("oneapi-mermaid-" + System.currentTimeMillis() + ".svg", "image/svg+xml", svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void saveRenderedSvg(WebView web, String fallback) {
        try {
            web.evaluateJavascript("(function(){var s=document.querySelector('svg');return s?s.outerHTML:'';})()", value -> {
                String svg = value == null ? "" : value;
                if (svg.startsWith("\"") && svg.endsWith("\"")) {
                    svg = svg.substring(1, svg.length() - 1)
                            .replace("\\u003C", "<")
                            .replace("\\\"", "\"")
                            .replace("\\n", "\n");
                }
                if (svg.trim().isEmpty()) {
                    saveMermaidSvg(fallback);
                } else {
                    saveBytes("oneapi-mermaid-" + System.currentTimeMillis() + ".svg", "image/svg+xml", svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            });
        } catch (Exception ignored) {
            saveMermaidSvg(fallback);
        }
    }

    private void saveMermaidPng(String source) {
        Bitmap bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(31, 41, 55));
        paint.setTextSize(24);
        int y = 42;
        for (String line : (source == null ? "" : source).split("\n")) {
            canvas.drawText(line, 32, y, paint);
            y += 34;
            if (y > 760) break;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "oneapi-mermaid-" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OneAPI");
            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return;
            try (OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                if (output != null) bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            }
        } catch (Exception ignored) {
        }
    }

    private void saveWebViewPng(WebView web, String fallback) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(Math.max(1, web.getWidth()), Math.max(1, web.getHeight()), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            web.draw(canvas);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "oneapi-mermaid-" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OneAPI");
            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return;
            try (OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                if (output != null) bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            }
        } catch (Exception ignored) {
            saveMermaidPng(fallback);
        }
    }

    private String mermaidHtml(String source) {
        String clean = normalizeMermaid(source);
        return "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>"
                + "<script src=\"https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js\"></script>"
                + "<style>body{margin:0;background:transparent;font-family:sans-serif;color:#1f2937}"
                + "#diagram{padding:12px;overflow:auto}.fallback{white-space:pre-wrap;font-family:monospace;font-size:12px;padding:12px;color:#4b5563}</style>"
                + "</head><body><div id=\"diagram\"></div>"
                + "<script>"
                + "const src=" + jsString(clean) + ";"
                + "mermaid.initialize({startOnLoad:false,securityLevel:'loose',theme:'default'});"
                + "mermaid.parseError=function(e){document.getElementById('diagram').innerHTML='<pre class=\"fallback\">Mermaid 解析失败\\n\\n'+escapeHtml(src)+'</pre>';};"
                + "function escapeHtml(s){return String(s).replace(/[&<>]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;'}[c];});}"
                + "(async function(){try{const r=await mermaid.render('m'+Date.now(),src);document.getElementById('diagram').innerHTML=r.svg;}catch(e){mermaid.parseError(e);}})();"
                + "</script></body></html>";
    }

    private String normalizeMermaid(String source) {
        String value = source == null ? "" : source.trim()
                .replace("\u200B", "")
                .replace("\uFEFF", "");
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        String[] starts = new String[]{
                "graph ", "flowchart ", "sequencediagram", "classdiagram", "statediagram",
                "erdiagram", "journey", "gantt", "pie", "mindmap", "timeline",
                "gitgraph", "quadrantchart", "xychart-beta", "block-beta", "packet-beta",
                "architecture-beta"
        };
        int best = -1;
        for (String start : starts) {
            int at = lower.indexOf(start);
            if (at >= 0 && (best < 0 || at < best)) best = at;
        }
        if (best > 0) value = value.substring(best).trim();
        return value;
    }

    private String jsString(String value) {
        return "\"" + (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("</script>", "<\\/script>") + "\"";
    }

    private void saveBytes(String name, String mime, byte[] bytes) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, name);
            values.put(MediaStore.Downloads.MIME_TYPE, mime);
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return;
            try (OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                if (output != null) output.write(bytes);
            }
        } catch (Exception ignored) {
        }
    }

    private String escapeXml(String value) {
        return (value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
