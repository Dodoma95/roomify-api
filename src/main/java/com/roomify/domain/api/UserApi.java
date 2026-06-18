package com.roomify.domain.api;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.PageInfoInput;
import com.roomify.presentation.models.in.UpdateMeRequest;
import com.roomify.presentation.models.in.UpdateUserRoleRequest;
import com.roomify.presentation.models.in.UserFilterInput;
import com.roomify.presentation.models.out.UserAdminResponse;
import com.roomify.presentation.models.out.UserPage;
import com.roomify.presentation.models.out.UserResponse;
import com.roomify.shared.exception.user.AvatarFormatInvalidException;
import com.roomify.shared.exception.user.AvatarTooLargeException;
import com.roomify.shared.exception.user.RoleAlreadyAssignedException;
import com.roomify.shared.exception.user.RoleNotAssignedException;
import com.roomify.shared.exception.user.RoleNotFoundException;
import com.roomify.shared.exception.user.UserActionForbiddenException;
import com.roomify.shared.exception.user.UserNotFoundException;

public interface UserApi {

    void deleteUserById(@NonNull Long id, @NonNull User currentUser) throws UserNotFoundException, UserActionForbiddenException;

    void deleteMe(@NonNull User currentUser) throws UserNotFoundException;

    UserResponse updateMe(@NonNull User currentUser, @NonNull UpdateMeRequest request) throws UserNotFoundException;

    UserResponse updateAvatar(@NonNull User currentUser, @NonNull MultipartFile file)
            throws AvatarTooLargeException, AvatarFormatInvalidException, UserNotFoundException;

    @NonNull UserPage searchUsers(@Nullable UserFilterInput filter, @NonNull PageInfoInput pagination);

    @NonNull UserAdminResponse updateUserRole(@NonNull Long id, @NonNull UpdateUserRoleRequest request, @NonNull User currentUser)
            throws UserNotFoundException, RoleNotFoundException, UserActionForbiddenException, RoleAlreadyAssignedException, RoleNotAssignedException;

}
