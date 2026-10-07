-- 대중교통 경로(TRANSIT) 지원을 위한 navigation 컬럼 추가.
-- 기존 row는 전부 도보 전용 경로이므로 route_type 기본값을 WALK_ONLY로 채우고,
-- leg 관련 컬럼은 TRANSIT 경로에서만 의미가 있으므로 NULL 허용.
ALTER TABLE navigation
    ADD COLUMN route_type         VARCHAR(20) NOT NULL DEFAULT 'WALK_ONLY', -- RouteType: WALK_ONLY, TRANSIT
    ADD COLUMN transit_legs       JSONB,                                    -- leg(구간) 목록, TRANSIT 경로에서만 사용
    ADD COLUMN current_leg_index  INTEGER,                                  -- transit_legs 배열에서 현재 진행 중인 구간 인덱스
    ADD COLUMN current_leg_state  VARCHAR(20);                              -- LegState: WALK_TO_STOP, WAITING_AT_STOP, BOARDING_CONFIRM, ON_BUS, ALIGHTING_CONFIRM, WALK_TO_DEST
