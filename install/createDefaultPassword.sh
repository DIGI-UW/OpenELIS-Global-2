#!/bin/bash
PROGNAME=$0
pw=''

cli_flag=false

print_usage() {
  printf "./createDefaultPassword.sh -c -p <password>"
}

while getopts 'cp:' flag; do
  case "${flag}" in
    c) cli_flag=true ;;
    p) pw=${OPTARG} ;;
    *) print_usage
       exit 1 ;;
  esac
done

#get location of this script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  buildDir="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
buildDir="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

#and other important locations
projectDir="${buildDir}/.."
resourcesDir="${projectDir}/src/main/resources"

echo "Please enter a default password that can be used to login into the default admin account that is created on install"
echo "This password will be stored in the created project war as a hash. "
echo "This is technically secure, but it is recommended to change the password on installation"

echo "Output location: ${resourcesDir}"

mkdir -p  ${resourcesDir};

if [ $cli_flag == true ]
then
  htpasswd -bcBC 12 ${resourcesDir}/adminPassword.txt admin ${pw}
else
  htpasswd -cBC 12 ${resourcesDir}/adminPassword.txt admin
fi

result=$?

echo "Result (for debugging): ${result}"

while [ $result != 0 ]; do
  if [ $result == 3 ]
  then
    echo "passwords did not match"
  else
    echo "an error occured creating the password"
  fi
  while [ $cli_flag == false ]; do
    read -p "try again? [Y]es [N]o: " yn
    case $yn in
      [Yy][Ee][Ss]|[Yy] ) break;;
      [Nn][Oo]|[Nn] ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
  done
  if [ $cli_flag == true ]
  then
    htpasswd -bcBC 12 ${resourcesDir}/adminPassword.txt admin ${pw}
  else
    htpasswd -cBC 12 ${resourcesDir}/adminPassword.txt admin
  fi
  result=$?
done
