package center.oneapi.mobile.ui.markdown;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MarkdownParserTest {
    @Test
    public void parsesMarkdownTableAsSingleBlock() {
        MarkdownParser parser = new MarkdownParser();
        List<MarkdownBlock> blocks = parser.parse("| A | B |\n|---|---|\n| one | two |\n| three | four |");

        assertEquals(1, blocks.size());
        assertEquals(MarkdownBlock.TABLE, blocks.get(0).type);
        assertEquals(3, blocks.get(0).tableRows.size());
        assertEquals("two", blocks.get(0).tableRows.get(1).get(1));
    }

    @Test
    public void parsesAlignedTableAndEscapedPipes() {
        MarkdownParser parser = new MarkdownParser();
        List<MarkdownBlock> blocks = parser.parse("| Name | Value |\n| :--- | ---: |\n| a\\|b | 123 |");

        assertEquals(1, blocks.size());
        assertEquals(MarkdownBlock.TABLE, blocks.get(0).type);
        assertEquals("a|b", blocks.get(0).tableRows.get(1).get(0));
        assertEquals("123", blocks.get(0).tableRows.get(1).get(1));
    }

    @Test
    public void doesNotTreatMismatchedPipeTextAsTable() {
        MarkdownParser parser = new MarkdownParser();
        List<MarkdownBlock> blocks = parser.parse("A | B\nnot a separator\nonly | text");

        assertEquals(1, blocks.size());
        assertEquals(MarkdownBlock.PARAGRAPH, blocks.get(0).type);
    }

    @Test
    public void splitsEnglishThinkingAtBlankLine() {
        MarkdownParser parser = new MarkdownParser();
        MarkdownParser.ThinkingSplit split = parser.splitThinking("Thinking:\nAssessing options\n\nFinal answer");

        assertEquals("Assessing options", split.thinking);
        assertEquals("Final answer", split.body);
    }

    @Test
    public void splitsXmlThinkingTags() {
        MarkdownParser parser = new MarkdownParser();
        MarkdownParser.ThinkingSplit split = parser.splitThinking("<thinking>Hidden</thinking>\nVisible");

        assertEquals("Hidden", split.thinking);
        assertEquals("Visible", split.body);
    }
}
