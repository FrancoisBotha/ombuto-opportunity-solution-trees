package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.OpportunitySolutionTreeApp;
import com.opportunity.tree.config.AsyncSyncConfiguration;
import com.opportunity.tree.config.EmbeddedSQL;
import com.opportunity.tree.config.JacksonConfiguration;
import com.opportunity.tree.config.TestSecurityConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * BKRST fix C3: an upload over {@code spring.servlet.multipart.max-file-size}.
 *
 * <p>MockMvc hands the controller a {@code MockMultipartFile} and never runs the servlet
 * container's multipart resolver, so it cannot see this failure at all — the request has to be a
 * real one over a real port. The limit is lowered to a few kilobytes here so the test does not have
 * to push the production-sized limit through a socket.
 *
 * <p>The body is parsed lazily, the first time a filter asks for a request parameter, which for a
 * POST is Spring Security's CSRF filter — before authentication and long before the dispatcher
 * servlet. So this request is deliberately anonymous: it never gets far enough to be authenticated,
 * and the answer must still be the documented 413 with its message key rather than a bare container
 * error page.
 */
@SpringBootTest(
    classes = {
        OpportunitySolutionTreeApp.class,
        JacksonConfiguration.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        com.opportunity.tree.config.JacksonHibernateConfiguration.class,
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = { "spring.servlet.multipart.max-file-size=8KB", "spring.servlet.multipart.max-request-size=8KB" }
)
@EmbeddedSQL
class UploadSizeLimitIT {

    private static final String BOUNDARY = "----bkrstUploadLimitBoundary";

    @LocalServerPort
    private int port;

    @Test
    void anArchiveOverTheMultipartLimitIsRejectedWithATypedPayloadTooLarge() throws Exception {
        byte[] oversized = new byte[64 * 1024];
        java.util.Arrays.fill(oversized, (byte) 'x');

        HttpResponse<String> response = post("/api/admin/backup/restore", multipartBody(oversized));

        assertThat(response.statusCode()).as("an over-sized upload is a 413, not a bare 403/500").isEqualTo(413);
        assertThat(response.body()).contains("error.upload.tooLarge");
        assertThat(response.body()).doesNotContain("Exception");
    }

    @Test
    void anArchiveUnderTheLimitStillReachesTheSecurityLayer() throws Exception {
        HttpResponse<String> response = post("/api/admin/backup/restore", multipartBody("{}".getBytes(StandardCharsets.UTF_8)));

        assertThat(response.statusCode())
            .as("under the limit the request is answered by security, not by the size filter")
            .isNotEqualTo(413);
    }

    private HttpResponse<String> post(String path, byte[] body) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static byte[] multipartBody(byte[] content) {
        String prologue =
            "--" +
            BOUNDARY +
            "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"backup.json\"\r\n" +
            "Content-Type: application/json\r\n\r\n";
        String epilogue = "\r\n--" + BOUNDARY + "--\r\n";
        byte[] head = prologue.getBytes(StandardCharsets.UTF_8);
        byte[] tail = epilogue.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[head.length + content.length + tail.length];
        System.arraycopy(head, 0, body, 0, head.length);
        System.arraycopy(content, 0, body, head.length, content.length);
        System.arraycopy(tail, 0, body, head.length + content.length, tail.length);
        return body;
    }
}
