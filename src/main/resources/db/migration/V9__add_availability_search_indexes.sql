-- Index pour la sous-requête de disponibilité sur bookings :
-- place_id + status + dates → couvre le NOT EXISTS dans PlaceSpecifications.isAvailableBetween
CREATE INDEX IF NOT EXISTS idx_bookings_place_status_dates
    ON roomify.bookings (place_id, status, start_date, end_date);

-- Index pour la sous-requête de disponibilité sur place_unavailability :
-- place_id + dates → couvre le NOT EXISTS dans PlaceSpecifications.isAvailableBetween
-- et la requête findByPlaceIdAndOverlappingPeriod
CREATE INDEX IF NOT EXISTS idx_place_unavailability_place_dates
    ON roomify.place_unavailability (place_id, start_date, end_date);
