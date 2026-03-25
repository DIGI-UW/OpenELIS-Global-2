FROM python:3.11-slim

WORKDIR /app

RUN pip install --no-cache-dir uv

COPY pyproject.toml /app/pyproject.toml
COPY uv.lock /app/uv.lock
COPY README.md /app/README.md
RUN uv sync --frozen --no-dev --no-install-project

COPY src /app/src

CMD ["uv", "run", "uvicorn", "src.agents.router_server:app", "--host", "0.0.0.0", "--port", "9100"]
