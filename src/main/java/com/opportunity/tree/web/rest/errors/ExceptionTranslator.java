package com.opportunity.tree.web.rest.errors;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

import com.opportunity.tree.service.NodeWriteRuleException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.hibernate.JDBCException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tech.jhipster.config.JHipsterConstants;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause.ProblemDetailWithCauseBuilder;
import tech.jhipster.web.util.HeaderUtil;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 */
@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    private static final String FIELD_ERRORS_KEY = "fieldErrors";
    private static final String MESSAGE_KEY = "message";
    private static final String PATH_KEY = "path";
    private static final boolean CASUAL_CHAIN_ENABLED = false;
    static final String ERR_DATA_INTEGRITY = "error.dataintegrity";
    static final String DATA_INTEGRITY_DETAIL =
        "This record is still referenced by other data (for example child nodes or links) and cannot be changed or deleted.";

    static final String ERR_DUPLICATE = "error.duplicate";
    static final String DUPLICATE_DETAIL = "This conflicts with an existing record: a value that must be unique is already in use.";

    /** Detail of any other failed database statement: the driver's message can carry SQL, so it is never returned. */
    static final String DATA_ACCESS_DETAIL = "Failure during data access";

    /** SQLState of a unique-key violation (PostgreSQL unique_violation, H2 DUPLICATE_KEY_1). */
    private static final String UNIQUE_VIOLATION_STATE = "23505";

    static final String CONCURRENCY_DETAIL = "Someone else changed this at the same time. Please reload and try again.";

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionTranslator.class);

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final Environment env;

    public ExceptionTranslator(Environment env) {
        this.env = env;
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleAnyException(Throwable ex, NativeWebRequest request) {
        if (isConstraintViolation(ex)) {
            return dataIntegrityConflict(ex, request);
        }
        LOG.debug("Converting Exception to Problem Details:", ex);
        ProblemDetailWithCause pdCause = wrapAndCustomizeProblem(ex, request);
        return handleExceptionInternal((Exception) ex, pdCause, buildHeaders(ex), HttpStatusCode.valueOf(pdCause.getStatus()), request);
    }

    /** Tree node write-rule violations raised by the service layer are reported as a 400 (TREE-002). */
    @ExceptionHandler
    public ResponseEntity<Object> handleNodeWriteRuleException(NodeWriteRuleException ex, NativeWebRequest request) {
        return handleAnyException(new BadRequestAlertException(ex.getMessage(), ex.getEntityName(), ex.getErrorKey()), request);
    }

    /**
     * A write that breaks a database constraint — typically a generated admin DELETE of a node
     * that still has children, links or other references — is a 409 with a generic message.
     * The SQL / constraint text is logged server-side only, never returned to the client.
     */
    @ExceptionHandler
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex, NativeWebRequest request) {
        return dataIntegrityConflict(ex, request);
    }

    /**
     * The same 409 for a constraint violation Spring did not translate into a
     * {@link DataIntegrityViolationException}: a raw Hibernate
     * {@code org.hibernate.exception.ConstraintViolationException} from a service's flush, or one
     * raised at transaction commit (wrapped in a {@code TransactionSystemException} /
     * {@code RollbackException}). Detected anywhere in the cause chain (see
     * {@link #isConstraintViolation}); the SQL text is only logged.
     */
    private ResponseEntity<Object> dataIntegrityConflict(Throwable ex, NativeWebRequest request) {
        LOG.warn("Data integrity violation on {}: {}", extractURI(request), mostSpecificMessage(ex));
        // A unique key clash is not "still referenced by other data": it gets its own generic wording.
        boolean duplicate = hasSqlState(ex, UNIQUE_VIOLATION_STATE);
        ProblemDetailWithCause problem = ProblemDetailWithCauseBuilder.instance()
            .withStatus(HttpStatus.CONFLICT.value())
            .withDetail(duplicate ? DUPLICATE_DETAIL : DATA_INTEGRITY_DETAIL)
            .withProperty(MESSAGE_KEY, duplicate ? ERR_DUPLICATE : ERR_DATA_INTEGRITY)
            .build();
        Exception exception = ex instanceof Exception e ? e : new IllegalStateException(ex);
        return handleExceptionInternal(exception, customizeProblem(problem, ex, request), null, HttpStatus.CONFLICT, request);
    }

    /**
     * Is a database constraint violation anywhere in the cause chain? Spring's
     * {@link DataIntegrityViolationException}, Hibernate's {@code ConstraintViolationException},
     * {@link java.sql.SQLIntegrityConstraintViolationException}, or any SQL exception with an
     * SQLState of class 23 (integrity constraint violation).
     */
    static boolean isConstraintViolation(Throwable error) {
        int depth = 0;
        for (Throwable t = error; t != null && depth < 32; t = t.getCause() == t ? null : t.getCause(), depth++) {
            if (
                t instanceof DataIntegrityViolationException ||
                t instanceof org.hibernate.exception.ConstraintViolationException ||
                t instanceof java.sql.SQLIntegrityConstraintViolationException
            ) {
                return true;
            }
            String state = t instanceof JDBCException j ? j.getSQLState() : t instanceof SQLException sql ? sql.getSQLState() : null;
            if (state != null && state.startsWith("23")) {
                return true;
            }
        }
        return false;
    }

    /** Does any exception in the cause chain carry this SQLState? */
    static boolean hasSqlState(Throwable error, String sqlState) {
        int depth = 0;
        for (Throwable t = error; t != null && depth < 32; t = t.getCause() == t ? null : t.getCause(), depth++) {
            String state = t instanceof JDBCException j ? j.getSQLState() : t instanceof SQLException sql ? sql.getSQLState() : null;
            if (sqlState.equals(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is a failed database statement anywhere in the cause chain (a Hibernate {@link JDBCException}, a
     * {@link SQLException} or a Spring {@link DataAccessException})? Their messages can quote the SQL.
     */
    static boolean isDatabaseError(Throwable error) {
        int depth = 0;
        for (Throwable t = error; t != null && depth < 32; t = t.getCause() == t ? null : t.getCause(), depth++) {
            if (t instanceof JDBCException || t instanceof SQLException || t instanceof DataAccessException) {
                return true;
            }
        }
        return false;
    }

    private static String mostSpecificMessage(Throwable ex) {
        Throwable last = ex;
        int depth = 0;
        for (Throwable t = ex; t != null && depth < 32; t = t.getCause() == t ? null : t.getCause(), depth++) {
            last = t;
        }
        return last.getMessage();
    }

    /**
     * Lock contention between concurrent writes (deadlock, lock timeout, optimistic-lock clash),
     * whether Spring translated it ({@link ConcurrencyFailureException}, incl.
     * {@code CannotAcquireLockException} / {@code PessimisticLockingFailureException}) or it came
     * straight from JPA: a 409 with a generic message; the SQL text is only logged.
     */
    @ExceptionHandler(
        { ConcurrencyFailureException.class, PessimisticLockException.class, LockTimeoutException.class, OptimisticLockException.class }
    )
    public ResponseEntity<Object> handleConcurrencyFailure(Exception ex, NativeWebRequest request) {
        LOG.warn("Concurrent write conflict on {}: {}", extractURI(request), ex.getMessage());
        ProblemDetailWithCause problem = ProblemDetailWithCauseBuilder.instance()
            .withStatus(HttpStatus.CONFLICT.value())
            .withDetail(CONCURRENCY_DETAIL)
            .withProperty(MESSAGE_KEY, ErrorConstants.ERR_CONCURRENCY_FAILURE)
            .build();
        return handleExceptionInternal(ex, customizeProblem(problem, ex, request), null, HttpStatus.CONFLICT, request);
    }

    @SuppressWarnings("java:S2638")
    @Nullable
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex,
        @Nullable Object body,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest request
    ) {
        body = body == null ? wrapAndCustomizeProblem((Throwable) ex, (NativeWebRequest) request) : body;
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    protected ProblemDetailWithCause wrapAndCustomizeProblem(Throwable ex, NativeWebRequest request) {
        return customizeProblem(getProblemDetailWithCause(ex), ex, request);
    }

    private ProblemDetailWithCause getProblemDetailWithCause(Throwable ex) {
        if (
            ex instanceof ErrorResponseException exp && exp.getBody() instanceof ProblemDetailWithCause problemDetailWithCause
        ) return problemDetailWithCause;
        return ProblemDetailWithCauseBuilder.instance().withStatus(toStatus(ex).value()).build();
    }

    protected ProblemDetailWithCause customizeProblem(ProblemDetailWithCause problem, Throwable err, NativeWebRequest request) {
        if (problem.getStatus() <= 0) problem.setStatus(toStatus(err));

        if (problem.getType() == null || problem.getType().equals(URI.create("about:blank"))) problem.setType(getMappedType(err));

        // higher precedence to Custom/ResponseStatus types
        String title = extractTitle(err, problem.getStatus());
        String problemTitle = problem.getTitle();
        if (problemTitle == null || !problemTitle.equals(title)) {
            problem.setTitle(title);
        }

        if (problem.getDetail() == null) {
            // higher precedence to cause
            problem.setDetail(getCustomizedErrorDetails(err));
        }

        Map<String, Object> problemProperties = problem.getProperties();
        if (problemProperties == null || !problemProperties.containsKey(MESSAGE_KEY)) problem.setProperty(
            MESSAGE_KEY,
            getMappedMessageKey(err) != null ? getMappedMessageKey(err) : "error.http." + problem.getStatus()
        );

        if (problemProperties == null || !problemProperties.containsKey(PATH_KEY)) problem.setProperty(PATH_KEY, getPathValue(request));

        if (
            (err instanceof MethodArgumentNotValidException fieldException) &&
            (problemProperties == null || !problemProperties.containsKey(FIELD_ERRORS_KEY))
        ) problem.setProperty(FIELD_ERRORS_KEY, getFieldErrors(fieldException));

        problem.setCause(buildCause(err.getCause(), request).orElse(null));

        return problem;
    }

    private String extractTitle(Throwable err, int statusCode) {
        return getCustomizedTitle(err) != null ? getCustomizedTitle(err) : extractTitleForResponseStatus(err, statusCode);
    }

    private List<FieldErrorVM> getFieldErrors(MethodArgumentNotValidException ex) {
        return ex
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(f ->
                new FieldErrorVM(
                    f.getObjectName().replaceFirst("DTO$", ""),
                    f.getField(),
                    StringUtils.isNotBlank(f.getDefaultMessage()) ? f.getDefaultMessage() : f.getCode()
                )
            )
            .toList();
    }

    private String extractTitleForResponseStatus(Throwable err, int statusCode) {
        var specialStatus = extractResponseStatus(err);
        return specialStatus == null ? HttpStatus.valueOf(statusCode).getReasonPhrase() : specialStatus.reason();
    }

    private String extractURI(NativeWebRequest request) {
        HttpServletRequest nativeRequest = request.getNativeRequest(HttpServletRequest.class);
        return nativeRequest != null ? nativeRequest.getRequestURI() : StringUtils.EMPTY;
    }

    private HttpStatus toStatus(final Throwable throwable) {
        // Let the ErrorResponse take this responsibility
        if (throwable instanceof ErrorResponse err) return HttpStatus.valueOf(err.getBody().getStatus());

        return Optional.ofNullable(getMappedStatus(throwable)).orElse(
            Optional.ofNullable(resolveResponseStatus(throwable)).map(ResponseStatus::value).orElse(HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }

    private ResponseStatus extractResponseStatus(final Throwable throwable) {
        return Optional.ofNullable(resolveResponseStatus(throwable)).orElse(null);
    }

    private ResponseStatus resolveResponseStatus(final Throwable type) {
        final ResponseStatus candidate = findMergedAnnotation(type.getClass(), ResponseStatus.class);
        return candidate == null && type.getCause() != null ? resolveResponseStatus(type.getCause()) : candidate;
    }

    private URI getMappedType(Throwable err) {
        if (err instanceof MethodArgumentNotValidException) return ErrorConstants.CONSTRAINT_VIOLATION_TYPE;
        return ErrorConstants.DEFAULT_TYPE;
    }

    private String getMappedMessageKey(Throwable err) {
        if (err instanceof MethodArgumentNotValidException) {
            return ErrorConstants.ERR_VALIDATION;
        } else if (err instanceof ConcurrencyFailureException || err.getCause() instanceof ConcurrencyFailureException) {
            return ErrorConstants.ERR_CONCURRENCY_FAILURE;
        }
        return null;
    }

    private String getCustomizedTitle(Throwable err) {
        if (err instanceof MethodArgumentNotValidException) return "Method argument not valid";
        return null;
    }

    private String getCustomizedErrorDetails(Throwable err) {
        // In every profile: a database error's message (or its cause's) may carry SQL text.
        if (isDatabaseError(err)) {
            LOG.warn("Database error: {}", mostSpecificMessage(err));
            return DATA_ACCESS_DETAIL;
        }
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_PRODUCTION)) {
            if (err instanceof HttpMessageConversionException) return "Unable to convert http message";
            if (containsPackageName(err.getMessage())) return "Unexpected runtime exception";
        }
        return err.getCause() != null ? err.getCause().getMessage() : err.getMessage();
    }

    private HttpStatus getMappedStatus(Throwable err) {
        // Where we disagree with Spring defaults
        if (err instanceof AccessDeniedException) return HttpStatus.FORBIDDEN;
        if (err instanceof ConcurrencyFailureException) return HttpStatus.CONFLICT;
        if (err instanceof BadCredentialsException) return HttpStatus.UNAUTHORIZED;
        return null;
    }

    private URI getPathValue(NativeWebRequest request) {
        if (request == null) return URI.create("about:blank");
        return URI.create(extractURI(request));
    }

    private HttpHeaders buildHeaders(Throwable err) {
        return err instanceof BadRequestAlertException badRequestAlertException
            ? HeaderUtil.createFailureAlert(
                  applicationName,
                  true,
                  badRequestAlertException.getEntityName(),
                  badRequestAlertException.getErrorKey(),
                  badRequestAlertException.getMessage()
              )
            : null;
    }

    public Optional<ProblemDetailWithCause> buildCause(final Throwable throwable, NativeWebRequest request) {
        if (throwable != null && isCasualChainEnabled()) {
            return Optional.of(customizeProblem(getProblemDetailWithCause(throwable), throwable, request));
        }
        return Optional.ofNullable(null);
    }

    private boolean isCasualChainEnabled() {
        // Customize as per the needs
        return CASUAL_CHAIN_ENABLED;
    }

    private boolean containsPackageName(String message) {
        // This list is for sure not complete
        return Strings.CS.containsAny(message, "org.", "java.", "net.", "jakarta.", "javax.", "com.", "io.", "de.", "com.opportunity.tree");
    }
}
