package com.samo.engine.assets.internal;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.Locale;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

final class StbTextureImporter {
    private StbTextureImporter() {

    }

    static TextureImage importFile(Path sourcePath) {

        if (sourcePath == null) {
            throw new NullPointerException("sourcePath");
        }

        String fileName = sourcePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".png") && !fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")) {
            throw new AssetCookerException(sourcePath + ": TEXTURE source must use .png, .jpg, or .jpeg");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer sourceChannels = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(false);
            ByteBuffer decoded = STBImage.stbi_load(sourcePath.toString(), width, height, sourceChannels, 4);
            if (decoded == null) {
                String reason = STBImage.stbi_failure_reason();
                throw new AssetCookerException(sourcePath + ": image decode failed" + (reason == null ? "" : ": " + reason));
            }

            try {
                int decodedWidth = width.get(0);
                int decodedHeight = height.get(0);
                int byteCount;
                try {
                    byteCount = TextureSizes.rgba8ByteCount(decodedWidth, decodedHeight);
                } catch (IllegalArgumentException exception) {
                    throw new AssetCookerException(sourcePath + ": invalid decoded image dimensions " + decodedWidth + "x" + decodedHeight, exception);
                }
                if (decoded.remaining() < byteCount) {
                    throw new AssetCookerException(sourcePath + ": decoded image buffer is shorter than expected RGBA8 payload");
                }

                byte[] rgba8 = new byte[byteCount];
                decoded.get(0, rgba8);
                return new TextureImage(decodedWidth, decodedHeight, rgba8);
            } finally {
                STBImage.stbi_image_free(decoded);
            }
        }

    }
}
