package com.roomify.domain.service.user;

import java.util.Optional;

import org.jspecify.annotations.NonNull;

import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.place.Place;
import com.roomify.infrastucture.models.user.User;

import lombok.experimental.UtilityClass;

import static java.util.Collections.emptySet;

@UtilityClass
public class UserUtils {

    public static boolean isAdmin(@NonNull User currentUser) {
        var userRoles = Optional.ofNullable(currentUser.getRolesEnum()).orElse(emptySet());
        return userRoles.contains(RoleEnum.ADMIN) || userRoles.contains(RoleEnum.SUPER_ADMIN);
    }

    public static boolean isOwner(@NonNull User currentUser, @NonNull Place place) {
        var idOwnerPlace = Optional.of(place).map(Place::getOwner).map(User::getId).orElse(null);
        return currentUser.getId().equals(idOwnerPlace);
    }

}
