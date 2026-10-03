package com.feedstartup.util;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Recognises an uploaded video from its leading bytes - the video counterpart of
 * {@link ImageFiles#sniff}. Only formats every major browser can play in a {@code <video>} tag
 * are accepted: MP4 and WebM.
 */
public final class VideoFiles {

    public static final String ALLOWED_TYPES_LABEL = "MP4 or WebM";

    /** How many leading bytes {@link #sniff} looks at. */
    public static final int HEAD_BYTES = 64;

    // ISO-BMFF brands that share MP4's "ftyp" container but aren't playable video: AVIF/HEIF
    // stills and sequences, and QuickTime (.mov), which Firefox and most Android browsers can't play.
    private static final Set<String> NON_VIDEO_BRANDS = Set.of(
            "avif", "avis", "heic", "heix", "heim", "heis", "hevc", "hevx", "mif1", "msf1", "qt  ");

    private VideoFiles() {}

    /** One recognised video format: the extension a stored copy gets and the type it is served as. */
    public record VideoType(String extension, String contentType) {}

    /** Empty when {@code head} isn't the start of a supported video. */
    public static Optional<VideoType> sniff(byte[] head) {
        if (head == null || head.length < 12) return Optional.empty();
        // MP4: bytes 4-7 are "ftyp", then the 4-character major brand.
        if (head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') {
            String brand = new String(head, 8, 4, StandardCharsets.US_ASCII);
            return NON_VIDEO_BRANDS.contains(brand) ? Optional.empty() : Optional.of(new VideoType(".mp4", "video/mp4"));
        }
        // WebM: an EBML header (1A 45 DF A3) whose DocType is "webm" - plain Matroska (.mkv) shares
        // the header but not the DocType, and browsers don't reliably play it.
        if ((head[0] & 0xFF) == 0x1A && (head[1] & 0xFF) == 0x45 && (head[2] & 0xFF) == 0xDF && (head[3] & 0xFF) == 0xA3
                && new String(head, StandardCharsets.US_ASCII).contains("webm")) {
            return Optional.of(new VideoType(".webm", "video/webm"));
        }
        return Optional.empty();
    }
}
