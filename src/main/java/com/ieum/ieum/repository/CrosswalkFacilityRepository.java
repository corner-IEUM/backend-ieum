package com.ieum.ieum.repository;

import com.ieum.ieum.domain.CrosswalkFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CrosswalkFacilityRepository extends JpaRepository<CrosswalkFacility, Long> {

    @Query(value = "SELECT c.id, c.management_no, c.has_curb_cut, c.has_braille_block, " +
            "c.has_sound_signal, c.has_pedestrian_signal, c.width_m, c.raised_crosswalk, " +
            "c.crossing_length_m, c.green_signal_sec, c.source_updated_at " +
            "FROM crosswalks c " +
            "WHERE ST_DWithin(c.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusM) " +
            "ORDER BY ST_Distance(c.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) " +
            "LIMIT 1", nativeQuery = true)
    Optional<CrosswalkFacility> findNearest(@Param("lng") double lng, @Param("lat") double lat, @Param("radiusM") double radiusM);
}
