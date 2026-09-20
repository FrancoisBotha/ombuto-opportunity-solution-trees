package com.opportunity.tree.web.rest.errors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests {@link ExceptionTranslator} controller advice.
 */
@WithMockUser
@AutoConfigureMockMvc
@IntegrationTest
class ExceptionTranslatorIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testConcurrencyFailure() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/concurrency-failure").with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value(ErrorConstants.ERR_CONCURRENCY_FAILURE));
    }

    @Test
    void dataIntegrityViolationIsAConflictWithoutSql() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/data-integrity").with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.dataintegrity"))
            .andExpect(jsonPath("$.detail").value(ExceptionTranslator.DATA_INTEGRITY_DETAIL))
            .andExpect(content().string(not(containsString("delete from"))))
            .andExpect(content().string(not(containsString("foreign key"))));
    }

    @Test
    void untranslatedConstraintViolationsAreAConflictWithoutSql() throws Exception {
        for (String path : new String[] { "hibernate-constraint-violation", "constraint-violation-at-commit" }) {
            mockMvc
                .perform(get("/api/exception-translator-test/" + path).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.message").value("error.dataintegrity"))
                .andExpect(jsonPath("$.detail").value(ExceptionTranslator.DATA_INTEGRITY_DETAIL))
                .andExpect(content().string(not(containsString("delete from"))))
                .andExpect(content().string(not(containsString("foreign key"))))
                .andExpect(content().string(not(containsString("fk_comment"))));
        }
    }

    @Test
    void uniqueViolationsAreAConflictWithTheirOwnGenericMessage() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/unique-violation").with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(ExceptionTranslator.ERR_DUPLICATE))
            .andExpect(jsonPath("$.detail").value(ExceptionTranslator.DUPLICATE_DETAIL))
            .andExpect(content().string(not(containsString("ux_team_member"))))
            .andExpect(content().string(not(containsString("insert into"))))
            .andExpect(content().string(not(containsString("referenced"))));
    }

    @Test
    void otherDatabaseErrorsNeverReturnTheirSqlAsDetail() throws Exception {
        for (String path : new String[] { "jdbc-error", "sql-exception" }) {
            mockMvc
                .perform(get("/api/exception-translator-test/" + path).with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(ExceptionTranslator.DATA_ACCESS_DETAIL))
                .andExpect(content().string(not(containsString("secret"))))
                .andExpect(content().string(not(containsString("select"))));
        }
    }

    @Test
    void errorResponsesNeverPutJavaToStringInTheDetail() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/bad-request-alert").with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.cycle"))
            .andExpect(jsonPath("$.detail").value("A node cannot move under its own descendant"))
            .andExpect(content().string(not(containsString("ProblemDetailWithCause"))))
            .andExpect(content().string(not(containsString("BAD_REQUEST"))));
        mockMvc
            .perform(get("/api/exception-translator-test/response-status-without-reason").with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(content().string(not(containsString("409 CONFLICT"))))
            .andExpect(content().string(not(containsString("ProblemDetail"))));
    }

    @Test
    void responseStatusReasonIsTheCleanDetail() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/response-status-with-reason").with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("The team was changed meanwhile"))
            .andExpect(content().string(not(containsString("409 CONFLICT"))))
            .andExpect(content().string(not(containsString("ResponseStatusException"))))
            .andExpect(content().string(not(containsString("ProblemDetail"))));
    }

    @Test
    void lockFailuresAreAConflictWithoutSql() throws Exception {
        for (String path : new String[] { "cannot-acquire-lock", "jpa-pessimistic-lock" }) {
            mockMvc
                .perform(get("/api/exception-translator-test/" + path).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ErrorConstants.ERR_CONCURRENCY_FAILURE))
                .andExpect(jsonPath("$.detail").value(ExceptionTranslator.CONCURRENCY_DETAIL))
                .andExpect(content().string(not(containsString("delete from"))));
        }
    }

    @Test
    void testMethodArgumentNotValid() throws Exception {
        mockMvc
            .perform(
                post("/api/exception-translator-test/method-argument").content("{}").contentType(MediaType.APPLICATION_JSON).with(csrf())
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value(ErrorConstants.ERR_VALIDATION))
            .andExpect(jsonPath("$.fieldErrors.[0].objectName").value("test"))
            .andExpect(jsonPath("$.fieldErrors.[0].field").value("test"))
            .andExpect(jsonPath("$.fieldErrors.[0].message").value("must not be null"));
    }

    /**
     * A validation 400 must describe the request, never the server. Until this was fixed the
     * {@code detail} was {@code MethodArgumentNotValidException#getMessage()}, which reads
     * "Validation failed for argument [0] in public org.springframework.http.ResponseEntity&lt;…&gt;
     * com.opportunity.tree.web.rest.….methodArgument(…)": the controller class, its package, the
     * DTO type and the whole method signature, handed to any caller that posts an invalid body.
     * (Under {@code prod} the package check swapped it for "Unexpected runtime exception", hiding
     * the leak but reporting a validation failure as an internal error — the wrong cause.) The
     * fields at fault stay available in {@code fieldErrors}, which names only DTO fields.
     */
    @Test
    void methodArgumentNotValid_detailNamesNoControllerPackageOrSignature() throws Exception {
        mockMvc
            .perform(
                post("/api/exception-translator-test/method-argument").content("{}").contentType(MediaType.APPLICATION_JSON).with(csrf())
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(ExceptionTranslator.VALIDATION_DETAIL))
            .andExpect(jsonPath("$.fieldErrors.[0].field").value("test"))
            .andExpect(content().string(not(containsString("com.opportunity.tree"))))
            .andExpect(content().string(not(containsString("org.springframework"))))
            .andExpect(content().string(not(containsString("ResponseEntity"))))
            .andExpect(content().string(not(containsString("Validation failed for argument"))))
            .andExpect(content().string(not(containsString("Unexpected runtime exception"))));
    }

    @Test
    void testMissingServletRequestPartException() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/missing-servlet-request-part").with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.400"));
    }

    @Test
    void testMissingServletRequestParameterException() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/missing-servlet-request-parameter").with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.400"));
    }

    @Test
    void testAccessDenied() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/access-denied").with(csrf()))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.403"))
            .andExpect(jsonPath("$.detail").value("test access denied!"));
    }

    @Test
    void testUnauthorized() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/unauthorized").with(csrf()))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.401"))
            .andExpect(jsonPath("$.path").value("/api/exception-translator-test/unauthorized"))
            .andExpect(jsonPath("$.detail").value("test authentication failed!"));
    }

    @Test
    void testMethodNotSupported() throws Exception {
        mockMvc
            .perform(post("/api/exception-translator-test/access-denied").with(csrf()))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.405"))
            .andExpect(jsonPath("$.detail").value("Request method 'POST' is not supported"))
            .andExpect(content().string(not(containsString("HttpRequestMethodNotSupportedException"))));
    }

    @Test
    void testExceptionWithResponseStatus() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/response-status").with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.400"))
            .andExpect(jsonPath("$.title").value("test response status"));
    }

    @Test
    void testInternalServerError() throws Exception {
        mockMvc
            .perform(get("/api/exception-translator-test/internal-server-error").with(csrf()))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.500"))
            .andExpect(jsonPath("$.title").value("Internal Server Error"));
    }
}
