package com.roomify.domain.spi;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;

import com.roomify.domain.models.PlaceSearchFilter;
import com.roomify.infrastucture.models.place.Place;
import com.roomify.infrastucture.models.user.User;

public interface PlaceSpi {

    boolean existsPlaceByUser(@NonNull User user, @NonNull String name, @NonNull String address);

    boolean existsPlaceByUserExcluding(@NonNull User user, @NonNull String name, @NonNull String address, @NonNull Long excludeId);

    Optional<Place> findById(@NonNull Long id);

    Place insertPlace(@NonNull Place place);

    Place updatePlace(@NonNull Place place);

    void deletePlace(@NonNull Long id);

    Page<Place> searchPlaces(@NonNull PlaceSearchFilter filter);

}
