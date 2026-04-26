package com.roomify.infrastucture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.roomify.infrastucture.models.place.Place;
import com.roomify.infrastucture.models.user.User;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {

    boolean existsByOwnerAndNameIgnoreCaseAndNormalizedAddressIgnoreCase(
            User owner,
            String name,
            String address
    );

    boolean existsByOwnerAndNameIgnoreCaseAndNormalizedAddressIgnoreCaseAndIdNot(
            User owner,
            String name,
            String normalizedAddress,
            Long id
    );

}
