package com.opportunity.tree.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Parses an uploaded {@code .txt}, {@code .vtt} or {@code .srt} transcript file into plain text
 * for review (Epic 12 / MTRANS-003).
 *
 * <p>The parser is stateless and does no I/O beyond decoding the given byte array as UTF-8. It
 * treats the bytes as text only — no HTML rendering, no document parser is introduced (NFR-023).
 * Nothing is stored: this class only converts bytes to a normalised string. The caller is
 * responsible for team- and role-scoped access, and for choosing whether to log or discard the
 * output. This service itself logs nothing — the parsed text must never appear in application
 * logs (NFR-022).
 *
 * <p>Validation happens in a strict order so the client gets a single, deterministic error key:
 *
 * <ol>
 *   <li>Extension — one of {@code .txt}, {@code .vtt}, {@code .srt} (case-insensitive).</li>
 *   <li>Size — up to {@link #MAX_UPLOAD_BYTES} bytes.</li>
 *   <li>Non-empty after UTF-8 decoding and whitespace strip.</li>
 *   <li>Well-formed UTF-8 (rejected before parsing, never truncated silently).</li>
 * </ol>
 *
 * <p>VTT/SRT cleanup removes timestamp cues ({@code hh:mm:ss.mmm --> hh:mm:ss.mmm}), cue
 * numbering, and structural headers ({@code WEBVTT}, {@code NOTE}, {@code STYLE}, {@code REGION}
 * blocks) while preserving spoken text, line breaks and speaker labels. The parser degrades
 * gracefully on unrecognised dialect variants — Zoom, Teams and Granola each write speaker
 * labels differently, and the recovery is "keep the text, drop the timing" rather than fail
 * the upload.
 */
@Component
public class TranscriptUploadParser {

    /** Documented ceiling — 1 MiB, per acceptance criterion 2 (matches the pasted-body ceiling). */
    public static final int MAX_UPLOAD_BYTES = 1024 * 1024;

    public static final String ENTITY_NAME = "meetingTranscript";

    private static final Pattern TIMESTAMP_LINE = Pattern.compile(".*-->.*");
    private static final Pattern CUE_NUMBER_LINE = Pattern.compile("\\d+");
    private static final Pattern VOICE_TAG = Pattern.compile("<v(?:\\.[^\\s>]+)*\\s+([^>]+)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern OTHER_CUE_TAG = Pattern.compile("</?[a-zA-Z][^>]*>");

    public String parse(String filename, byte[] bytes) {
        Format format = detectFormat(filename);
        if (bytes.length > MAX_UPLOAD_BYTES) {
            throw reject("transcriptuploadtoolarge", "Uploaded file exceeds the " + MAX_UPLOAD_BYTES + " byte limit");
        }
        if (bytes.length == 0) {
            throw reject("transcriptuploadempty", "Uploaded file is empty");
        }
        String text = decodeStrictUtf8(bytes);
        // A file of pure whitespace is empty from the caller's point of view, and would leave the
        // pasted-body validator with nothing to save — reject it here with a clearer key.
        if (text.strip().isEmpty()) {
            throw reject("transcriptuploadempty", "Uploaded file is empty");
        }
        return switch (format) {
            case TXT -> text.strip();
            case VTT, SRT -> cleanCues(text);
        };
    }

    private Format detectFormat(String filename) {
        String name = filename == null ? "" : filename;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw reject("transcriptuploadextension", "Uploaded file must be .txt, .vtt or .srt");
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "txt" -> Format.TXT;
            case "vtt" -> Format.VTT;
            case "srt" -> Format.SRT;
            default -> throw reject("transcriptuploadextension", "Uploaded file must be .txt, .vtt or .srt");
        };
    }

    private String decodeStrictUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        } catch (CharacterCodingException e) {
            throw reject("transcriptuploadmalformed", "Uploaded file is not valid UTF-8 text");
        }
    }

    /**
     * Strips VTT/SRT structure from {@code text} while keeping spoken content and speaker labels.
     * The algorithm is deliberately loose so unrecognised dialects still produce readable output
     * ("keep the text, drop the timing" — Epic 12 §10). Blank lines between cues become paragraph
     * breaks; a run of blank lines is collapsed to one.
     */
    private String cleanCues(String text) {
        String[] lines = text.split("\\R", -1);
        List<String> out = new ArrayList<>(lines.length);
        boolean inBlock = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String stripped = line.strip();
            if (inBlock) {
                if (stripped.isEmpty()) {
                    inBlock = false;
                }
                continue;
            }
            if (isHeaderLine(stripped)) {
                continue;
            }
            if (startsSkipBlock(stripped)) {
                inBlock = true;
                continue;
            }
            if (TIMESTAMP_LINE.matcher(stripped).matches()) {
                continue;
            }
            if (CUE_NUMBER_LINE.matcher(stripped).matches() && nextNonBlankIsTimestamp(lines, i + 1)) {
                continue;
            }
            out.add(extractCueText(line));
        }
        return collapseBlankRuns(out);
    }

    private static boolean isHeaderLine(String stripped) {
        // WEBVTT header — may carry a text description after it (e.g. "WEBVTT Kind: captions").
        return stripped.equals("WEBVTT") || stripped.startsWith("WEBVTT ") || stripped.startsWith("WEBVTT\t");
    }

    private static boolean startsSkipBlock(String stripped) {
        if (stripped.equals("STYLE") || stripped.equals("REGION")) {
            return true;
        }
        // NOTE optionally followed by whitespace + text — spec-defined comment block.
        if (stripped.equals("NOTE") || stripped.startsWith("NOTE ") || stripped.startsWith("NOTE\t")) {
            return true;
        }
        return false;
    }

    private static boolean nextNonBlankIsTimestamp(String[] lines, int from) {
        for (int j = from; j < lines.length; j++) {
            String s = lines[j].strip();
            if (s.isEmpty()) {
                continue;
            }
            return TIMESTAMP_LINE.matcher(s).matches();
        }
        return false;
    }

    private static String extractCueText(String line) {
        Matcher voice = VOICE_TAG.matcher(line);
        if (voice.find()) {
            String speaker = voice.group(1).trim();
            String rest = line.substring(voice.end());
            return (speaker + ": " + OTHER_CUE_TAG.matcher(rest).replaceAll("")).stripTrailing();
        }
        return OTHER_CUE_TAG.matcher(line).replaceAll("").stripTrailing();
    }

    private static String collapseBlankRuns(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        boolean previousBlank = true; // trims leading blank lines
        for (String raw : lines) {
            boolean blank = raw.strip().isEmpty();
            if (blank) {
                if (!previousBlank) {
                    sb.append('\n');
                }
                previousBlank = true;
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(raw);
            previousBlank = false;
        }
        // Trim trailing blank(s) — collapseBlankRuns leaves at most one trailing newline.
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == '\n') {
            end--;
        }
        sb.setLength(end);
        return sb.toString();
    }

    private static NodeWriteRuleException reject(String errorKey, String message) {
        return new NodeWriteRuleException(message, ENTITY_NAME, errorKey);
    }

    private enum Format {
        TXT,
        VTT,
        SRT,
    }
}
