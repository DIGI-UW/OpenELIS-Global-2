# OpenELIS Global 2

This is the OpenELIS rewrite onto Java Spring, and with all new technology and features. 

Please visit our [website](http://www.openelis-global.org/) for more information.

You can find detailed setup instructions on our [docs page](http://docs.openelis-global.org/).

---

## ✅ CI Status

[![Maven Build Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/ci.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/ci.yml)
![Coverage](https://raw.githubusercontent.com/DIGI-UW/OpenELIS-Global-2/refs/heads/gh-pages/badges/jacoco.svg)

[![Publish OpenELIS WebApp Docker Image Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/publish-and-test.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/publish-and-test.yml)

[![End to End QA Tests Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/frontend-qa.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/frontend-qa.yml)

[![Installer Build Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/build-installer.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/build-installer.yml)

---

## 🛠 Requirements

1. Install [Docker](https://docs.docker.com/engine/install/)
2. Install [Docker Compose](https://docs.docker.com/compose/install/)
3. Install [Java 21](https://openjdk.org/install/) for development

---

## 💿 Offline Installation Using the OpenELIS Global2 Installer

Download the installer from the [Release Assets](https://github.com/DIGI-UW/OpenELIS-Global-2/releases).

Full instructions are available in the [installation guide](https://docs.openelis-global.org/en/latest/install/).

---

## 🐳 Running in Docker (Out of the Box)

See [OpenELIS-Docker Setup](https://github.com/DIGI-UW/openelis-docker)

### Using published Docker images:

```bash
docker-compose up -d
```

### Building Docker images from source:

```bash
docker-compose -f build.docker-compose.yml up -d --build
```

---

## 🧑‍💻 Run from Source (For Developers)

### Clone and Build

```bash
git clone https://github.com/your-username/OpenELIS-Global-2.git
cd OpenELIS-Global-2
git submodule update --init --recursive
```

### Build Submodules

```bash
cd dataexport
mvn clean install -DskipTests
cd ..
```

### Build WAR File

```bash
mvn clean install -DskipTests
```

### Run Locally Using Docker Compose (Development Mode)

```bash
docker-compose -f dev.docker-compose.yml up -d
```

### Notes for Developers

- **Frontend (React)**: supports **Hot Reloading**
- **Backend (Java)**:
  - Rebuild WAR file:
    ```bash
    mvn clean install -DskipTests
    ```
  - Restart backend container:
    ```bash
    docker-compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
    ```

---

## 🌐 Access the Application

| Interface     | URL                                      | Credentials              |
|---------------|------------------------------------------|--------------------------|
| Legacy UI     | https://localhost/api/OpenELIS-Global/   | `admin : adminADMIN!`   |
| New React UI  | https://localhost/                       | `admin : adminADMIN!`   |

> **Note**: If your browser warns about security:
> - Click "Advanced"
> - Click "Proceed to localhost"

---

## ✨ Formatting Code

### Frontend (React)

```bash
cd frontend
npm run format
```

### Backend (Java)

```bash
mvn spotless:apply
```

---

## 🔁 Pull Request Guidelines

Please follow the [Pull Request Tips](PULL_REQUEST_TIPS.md) to help us review your contributions effectively.

---

## 📜 Code of Conduct

All contributors must adhere to our [Code of Conduct](./CODE_OF_CONDUCT.md).
