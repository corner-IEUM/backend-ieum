package com.ieum.ieum.service;

import com.ieum.ieum.dto.TransitItinerary;
import com.ieum.ieum.dto.TransitLeg;
import com.ieum.ieum.dto.TransitRideLeg;
import com.ieum.ieum.dto.WalkLeg;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 실제 티맵 대중교통 API(POST /transit/routes) 응답(광나루 -> 아차산 인근)으로 파싱 로직을 검증
class TransitRouteParserTest {

    private final TransitRouteParser parser = new TransitRouteParser(JsonMapper.builder().build());

    private static final String SAMPLE_RESPONSE = """
            {
                "metaData": {
                    "plan": {
                        "itineraries": [
                            {
                                "fare": { "regular": { "totalFare": 1550 } },
                                "totalTime": 206,
                                "totalWalkTime": 84,
                                "transferCount": 0,
                                "totalDistance": 1550,
                                "totalWalkDistance": 94,
                                "legs": [
                                    {
                                        "mode": "WALK",
                                        "sectionTime": 24,
                                        "distance": 30,
                                        "start": { "name": "출발지", "lon": 127.103521464507, "lat": 37.5452991715677 },
                                        "end": { "name": "광나루", "lon": 127.1038638888889, "lat": 37.54526944444444 },
                                        "steps": [
                                            { "streetName": "보행자도로", "distance": 30, "description": "보행자도로를 따라 30m 이동", "linestring": "127.103516,37.545296 127.10383,37.545197" }
                                        ]
                                    },
                                    {
                                        "mode": "SUBWAY",
                                        "routeColor": "996CAC",
                                        "sectionTime": 122,
                                        "route": "수도권5호선",
                                        "routeId": "110053003",
                                        "distance": 1484,
                                        "service": 0,
                                        "start": { "name": "광나루", "lon": 127.1038638888889, "lat": 37.54526944444444 },
                                        "passStopList": {
                                            "stations": [
                                                { "index": 0, "stationName": "광나루", "lon": "127.103864", "lat": "37.545269", "stationID": "110537" },
                                                { "index": 1, "stationName": "아차산", "lon": "127.089692", "lat": "37.551953", "stationID": "110536" }
                                            ]
                                        },
                                        "end": { "name": "아차산", "lon": 127.08969166666667, "lat": 37.55195277777778 },
                                        "type": 5,
                                        "passShape": { "linestring": "127.103789,37.545292 127.103614,37.545344" }
                                    },
                                    {
                                        "mode": "WALK",
                                        "sectionTime": 60,
                                        "distance": 64,
                                        "start": { "name": "아차산", "lon": 127.08969166666667, "lat": 37.55195277777778 },
                                        "end": { "name": "도착지", "lon": 127.08956495447913, "lat": 37.55224917071854 },
                                        "steps": [
                                            { "streetName": "", "distance": 4, "description": "4m 이동", "linestring": "127.08967,37.55195 127.08968,37.551914" }
                                        ]
                                    }
                                ],
                                "pathType": 1
                            }
                        ]
                    }
                }
            }
            """;

    @Test
    void parsesItinerariesAndLegsFromRealResponse() {
        List<TransitItinerary> itineraries = parser.parseItineraries(SAMPLE_RESPONSE);

        assertEquals(1, itineraries.size());
        TransitItinerary itinerary = itineraries.get(0);
        assertEquals(206, itinerary.totalTime());
        assertEquals(1550, itinerary.fare());
        assertEquals(3, itinerary.legs().size());

        TransitLeg first = itinerary.legs().get(0);
        assertInstanceOf(WalkLeg.class, first);
        assertEquals("WALK", first.mode()); // WalkLeg도 mode가 채워져야 프론트가 모든 leg를 mode로 균일하게 분기 가능
        assertEquals(1, ((WalkLeg) first).steps().size());

        TransitLeg second = itinerary.legs().get(1);
        assertInstanceOf(TransitRideLeg.class, second);
        TransitRideLeg subwayLeg = (TransitRideLeg) second;
        assertEquals("SUBWAY", subwayLeg.mode());
        assertEquals("수도권5호선", subwayLeg.route());
        assertEquals(2, subwayLeg.stations().size());
        assertEquals("아차산", subwayLeg.stations().get(1).stationName());
        assertEquals(127.089692, subwayLeg.stations().get(1).lng(), 0.000001); // 문자열 lon도 정상 파싱되는지 확인
        assertTrue(!subwayLeg.inService()); // service:0 -> 운행 종료

        assertInstanceOf(WalkLeg.class, itinerary.legs().get(2));
    }
}
