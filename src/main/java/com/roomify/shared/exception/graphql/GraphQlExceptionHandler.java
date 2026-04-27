package com.roomify.shared.exception.graphql;

import org.jspecify.annotations.NonNull;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.roomify.shared.exception.place.PlaceFilterInvalidException;
import com.roomify.shared.exception.place.PlaceNotFoundException;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

@Component
public class GraphQlExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(@NonNull Throwable ex, @NonNull DataFetchingEnvironment env) {
        if (ex instanceof PlaceFilterInvalidException e) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(e.getMessage())
                    .build();
        }
        if (ex instanceof PlaceNotFoundException e) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.NOT_FOUND)
                    .message(e.getMessage())
                    .build();
        }
        if (ex instanceof AccessDeniedException) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.FORBIDDEN)
                    .message("Access denied")
                    .build();
        }
        return null;
    }
}
