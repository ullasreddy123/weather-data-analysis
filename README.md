# Weather Data Analysis (Java + Hadoop + Web Dashboard)

Analyses temperature, rainfall and humidity trends from historical weather data.

```
weather-data-analysis/
├── hadoop-job/        Hadoop MapReduce job (Java 8-compatible)
├── web-app/           Spring Boot REST API + dashboard (Chart.js)
├── data/weather.csv   Sample data: 5 Indian cities, 2019–2023, daily
├── scripts/run-hadoop-job.sh
├── Dockerfile / docker-compose.yml
└── pom.xml            Maven parent
```

## How it works
1. `weather.csv` (`date,city,temperature,rainfall,humidity`) is the input.
2. **MapReduce job**: Mapper emits `city,yyyy-MM` → readings, Combiner sums partials, Reducer writes
   `city,month,avgTemp,totalRain,avgHumidity,days` to `data/output/part-r-00000`.
3. **Web app** reads that output and serves it at `/api/*` with a dashboard at `/`.
   If no Hadoop output exists, it computes the same aggregation in plain Java, so it deploys anywhere.

## Run locally (needs JDK 17 + Maven)
```bash
mvn -DskipTests package
java -jar web-app/target/web-app.jar        # run from the project root
# open http://localhost:8080
```

## Run the Hadoop job
```bash
./scripts/run-hadoop-job.sh
```
Uses your local `hadoop` if installed, otherwise Docker (`apache/hadoop:3.3.6`). Then click **Reload data**
in the dashboard (or `POST /api/reload`). The header line "Data source" will say *Hadoop MapReduce output*.

On a real cluster:
```bash
hdfs dfs -mkdir -p /weather/input && hdfs dfs -put data/weather.csv /weather/input/
hadoop jar hadoop-job/target/weather-hadoop-job.jar com.weather.hadoop.WeatherJob /weather/input /weather/output
hdfs dfs -get /weather/output/part-r-* data/output/
```

## Deploy
**Docker (any VPS / cloud):**
```bash
docker compose up -d --build        # http://localhost:8080
```
**Render / Railway / Fly.io / Google Cloud Run:** point to this repo, choose the Dockerfile. The app reads `PORT`.

Environment variables: `PORT`, `WEATHER_OUTPUT_PATH` (default `data/output`), `WEATHER_DATA_PATH` (default `data/weather.csv`).

## API
| Endpoint | Description |
|---|---|
| `GET /api/cities` | list of cities |
| `GET /api/stats?city=Bengaluru` | monthly stats (all cities if omitted) |
| `GET /api/summary?city=Bengaluru` | averages, hottest/coldest/wettest month |
| `POST /api/reload` | re-read Hadoop output / CSV |

## Using your own dataset
Replace `data/weather.csv` with rows in the same format (header optional), re-run the job, reload.
