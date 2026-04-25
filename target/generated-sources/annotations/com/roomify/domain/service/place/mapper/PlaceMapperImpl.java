package com.roomify.domain.service.place.mapper;

import com.roomify.infrastucture.models.place.Place;
import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.out.OwnerInfo;
import com.roomify.presentation.models.out.PlaceResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-26T16:39:54+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.6 (Homebrew)"
)
@Component
public class PlaceMapperImpl implements PlaceMapper {

    @Override
    public PlaceResponse toResponse(Place place) {
        if ( place == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        String description = null;
        String type = null;
        String address = null;
        Integer capacity = null;
        BigDecimal pricePerHour = null;
        String status = null;
        OwnerInfo owner = null;

        id = place.getId();
        name = place.getName();
        description = place.getDescription();
        if ( place.getType() != null ) {
            type = place.getType().name();
        }
        address = place.getAddress();
        capacity = place.getCapacity();
        pricePerHour = place.getPricePerHour();
        if ( place.getStatus() != null ) {
            status = place.getStatus().name();
        }
        owner = toOwnerInfo( place.getOwner() );

        PlaceResponse placeResponse = new PlaceResponse( id, name, description, type, address, capacity, pricePerHour, status, owner );

        return placeResponse;
    }

    @Override
    public List<PlaceResponse> toResponseList(List<Place> places) {
        if ( places == null ) {
            return null;
        }

        List<PlaceResponse> list = new ArrayList<PlaceResponse>( places.size() );
        for ( Place place : places ) {
            list.add( toResponse( place ) );
        }

        return list;
    }

    @Override
    public OwnerInfo toOwnerInfo(User owner) {
        if ( owner == null ) {
            return null;
        }

        Long id = null;
        String firstName = null;
        String lastName = null;
        String email = null;

        id = owner.getId();
        firstName = owner.getFirstName();
        lastName = owner.getLastName();
        email = owner.getEmail();

        OwnerInfo ownerInfo = new OwnerInfo( id, firstName, lastName, email );

        return ownerInfo;
    }
}
