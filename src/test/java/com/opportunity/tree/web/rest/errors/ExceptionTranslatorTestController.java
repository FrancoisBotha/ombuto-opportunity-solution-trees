package com.opportunity.tree.web.rest.errors;

import jakarta.persistence.PessimisticLockException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exception-translator-test")
public class ExceptionTranslatorTestController {

    @GetMapping("/concurrency-failure")
    public void concurrencyFailure() {
        throw new ConcurrencyFailureException("test concurrency failure");
    }

    static final String RAW_SQL =
        "could not execute statement [ERROR: update or delete on table \"opportunity\" violates foreign key constraint] [delete from opportunity where id=?]";

    @GetMapping("/data-integrity")
    public void dataIntegrity() {
        throw new DataIntegrityViolationException(RAW_SQL, new java.sql.SQLException(RAW_SQL, "23503"));
    }

    /** What a service's own flush raises: Hibernate's exception, not translated by Spring. */
    @GetMapping("/hibernate-constraint-violation")
    public void hibernateConstraintViolation() {
        throw new org.hibernate.exception.ConstraintViolationException(
            RAW_SQL,
            new java.sql.SQLException(RAW_SQL, "23503"),
            "delete from opportunity where id=?",
            "fk_comment__opportunity_id"
        );
    }

    /** What a constraint violation raised at transaction commit looks like. */
    @GetMapping("/constraint-violation-at-commit")
    public void constraintViolationAtCommit() {
        throw new org.springframework.transaction.TransactionSystemException(
            "Could not commit JPA transaction",
            new jakarta.persistence.RollbackException(
                "Error while committing the transaction",
                new jakarta.persistence.PersistenceException(RAW_SQL, new java.sql.SQLException(RAW_SQL, "23503"))
            )
        );
    }

    @GetMapping("/cannot-acquire-lock")
    public void cannotAcquireLock() {
        throw new CannotAcquireLockException(RAW_SQL);
    }

    @GetMapping("/jpa-pessimistic-lock")
    public void jpaPessimisticLock() {
        throw new PessimisticLockException(RAW_SQL);
    }

    @PostMapping("/method-argument")
    public void methodArgument(@Valid @RequestBody TestDTO testDTO) {
        // empty method
    }

    @GetMapping("/missing-servlet-request-part")
    public void missingServletRequestPartException(@RequestPart("part") String part) {
        // empty method
    }

    @GetMapping("/missing-servlet-request-parameter")
    public void missingServletRequestParameterException(@RequestParam("param") String param) {
        // empty method
    }

    @GetMapping("/access-denied")
    public void accessdenied() {
        throw new AccessDeniedException("test access denied!");
    }

    @GetMapping("/unauthorized")
    public void unauthorized() {
        throw new BadCredentialsException("test authentication failed!");
    }

    @GetMapping("/response-status")
    public void exceptionWithResponseStatus() {
        throw new TestResponseStatusException();
    }

    @GetMapping("/internal-server-error")
    public void internalServerError() {
        throw new RuntimeException();
    }

    public static class TestDTO {

        @NotNull
        private String test;

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "test response status")
    @SuppressWarnings("serial")
    public static class TestResponseStatusException extends RuntimeException {}
}
