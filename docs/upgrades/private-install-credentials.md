# Production credentials belong to the installation

Application images no longer contain an initial administrator password or hash.
The application reads a plaintext password file only when neither the default
administrator nor another administrator exists. It validates the password using
the site's password policy, hashes it through the existing login service, and
stops application initialization if the file is missing or invalid. Startup does
not replace an existing administrator's password.

## Fresh installations

Copy `.env.example` to `.env`. Set three different private values:

- `OE_DB_PASSWORD`: the application database role, `clinlims`.
- `ADMIN_PASSWORD`: the database administrator role, `admin`.
- `POSTGRES_PASSWORD`: PostgreSQL's bootstrap account, `postgres`.

Create the initial OpenELIS administrator password without displaying it:

```sh
umask 077
mkdir -p secrets
printf 'OEa1!%s\n' "$(openssl rand -hex 24)" > secrets/initial-admin-password
docker compose pull
docker compose up -d --no-build
```

Retrieve the file locally when logging in as `admin`; keep it out of source
control, logs and shared installation packages. `OE_INITIAL_ADMIN_PASSWORD_FILE`
in `.env` selects the host file mounted as a Compose secret. The container
copies it to a private runtime file readable by the application user. Direct WAR
installations must set the application's `OE_INITIAL_ADMIN_PASSWORD_FILE`
environment variable to a readable local file. No build-time password generator
or bundled-password fallback remains. Password files contain plaintext only,
with an optional trailing line ending, and must fit bcrypt's 72-byte limit.

## Existing installations

Keep the existing database passwords and database volume. Set
`POSTGRES_PASSWORD` to the value previously supplied through `database.env`;
changing an environment variable does not rotate an existing PostgreSQL role's
password. The database initialization script runs only for an empty data
directory.

The application does not read the initial password file if an administrator
already exists. Compose still needs a mountable file at the configured host
path; an empty file is sufficient for an existing installation. Do not remove
the database volume or expect this upgrade to rotate previously shared
passwords. Rotation is a separate operational action.

The normal container path supplies database credentials through the environment
or the existing `datasource.password` secret. Tomcat resolves database and TLS
store passwords without putting them in JVM arguments. Remove old password
overrides (`-Ddatasource.password`, `-Doe.ssl.*password`,
`-Dhapi.ssl.*password`, and `-Djavax.net.ssl.*StorePassword`) from custom launch
configurations when adopting the new images. Keep the TLS store passwords in the
existing `SSL_KEYSTORE_PASSWORD` and `SSL_TRUSTSTORE_PASSWORD` environment
settings.

## Development and validation

The source development and CI Compose paths mount an explicit test-only password
fixture and supply test database credentials. These are not production defaults;
the fixture directory is excluded from image builds. Production Compose requires
installation credentials even if a development environment file was copied.
