#!/bin/bash

# Build script for multi-architecture Docker images
# Builds and pushes OpenELIS-Global-2 images for both ARM and AMD architectures

set -e

# Configuration
DOCKERHUB_USER="trigonaltechnology"
VERSION="${1:-latest}"
PLATFORMS="linux/amd64,linux/arm64"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

echo_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if docker buildx is available
if ! docker buildx version > /dev/null 2>&1; then
    echo_error "docker buildx is not available. Please install Docker Buildx."
    exit 1
fi

# Create and use a buildx builder if it doesn't exist
BUILDER_NAME="openelis-multiarch-builder"
if ! docker buildx inspect $BUILDER_NAME > /dev/null 2>&1; then
    echo_info "Creating buildx builder: $BUILDER_NAME"
    docker buildx create --name $BUILDER_NAME --use
    docker buildx inspect --bootstrap
else
    echo_info "Using existing buildx builder: $BUILDER_NAME"
    docker buildx use $BUILDER_NAME
fi

# Function to build and push an image
build_and_push() {
    local context=$1
    local dockerfile=$2
    local image_name=$3
    local tag="${DOCKERHUB_USER}/${image_name}:${VERSION}"
    
    echo_info "Building ${image_name}..."
    echo_info "  Context: ${context}"
    echo_info "  Dockerfile: ${dockerfile}"
    echo_info "  Tag: ${tag}"
    echo_info "  Platforms: ${PLATFORMS}"
    
    docker buildx build \
        --platform ${PLATFORMS} \
        --file "${dockerfile}" \
        --tag "${tag}" \
        --push \
        "${context}"
    
    if [ $? -eq 0 ]; then
        echo_info "Successfully built and pushed ${tag}"
    else
        echo_error "Failed to build ${image_name}"
        exit 1
    fi
}

# Build database image
echo_info "=========================================="
echo_info "Building OpenELIS Database Image"
echo_info "=========================================="
 build_and_push \
     "./db" \
     "./db/Dockerfile" \
     "openelis-global-2-database"

# Build backend image
echo_info "=========================================="
echo_info "Building OpenELIS Backend Image"
echo_info "=========================================="
 build_and_push \
     "." \
     "./Dockerfile" \
     "openelis-global-2"

# Build FHIR image (needs root context for tomcat directory)
echo_info "=========================================="
echo_info "Building OpenELIS FHIR Image"
echo_info "=========================================="
# build_and_push \
#     "." \
#     "./fhir/Dockerfile" \
#     "openelis-global-2-fhir"

# Build frontend image (production)
echo_info "=========================================="
echo_info "Building OpenELIS Frontend Image"
echo_info "=========================================="
build_and_push \
    "./frontend" \
    "./frontend/Dockerfile.prod" \
    "openelis-global-2-frontend"

echo_info "=========================================="
echo_info "All images built and pushed successfully!"
echo_info "=========================================="
echo_info "Images pushed to DockerHub:"
echo_info "  - ${DOCKERHUB_USER}/openelis-global-2-database:${VERSION}"
echo_info "  - ${DOCKERHUB_USER}/openelis-global-2:${VERSION}"
echo_info "  - ${DOCKERHUB_USER}/openelis-global-2-fhir:${VERSION}"
echo_info "  - ${DOCKERHUB_USER}/openelis-global-2-frontend:${VERSION}"

