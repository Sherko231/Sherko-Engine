package com.samo.engine.assets.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32C;

final class CookedTextureBinary {
    private static final byte[] MAGIC = {'S', 'T', 'E', 'X'};
    private static final int SCHEMA_VERSION = 1;
    private static final int FORMAT_RGBA8 = 1;
    private static final int HEADER_BYTES = 32;
    private static final int MIP_HEADER_BYTES = 12;

    private CookedTextureBinary() {

    }

    static byte[] encode(List<TextureMipLevel> mipLevels) {

        CookedTexture texture = validatedTexture(mipLevels);
        int bodyLength = bodyLength(texture.mipLevels());
        ByteBuffer body = ByteBuffer.allocate(bodyLength).order(ByteOrder.LITTLE_ENDIAN);
        for (TextureMipLevel level : texture.mipLevels()) {
            byte[] rgba8 = level.rgba8();
            body.putInt(level.width());
            body.putInt(level.height());
            body.putInt(rgba8.length);
            body.put(rgba8);
        }

        byte[] bodyBytes = body.array();
        CRC32C checksum = new CRC32C();
        checksum.update(bodyBytes, 0, bodyBytes.length);

        ByteBuffer file = ByteBuffer.allocate(Math.addExact(HEADER_BYTES, bodyLength)).order(ByteOrder.LITTLE_ENDIAN);
        file.put(MAGIC);
        file.putInt(SCHEMA_VERSION);
        file.putInt(FORMAT_RGBA8);
        file.putInt(texture.width());
        file.putInt(texture.height());
        file.putInt(texture.mipLevels().size());
        file.putInt(bodyLength);
        file.putInt((int) checksum.getValue());
        file.put(bodyBytes);
        return file.array();

    }

    static CookedTexture decode(byte[] bytes) {

        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        if (bytes.length < HEADER_BYTES) {
            throw invalid("truncated header");
        }

        ByteBuffer file = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (byte expected : MAGIC) {
            if (file.get() != expected) {
                throw invalid("bad magic");
            }
        }
        int version = file.getInt();
        if (version != SCHEMA_VERSION) {
            throw invalid("unsupported schema version " + version);
        }
        int format = file.getInt();
        if (format != FORMAT_RGBA8) {
            throw invalid("unsupported texture format " + format);
        }

        int baseWidth = file.getInt();
        int baseHeight = file.getInt();
        int mipCount = file.getInt();
        int declaredBodyLength = file.getInt();
        int declaredChecksum = file.getInt();

        requirePositive(baseWidth, "base width");
        requirePositive(baseHeight, "base height");
        if (mipCount != expectedMipCount(baseWidth, baseHeight)) {
            throw invalid("mip count does not describe a complete chain");
        }
        if (declaredBodyLength < 0 || declaredBodyLength != bytes.length - HEADER_BYTES) {
            throw invalid("body length mismatch or trailing data");
        }

        byte[] body = new byte[declaredBodyLength];
        file.get(body);
        CRC32C checksum = new CRC32C();
        checksum.update(body, 0, body.length);
        if ((int) checksum.getValue() != declaredChecksum) {
            throw invalid("checksum mismatch");
        }

        ByteBuffer records = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        ArrayList<TextureMipLevel> levels = new ArrayList<>(mipCount);
        int expectedWidth = baseWidth;
        int expectedHeight = baseHeight;
        for (int levelIndex = 0; levelIndex < mipCount; levelIndex++) {
            if (records.remaining() < MIP_HEADER_BYTES) {
                throw invalid("truncated mip header");
            }
            int width = records.getInt();
            int height = records.getInt();
            int byteLength = records.getInt();
            if (width != expectedWidth || height != expectedHeight) {
                throw invalid("mip dimensions are inconsistent at level " + levelIndex);
            }

            int expectedByteLength;
            try {
                expectedByteLength = TextureSizes.rgba8ByteCount(width, height);
            } catch (IllegalArgumentException exception) {
                throw invalid("invalid mip dimensions at level " + levelIndex, exception);
            }
            if (byteLength != expectedByteLength || records.remaining() < byteLength) {
                throw invalid("mip byte length mismatch at level " + levelIndex);
            }

            byte[] rgba8 = new byte[byteLength];
            records.get(rgba8);
            levels.add(new TextureMipLevel(width, height, rgba8));
            expectedWidth = Math.max(1, expectedWidth / 2);
            expectedHeight = Math.max(1, expectedHeight / 2);
        }
        if (records.hasRemaining()) {
            throw invalid("trailing mip body data");
        }
        return new CookedTexture(levels);

    }

    private static CookedTexture validatedTexture(List<TextureMipLevel> mipLevels) {

        CookedTexture texture = new CookedTexture(mipLevels);
        if (texture.mipLevels().size() != expectedMipCount(texture.width(), texture.height())) {
            throw new IllegalArgumentException("Texture mip levels must form a complete chain");
        }

        int expectedWidth = texture.width();
        int expectedHeight = texture.height();
        for (TextureMipLevel level : texture.mipLevels()) {
            if (level.width() != expectedWidth || level.height() != expectedHeight) {
                throw new IllegalArgumentException("Texture mip dimensions are inconsistent");
            }
            expectedWidth = Math.max(1, expectedWidth / 2);
            expectedHeight = Math.max(1, expectedHeight / 2);
        }
        return texture;

    }

    private static int bodyLength(List<TextureMipLevel> levels) {

        int length = 0;
        try {
            for (TextureMipLevel level : levels) {
                length = Math.addExact(length, MIP_HEADER_BYTES);
                length = Math.addExact(length, level.rgba8().length);
            }
            return length;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Cooked texture is too large", exception);
        }

    }

    private static int expectedMipCount(int width, int height) {

        requirePositive(width, "width");
        requirePositive(height, "height");
        int count = 1;
        while (width > 1 || height > 1) {
            width = Math.max(1, width / 2);
            height = Math.max(1, height / 2);
            count++;
        }
        return count;

    }

    private static void requirePositive(int value, String field) {

        if (value <= 0) {
            throw invalid(field + " must be positive");
        }

    }

    private static AssetCookerException invalid(String message) {

        return new AssetCookerException("Invalid STEX payload: " + message);

    }

    private static AssetCookerException invalid(String message, Throwable cause) {

        return new AssetCookerException("Invalid STEX payload: " + message, cause);

    }
}
