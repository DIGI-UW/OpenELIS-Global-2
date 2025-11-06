# Property files for server configuration

OpenELIS reads in properties from a few different sources. The property will be
set to the last specified source

- /src/main/resources/application.properties - contained in the docker container
  when compiled

- /var/lib/openelisglobal/secrets/common.properties - overwritten every time
  setup script is called based on values specified to script

- /var/lib/openelis-global/secrets/extra.properties - optional file for
  specifying attributes outside of setup script that is persistent

If you are looking at manually adding/setting a property (ie a facility fhir
repo), it should be added to extra.properties

# common.properties

These are the properties that are set in common.properties. Most values are
stored and read from the config directory at /var/lib/openelis-global/config/

- server.ssl.key-store - the keystore location inside the docker container

- server.ssl.key-store-password - keystore password collected from the user and
  persisted in config directory as KEYSTORE_PASSWORD

- server.ssl.key-password - key password collected from the user and persisted
  in config directory as KEYSTORE_PASSWORD

- server.ssl.trust-store - the truststore location inside the docker container

- server.ssl.trust-store-password - truststore password collected from the user
  and persisted in config directory as TRUSTSTORE_PASSWORD

- encryption.general.password - encrytption key for storing encrypted values in
  db and persisted in config directory as ENCRYPTION_KEY

- spring.datasource.driver

- spring.datasource.url - database url. These values are set from
  /etc/openelis-global/setup.ini

- spring.datasource.username

- spring.datasource.password - datasource password. These values are set from
  /var/lib/openelis-global/secrets/datasource.password

- org.openelisglobal.fhirstore.uri - the local fhir server (normally
  https://fhir.openelis.org:8443/fhir/)

- org.openelisglobal.remote.source.uri - the remote fhir server where orders are
  imported from (ie the CS) and persisted in config directory as
  REMOTE_FHIR_SOURCE

- org.openelisglobal.remote.source.updateStatus - normally true, sets whether OE
  should update the remote instance when it imports orders (move status from
  requested to accepted)

- org.openelisglobal.remote.source.identifier - the identifier(s) that this OE
  instance will try to import orders for (ie Organization/uuid) and persisted in
  config directory as FHIR_IDENTIFIER

- org.openelisglobal.task.useBasedOn

- org.openelisglobal.fhir.subscriber - the server where FHIR objects should be
  sent to (ie the CS) and persisted in config directory as CS_SERVER

- org.openelisglobal.fhir.subscriber.resources - the FHIR resources that are
  sent to the subscriber

# extra.properties

As the last loaded file, properties set here will overwrite ones set in other
files. As such, if it seems that the values in the config directory are not
being used, it could be that different values are being supplied in
extra.properties

For a full properties list, please see the
[wiki entry here](https://github.com/DIGI-UW/OpenELIS-Global-2/wiki/Properties)
