# Use the official PostgreSQL 14.4 image as base
FROM postgres:14.4

RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*
# Copy initialization scripts and data files into the container
# PostgreSQL will automatically run any *.sql or *.sh files in this folder
COPY ./db/dbInit /docker-entrypoint-initdb.d
COPY ./db/dbInit/OpenELIS-Global.sql /docker-entrypoint-initdb.d/OpenELIS-Global.sql
COPY ./db/dbInit/siteInfo.sql /docker-entrypoint-initdb.d/siteInfo.sql

RUN chmod -R 755 /docker-entrypoint-initdb.d \
    && chown -R postgres:postgres /docker-entrypoint-initdb.d

# Optional: Set working directory (not strictly needed)
WORKDIR /docker-entrypoint-initdb.d

# Expose the PostgreSQL port
EXPOSE 5432