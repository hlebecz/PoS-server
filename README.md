**This repository includes exclusively application files**
Here is full docker-compose configuration in case needed:

////////////////////////////////////////////////////////
services:
  app:
    build:
      context: ./PoS-server
      dockerfile: ./app.Dockerfile
    ports:
      - "8080:8080"
    environment:
      - DB_HOST=${DB_HOST}
    depends_on:
      - db

  db:
    image: postgres:latest
    restart: unless-stopped
    volumes:
      - pgdata:/var/lib/postgresql
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_USER=${POSTGRES_USER}
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
      - POSTGRES_DB=${POSTGRES_DB}

  pgadmin:
    image: dpage/pgadmin4:latest
    restart: unless-stopped
    depends_on:
      - db
    environment:
        - PGADMIN_DEFAULT_EMAIL=${PGADMIN_DEFAULT_EMAIL}
        - PGADMIN_DEFAULT_PASSWORD=${PGADMIN_DEFAULT_PASSWORD}
    ports:
        - "8081:80"
    volumes:
        - pgadmin-data:/var/lib/pgadmin
volumes:
  pgdata:
  pgadmin-data:.
