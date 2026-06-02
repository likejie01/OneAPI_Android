package center.oneapi.mobile.ui.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownParser {
    public List<MarkdownBlock> parse(String source) {
        List<MarkdownBlock> out = new ArrayList<>();
        String clean = source == null ? "" : source.replace("\r\n", "\n").replace('\r', '\n').trim();
        ThinkingSplit split = splitThinking(clean);
        if (!split.thinking.isEmpty()) {
            out.add(MarkdownBlock.text(MarkdownBlock.THINKING, split.thinking));
        }
        parseBody(split.body.isEmpty() && split.thinking.isEmpty() ? clean : split.body, out);
        return out;
    }

    public ThinkingSplit splitThinking(String source) {
        String clean = source == null ? "" : source.trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.startsWith("<thinking>")) {
            int end = lower.indexOf("</thinking>");
            if (end > "<thinking>".length()) {
                return new ThinkingSplit(
                        clean.substring("<thinking>".length(), end).trim(),
                        clean.substring(end + "</thinking>".length()).trim());
            }
        }
        String prefix = "";
        if (clean.startsWith("思考过程\n")) {
            prefix = "思考过程\n";
        } else if (lower.startsWith("thinking:\n")) {
            prefix = clean.substring(0, "thinking:\n".length());
        } else if (lower.startsWith("thinking：\n")) {
            prefix = clean.substring(0, "thinking：\n".length());
        } else if (lower.startsWith("thinking:")) {
            prefix = clean.substring(0, "thinking:".length());
        } else if (lower.startsWith("thinking：")) {
            prefix = clean.substring(0, "thinking：".length());
        }
        if (prefix.isEmpty()) {
            return new ThinkingSplit("", clean);
        }
        String rest = clean.substring(prefix.length()).trim();
        Boundary boundary = thinkingBoundary(rest);
        if (boundary.index < 0) {
            return new ThinkingSplit(rest, "");
        }
        return new ThinkingSplit(rest.substring(0, boundary.index).trim(), rest.substring(boundary.bodyStart).trim());
    }

    private Boundary thinkingBoundary(String rest) {
        int split = rest.indexOf("\n\n");
        int bodyStart = split < 0 ? -1 : split + 2;
        String lower = rest.toLowerCase(Locale.ROOT);
        for (String marker : new String[]{"\n结论", "\n正文", "\n答复", "\n答案", "\n总结", "\n# ", "\n## ", "\n### "}) {
            int at = lower.indexOf(marker.toLowerCase(Locale.ROOT));
            if (at >= 0 && (split < 0 || at < split)) {
                split = at;
                bodyStart = at + 1;
            }
        }
        return new Boundary(split, bodyStart);
    }

    private void parseBody(String source, List<MarkdownBlock> out) {
        String[] lines = (source == null ? "" : source).split("\n", -1);
        StringBuilder paragraph = new StringBuilder();
        StringBuilder code = new StringBuilder();
        boolean inCode = false;
        String codeLanguage = "";
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                if (inCode) {
                    out.add(MarkdownBlock.code(codeLanguage, trimTrailingNewline(code.toString())));
                    code.setLength(0);
                    codeLanguage = "";
                    inCode = false;
                } else {
                    flushParagraph(out, paragraph);
                    inCode = true;
                    codeLanguage = trimmed.length() > 3 ? trimmed.substring(3).trim().toLowerCase(Locale.ROOT) : "";
                }
                continue;
            }
            if (inCode) {
                code.append(line).append('\n');
                continue;
            }
            if (trimmed.isEmpty()) {
                flushParagraph(out, paragraph);
                continue;
            }
            if (isTableStart(lines, i)) {
                flushParagraph(out, paragraph);
                int columns = cells(lines[i]).size();
                int end = i + 2;
                List<List<String>> rows = new ArrayList<>();
                rows.add(normalizeCells(cells(lines[i]), columns));
                while (end < lines.length && isTableData(lines[end], columns)) {
                    rows.add(normalizeCells(cells(lines[end]), columns));
                    end++;
                }
                out.add(MarkdownBlock.table(rows));
                i = end - 1;
                continue;
            }
            Matcher heading = Pattern.compile("^(#{1,6})\\s+(.+)$").matcher(trimmed);
            if (heading.find()) {
                flushParagraph(out, paragraph);
                out.add(MarkdownBlock.heading(heading.group(2), heading.group(1).length()));
                continue;
            }
            if (trimmed.startsWith(">")) {
                flushParagraph(out, paragraph);
                out.add(MarkdownBlock.text(MarkdownBlock.QUOTE, trimmed.replaceFirst("^>\\s*", "")));
                continue;
            }
            if (trimmed.matches("^[-*+]\\s+.+$")) {
                flushParagraph(out, paragraph);
                out.add(MarkdownBlock.text(MarkdownBlock.BULLET, trimmed.replaceFirst("^[-*+]\\s+", "")));
                continue;
            }
            if (trimmed.matches("^\\d+[\\.、]\\s+.+$")) {
                flushParagraph(out, paragraph);
                out.add(MarkdownBlock.text(MarkdownBlock.BULLET, trimmed));
                continue;
            }
            if (paragraph.length() > 0) {
                paragraph.append('\n');
            }
            paragraph.append(line);
        }
        if (inCode && code.length() > 0) {
            out.add(MarkdownBlock.code(codeLanguage, trimTrailingNewline(code.toString())));
        }
        flushParagraph(out, paragraph);
    }

    private void flushParagraph(List<MarkdownBlock> out, StringBuilder paragraph) {
        if (paragraph.length() == 0) {
            return;
        }
        out.add(MarkdownBlock.text(MarkdownBlock.PARAGRAPH, paragraph.toString().trim()));
        paragraph.setLength(0);
    }

    private boolean isTableStart(String[] lines, int index) {
        if (index + 1 >= lines.length || !hasUnescapedPipe(lines[index])) return false;
        List<String> header = cells(lines[index]);
        List<String> separator = cells(lines[index + 1]);
        return header.size() >= 2
                && separator.size() == header.size()
                && isTableSeparatorCells(separator);
    }

    private boolean isTableSeparator(String line) {
        if (line == null || !hasUnescapedPipe(line)) return false;
        return isTableSeparatorCells(cells(line));
    }

    private boolean isTableSeparatorCells(List<String> cells) {
        if (cells.size() < 2) return false;
        for (String cell : cells) {
            if (!cell.matches(":?-{3,}:?")) return false;
        }
        return true;
    }

    private boolean isTableData(String line, int columns) {
        if (line == null || line.trim().isEmpty() || isTableSeparator(line) || !hasUnescapedPipe(line)) return false;
        List<String> row = cells(line);
        return row.size() == columns;
    }

    private List<String> cells(String line) {
        String clean = line == null ? "" : line.trim();
        if (clean.startsWith("|")) clean = clean.substring(1);
        if (clean.endsWith("|")) clean = clean.substring(0, clean.length() - 1);
        List<String> out = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < clean.length(); i++) {
            char ch = clean.charAt(i);
            if (escaped) {
                cell.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '|') {
                out.add(cell.toString().trim());
                cell.setLength(0);
                continue;
            }
            cell.append(ch);
        }
        if (escaped) {
            cell.append('\\');
        }
        out.add(cell.toString().trim());
        return out;
    }

    private boolean hasUnescapedPipe(String line) {
        if (line == null) return false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '|') {
                return true;
            }
        }
        return false;
    }

    private List<String> normalizeCells(List<String> source, int columns) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            out.add(i < source.size() ? source.get(i) : "");
        }
        return out;
    }

    private String trimTrailingNewline(String value) {
        String out = value == null ? "" : value;
        while (out.endsWith("\n")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    public static class ThinkingSplit {
        public final String thinking;
        public final String body;

        ThinkingSplit(String thinking, String body) {
            this.thinking = thinking == null ? "" : thinking;
            this.body = body == null ? "" : body;
        }
    }

    private static class Boundary {
        final int index;
        final int bodyStart;

        Boundary(int index, int bodyStart) {
            this.index = index;
            this.bodyStart = bodyStart;
        }
    }
}
