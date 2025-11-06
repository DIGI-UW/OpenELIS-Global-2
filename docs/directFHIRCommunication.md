# Direct FHIR store communication

## Information about the co-resident FHIR store

We deploy an instance of the hapi-fhir-jpaserver beside every instance of
OpenELIS-GLobal-2 to provide a way to quickly store and query FHIR resources.
Usually the communication with the FHIR store is handled through communication
by OpenELIS-Global-2, but occaisionally there is a need (debugging, running a
reindex, etc) for someone to interact with the FHIR store directly. The FHIR api
is well documented online and will not be covered here, but since communication
to the FHIR store is locked down to only accept communication over HTTPS with
Client Authentication, instructions for communicating via terminal will be
covered.

## https_proxy

If your server has an https proxy configured, it may interrupt the
communication. To alleviate this and communicate directly to localhost, run:

```
unset https_proxy
```

## Extract certs and keys for use with curl

We will need the key and cert to communicate that OpenELIS-GLobal-2 uses for
backend communication. To acomplish this, you will need the password to your
keystore. This is located in
`/var/lib/openelis-global/secrets/common.properties` under the
`server.ssl.key-store-password` property.

The key and cert can be retreived from the keystore, but it is important to
delete the key after you are done with your comunications, so that a plaintext
key isn't left on the server.

### Extract cert

```
sudo openssl pkcs12 -in /etc/openelis-global/keystore -nodes -nokeys -out /etc/openelis-global/cert.pem
```

### Extract key

```
sudo openssl pkcs12 -in /etc/openelis-global/keystore -nodes -nocerts -out /etc/openelis-global/key.pem
```

### Delete key

```
sudo rm /etc/openelis-global/key.pem
```

## Communicating with curl

### GET Request

```
sudo curl --cert /etc/openelis-global/cert.pem --key /etc/openelis-global/key.pem  -k 'https://localhost:8444/fhir/'
```

### POST Request

```
sudo curl -X POST -H "Content-Type: application/json" -d '@filename' --cert /etc/openelis-global/cert.pem --key /etc/openelis-global/key.pem  -k 'https://192.168.12.157:8444/fhir/'

```
