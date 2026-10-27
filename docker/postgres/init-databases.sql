-- Rydr Microservices Database Initialization Script
-- Automatically creates required databases and enables PostGIS / UUID extensions

-- Create Databases
CREATE DATABASE trips;
CREATE DATABASE ratings;
CREATE DATABASE payments;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE trips TO postgres;
GRANT ALL PRIVILEGES ON DATABASE ratings TO postgres;
GRANT ALL PRIVILEGES ON DATABASE payments TO postgres;

-- Connect to 'users' and enable uuid-ossp
\connect users;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Connect to 'trips' and enable PostGIS + uuid-ossp
\connect trips;
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Connect to 'ratings' and enable uuid-ossp
\connect ratings;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Connect to 'payments' and enable uuid-ossp
\connect payments;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

