CREATE TABLE movie (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255)     NOT NULL,
    coord_x        DOUBLE PRECISION NOT NULL CHECK (coord_x > -130),
    coord_y        DOUBLE PRECISION NOT NULL CHECK (coord_y <= 388),
    creation_date  TIMESTAMPTZ      NOT NULL DEFAULT now(),
    oscars_count   BIGINT           NOT NULL CHECK (oscars_count >= 0),
    budget         INTEGER                   CHECK (budget > 0),
    genre          VARCHAR(64)      NOT NULL,
    mpaa_rating    VARCHAR(64)      NOT NULL,
    sw_name        VARCHAR(255),
    sw_height      DOUBLE PRECISION          CHECK (sw_height > 0),
    sw_eye_color   VARCHAR(64),
    sw_hair_color  VARCHAR(64),
    sw_nationality VARCHAR(64),
    sw_loc_x       REAL,
    sw_loc_y       INTEGER,
    sw_loc_z       BIGINT,
    CONSTRAINT screenwriter_all_or_nothing CHECK (
        (sw_name IS NULL AND sw_height IS NULL AND sw_eye_color IS NULL AND sw_hair_color IS NULL
         AND sw_nationality IS NULL AND sw_loc_x IS NULL AND sw_loc_y IS NULL AND sw_loc_z IS NULL)
     OR (sw_name IS NOT NULL AND sw_height IS NOT NULL AND sw_eye_color IS NOT NULL
         AND sw_nationality IS NOT NULL AND sw_loc_x IS NOT NULL AND sw_loc_y IS NOT NULL
         AND sw_loc_z IS NOT NULL))
);
