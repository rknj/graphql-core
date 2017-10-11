package org.jahia.modules.graphql.provider.dxm;

import graphql.ExceptionWhileDataFetching;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.ExecutionPath;
import graphql.language.SourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

/**
 * Custom DataFetchingExceptionHandler
 */
public class JCRDataFetchingExceptionHandler implements DataFetcherExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(JCRDataFetchingExceptionHandler.class);

    @Override
    public void accept(DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();

        // Unwrap exception from MethodDataFetcher
        if (exception instanceof RuntimeException && exception.getCause() instanceof InvocationTargetException) {
            exception = ((InvocationTargetException) exception.getCause()).getTargetException();
        }

        SourceLocation sourceLocation = handlerParameters.getField().getSourceLocation();
        ExecutionPath path = handlerParameters.getPath();

        if (exception instanceof BaseGqlClientException) {
            handlerParameters.getExecutionContext().addError(new SimpleGraphQLError(exception.getMessage(), path.toList(), Collections.singletonList(sourceLocation), ((BaseGqlClientException) exception).getErrorType()), path);
        } else {
            ExceptionWhileDataFetching error = new ExceptionWhileDataFetching(path, exception, sourceLocation);
            handlerParameters.getExecutionContext().addError(error, handlerParameters.getPath());
            log.warn(error.getMessage(), exception);
        }
    }

}
