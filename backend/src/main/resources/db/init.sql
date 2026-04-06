-- PostGIS 확장 활성화
CREATE EXTENSION IF NOT EXISTS postgis;

-- 안전 시설 테이블
CREATE TABLE IF NOT EXISTS safety_facility (
    id              BIGSERIAL PRIMARY KEY,
    facility_type   VARCHAR(20)  NOT NULL,
    name            VARCHAR(100),
    address         VARCHAR(200),
    location        GEOMETRY(Point, 4326) NOT NULL,
    data_source     VARCHAR(50),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_facility_location ON safety_facility USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_facility_type     ON safety_facility (facility_type);

-- 위험 구역 테이블
CREATE TABLE IF NOT EXISTS danger_zone (
    id              BIGSERIAL PRIMARY KEY,
    grid_polygon    GEOMETRY(Polygon, 4326) NOT NULL,
    safety_score    DECIMAL(5,2) NOT NULL,
    facility_count  INTEGER DEFAULT 0,
    risk_level      VARCHAR(10),
    calculated_at   TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_danger_zone_polygon ON danger_zone USING GIST (grid_polygon);
CREATE INDEX IF NOT EXISTS idx_danger_zone_risk    ON danger_zone (risk_level);
