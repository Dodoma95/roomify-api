package com.roomify.presentation.resolver;

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.roomify.domain.api.UserApi;
import com.roomify.presentation.models.in.PageInfoInput;
import com.roomify.presentation.models.in.UserFilterInput;
import com.roomify.presentation.models.out.UserPage;
import com.roomify.shared.exception.user.UserFilterInvalidException;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;

@Controller
@Slf4j
public class UserResolver {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserApi userApi;

    public UserResolver(UserApi userApi) {
        this.userApi = userApi;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public UserPage users(@Argument UserFilterInput filter, @Argument PageInfoInput pagination) {
        var validatedPagination = normalizePagination(pagination);
        log.debug("GraphQL users query — filter={}, page={}, pageSize={}",
                filter, validatedPagination.getPage(), validatedPagination.getPageSize());
        return userApi.searchUsers(filter, validatedPagination);
    }

    private PageInfoInput normalizePagination(@Nullable PageInfoInput pagination) {
        if (isNull(pagination)) {
            return new PageInfoInput(0, DEFAULT_PAGE_SIZE);
        }

        int page = Objects.requireNonNullElse(pagination.getPage(), 0);
        int pageSize = Objects.requireNonNullElse(pagination.getPageSize(), DEFAULT_PAGE_SIZE);
        pagination.setPage(page);
        pagination.setPageSize(pageSize);

        if (page < 0) {
            throw UserFilterInvalidException.builder().message("page must be >= 0").build();
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw UserFilterInvalidException.builder()
                    .message("pageSize must be between 1 and " + MAX_PAGE_SIZE)
                    .build();
        }
        return pagination;
    }
}
