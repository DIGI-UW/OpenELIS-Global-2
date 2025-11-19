#!/bin/bash
PROGNAME=$0
CALL_DIR=$PWD

INSTALLER_CREATION_DIR="OEInstaller"
STAGING_DIR="OEInstaller_stagingDir"

usage() {
  cat << EOF >&2
Usage: $PROGNAME [-b <branch>] [-l] [-i]

-b <branch>: git branch to build from
         -i: create installer
EOF
  exit 1
}

branch=master
createInstaller=false
while getopts :b:i opt; do
  case $opt in
    (b) branch=$OPTARG;;
    (i) createInstaller=true;;
    (*) usage
  esac
done
shift "$((OPTIND - 1))"

#get location of this script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  BUILD_SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
BUILD_SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
INSTALL_DIR="${BUILD_SCRIPT_DIR}/install"

#and other important locations
PROJECT_DIR="${BUILD_SCRIPT_DIR}"
#CONSOLIDATED_SERVER_DIR="${PROJECT_DIR}/Consolidated-Server/"

if [ $createInstaller == true ]
then
    cd ${CALL_DIR}

    if [ -d "${INSTALLER_CREATION_DIR}" ]
    then
        while true; do
            read -p "Installer directory has been detected, replace it? [Y]es [N]o: " yn
            case $yn in
                [Yy][Ee][Ss]|[Yy] ) break;;
                [Nn][Oo]|[Nn] ) exit;;
                * ) echo "Please answer yes or no.";;
            esac
        done
        rm -r ${INSTALLER_CREATION_DIR}
    fi
fi

cd ${PROJECT_DIR}
echo Will build from $branch
#cd source/openelisglobal-core
#git checkout -- app/src/build.properties
git checkout -B $branch
if [ $? != 0 ]
then
    echo
    echo "branch is not local will try to create"
    echo

    git checkout -b $branch origin/$branch

    if [ $? != 0 ]
    then
        echo
        echo "$branch not found in main repository. Check name"
        exit 1
    fi
fi
git pull origin $branch
git submodule update --recursive

#git rev-list HEAD | tac | nl | tail -n 1 | sed 's/\t/ hash-/g'  |sed 's/\s\{2,\}/revision-/g' > ../../version.txt
#cd ../..
#sed '2!d' source/openelisglobal-core/app/src/build.properties  > build.txt
cd ${CALL_DIR}

echo "creating docker images"

#create data import docker image
#bash ${INSTALL_DIR}/buildProject.sh -dl ${CONSOLIDATED_SERVER_DIR}
bash ${INSTALL_DIR}/buildProject.sh -dl ${PROJECT_DIR}/fhir -t hapi-fhir-jpaserver
SUCCESS=$?
if [ $SUCCESS != 0 ]
then
    echo
    echo build failed hapi-fhir-jpaserver
    exit $SUCCESS
fi
#create the frontend docker image
bash ${INSTALL_DIR}/buildProject.sh -dl ${PROJECT_DIR}/frontend -t openelisglobal-frontend
SUCCESS=$?
if [ $SUCCESS != 0 ]
then
    echo
    echo build failed openelisglobal-frontend
    exit $SUCCESS
fi
#create the frontend docker image
bash ${INSTALL_DIR}/buildProject.sh -dl ${PROJECT_DIR}/nginx-proxy -t nginx-proxy
SUCCESS=$?
if [ $SUCCESS != 0 ]
then
    echo
    echo build failed nginx-proxy
    exit $SUCCESS
fi
#create the docker image
bash ${INSTALL_DIR}/buildProject.sh -dl ${PROJECT_DIR} -t openelisglobal
SUCCESS=$?
if [ $SUCCESS != 0 ]
then
    echo
    echo build failed openelisglobal
    exit $SUCCESS
fi

createLinuxInstaller() {
    context=$1
    backupFile=$2
    installerName="${context}_${projectVersion}_Installer"

    echo "creating installer for context ${context}"
    mkdir -p ${INSTALLER_CREATION_DIR}/linux/${installerName}
    cp -r ${INSTALL_DIR}/installerTemplate/linux/* ${INSTALLER_CREATION_DIR}/linux/${installerName}
    cp OpenELIS-Global_DockerImage.tar.gz ${INSTALLER_CREATION_DIR}/linux/${installerName}/dockerImage/${context}-${projectVersion}.tar.gz
    cp Postgres_DockerImage.tar.gz ${INSTALLER_CREATION_DIR}/linux/${installerName}/dockerImage/Postgres_DockerImage.tar.gz
    cp JPAServer_DockerImage.tar.gz ${INSTALLER_CREATION_DIR}/linux/${installerName}/dockerImage/JPAServer_DockerImage.tar.gz
    cp AutoHeal_DockerImage.tar.gz ${INSTALLER_CREATION_DIR}/linux/${installerName}/dockerImage/AutoHeal_DockerImage.tar.gz
    cp NGINX_DockerImage.tar.gz ${INSTALLER_CREATION_DIR}/linux/${installerName}/dockerImage/NGINX_DockerImage.tar.gz
    cp ReactFrontend_DockerImage.tar.gz ${INSTALLER_CREATION_DIR}/linux/${installerName}/dockerImage/ReactFrontend_DockerImage.tar.gz


    chmod +x ${INSTALLER_CREATION_DIR}/linux/${installerName}/scripts/*.sh

    cd ${INSTALLER_CREATION_DIR}/linux
    tar -cf ${installerName}.tar ${installerName}
    gzip ${installerName}.tar
    cd ${CALL_DIR}
}

if [ $createInstaller == true ]
then
    cd ${PROJECT_DIR}
    #get useful info from the maven project
    output=$({ echo 'ARTIFACT_ID=${project.artifactId}';\
        echo 'PROJECT_VERSION=${project.version}'; } \
      | mvn help:evaluate --non-recursive )
    artifactId=$(echo "$output" | grep '^ARTIFACT_ID' | cut -d = -f 2)
    projectVersion=$(echo "$output" | grep '^PROJECT_VERSION' | cut -d = -f 2)
    cd ${CALL_DIR}

    echo "saving docker image as OpenELIS-Global_DockerImage.tar.gz"
    docker save openelisglobal:latest | gzip > OpenELIS-Global_DockerImage.tar.gz
    echo "saving React frontend docker image"
    docker save openelisglobal-frontend:latest | gzip > ReactFrontend_DockerImage.tar.gz
    echo "saving JPA Server docker image"
    docker save hapi-fhir-jpaserver:latest | gzip > JPAServer_DockerImage.tar.gz
    echo "saving Postgres docker image"
    docker pull postgres:14.4
    docker save postgres:14.4 | gzip > Postgres_DockerImage.tar.gz
    echo "saving Autoheal docker image"
    docker pull willfarrell/autoheal:1.2.0
    docker save willfarrell/autoheal:1.2.0 | gzip > AutoHeal_DockerImage.tar.gz
    echo "saving NGINX docker image"
    docker save nginx-proxy | gzip > NGINX_DockerImage.tar.gz

    mkdir ${STAGING_DIR}
    createLinuxInstaller OpenELIS-Global OffSiteBackupLinux.pl


    rm OpenELIS-Global_DockerImage*.tar.gz
    rm Postgres_DockerImage.tar.gz
    rm JPAServer_DockerImage.tar.gz
    rm AutoHeal_DockerImage.tar.gz
    rm NGINX_DockerImage.tar.gz
    rm ReactFrontend_DockerImage.tar.gz

    rm -r ${STAGING_DIR}

fi
