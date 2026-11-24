# JanManch

JanManch is a video sharing and streaming platform: users sign up, upload videos,
watch, like, comment, subscribe and search. It is built with Spring Boot, MySQL,
Redis, Kafka and AWS S3.

## Features

| Area | What it does |
|---|---|
| Accounts | Sign up and login with JWT; every user gets a channel |
| Upload | Video upload to local disk or S3, with title, description and tags |
| Processing | Kafka event after upload; a consumer reads the duration and creates a thumbnail with FFmpeg |
| Playback | Streaming with seek support; view counting buffered in Redis |
| Engagement | Like/dislike, comments, channel subscriptions |
| Discovery | Home feed, trending, search by title or tag, subscriptions feed, watch history |
| Performance | Redis cache on feed and trending; database indexes; batched view-count writes |
| Front end | Simple web pages served by the same application |

## Architecture

```
Browser ──► Spring Boot API ──► MySQL        (users, videos, comments, ...)
                │   │
                │   ├────────► Redis         (feed cache, view counters)
                │   └────────► S3 / disk     (video files, thumbnails)
                ▼
             Kafka topic "janmanch.video-uploaded"
                ▼
        Processing consumer (FFmpeg) ──► marks the video READY
```

## Requirements

- Java 17 or newer, Maven 3.9+
- Docker (MySQL, Redis and Kafka run in containers)
- FFmpeg (optional locally): without it videos still play and the site shows a
  frame from the video instead of a thumbnail

### Installing FFmpeg on Windows

```powershell
winget install Gyan.FFmpeg
```

Close and reopen the terminal (and IntelliJ), then check with `ffmpeg -version`.
If FFmpeg is installed elsewhere, point the app to it instead:

```powershell
$env:FFMPEG_PATH="C:\ffmpeg\bin\ffmpeg.exe"
$env:FFPROBE_PATH="C:\ffmpeg\bin\ffprobe.exe"
```

On start-up the app creates thumbnails for any older videos that have none.

## Run locally

```bash
docker compose up -d        # MySQL, Redis, Kafka
mvn spring-boot:run         # application on http://localhost:8080
```

- Web app: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

Sign up, upload a short MP4, wait a few seconds for processing, then open it
from the home page.

## API

| Method | Endpoint | Login | Purpose |
|---|---|---|---|
| POST | `/auth/signup`, `/auth/login` | No | Register, get a token |
| GET | `/users/me` | Yes | Profile and channel |
| GET | `/users/me/history` | Yes | Watch history |
| POST | `/videos` (multipart: file, title, description, tags) | Yes | Upload |
| GET | `/videos/feed?page=&size=` | No | Home feed (cached) |
| GET | `/videos/trending` | No | Most viewed (cached) |
| GET | `/videos/search?q=` | No | Search by title or tag |
| GET | `/videos/subscriptions` | Yes | Videos from subscribed channels |
| GET | `/videos/{id}` | No | Details and stream URL |
| POST | `/videos/{id}/view` | No | Count a view |
| DELETE | `/videos/{id}` | Yes | Delete own video |
| PUT | `/videos/{id}/reaction` (`LIKE`, `DISLIKE`, `NONE`) | Yes | React |
| GET / POST | `/videos/{id}/comments` | GET no / POST yes | List, add comments |
| DELETE | `/comments/{id}` | Yes | Delete own comment |
| GET | `/channels/{id}`, `/channels/{id}/videos` | No | Channel page |
| POST / DELETE | `/channels/{id}/subscribe` | Yes | Subscribe, unsubscribe |

Send the token as `Authorization: Bearer <token>`.

## Configuration

| Variable | Purpose | Local default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection | local Docker database |
| `REDIS_HOST`, `REDIS_PORT` | Redis connection | `localhost`, `6379` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka connection | `localhost:9092` |
| `STORAGE_TYPE` | `local` or `s3` | `local` |
| `STORAGE_LOCAL_DIR` | Folder for local files | `./data/media` |
| `S3_BUCKET`, `AWS_REGION` | Used when `STORAGE_TYPE=s3` | `janmanch-videos`, `ap-south-1` |
| `CACHE_TYPE` | `redis`, or `none` to disable caching | `redis` |
| `JWT_SECRET` | Token signing key, 32+ characters | development value; replace when deployed |

Never commit real secrets. Set them as environment variables.

## Project structure

```
src/main/java/com/janmanch
├── auth         sign up, login, JWT
├── user         users, profile, history endpoint
├── channel      channels and subscriptions
├── video        upload, feed, search, details, view counting
├── engagement   likes and comments
├── history      watch history
├── processing   Kafka topic, consumer, FFmpeg wrapper
├── storage      local disk and S3 storage
├── config       security configuration
└── common       error handling, paging
src/main/resources/static   web pages (HTML, CSS, JavaScript)
docs/            database schema, AWS deployment guide
benchmark/       seed data, k6 load test, benchmark steps
```

## More

- `docs/schema.sql`: database schema
- `docs/aws-deployment.md`: AWS deployment steps
- `benchmark/README.md`: how to measure the feed optimisation

## Not included yet

Multiple resolutions and adaptive streaming (HLS), recommendations, live
streaming, email verification, content moderation tools and rate limiting.

## Acknowledgement

AI tools were used in building this project.
