FROM python:3.11-slim

WORKDIR /app

RUN pip install --no-cache-dir poetry==1.8.3

COPY pyproject.toml /app/pyproject.toml
COPY README.md /app/README.md
RUN poetry install --no-root --only main

COPY src /app/src

CMD ["poetry", "run", "uvicorn", "src.server:app", "--host", "0.0.0.0", "--port", "9102"]
