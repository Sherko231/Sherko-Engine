package com.samo.engine.assets.internal;

import java.util.ArrayList;
import java.util.List;

final class TextureMipChain {
    private TextureMipChain() {

    }

    static List<TextureMipLevel> generate(TextureImage image) {

        if (image == null) {
            throw new NullPointerException("image");
        }

        ArrayList<TextureMipLevel> levels = new ArrayList<>();
        TextureMipLevel current = new TextureMipLevel(image.width(), image.height(), image.rgba8());
        levels.add(current);

        while (current.width() > 1 || current.height() > 1) {
            current = downsample(current);
            levels.add(current);
        }
        return List.copyOf(levels);

    }

    private static TextureMipLevel downsample(TextureMipLevel source) {

        int targetWidth = Math.max(1, source.width() / 2);
        int targetHeight = Math.max(1, source.height() / 2);
        byte[] sourceBytes = source.rgba8();
        byte[] targetBytes = new byte[TextureSizes.rgba8ByteCount(targetWidth, targetHeight)];

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int sourceX = x * 2;
                int sourceY = y * 2;
                int targetBase = (y * targetWidth + x) * 4;
                for (int channel = 0; channel < 4; channel++) {
                    int sum = 0;
                    int count = 0;
                    for (int offsetY = 0; offsetY < 2; offsetY++) {
                        int sampleY = sourceY + offsetY;
                        if (sampleY >= source.height()) {
                            continue;
                        }
                        for (int offsetX = 0; offsetX < 2; offsetX++) {
                            int sampleX = sourceX + offsetX;
                            if (sampleX >= source.width()) {
                                continue;
                            }
                            int sourceBase = (sampleY * source.width() + sampleX) * 4;
                            sum += Byte.toUnsignedInt(sourceBytes[sourceBase + channel]);
                            count++;
                        }
                    }
                    targetBytes[targetBase + channel] = (byte) (sum / count);
                }
            }
        }

        return new TextureMipLevel(targetWidth, targetHeight, targetBytes);

    }
}
