package com.pantheon.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class Mp4DurationReaderTest {

    @Test
    void readsDurationFromVersionZeroMvhd() {
        byte[] mp4 = buildMp4(version0MvhdContent(1000, 30_000));

        OptionalDouble duration = Mp4DurationReader.durationSeconds(mp4);

        assertThat(duration).isPresent();
        assertThat(duration.getAsDouble()).isCloseTo(30.0, within(0.001));
    }

    @Test
    void readsDurationFromVersionOneMvhd() {
        byte[] mp4 = buildMp4(version1MvhdContent(1000, 45_000L));

        OptionalDouble duration = Mp4DurationReader.durationSeconds(mp4);

        assertThat(duration).isPresent();
        assertThat(duration.getAsDouble()).isCloseTo(45.0, within(0.001));
    }

    @Test
    void returnsEmptyWhenNoMoovBoxPresent() {
        OptionalDouble duration = Mp4DurationReader.durationSeconds("not an mp4 file at all".getBytes());

        assertThat(duration).isEmpty();
    }

    @Test
    void returnsEmptyWhenMoovHasNoMvhd() throws IOException {
        var out = new ByteArrayOutputStream();
        var dos = new DataOutputStream(out);
        dos.writeInt(8); // empty moov box (header only, no children)
        dos.writeBytes("moov");

        assertThat(Mp4DurationReader.durationSeconds(out.toByteArray())).isEmpty();
    }

    @Test
    void returnsEmptyWhenTimescaleIsZero() {
        byte[] mp4 = buildMp4(version0MvhdContent(0, 30_000));

        assertThat(Mp4DurationReader.durationSeconds(mp4)).isEmpty();
    }

    private static byte[] version0MvhdContent(int timescale, int durationUnits) {
        try {
            var content = new ByteArrayOutputStream();
            var dos = new DataOutputStream(content);
            dos.writeInt(0); // version(1) + flags(3)
            dos.writeInt(0); // creation_time
            dos.writeInt(0); // modification_time
            dos.writeInt(timescale);
            dos.writeInt(durationUnits);
            return content.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] version1MvhdContent(int timescale, long durationUnits) {
        try {
            var content = new ByteArrayOutputStream();
            var dos = new DataOutputStream(content);
            dos.writeInt(0x01000000); // version(1) = 1, flags(3) = 0
            dos.writeLong(0); // creation_time (64-bit)
            dos.writeLong(0); // modification_time (64-bit)
            dos.writeInt(timescale);
            dos.writeLong(durationUnits);
            return content.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] buildMp4(byte[] mvhdContent) {
        try {
            var out = new ByteArrayOutputStream();
            var dos = new DataOutputStream(out);
            dos.writeInt(16); // ftyp box: 8-byte header + 8-byte content
            dos.writeBytes("ftyp");
            dos.writeBytes("isom");
            dos.writeInt(0);

            int mvhdBoxSize = 8 + mvhdContent.length;
            dos.writeInt(8 + mvhdBoxSize); // moov box size
            dos.writeBytes("moov");
            dos.writeInt(mvhdBoxSize);
            dos.writeBytes("mvhd");
            dos.write(mvhdContent);

            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
