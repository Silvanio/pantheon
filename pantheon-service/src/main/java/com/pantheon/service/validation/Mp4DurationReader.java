package com.pantheon.service.validation;

import java.nio.charset.StandardCharsets;
import java.util.OptionalDouble;

/**
 * Reads an MP4 (ISO-BMFF) file's overall duration straight from its {@code moov > mvhd} box,
 * without decoding any audio/video or shelling out to an external tool — see
 * restrict-site-document-file-types's design.md "MP4 duration cap". Only walks the box list
 * structurally enough to reach {@code mvhd}; does not recurse into every container type.
 */
public final class Mp4DurationReader {

    private Mp4DurationReader() {
    }

    /** Duration in seconds, or empty if the file isn't a parseable MP4 with a readable {@code mvhd} box. */
    public static OptionalDouble durationSeconds(byte[] bytes) {
        Box moov = findBox(bytes, 0, bytes.length, "moov");
        if (moov == null) {
            return OptionalDouble.empty();
        }
        Box mvhd = findBox(bytes, moov.contentStart(), moov.contentEnd(), "mvhd");
        if (mvhd == null) {
            return OptionalDouble.empty();
        }
        return parseMvhd(bytes, mvhd.contentStart(), mvhd.contentEnd());
    }

    private static OptionalDouble parseMvhd(byte[] bytes, int contentStart, int contentEnd) {
        if (contentEnd - contentStart < 4) {
            return OptionalDouble.empty();
        }
        int version = bytes[contentStart] & 0xFF;
        int p = contentStart + 4; // skip version (1 byte) + flags (3 bytes)
        long timescale;
        long duration;
        if (version == 1) {
            if (p + 8 + 8 + 4 + 8 > contentEnd) {
                return OptionalDouble.empty();
            }
            p += 8 + 8; // creation_time(8) + modification_time(8)
            timescale = readUint32(bytes, p);
            p += 4;
            duration = readUint64(bytes, p);
        } else {
            if (p + 4 + 4 + 4 + 4 > contentEnd) {
                return OptionalDouble.empty();
            }
            p += 4 + 4; // creation_time(4) + modification_time(4)
            timescale = readUint32(bytes, p);
            p += 4;
            duration = readUint32(bytes, p);
        }
        if (timescale <= 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of((double) duration / (double) timescale);
    }

    /** Scans the sequential box list in {@code [start, end)} for the first box of {@code type}; does not recurse. */
    private static Box findBox(byte[] bytes, int start, int end, String type) {
        int pos = start;
        while (pos + 8 <= end) {
            long size = readUint32(bytes, pos);
            String boxType = new String(bytes, pos + 4, 4, StandardCharsets.US_ASCII);

            int headerSize = 8;
            long boxSize;
            if (size == 1) {
                if (pos + 16 > end) {
                    return null;
                }
                boxSize = readUint64(bytes, pos + 8);
                headerSize = 16;
            } else if (size == 0) {
                boxSize = end - pos;
            } else {
                boxSize = size;
            }

            if (boxSize < headerSize || pos + boxSize > end) {
                return null;
            }
            if (boxType.equals(type)) {
                return new Box(pos + headerSize, (int) (pos + boxSize));
            }
            pos += boxSize;
        }
        return null;
    }

    private static long readUint32(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFFL) << 24
                | (bytes[offset + 1] & 0xFFL) << 16
                | (bytes[offset + 2] & 0xFFL) << 8
                | (bytes[offset + 3] & 0xFFL);
    }

    private static long readUint64(byte[] bytes, int offset) {
        return (readUint32(bytes, offset) << 32) | readUint32(bytes, offset + 4);
    }

    private record Box(int contentStart, int contentEnd) {
    }
}
