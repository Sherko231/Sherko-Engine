package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextureMipChainTest {
    @Test
    void generatesDeterministicCompleteMipChainWithFloorAverages() {

        byte[] base = new byte[3 * 5 * 4];
        for (int pixel = 0; pixel < 15; pixel++) {
            int value = pixel * 10;
            int offset = pixel * 4;
            base[offset] = (byte) value;
            base[offset + 1] = (byte) (value + 1);
            base[offset + 2] = (byte) (value + 2);
            base[offset + 3] = (byte) 255;
        }

        List<TextureMipLevel> levels = TextureMipChain.generate(new TextureImage(3, 5, base));

        assertThat(levels).hasSize(3);
        assertThat(levels.get(0).width()).isEqualTo(3);
        assertThat(levels.get(0).height()).isEqualTo(5);
        assertThat(levels.get(1).width()).isEqualTo(1);
        assertThat(levels.get(1).height()).isEqualTo(2);
        assertThat(levels.get(2).width()).isEqualTo(1);
        assertThat(levels.get(2).height()).isEqualTo(1);

        assertThat(levels.get(1).rgba8()).containsExactly((byte) 20, (byte) 21, (byte) 22, (byte) 255, (byte) 80, (byte) 81, (byte) 82, (byte) 255);
        assertThat(levels.get(2).rgba8()).containsExactly((byte) 50, (byte) 51, (byte) 52, (byte) 255);

    }
}
