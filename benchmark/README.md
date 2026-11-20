# Feed benchmark: before and after optimisation

This measures the key feature for the report: the home feed (`GET /videos/feed`).
The optimisation has two parts: a database index on `(status, uploaded_at)` and a
60-second Redis cache.

## 1. Load test data

Start the application once so the tables exist, then:

```bash
docker exec -i janmanch-mysql mysql -ujanmanch -pjanmanch_local janmanch_db < benchmark/seed.sql
```

## 2. "Before" run: no cache, no index

```bash
docker exec -i janmanch-mysql mysql -ujanmanch -pjanmanch_local janmanch_db \
  -e "ALTER TABLE videos DROP INDEX idx_videos_status_uploaded;"

CACHE_TYPE=none mvn spring-boot:run      # in one terminal
k6 run benchmark/feed-test.js            # in another terminal
```

Save a screenshot of the k6 summary. Stop the application.

## 3. "After" run: index and cache

```bash
mvn spring-boot:run                      # Hibernate adds the index back on startup
k6 run benchmark/feed-test.js
```

If the index was not recreated, add it by hand:

```bash
docker exec -i janmanch-mysql mysql -ujanmanch -pjanmanch_local janmanch_db \
  -e "CREATE INDEX idx_videos_status_uploaded ON videos (status, uploaded_at);"
```

## 4. Record the results

Fill this table from the two k6 summaries and use it in the report.

| Metric | Before (no index, no cache) | After (index + Redis) |
|---|---|---|
| Average response time (`http_req_duration` avg) | | |
| 95th percentile (`p(95)`) | | |
| Requests per second (`http_reqs`) | | |
| Failed requests (`http_req_failed`) | | |

To show the index effect on its own, run this in MySQL before and after dropping
the index and include both outputs:

```sql
EXPLAIN SELECT * FROM videos WHERE status = 'READY' ORDER BY uploaded_at DESC LIMIT 20;
```
