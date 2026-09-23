package com.opportunity.tree.web.rest;

import static com.opportunity.tree.web.rest.TreeCollaborationFixture.EDITOR;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OUTSIDER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.OWNER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.VIEWER;
import static com.opportunity.tree.web.rest.TreeCollaborationFixture.who;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.MeetingTranscript;
import com.opportunity.tree.service.TranscriptUploadParser;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@code /api/tree/teams/{teamId}/transcripts/parse} parse-only upload
 * endpoint (Epic 12 / MTRANS-003). Covers:
 *
 * <ul>
 *   <li>OWNER and EDITOR can upload .txt, .vtt and .srt and receive plain text back.</li>
 *   <li>VIEWER, non-member and admin-without-membership are rejected with 403.</li>
 *   <li>Unknown extensions, empty and oversize uploads are rejected with a stable error key.</li>
 *   <li>Nothing is stored: MeetingTranscript rows are never created by the parse endpoint.</li>
 * </ul>
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class TreeMeetingTranscriptParseIT {

    private static final String PARSE = "/api/tree/teams/{teamId}/transcripts/parse";

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mvc;

    private TreeCollaborationFixture f;

    @BeforeEach
    void setUp() {
        f = new TreeCollaborationFixture(em);
    }

    private MockMultipartFile file(String filename, String contentType, String body) {
        return new MockMultipartFile("file", filename, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile file(String filename, byte[] bytes) {
        return new MockMultipartFile("file", filename, "text/plain", bytes);
    }

    // --- Criterion 1: editors and owners can upload txt/vtt/srt and get plain text ---

    @Test
    void ownerUploadsPlainTextAndReceivesParsedBody() throws Exception {
        MockMultipartFile upload = file("meeting.txt", "text/plain", "Alex: Hello.\nSam: Hi.");
        mvc
            .perform(multipart(PARSE, f.team.getId()).file(upload).with(who(OWNER)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("Alex: Hello.\nSam: Hi."));
        assertThat(f.count("select count(t) from MeetingTranscript t")).isZero();
    }

    @Test
    void editorUploadsVttAndTimestampsAndCueNumbersAreStripped() throws Exception {
        String vtt =
            "WEBVTT\n\n" +
            "1\n" +
            "00:00:00.000 --> 00:00:04.000\n" +
            "<v Alex>Welcome to the call.\n\n" +
            "2\n" +
            "00:00:04.500 --> 00:00:07.000\n" +
            "<v Sam>Thanks for joining.\n";
        MockMultipartFile upload = file("meeting.vtt", "text/vtt", vtt);
        mvc
            .perform(multipart(PARSE, f.team.getId()).file(upload).with(who(EDITOR)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("Alex: Welcome to the call.")))
            .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("Sam: Thanks for joining.")))
            .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("-->"))))
            .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("WEBVTT"))));
        assertThat(f.count("select count(t) from MeetingTranscript t")).isZero();
    }

    @Test
    void editorUploadsSrtAndTimestampsAndCueNumbersAreStripped() throws Exception {
        String srt =
            "1\n" + "00:00:00,000 --> 00:00:04,000\n" + "Alex: Welcome.\n\n" + "2\n" + "00:00:04,500 --> 00:00:07,000\n" + "Sam: Thanks.\n";
        MockMultipartFile upload = file("meeting.srt", "application/x-subrip", srt);
        mvc
            .perform(multipart(PARSE, f.team.getId()).file(upload).with(who(EDITOR)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("Alex: Welcome.")))
            .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("Sam: Thanks.")))
            .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("-->"))));
    }

    @Test
    void editorUploadsVttDialectWithoutSpeakerTagsAndTextIsRecovered() throws Exception {
        // Granola-style: no <v> tags, speaker printed inline; still expected to parse.
        String vtt =
            "WEBVTT\n\n" +
            "00:00:00.000 --> 00:00:03.000\n" +
            "Alex: Kicking off.\n\n" +
            "00:00:03.500 --> 00:00:06.000\n" +
            "Sam: Sounds good.\n";
        mvc
            .perform(multipart(PARSE, f.team.getId()).file(file("call.vtt", "text/vtt", vtt)).with(who(EDITOR)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("Alex: Kicking off.")))
            .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("Sam: Sounds good.")));
    }

    // --- Criterion 5: role / team access ---

    @Test
    void viewerCannotUpload() throws Exception {
        mvc
            .perform(multipart(PARSE, f.team.getId()).file(file("m.txt", "text/plain", "hi")).with(who(VIEWER)).with(csrf()))
            .andExpect(status().isForbidden());
        assertThat(f.count("select count(t) from MeetingTranscript t")).isZero();
    }

    @Test
    void nonMemberAndAdminCannotUpload() throws Exception {
        for (String login : List.of(OUTSIDER, "admin")) {
            mvc
                .perform(multipart(PARSE, f.team.getId()).file(file("m.txt", "text/plain", "hi")).with(who(login)).with(csrf()))
                .andExpect(status().isForbidden());
        }
        assertThat(f.count("select count(t) from MeetingTranscript t")).isZero();
    }

    @Test
    void cannotUploadToAnotherTeam() throws Exception {
        // The editor is a member of f.team, not f.otherTeam.
        mvc
            .perform(multipart(PARSE, f.otherTeam.getId()).file(file("m.txt", "text/plain", "hi")).with(who(EDITOR)).with(csrf()))
            .andExpect(status().isForbidden());
    }

    // --- Criterion 2: wrong extension, empty and oversize are rejected ---

    @Test
    void wrongExtensionIsRejected() throws Exception {
        mvc
            .perform(multipart(PARSE, f.team.getId()).file(file("m.docx", "application/octet-stream", "x")).with(who(EDITOR)).with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.transcriptuploadextension"));
        assertThat(f.count("select count(t) from MeetingTranscript t")).isZero();
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        mvc
            .perform(multipart(PARSE, f.team.getId()).file(file("m.txt", new byte[0])).with(who(EDITOR)).with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.transcriptuploadempty"));
    }

    @Test
    void malformedTextIsRejected() throws Exception {
        byte[] bad = new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0xFD };
        mvc
            .perform(multipart(PARSE, f.team.getId()).file(file("m.txt", bad)).with(who(EDITOR)).with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.transcriptuploadmalformed"));
    }

    @Test
    void filesOverOneMibAreRejected() throws Exception {
        byte[] big = new byte[TranscriptUploadParser.MAX_UPLOAD_BYTES + 1];
        java.util.Arrays.fill(big, (byte) 'a');
        mvc
            .perform(multipart(PARSE, f.team.getId()).file(file("big.txt", big)).with(who(EDITOR)).with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.transcriptuploadtoolarge"));
        assertThat(f.count("select count(t) from MeetingTranscript t")).isZero();
    }

    // --- Criterion 4: no MeetingTranscript row is stored by any successful parse ---

    @Test
    void successfulParseDoesNotWriteAnyMeetingTranscriptRow() throws Exception {
        String vtt = "WEBVTT\n\n00:00:00.000 --> 00:00:01.000\nAlex: Hello.\n";
        mvc
            .perform(multipart(PARSE, f.team.getId()).file(file("m.vtt", "text/vtt", vtt)).with(who(EDITOR)).with(csrf()))
            .andExpect(status().isOk());
        List<MeetingTranscript> rows = em.createQuery("select t from MeetingTranscript t", MeetingTranscript.class).getResultList();
        assertThat(rows).isEmpty();
    }
}
