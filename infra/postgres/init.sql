-- =====================================================================
-- Initialisation PostgreSQL — base et utilisateur du tender-service.
-- (Keycloak tourne en mode dev avec sa propre base H2 embarquée.)
-- =====================================================================
CREATE DATABASE tender_db;
CREATE USER tender_user WITH ENCRYPTED PASSWORD 'tender_pass';
GRANT ALL PRIVILEGES ON DATABASE tender_db TO tender_user;

\connect tender_db
GRANT ALL ON SCHEMA public TO tender_user;