package center.oneapi.mobile.ui.markdown;

import java.util.ArrayList;
import java.util.List;

public class MarkdownBlock {
    public static final int PARAGRAPH = 1;
    public static final int HEADING = 2;
    public static final int BULLET = 3;
    public static final int CODE = 4;
    public static final int QUOTE = 5;
    public static final int TABLE = 6;
    public static final int THINKING = 7;

    public final int type;
    public final String text;
    public final String language;
    public final int level;
    public final List<List<String>> tableRows;

    private MarkdownBlock(int type, String text, String language, int level, List<List<String>> tableRows) {
        this.type = type;
        this.text = text == null ? "" : text;
        this.language = language == null ? "" : language;
        this.level = level;
        this.tableRows = tableRows == null ? new ArrayList<>() : tableRows;
    }

    public static MarkdownBlock text(int type, String text) {
        return new MarkdownBlock(type, text, "", 0, null);
    }

    public static MarkdownBlock code(String language, String text) {
        return new MarkdownBlock(CODE, text, language, 0, null);
    }

    public static MarkdownBlock heading(String text, int level) {
        return new MarkdownBlock(HEADING, text, "", level, null);
    }

    public static MarkdownBlock table(List<List<String>> rows) {
        return new MarkdownBlock(TABLE, "", "", 0, rows);
    }
}
