package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StbTextureImporterTest {
    @TempDir
    Path tempDir;

    @Test
    void decodesPngToUnflippedRgba8AndAcceptsMixedCaseExtension() throws Exception {

        Path source = tempDir.resolve("pixels.PnG");
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF0A141E);
        image.setRGB(1, 0, 0xFF28323C);
        image.setRGB(0, 1, 0xFF46505A);
        image.setRGB(1, 1, 0x80646E78);
        assertThat(ImageIO.write(image, "png", source.toFile())).isTrue();

        TextureImage decoded = StbTextureImporter.importFile(source);

        assertThat(decoded.width()).isEqualTo(2);
        assertThat(decoded.height()).isEqualTo(2);
        assertThat(decoded.rgba8()).containsExactly((byte) 10, (byte) 20, (byte) 30, (byte) 255, (byte) 40, (byte) 50, (byte) 60, (byte) 255, (byte) 70, (byte) 80, (byte) 90,
            (byte) 255, (byte) 100, (byte) 110, (byte) 120, (byte) 128);

    }

    @Test
    void decodesJpegToRgba8() throws Exception {

        Path source = tempDir.resolve("photo.jpeg");
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x102030);
        image.setRGB(1, 0, 0x405060);
        image.setRGB(2, 0, 0x708090);
        image.setRGB(0, 1, 0x90A0B0);
        image.setRGB(1, 1, 0xB0C0D0);
        image.setRGB(2, 1, 0xD0E0F0);
        assertThat(ImageIO.write(image, "jpg", source.toFile())).isTrue();

        TextureImage decoded = StbTextureImporter.importFile(source);

        assertThat(decoded.width()).isEqualTo(3);
        assertThat(decoded.height()).isEqualTo(2);
        assertThat(decoded.rgba8()).hasSize(3 * 2 * 4);
        for (int index = 3; index < decoded.rgba8().length; index += 4) {
            assertThat(Byte.toUnsignedInt(decoded.rgba8()[index])).isEqualTo(255);
        }

    }

    @Test
    void rejectsUnsupportedAndMalformedSourcesWithPathDiagnostics() throws Exception {

        Path unsupported = tempDir.resolve("texture.tga");
        Files.write(unsupported, new byte[]{1, 2, 3});
        assertThatThrownBy(() -> StbTextureImporter.importFile(unsupported)).isInstanceOf(AssetCookerException.class).hasMessageContaining(unsupported.toString())
            .hasMessageContaining(".png");

        Path malformed = tempDir.resolve("broken.png");
        Files.write(malformed, new byte[]{1, 2, 3, 4});
        assertThatThrownBy(() -> StbTextureImporter.importFile(malformed)).isInstanceOf(AssetCookerException.class).hasMessageContaining(malformed.toString())
            .hasMessageContaining("decode failed");

    }
}
