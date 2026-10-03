package com.feedstartup.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoFilesTest {

    @Test
    void recognisesMp4AndWebmFromTheirBytes() {
        assertEquals("video/mp4", VideoFiles.sniff(pad(ascii("\0\0\0\u0018ftypisom"))).orElseThrow().contentType());
        assertEquals(".mp4", VideoFiles.sniff(pad(ascii("\0\0\0 ftypmp42"))).orElseThrow().extension());
        byte[] webm = pad(new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, (byte) 0x9F, 0x42, (byte) 0x82, (byte) 0x84, 'w', 'e', 'b', 'm'});
        assertEquals(".webm", VideoFiles.sniff(webm).orElseThrow().extension());
    }

    @Test
    void rejectsStillImagesQuickTimeAndMatroskaThatShareTheContainers() {
        assertTrue(VideoFiles.sniff(pad(ascii("\0\0\0 ftypavif"))).isEmpty());
        assertTrue(VideoFiles.sniff(pad(ascii("\0\0\0 ftypheic"))).isEmpty());
        assertTrue(VideoFiles.sniff(pad(ascii("\0\0\0\u0014ftypqt  "))).isEmpty());
        byte[] mkv = pad(new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, (byte) 0x9F, 0x42, (byte) 0x82, (byte) 0x88, 'm', 'a', 't', 'r', 'o', 's', 'k', 'a'});
        assertTrue(VideoFiles.sniff(mkv).isEmpty());
    }

    @Test
    void rejectsNonVideosWhateverTheyAreCalled() {
        assertTrue(VideoFiles.sniff(pad(ascii("<html><body>"))).isEmpty());
        assertTrue(VideoFiles.sniff(new byte[]{1, 2, 3}).isEmpty());
        assertTrue(VideoFiles.sniff(null).isEmpty());
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] pad(byte[] head) {
        return Arrays.copyOf(head, VideoFiles.HEAD_BYTES);
    }
}
