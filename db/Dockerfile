# Keep the PostgreSQL major version on a supported operating-system base.
FROM postgres:14.24-bookworm

RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*
# Copy initialization scripts and data files into the container
# PostgreSQL will automatically run any *.sql or *.sh files in this folder
COPY ./db/dbInit /docker-entrypoint-initdb.d
COPY ./db/dbInit/OpenELIS-Global.sql /docker-entrypoint-initdb.d/OpenELIS-Global.sql
COPY ./db/dbInit/siteInfo.sql /docker-entrypoint-initdb.d/siteInfo.sql

RUN chmod -R 755 /docker-entrypoint-initdb.d \
    && chown -R postgres:postgres /docker-entrypoint-initdb.d

# Existing volumes: rebuild indexes once when the OS collation library changed (see the script).
COPY ./db/docker-entrypoint-collation.sh /usr/local/bin/docker-entrypoint-collation.sh
RUN chmod 755 /usr/local/bin/docker-entrypoint-collation.sh

# Optional: Set working directory (not strictly needed)
WORKDIR /docker-entrypoint-initdb.d

# Expose the PostgreSQL port
EXPOSE 5432

ENTRYPOINT ["docker-entrypoint-collation.sh"]
CMD ["postgres"]
