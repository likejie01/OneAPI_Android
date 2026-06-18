package center.oneapi.mobile.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImageSampleSizeTest {
    @Test
    public void samplesLargeImageForSmallThumbnail() {
        assertEquals(32, ImageSampleSize.calculate(4032, 3024, 52, 52));
    }

    @Test
    public void keepsSmallImageAtOriginalSample() {
        assertEquals(1, ImageSampleSize.calculate(320, 240, 520, 520));
    }

    @Test
    public void handlesInvalidDimensions() {
        assertEquals(1, ImageSampleSize.calculate(0, 3024, 52, 52));
        assertEquals(1, ImageSampleSize.calculate(4032, 3024, 0, 52));
    }
}
