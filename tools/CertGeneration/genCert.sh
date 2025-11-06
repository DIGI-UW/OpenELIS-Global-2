#!/bin/bash
CALL_DIR=$PWD

read -p 'Country Name: ' COUNTRY_NAME
echo "using country name $COUNTRY_NAME"
read -p 'would you like to use a central ROOT CA (press enter to skip, or name the central root): ' CENTRAL_ROOT
if [[ $CENTRAL_ROOT == "" ]];
then
  ROOT_CA_DIR=$CALL_DIR/$COUNTRY_NAME/ca/root
else
  ROOT_CA_DIR=$CALL_DIR/$CENTRAL_ROOT/ca/root
fi
INT_CA_DIR=$CALL_DIR/$COUNTRY_NAME/ca/intermediate
echo "using directory $INT_CA_DIR for CA generation"

#get location of this script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
if [[ ! -d "$ROOT_CA_DIR" || ! -d "$INT_CA_DIR" ]]
then
  echo "CAs do not exist yet. Generating them"
  $SCRIPT_DIR/generateCAs.sh $ROOT_CA_DIR $INT_CA_DIR $COUNTRY_NAME
fi

read -p 'Full Server Name (ie. cs.openelis.org): ' SERVER_NAME
echo "using server name $SERVER_NAME"
CERT_DIR_NAME=${SERVER_NAME}_ssl
CERT_DIR=$CALL_DIR/$COUNTRY_NAME/$CERT_DIR_NAME
mkdir -p  $CERT_DIR


cd $INT_CA_DIR
read -sp "Specify a password for the server's keys and keystores:" PASSWORD
echo
read -sp "Confirm password for the server's keys and keystores:" CONFIRM_PASSWORD
echo
while [ $PASSWORD != $CONFIRM_PASSWORD ];
do
  echo "passwords don't match, please try again"
  read -sp "Specify a password for the server's keys and keystores:" PASSWORD
  echo
  read -sp "Confirm password for the server's keys and keystores:" CONFIRM_PASSWORD
  echo
done

echo "making cert for $SERVER_NAME"
cp $SCRIPT_DIR/openssl_csr_san.cnf $INT_CA_DIR/${SERVER_NAME}_openssl_csr_san.cnf

DNS_ENTRIES=("*.openelis.org")
read -p 'Would you like a DNS entry for this cert (enter dns entry or enter to skip): ' DNS_ENTRY
until [[ $DNS_ENTRY == "" ]];
do
  echo "adding $DNS_ENTRY"
  DNS_ENTRIES+=($DNS_ENTRY)
  read -p 'Would you like another DNS entry for this cert (enter dns entry or enter to skip): ' DNS_ENTRY
done
IP_ENTRIES=()
read -p 'Would you like an IP entry for this cert (enter ip entry or enter to skip): ' IP_ENTRY
until [[ $IP_ENTRY == "" ]];
do
  echo "adding $IP_ENTRY"
  IP_ENTRIES+=($IP_ENTRY)
  read -p 'Would you like another IP entry for this cert (enter ip entry or enter to skip): ' IP_ENTRY
done

echo "" >> $INT_CA_DIR/${SERVER_NAME}_openssl_csr_san.cnf
for i in "${!DNS_ENTRIES[@]}"; do
  position=$(( $i + 1 ))
  echo "DNS.$position = ${DNS_ENTRIES[$i]}" >> $INT_CA_DIR/${SERVER_NAME}_openssl_csr_san.cnf
done
for i in "${!IP_ENTRIES[@]}"; do
  position=$(( $i + 1 ))
  echo "IP.$position  = ${IP_ENTRIES[$i]}" >> $INT_CA_DIR/${SERVER_NAME}_openssl_csr_san.cnf
done

cd $INT_CA_DIR
find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i s/FQDN_VAR/$SERVER_NAME/g
read -sp 'enter the CA keys password:' CA_PASSWORD
echo

openssl req -out $INT_CA_DIR/csr/$SERVER_NAME.csr.pem \
  -newkey rsa:2048 \
  -keyout $INT_CA_DIR/private/$SERVER_NAME.key.pem \
  -passout pass:$PASSWORD \
  -config $INT_CA_DIR/${SERVER_NAME}_openssl_csr_san.cnf
echo "made key for $SERVER_NAME"
echo "made csr for $SERVER_NAME"


openssl ca -config $INT_CA_DIR/openssl_intermediate.cnf \
  -extensions client_server_cert -days 1875 -notext -md sha256 \
  -batch -passin pass:$CA_PASSWORD \
  -in $INT_CA_DIR/csr/$SERVER_NAME.csr.pem \
  -out $INT_CA_DIR/certs/$SERVER_NAME.crt.pem \
  -rand_serial
