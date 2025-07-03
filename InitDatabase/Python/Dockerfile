# Dockerfile for the Python import script
# Creates a slim image and installs all required dependencies

FROM python:3.11-slim

# Install Python dependencies
WORKDIR /app

COPY InitDatabase/Python/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY InitDatabase/Python/main.py ./main.py

# Copy CSV data used by the script
COPY InitDatabase/Python/ ./

# Default environment variables (can be overridden by docker-compose)
ENV DB_HOST=db \
    DB_PORT=5432 \
    DB_USER=user \
    DB_PASSWORD=password \
    DB_NAME=simdata

CMD ["python", "main.py"]