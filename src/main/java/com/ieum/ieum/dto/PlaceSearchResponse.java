package com.ieum.ieum.dto;

import java.util.List;

public record PlaceSearchResponse(List<Place> places) {
    public record Place(
            String placeName,    // 장소명 (예: 판교역)
            String roadAddress,  // 도로명주소 (구분용)
            double lng,          // 경도 — 이걸 /route의 endLng로 넘김
            double lat,          // 위도 — endLat
            Integer distance     // 현재 위치서 거리(m), 위치 없으면 null
    ) {}
}