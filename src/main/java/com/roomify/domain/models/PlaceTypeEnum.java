package com.roomify.domain.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of place")
public enum PlaceTypeEnum {
    @Schema(description = "Room dedicated for meetings")
    MEETING_ROOM,
    @Schema(description = "Shared workspace")
    COWORKING_SPACE,
    @Schema(description = "Event space")
    EVENT_SPACE,
    @Schema(description = "Private party room")
    PARTY_ROOM,
    @Schema(description = "Photo or video studio")
    STUDIO
}
