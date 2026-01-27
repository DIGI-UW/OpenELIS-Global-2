
CREATE USER postgres PASSWORD 'admin';
CREATE USER admin SUPERUSER PASSWORD 'superuser';
ALTER DATABASE clinlims OWNER TO clinlims;

-- Enable UUID extension for inventory tables
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
