package com.opportunity.tree.config.mcp;

import java.util.Arrays;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Keeps framework internals out of MCP tool error messages.
 *
 * <p>When an agent calls a tool with an argument the framework cannot bind — a number outside the
 * range of {@code long}, a value of the wrong JSON type — Spring AI's {@code MethodToolCallback}
 * fails during argument deserialization and the MCP adapter ({@code McpToolUtils}) copies the raw
 * exception message straight into the tool result. That message leaks Jackson internals, e.g.
 * <pre>Numeric value (…) out of range of `long` … StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION …</pre>
 * which is exactly the kind of internal detail NFR-021 and the epic's error-hygiene expectation
 * forbid (no class names, no parser internals, no stack traces).
 *
 * <p>This {@link BeanPostProcessor} wraps every {@link ToolCallbackProvider} in the context so that
 * each {@link ToolCallback} it exposes runs behind {@link SanitizingToolCallback}. A binding or
 * deserialization failure is rewritten to a generic, actionable hint; the tools' own deliberate
 * validation messages ("Unknown node type …", "Node id 5 is a PRODUCT, not a SOLUTION", "Access
 * denied") are passed through unchanged so agents still get useful guidance.
 */
@Component
public class McpToolErrorSanitizer implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof ToolCallbackProvider provider) {
            ToolCallback[] wrapped = Arrays.stream(provider.getToolCallbacks())
                .map(SanitizingToolCallback::new)
                .toArray(ToolCallback[]::new);
            return (ToolCallbackProvider) () -> wrapped;
        }
        return bean;
    }

    /**
     * Delegates to a wrapped {@link ToolCallback} and sanitises argument-binding / deserialization
     * failures. Everything else — including the tools' own {@link IllegalArgumentException}s and
     * access-denied errors — is re-thrown untouched.
     */
    static final class SanitizingToolCallback implements ToolCallback {

        static final String GENERIC_ARGUMENT_ERROR =
            "One or more arguments were invalid: a value was the wrong type or outside the allowed " +
            "range. Ids must be whole numbers within range. Check the tool's parameter types and try again.";

        private final ToolCallback delegate;

        SanitizingToolCallback(ToolCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            try {
                return delegate.call(toolInput);
            } catch (RuntimeException ex) {
                throw sanitize(ex);
            }
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            try {
                return delegate.call(toolInput, toolContext);
            } catch (RuntimeException ex) {
                throw sanitize(ex);
            }
        }

        /**
         * Returns a generic {@link IllegalArgumentException} when the failure is a JSON
         * deserialization / number-binding problem (whose message would otherwise expose parser
         * internals); otherwise returns the original exception so deliberate validation messages
         * survive.
         */
        static RuntimeException sanitize(RuntimeException ex) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (isBindingFailure(cause)) {
                    return new IllegalArgumentException(GENERIC_ARGUMENT_ERROR);
                }
                if (cause.getCause() == cause) {
                    break;
                }
            }
            return ex;
        }

        private static boolean isBindingFailure(Throwable cause) {
            if (cause instanceof NumberFormatException) {
                return true;
            }
            // Match Jackson (2.x com.fasterxml.jackson, 3.x tools.jackson) by class name so no
            // hard compile-time dependency on the parser is introduced here.
            String className = cause.getClass().getName().toLowerCase(java.util.Locale.ROOT);
            return className.contains("jackson") || className.contains("json") && className.contains("parse");
        }
    }
}
