package com.opportunity.tree.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Turns an over-sized multipart upload into a clean {@code 413} (BKRST fix C3).
 *
 * <p>The multipart body is parsed lazily, the first time something asks for a request parameter.
 * For a POST that is Spring Security's {@code CsrfFilter}, long before the dispatcher servlet and
 * therefore long before {@code @ControllerAdvice} can see anything: the
 * {@link MaxUploadSizeExceededException} escapes the whole filter chain and the client gets a bare
 * container error page. Sitting outermost — ahead of the security chain — this filter catches it
 * wherever in the chain it was raised and writes the same RFC 7807 body the
 * {@code ExceptionTranslator} produces for the dispatcher path, so the UI can show one message for
 * both.
 *
 * <p>The answer deliberately does not depend on who is calling: the request never got far enough to
 * be authenticated, and the size of a request body is not something an unauthenticated caller
 * learns anything from.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UploadSizeLimitFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(UploadSizeLimitFilter.class);

    /** Message key the client maps to a "that file is too large" message. */
    public static final String ERR_UPLOAD_TOO_LARGE = "error.upload.tooLarge";

    public static final String TOO_LARGE_DETAIL = "The uploaded file is larger than this server accepts.";

    private final long maxRequestSize;

    public UploadSizeLimitFilter(MultipartProperties multipartProperties) {
        DataSize configured = multipartProperties.getMaxRequestSize();
        this.maxRequestSize = configured == null ? -1 : configured.toBytes();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        // Checked up front, from Content-Length, rather than waiting for the parse to fail: Tomcat
        // does not let that failure out as an exception. It records it, sets the status to 413
        // itself and hands back an empty parameter map, so the request carries on through the
        // security chain and the caller ends up with the container's HTML error page instead of a
        // body anything can read.
        if (isOverSized(request)) {
            reject(request, response, "Content-Length " + request.getContentLengthLong() + " over the " + maxRequestSize + " byte limit");
            return;
        }
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException failure) {
            // A chunked upload has no Content-Length to check, so the parse failure is still caught
            // wherever in the chain it was raised.
            if (!isTooLarge(failure) || response.isCommitted()) {
                throw failure;
            }
            reject(request, response, failure.getMessage());
        }
    }

    private boolean isOverSized(HttpServletRequest request) {
        if (maxRequestSize < 0) {
            return false;
        }
        String contentType = request.getContentType();
        return (
            contentType != null &&
            contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/") &&
            request.getContentLengthLong() > maxRequestSize
        );
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String reason) throws IOException {
        LOG.warn("Rejected an over-sized upload on {}: {}", request.getRequestURI(), reason);
        response.reset();
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response
            .getWriter()
            .write(
                "{\"type\":\"https://www.jhipster.tech/problem/problem-with-message\"," +
                "\"title\":\"Payload Too Large\",\"status\":413," +
                "\"detail\":\"" +
                TOO_LARGE_DETAIL +
                "\",\"message\":\"" +
                ERR_UPLOAD_TOO_LARGE +
                "\",\"path\":\"" +
                escape(request.getRequestURI()) +
                "\"}"
            );
        response.flushBuffer();
    }

    private static boolean isTooLarge(Throwable error) {
        for (Throwable t = error; t != null && t.getCause() != t; t = t.getCause()) {
            if (t instanceof MaxUploadSizeExceededException) {
                return true;
            }
        }
        return false;
    }

    /** The URI goes into a JSON string literal; it is never trusted to be free of quotes or backslashes. */
    private static String escape(String uri) {
        return uri == null ? "" : uri.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
