package com.opportunity.tree.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TranscriptUploadParser} (Epic 12 / MTRANS-003):
 *
 * <ul>
 *   <li>Format acceptance for {@code .txt}, {@code .vtt}, {@code .srt}.</li>
 *   <li>VTT and SRT cleanup: timestamps and cue numbering removed, speaker labels kept.</li>
 *   <li>Dialect variants (Zoom / Teams / Granola-style) recovered without a hard failure.</li>
 *   <li>Rejections: empty, too large, unknown extension, malformed UTF-8.</li>
 * </ul>
 */
class TranscriptUploadParserTest {

    private final TranscriptUploadParser parser = new TranscriptUploadParser();

    @Test
    void parsesPlainTextAsIs() {
        String body = "Alex: Welcome.\nSam: Thanks for joining.\n";
        String out = parser.parse("meeting.txt", body.getBytes(StandardCharsets.UTF_8));
        assertThat(out).isEqualTo("Alex: Welcome.\nSam: Thanks for joining.");
    }

    @Test
    void parsesVttAndRemovesHeaderTimestampsAndCueNumbers() {
        String vtt =
            "WEBVTT\n" +
            "\n" +
            "NOTE\n" +
            "This is a note that should be dropped.\n" +
            "\n" +
            "1\n" +
            "00:00:00.000 --> 00:00:04.000\n" +
            "<v Alex>Welcome to the call.\n" +
            "\n" +
            "2\n" +
            "00:00:04.500 --> 00:00:07.000\n" +
            "<v Sam>Thanks for having me.\n";
        String out = parser.parse("meeting.vtt", vtt.getBytes(StandardCharsets.UTF_8));
        assertThat(out).contains("Alex: Welcome to the call.");
        assertThat(out).contains("Sam: Thanks for having me.");
        assertThat(out).doesNotContain("WEBVTT");
        assertThat(out).doesNotContain("-->");
        assertThat(out).doesNotContain("NOTE");
        assertThat(out).doesNotContain("should be dropped");
        // Cue numbering removed.
        assertThat(out).doesNotContainPattern("(?m)^[12]$");
    }

    @Test
    void parsesSrtAndRemovesTimestampsAndCueNumbers() {
        String srt =
            "1\n" +
            "00:00:00,000 --> 00:00:04,000\n" +
            "Alex: Welcome to the call.\n" +
            "\n" +
            "2\n" +
            "00:00:04,500 --> 00:00:07,000\n" +
            "Sam: Thanks for having me.\n";
        String out = parser.parse("meeting.srt", srt.getBytes(StandardCharsets.UTF_8));
        assertThat(out).contains("Alex: Welcome to the call.");
        assertThat(out).contains("Sam: Thanks for having me.");
        assertThat(out).doesNotContain("-->");
        assertThat(out).doesNotContainPattern("(?m)^[12]$");
    }

    @Test
    void recoversVttDialectWithoutRecognisedSpeakerTags() {
        // Zoom-style: no <v> tags, speaker printed inline; timestamps and cue numbers still present.
        String vtt =
            "WEBVTT\n" +
            "\n" +
            "1\n" +
            "00:00:00.000 --> 00:00:03.000\n" +
            "Alex: Kicking off.\n" +
            "\n" +
            "2\n" +
            "00:00:03.500 --> 00:00:06.000\n" +
            "Sam: Sounds good.\n";
        String out = parser.parse("call.vtt", vtt.getBytes(StandardCharsets.UTF_8));
        assertThat(out).contains("Alex: Kicking off.");
        assertThat(out).contains("Sam: Sounds good.");
        assertThat(out).doesNotContain("-->");
        assertThat(out).doesNotContain("WEBVTT");
    }

    @Test
    void stripsInlineVttCueTagsButKeepsText() {
        String vtt = "WEBVTT\n\n" + "00:00:00.000 --> 00:00:04.000\n" + "<v.first Alex>Hello <c.emphasis>everyone</c>.\n";
        String out = parser.parse("meeting.vtt", vtt.getBytes(StandardCharsets.UTF_8));
        assertThat(out).contains("Alex: Hello everyone.");
        assertThat(out).doesNotContain("<c");
        assertThat(out).doesNotContain("<v");
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertThatThrownBy(() -> parser.parse("meeting.docx", "hi".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(NodeWriteRuleException.class)
            .hasFieldOrPropertyWithValue("errorKey", "transcriptuploadextension");
        assertThatThrownBy(() -> parser.parse("no-extension", "hi".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(NodeWriteRuleException.class)
            .hasFieldOrPropertyWithValue("errorKey", "transcriptuploadextension");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> parser.parse("meeting.txt", new byte[0]))
            .isInstanceOf(NodeWriteRuleException.class)
            .hasFieldOrPropertyWithValue("errorKey", "transcriptuploadempty");
    }

    @Test
    void rejectsWhitespaceOnlyFile() {
        assertThatThrownBy(() -> parser.parse("meeting.txt", "   \n\n\t\n".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(NodeWriteRuleException.class)
            .hasFieldOrPropertyWithValue("errorKey", "transcriptuploadempty");
    }

    @Test
    void rejectsFilesLargerThanOneMiB() {
        byte[] big = new byte[TranscriptUploadParser.MAX_UPLOAD_BYTES + 1];
        java.util.Arrays.fill(big, (byte) 'a');
        assertThatThrownBy(() -> parser.parse("big.txt", big))
            .isInstanceOf(NodeWriteRuleException.class)
            .hasFieldOrPropertyWithValue("errorKey", "transcriptuploadtoolarge");
    }

    @Test
    void rejectsMalformedUtf8() {
        // Standalone 0xFF is not legal UTF-8.
        byte[] bad = new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0xFD };
        assertThatThrownBy(() -> parser.parse("meeting.txt", bad))
            .isInstanceOf(NodeWriteRuleException.class)
            .hasFieldOrPropertyWithValue("errorKey", "transcriptuploadmalformed");
    }

    @Test
    void extensionMatchIsCaseInsensitive() {
        String body = "hi";
        assertThat(parser.parse("meeting.TXT", body.getBytes(StandardCharsets.UTF_8))).isEqualTo("hi");
        assertThat(
            parser.parse("meeting.Vtt", ("WEBVTT\n\n00:00:00.000 --> 00:00:01.000\nhi\n").getBytes(StandardCharsets.UTF_8))
        ).contains("hi");
        assertThat(parser.parse("meeting.SRT", ("1\n00:00:00,000 --> 00:00:01,000\nhi\n").getBytes(StandardCharsets.UTF_8))).contains("hi");
    }

    @Test
    void preservesLineBreaksBetweenCues() {
        String vtt =
            "WEBVTT\n\n" + "1\n00:00:00.000 --> 00:00:04.000\nFirst line.\n\n" + "2\n00:00:05.000 --> 00:00:07.000\nSecond line.\n";
        String out = parser.parse("meeting.vtt", vtt.getBytes(StandardCharsets.UTF_8));
        // Cues must not run into each other on one line.
        assertThat(out).contains("First line.\n").contains("Second line.");
        // At least one blank line between them (paragraph break).
        assertThat(out.split("\\R+")).contains("First line.", "Second line.");
    }
}
