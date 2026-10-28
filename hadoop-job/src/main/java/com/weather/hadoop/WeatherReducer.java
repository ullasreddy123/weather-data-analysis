package com.weather.hadoop;

import java.io.IOException;
import java.util.Locale;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/** Aggregates monthly weather per city. */
public class WeatherReducer {

    /** Partial sums: "sumTemp,sumRain,sumHum,count" -> same format (safe to run 0..n times). */
    public static class SumCombiner extends Reducer<Text, Text, Text, Text> {
        private final Text out = new Text();

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context ctx) throws IOException, InterruptedException {
            double[] s = sum(values);
            out.set(s[0] + "," + s[1] + "," + s[2] + "," + (long) s[3]);
            ctx.write(key, out);
        }
    }

    /** Final output line: city,month,avgTemp,totalRain,avgHumidity,days */
    public static class StatsReducer extends Reducer<Text, Text, Text, Text> {
        private final Text out = new Text();

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context ctx) throws IOException, InterruptedException {
            double[] s = sum(values);
            long n = (long) s[3];
            if (n == 0) {
                return;
            }
            out.set(String.format(Locale.US, "%.2f,%.2f,%.2f,%d", s[0] / n, s[1], s[2] / n, n));
            ctx.write(key, out);
        }
    }

    static double[] sum(Iterable<Text> values) {
        double t = 0, r = 0, h = 0, n = 0;
        for (Text v : values) {
            String[] p = v.toString().split(",");
            t += Double.parseDouble(p[0]);
            r += Double.parseDouble(p[1]);
            h += Double.parseDouble(p[2]);
            n += Double.parseDouble(p[3]);
        }
        return new double[] {t, r, h, n};
    }
}
