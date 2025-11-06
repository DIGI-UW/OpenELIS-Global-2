#!/bin/bash
CALL_DIR=$PWD
ROOT_CA_DIR=$1
INT_CA_DIR=$2
COUNTRY_NAME=$3

if [ -z "$COUNTRY_NAME" ]
then
  read -p 'Country Name: ' COUNTRY_NAME
  echo
fi
if [ -z "$INT_CA_DIR" ]
then
  INT_CA_DIR=$CALL_DIR/$COUNTRY_NAME/ca/intermediate
fi
if [ -z "$ROOT_CA_DIR" ]
then
  ROOT_CA_DIR=$CALL_DIR/$COUNTRY_NAME/ca/root
fi
echo "using country name $COUNTRY_NAME"
echo "using directory $ROOT_CA_DIR for ROOT CA generation"
echo "using directory $INT_CA_DIR for Intermediate CA generation"

#get location of this script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

echo "Make sure to choose a secure password and store it somewhere secure!"
read -sp 'Specify a password for the CA keys and keystores:' PASSWORD
echo
read -sp 'Confirm password for the CA keys and keystores:' CONFIRM_PASSWORD
echo

while [ $PASSWORD != $CONFIRM_PASSWORD ];
do
  echo "passwords don't match, please try again"
  read -sp 'Specify a password for the CA keys and keystores:' PASSWORD
  echo
  read -sp 'Confirm password for the CA keys and keystores:' CONFIRM_PASSWORD
  echo
done

if [[ ! -d "$ROOT_CA_DIR" ]]
then
  echo "making root CA in $ROOT_CA_DIR"
  mkdir -p $ROOT_CA_DIR
  cd $ROOT_CA_DIR
  mkdir newcerts certs crl private requests

  touch index.txt
  touch index.txt.attr
  echo '1000' > serial
  cp $SCRIPT_DIR/openssl_root.cnf $ROOT_CA_DIR/openssl_root.cnf

  echo "running find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i s/DOMAINNAME/ROOT/g"
  echo "running find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i s/ROOT_DIRECTORY/${ROOT_CA_DIR//\//\\\/}/g"
  find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i s/DOMAINNAME/ROOT/g
  find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i s/ROOT_DIRECTORY/${ROOT_CA_DIR//\//\\\/}/g

  openssl genrsa -aes256 -out private/ca.ROOT.key.pem -passout pass:$PASSWORD 4096
  echo "made root key for $COUNTRY_NAME"

  openssl req -config openssl_root.cnf -new -x509 -sha256 \
    -extensions v3_ca -key $ROOT_CA_DIR/private/ca.ROOT.key.pem \
    -passin pass:$PASSWORD \
    -out $ROOT_CA_DIR/certs/ca.ROOT.crt.pem -days 3650 -set_serial 0
  echo "made root crt for $COUNTRY_NAME"
fi



if [[ ! -d "$INT_CA_DIR" ]]
then
  echo "making intermediate CA"
  mkdir -p $INT_CA_DIR
  cd $INT_CA_DIR
  mkdir certs newcerts crl csr private
  touch index.txt
  touch index.txt.attr
  echo 1000 > $INT_CA_DIR/crlnumber
  echo '1234' > serial
  cp $SCRIPT_DIR/openssl_intermediate.cnf $INT_CA_DIR/openssl_intermediate.cnf

  echo "running find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i s/DOMAINNAME/${COUNTRY_NAME//\//\\\/}/g"
  echo "running find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i s/INT_DIRECTORY/${INT_CA_DIR//\//\\\/}/g"
  find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i s/DOMAINNAME/${COUNTRY_NAME//\//\\\/}/g
  find ./ \( -type d -name .git -prune \) -o -type f -print0 | xargs -0 sed -i s/INT_DIRECTORY/${INT_CA_DIR//\//\\\/}/g

  openssl req -config $INT_CA_DIR/openssl_intermediate.cnf -new \
    -newkey rsa:4096 -keyout $INT_CA_DIR/private/int.$COUNTRY_NAME.key.pem \
    -passout pass:$PASSWORD \
    -out $INT_CA_DIR/csr/int.$COUNTRY_NAME.csr
  echo "made intermediate key for $COUNTRY_NAME"
  echo "made intermediate csr for $COUNTRY_NAME"

  openssl ca -config $ROOT_CA_DIR/openssl_root.cnf -extensions v3_intermediate_ca -days 3650 \
    -notext -md sha256 -in $INT_CA_DIR/csr/int.$COUNTRY_NAME.csr \
    -batch -passin pass:$PASSWORD  \
    -out $INT_CA_DIR/certs/int.$COUNTRY_NAME.crt.pem \
    -rand_serial
  echo "made intermediate crt for $COUNTRY_NAME"

  cat $INT_CA_DIR/certs/int.$COUNTRY_NAME.crt.pem $ROOT_CA_DIR/certs/ca.ROOT.crt.pem \
    > $INT_CA_DIR/certs/chain.$COUNTRY_NAME.crt.pem
  echo "made CA chain crt for $COUNTRY_NAME"
fi
