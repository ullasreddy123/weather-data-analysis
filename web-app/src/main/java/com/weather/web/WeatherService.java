package com.weather.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Serves monthly weather statistics.
 * 1) Prefers the CSV produced by the Hadoop MapReduce job (data/output/part-r-*).
 * 2) Falls back to computing the same aggregation in-process from data/weather.csv,
 *    so the app is always deployable even without a Hadoop cluster.
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final Path outputDir;
    private final Path dataFile;
    private volatile List<MonthlyStat> stats = List.of();
    private volatile String source = "none";

    public WeatherService(@Value("${weather.output.path}") String outputPath,
                          @Value("${weather.data.path}") String dataPath) {
        this.outputDir = Path.of(outputPath);
        this.dataFile = Path.of(dataPath);
        reload();
    }

    public synchronized String reload() {
        try {
            List<MonthlyStat> fromHadoop = loadHadoopOutput();
            if (!fromHadoop.isEmpty()) {
                stats = fromHadoop;
                source = "Hadoop MapReduce output";
            } else {
                stats = aggregateRawCsv();
                source = "In-process Java aggregation (no Hadoop output found)";
            }
        } catch (IOException e) {
            log.error("Could not load weather data", e);
            stats = List.of();
            source = "error: " + e.getMessage();
        }
        log.info("Loaded {} monthly rows from: {}", stats.size(), source);
        return source;
    }

    public String getSource() {
        return source;
    }

    public List<String> cities() {
        return stats.stream().map(MonthlyStat::city).distinct().sorted().toList();
    }

    public List<MonthlyStat> stats(String city) {
        return stats.stream()
                .filter(s -> city == null || city.isBlank() || s.city().equalsIgnoreCase(city))
                .sorted(Comparator.comparing(MonthlyStat::city).thenComparing(MonthlyStat::month))
                .toList();
    }

    public Map<String, Object> summary(String city) {
        List<MonthlyStat> list = stats(city);
        boolean all = city == null || city.isBlank();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("source", source);
        m.put("city", all ? "All cities" : city);
        m.put("months", list.size());
        if (list.isEmpty()) {
            return m;
        }
        MonthlyStat hot = list.stream().max(Comparator.comparingDouble(MonthlyStat::avgTemp)).get();
        MonthlyStat cold = list.stream().min(Comparator.comparingDouble(MonthlyStat::avgTemp)).get();
        MonthlyStat wet = list.stream().max(Comparator.comparingDouble(MonthlyStat::totalRain)).get();
        double totalDays = list.stream().mapToInt(MonthlyStat::days).sum();
        m.put("avgTemp", round(list.stream().mapToDouble(s -> s.avgTemp() * s.days()).sum() / totalDays));
        m.put("avgHumidity", round(list.stream().mapToDouble(s -> s.avgHumidity() * s.days()).sum() / totalDays));
        m.put("totalRain", round(list.stream().mapToDouble(MonthlyStat::totalRain).sum()));
        m.put("hottestMonth", hot.month() + (all ? " (" + hot.city() + ")" : ""));
        m.put("hottestTemp", hot.avgTemp());
        m.put("coldestMonth", cold.month() + (all ? " (" + cold.city() + ")" : ""));
        m.put("coldestTemp", cold.avgTemp());
        m.put("wettestMonth", wet.month() + (all ? " (" + wet.city() + ")" : ""));
        m.put("wettestRain", wet.totalRain());
        return m;
    }

    // ---------- loaders ----------

    private List<MonthlyStat> loadHadoopOutput() throws IOException {
        List<MonthlyStat> result = new ArrayList<>();
        if (!Files.isDirectory(outputDir)) {
            return result;
        }
        List<Path> parts;
        try (Stream<Path> files = Files.list(outputDir)) {
            parts = files.filter(f -> f.getFileName().toString().startsWith("part-")).toList();
        }
        for (Path p : parts) {
            for (String line : Files.readAllLines(p)) {
                String[] f = line.trim().split(",");
                if (f.length == 6) {
                    result.add(new MonthlyStat(f[0], f[1], Double.parseDouble(f[2]),
                            Double.parseDouble(f[3]), Double.parseDouble(f[4]), Integer.parseInt(f[5])));
                }
            }
        }
        return result;
    }

    private List<MonthlyStat> aggregateRawCsv() throws IOException {
        List<MonthlyStat> result = new ArrayList<>();
        if (!Files.isRegularFile(dataFile)) {
            return result;
        }
        Map<String, double[]> acc = new TreeMap<>(); // key "city,month" -> [sumT, sumR, sumH, n]
        for (String line : Files.readAllLines(dataFile)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("date")) {
                continue;
            }
            String[] f = line.split(",");
            if (f.length < 5) {
                continue;
            }
            try {
                double t = Double.parseDouble(f[2].trim());
                double r = Double.parseDouble(f[3].trim());
                double h = Double.parseDouble(f[4].trim());
                double[] a = acc.computeIfAbsent(f[1].trim() + "," + f[0].trim().substring(0, 7), k -> new double[4]);
                a[0] += t;
                a[1] += r;
                a[2] += h;
                a[3] += 1;
            } catch (RuntimeException ignored) {
                // skip malformed rows, like the MapReduce job does
            }
        }
        acc.forEach((k, a) -> {
            String[] ck = k.split(",");
            result.add(new MonthlyStat(ck[0], ck[1], round(a[0] / a[3]), round(a[1]), round(a[2] / a[3]), (int) a[3]));
        });
        return result;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
