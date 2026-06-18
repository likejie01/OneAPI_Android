package center.oneapi.mobile.ui;

final class ImageSampleSize {
    private ImageSampleSize() {
    }

    static int calculate(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) return 1;
        int sample = 1;
        int halfHeight = sourceHeight / 2;
        int halfWidth = sourceWidth / 2;
        while ((halfHeight / sample) >= targetHeight && (halfWidth / sample) >= targetWidth) {
            sample *= 2;
        }
        return Math.max(1, sample);
    }
}
