package com.roomify.domain.api;

import org.jspecify.annotations.NonNull;

import com.roomify.presentation.models.in.LoginRequest;
import com.roomify.presentation.models.out.LoginResponse;
import com.roomify.presentation.models.in.RegisterRequest;
import com.roomify.presentation.models.out.RegisterResponse;
import com.roomify.shared.exception.mail.InvalidTokenException;
import com.roomify.shared.exception.mail.TokenNotFoundException;
import com.roomify.shared.exception.user.AccountAlreadyVerifiedException;
import com.roomify.shared.exception.user.AccountNotVerifiedException;
import com.roomify.shared.exception.user.EmailAlreadyExistsException;
import com.roomify.shared.exception.user.RoleNotFoundException;
import com.roomify.shared.exception.user.UserNotFoundException;

public interface AuthApi {

    RegisterResponse register(@NonNull RegisterRequest request) throws RoleNotFoundException, EmailAlreadyExistsException;

    LoginResponse login(@NonNull LoginRequest request) throws UserNotFoundException, AccountNotVerifiedException;

    void verify(@NonNull String token) throws InvalidTokenException, TokenNotFoundException, AccountAlreadyVerifiedException;

    void resendVerification(@NonNull String email) throws UserNotFoundException, AccountAlreadyVerifiedException;

}
