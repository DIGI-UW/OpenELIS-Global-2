# quickstart.md — How to run & test Patient ID Documents feature (dev)

Prerequisites
- Java 21
- Maven 3.8+
- Node 16+
- Docker (optional for object storage)

Backend (build + run)

```bash
# from repo root
mvn clean install -DskipTests -Dmaven.test.skip=true
# Start app (existing dev flow)
docker compose -f dev.docker-compose.yml up -d oe.openelis.org
```

Frontend (dev)

```bash
cd frontend
npm install
npm start
```

Test the API (examples)

Upload document (multipart)

```bash
curl -v -u admin:password -X POST "https://localhost/rest/patients/P123/documents" \
  -F "file=@/path/to/national_id.jpg" \
  -F "documentType=NATIONAL_ID" \
  -F "description=Front of national id"
```

List documents

```bash
curl -u admin:password "https://localhost/rest/patients/P123/documents"
```

Download binary

```bash
curl -u admin:password -o national_id.jpg "https://localhost/rest/patients/P123/documents/{docId}/versions/{versionId}/content"
```

Note: Replace `admin:password` with appropriate credentials for local dev, and `https://localhost` with your dev URL. If object storage is used, configure signed URLs or proxy endpoints as required.

