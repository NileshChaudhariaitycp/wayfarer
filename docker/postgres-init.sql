-- Runs once, automatically, on the Postgres container's first startup
-- (official postgres image convention: anything in /docker-entrypoint-initdb.d/
-- executes against the default database created from POSTGRES_DB/POSTGRES_USER).
-- One database per service, matching the H2 database names each service
-- already used locally (authdb, userdb, etc.) — same database-per-service
-- boundary as local dev, just with a real DBMS instead of an in-memory one.
CREATE DATABASE authdb;
CREATE DATABASE userdb;
CREATE DATABASE flightdb;
CREATE DATABASE hoteldb;
CREATE DATABASE bookingdb;
CREATE DATABASE paymentdb;
CREATE DATABASE loyaltydb;
