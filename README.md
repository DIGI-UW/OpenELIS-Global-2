# OpenELIS Global 2

OpenELIS Global is open enterprise-level laboratory information system software
tailored for public health laboratories. OpenELIS is used at a national scale in
a variety of settings, from small general hospital labs, all the way up to
national reference labs, and all sizes in between.

Thousands of users use OpenELIS daily to make their laboratory jobs easier by
automating work plans, importing results from clinical analyzers, and supporting
complex workflows like pathology and cytology, reducing turnaround times, and
increasing result accuracy for better patient care.

OpenELIS Global meets all relevant ISO and SLIPTA requirements for the
accreditation of labs.

OpenELIS adheres to the strictest of security standards to keep your data safe
and supports fully featured, standards-based interoperability to make it easy to
receive lab orders and send results to other systems

Please vist our [website](http://www.openelis-global.org/) for more information.

You can find more information on how to set up OpenELIS at our
[docs page](http://docs.openelis-global.org/)

### CI Status

[![Maven Build Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/ci.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/ci.yml)
![Coverage](https://raw.githubusercontent.com/DIGI-UW/OpenELIS-Global-2/refs/heads/gh-pages/badges/jacoco.svg)

[![Publish OpenELIS WebApp Docker Image Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/publish-and-test.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/publish-and-test.yml)

[![End to End QA Tests Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/frontend-qa.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/frontend-qa.yml)

[![End to End QA Tests Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/build-installer.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/build-installer.yml)

### Requirements

1. You need to install [Docker](https://docs.docker.com/engine/install/) and
   [Docker compose](https://docs.docker.com/compose/install/)

1. For development , you need to install [Java](https://openjdk.org/install/) 21

### For Offline Installation Using the OpenELIS Global2 Installer

Download the OpenELIS Global Installer for each Release from the
[Release Assets](https://github.com/DIGI-UW/OpenELIS-Global-2/releases)

see full
[installation instructions](https://uwdigi.atlassian.net/wiki/x/EoBIDg#Downloaded-Installer-Offline-Setup)
for Offline Installation

### For running OpenELIS Global2 in Docker with default Settings out of the Box

see [OpenELIS-Docker setup](https://github.com/DIGI-UW/openelis-docker)

### For Running OpenELIS Global2 from Source Code

#### Running OpenELIS Global2 using docker compose With published docker images on dockerhub

    docker compose up -d

#### Running OpenELIS Global2 using docker compose with docker images built directly from the source code

    docker compose -f build.docker-compose.yml up -d --build

#### Running OpenELIS Global2 with docker compose For Development

Here Artifacts (ie the War file and React code) are compiled/built on the local
machine outside docker and just mounted into the docker compose setup. This
speeds up the development process

1.  Fork the
    [OpenELIS-Global Repository](https://github.com/DIGI-UW/OpenELIS-Global-2.git)
    and clone the forked repo. The `username` below is the `username` of your
    Github profile.

         git clone https://github.com/username/OpenELIS-Global-2.git

1.  innitialize and build sub modules

        cd OpenELIS-Global-2
        git submodule update --init --recursive
        cd dataexport
        mvn clean install -DskipTests

1.  Navigate back to the repository directory:

         cd ..

1.  Build the War file

          mvn clean install -DskipTests -Dmaven.test.skip=true

1.  Start the containers to mount the locally compiled artifacts

        docker compose -f dev.docker-compose.yml up -d

    Note : For Reflecting Local changes in the Running Containers ;

- Any Changes to the [Front-end](./frontend/) React Source Code will be directly
  Hot Reloaded in the UI
- For changes to the [Back-end](./src/) Java Source code

  - Run the maven build again to re-build the War file

         mvn clean install -DskipTests -Dmaven.test.skip=true

  - Recreate the Openelis webapp container

        docker compose -f dev.docker-compose.yml up -d  --no-deps --force-recreate oe.openelis.org

#### The Instances can be accessed at

| Instance     |                   URL                   | credentials (user : password) |
| ------------ | :-------------------------------------: | ----------------------------: |
| Legacy UI    | https://localhost/api/OpenELIS-Global/  |            admin: adminADMIN! |
| New React UI |           https://localhost/            |            admin: adminADMIN! |

**Note:** If your browser indicates that the website is not secure after
accessing any of these links, simply follow these steps:

1. Scroll down on the warning page.
2. Click on the "Advanced" button.
3. Finally, click on "Proceed to https://localhost" to access the development
   environment.

#### Formating the Source code after making changes

1.  After making UI changes to the [frontend](./frontend/) directory , run the
    formatter to properly format the Frontend code

        cd frontend
        npm run format

2.  After making changes to the [backend](./src/) directory, run the formatter
    to properly format the Java code

        mvn spotless:apply

#### To ensure your code passes the same checks as the CI pipeline, you can run the following commands from your project directory

1.  Run Code Formatting Check (Backend). This command checks code formatting and
    performs validation similar to the CI

        mvn spotless:check

1.  Run Build Check (Backend). This command builds the project similar to CI

        mvn clean install -Dspotless.check.skip=true

1.  To run Individual Integration Test

         mvn verify -Dit.test=<packageName>.<TestClassName>

1.  Run Frontend Formatting, Build, and E2E Test Checks similar to CI

    > **Note:** Frontend checks will only pass successfully if your development
    > environment is properly set up and running without issues.

        cd frontend/ # from project directory
        npm install
        npm run build
        npm run cy:run # this will run e2e testing same CI

### Testing Resources

For comprehensive testing guidance, see:

- **Testing Roadmap**: `.specify/guides/testing-roadmap.md` - Complete testing
  guide for both agents and humans
- **Test Templates**: `.specify/templates/testing/` - Standardized test
  templates
- **AGENTS.md**: Testing Strategy section - Overview of testing approach

### Pull request guidelines

Please follow the [pull request tips](PULL_REQUEST_TIPS.md) in order to make
life easy for the code reviewers by having a well defined and clean pull
request.

### code of conduct

Please see our [Contributor Code of Conduct](./CODE_OF_CONDUCT.md)
