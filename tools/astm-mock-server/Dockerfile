# ASTM LIS2-A2 Mock Server Docker Image
# For OpenELIS analyzer testing
# Reference: specs/004-astm-analyzer-mapping/plan.md

FROM python:3.11-slim

LABEL maintainer="OpenELIS Global <openelis@uw.edu>"
LABEL description="ASTM LIS2-A2 Mock Server for analyzer testing"

WORKDIR /app

# Copy server files
COPY server.py fields.json ./

# Server uses only Python standard library - no pip install needed

# Default port
EXPOSE 5000

# Environment variables (can be overridden)
ENV ASTM_PORT=5000
ENV ANALYZER_TYPE=HEMATOLOGY
ENV RESPONSE_DELAY_MS=100

# Health check - verify server is listening
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD python -c "import socket; s=socket.socket(); s.settimeout(2); s.connect(('localhost', 5000)); s.close()" || exit 1

# Run server
CMD ["python", "-u", "server.py"]