echo "made crt from csr for $SERVER_NAME"

cat $INT_CA_DIR/certs/$SERVER_NAME.crt.pem \
  $INT_CA_DIR/certs/int.$COUNTRY_NAME.crt.pem $ROOT_CA_DIR/certs/ca.ROOT.crt.pem \
  > $INT_CA_DIR/certs/chain.$SERVER_NAME.crt.pem
echo "made chain crt from crt for $SERVER_NAME"

openssl pkcs12 -inkey $INT_CA_DIR/private/$SERVER_NAME.key.pem \
  -passin pass:$PASSWORD \
  -passout pass:$PASSWORD \
  -in $INT_CA_DIR/certs/chain.$SERVER_NAME.crt.pem \
  -export -out $INT_CA_DIR/certs/$SERVER_NAME.keystore.p12
echo "made keystore for $SERVER_NAME"

if [[ ! -f "$ROOT_CA_DIR/certs/ca.$SERVER_NAME.truststore.p12" ]]
then
  keytool -import -alias ROOT_CA -file $ROOT_CA_DIR/certs/ca.ROOT.crt.pem \
    -noprompt -storetype pkcs12 \
    -storepass $PASSWORD \
    -keystore $ROOT_CA_DIR/certs/ca.$SERVER_NAME.truststore.p12
  echo "made CA truststore for $SERVER_NAME using root CA"
fi

if [[ ! -f "$INT_CA_DIR/certs/ca.$SERVER_NAME.truststore.p12" ]]
then
  cp $ROOT_CA_DIR/certs/ca.$SERVER_NAME.truststore.p12 $INT_CA_DIR/certs/ca.$SERVER_NAME.truststore.p12
  keytool -import -alias intermediate_ca -file $INT_CA_DIR/certs/int.$COUNTRY_NAME.crt.pem \
    -noprompt -storetype pkcs12 \
    -storepass $PASSWORD \
    -keystore $INT_CA_DIR/certs/ca.$SERVER_NAME.truststore.p12
  echo "imported intermediate CA into truststore for $SERVER_NAME"
fi

if [[ ! -f "$INT_CA_DIR/certs/int.$SERVER_NAME.truststore.p12" ]]
then
  keytool -import -alias intermediate_ca -file $INT_CA_DIR/certs/int.$COUNTRY_NAME.crt.pem \
    -noprompt -storetype pkcs12 \
    -storepass $PASSWORD \
    -keystore $INT_CA_DIR/certs/int.$SERVER_NAME.truststore.p12
  echo "made intermediate CA for $SERVER_NAME using intermediate CA"
fi

if [[ ! -f "$INT_CA_DIR/certs/$SERVER_NAME.truststore.p12" ]]
then
  keytool -import -alias $SERVER_NAME -file $INT_CA_DIR/certs/$SERVER_NAME.crt.pem \
    -noprompt -storetype pkcs12 \
    -storepass $PASSWORD \
    -keystore $INT_CA_DIR/certs/$SERVER_NAME.truststore.p12
  cp $INT_CA_DIR/certs/$SERVER_NAME.truststore.p12 $CERT_DIR/$SERVER_NAME.truststore.p12
  echo "made truststore for only $SERVER_NAME"
else
  echo "old truststore found. erring on caution and leaving it as is. individual truststore not created "
fi
mv $INT_CA_DIR/certs/ca.$SERVER_NAME.truststore.p12 $CERT_DIR/ca.$SERVER_NAME.truststore.p12
mv $INT_CA_DIR/certs/int.$SERVER_NAME.truststore.p12 $CERT_DIR/int.$SERVER_NAME.truststore.p12
mv $INT_CA_DIR/certs/$SERVER_NAME.truststore.p12 $CERT_DIR/$SERVER_NAME.truststore.p12
mv $INT_CA_DIR/certs/chain.$SERVER_NAME.crt.pem $CERT_DIR/chain.$SERVER_NAME.crt.pem
mv $INT_CA_DIR/certs/$SERVER_NAME.crt.pem $CERT_DIR/$SERVER_NAME.crt.pem
mv $INT_CA_DIR/certs/$SERVER_NAME.keystore.p12 $CERT_DIR/$SERVER_NAME.keystore.p12
mv $INT_CA_DIR/private/$SERVER_NAME.key.pem $CERT_DIR/$SERVER_NAME.key.pem
mv $INT_CA_DIR/${SERVER_NAME}_openssl_csr_san.cnf $CERT_DIR/${SERVER_NAME}_openssl_csr_san.cnf

cd $CERT_DIR/..
tar -czf $CERT_DIR_NAME.tar.gz $CERT_DIR_NAME
