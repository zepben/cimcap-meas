CREATE DATABASE measurements;
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

CREATE TABLE IF NOT EXISTS analog_values (
 timestamp        TIMESTAMP       NOT NULL,
 write_time     TIMESTAMP      NOT NULL,
 analog_mrid        TEXT              NOT NULL,
 value       DOUBLE PRECISION  NOT NULL
);

CREATE TABLE IF NOT EXISTS discrete_values (
 timestamp        TIMESTAMP       NOT NULL,
 write_time     TIMESTAMP      NOT NULL,
 discrete_mrid        TEXT              NOT NULL,
 value       INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS accumulator_values (
 timestamp          TIMESTAMP   NOT NULL,
 write_time         TIMESTAMP   NOT NULL,
 accumulator_mrid   TEXT        NOT NULL,
 value              INTEGER     NOT NULL
);


SELECT create_hypertable('analog_values', 'timestamp', if_not_exists => TRUE);
SELECT create_hypertable('discrete_values', 'timestamp', if_not_exists => TRUE);
SELECT create_hypertable('accumulator_values', 'timestamp', if_not_exists => TRUE);
