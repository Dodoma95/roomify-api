package com.roomify.domain.api;

import org.jspecify.annotations.NonNull;

import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.UpdateMeRequest;
import com.roomify.presentation.models.out.UserResponse;
import com.roomify.shared.exception.user.UserActionForbiddenException;
import com.roomify.shared.exception.user.UserNotFoundException;

public interface UserApi {

    void deleteUserById(@NonNull Long id, @NonNull User currentUser) throws UserNotFoundException, UserActionForbiddenException;

    void deleteMe(@NonNull User currentUser) throws UserNotFoundException;

    UserResponse updateMe(@NonNull User currentUser, @NonNull UpdateMeRequest request) throws UserNotFoundException;

}
