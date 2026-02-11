# OpenELIS Global 2 - Development Container
# Provides Java 21, Maven, Node.js 20, and common dev tools for cloud IDEs
# (Cursor, VS Code, GitHub Codespaces)
FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

# Avoid prompts from apt
ENV DEBIAN_FRONTEND=noninteractive

# Java 21 (MANDATORY per AGENTS.md)
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-21-jdk \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Maven
RUN apt-get update && apt-get install -y --no-install-recommends \
    maven \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Node.js 20 (per frontend/package.json and CI)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Git, Docker CLI (for docker compose - actual daemon via Docker-in-Docker if needed)
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    docker.io \
    docker-compose-v2 \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Verify installations
RUN java -version && mvn -version && node -version && npm -version

# Default to project root when opening shell
WORKDIR /workspace
