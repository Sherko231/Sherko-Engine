package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StbVorbisAudioImporterTest {
    @TempDir
    Path tempDir;

    @Test
    void validatesMonoAndMixedCaseExtensionWithIndependentMetadataOracle() throws Exception {

        byte[] sourceBytes = VorbisTestFixtures.read("p6/mono-22050.ogg.b64");
        Path source = tempDir.resolve("mono.OgG");
        Files.write(source, sourceBytes);

        CookedAudio audio = StbVorbisAudioImporter.importFile(source);

        assertThat(audio.channels()).isEqualTo(1);
        assertThat(audio.sampleRate()).isEqualTo(22050);
        assertThat(audio.vorbisPayload()).isEqualTo(sourceBytes);

    }

    @Test
    void validatesStereoWithIndependentMetadataOracle() throws Exception {

        byte[] sourceBytes = VorbisTestFixtures.read("p6/stereo-44100.ogg");
        Path source = tempDir.resolve("stereo.ogg");
        Files.write(source, sourceBytes);

        CookedAudio audio = StbVorbisAudioImporter.importFile(source);

        assertThat(audio.channels()).isEqualTo(2);
        assertThat(audio.sampleRate()).isEqualTo(44100);
        assertThat(audio.vorbisPayload()).isEqualTo(sourceBytes);

    }

    @Test
    void rejectsUnsupportedMalformedTruncatedAndMultichannelSourcesWithPathDiagnostics() throws Exception {

        Path unsupported = tempDir.resolve("audio.wav");
        Files.write(unsupported, new byte[]{1, 2, 3});
        assertThatThrownBy(() -> StbVorbisAudioImporter.importFile(unsupported)).isInstanceOf(AssetCookerException.class).hasMessageContaining(unsupported.toString())
            .hasMessageContaining(".ogg");

        Path malformed = tempDir.resolve("malformed.ogg");
        Files.write(malformed, new byte[]{'O', 'g', 'g', 'S', 1, 2, 3, 4});
        assertThatThrownBy(() -> StbVorbisAudioImporter.importFile(malformed)).isInstanceOf(AssetCookerException.class).hasMessageContaining(malformed.toString())
            .hasMessageContaining("open failed");

        byte[] valid = VorbisTestFixtures.read("p6/stereo-44100.ogg");
        Path truncated = tempDir.resolve("truncated.ogg");
        Files.write(truncated, Arrays.copyOf(valid, 128));
        assertThatThrownBy(() -> StbVorbisAudioImporter.importFile(truncated)).isInstanceOf(AssetCookerException.class).hasMessageContaining(truncated.toString());

        Path multichannel = tempDir.resolve("surround.ogg");
        Files.write(multichannel, VorbisTestFixtures.read("p6/three-channel-32000.ogg.b64"));
        assertThatThrownBy(() -> StbVorbisAudioImporter.importFile(multichannel)).isInstanceOf(AssetCookerException.class).hasMessageContaining(multichannel.toString())
            .hasMessageContaining("mono or stereo").hasMessageContaining("3");

    }
}
