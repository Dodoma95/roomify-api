package com.roomify.domain.service.user.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.out.UserAdminResponse;

@Mapper
public interface UserMapper {

    @Mapping(target = "roles", source = "rolesEnum")
    UserAdminResponse toAdminResponse(User user);

    List<UserAdminResponse> toAdminResponseList(List<User> users);
}
