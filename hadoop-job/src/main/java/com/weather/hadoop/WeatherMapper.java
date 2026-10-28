package com.weather.hadoop;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Input line : date,city,temperature,rainfall,humidity   (e.g. 2022-06-14,Bengaluru,24.3,12.5,81)
 * Output     : key = "city,yyyy-MM"   value = "temp,rain,humidity,1"
 */
public class WeatherMapper extends Mapper<LongWritable, Text, Text, Text> {

    private final Text outKey = new Text();
    private final Text outVal = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context ctx) throws IOException, InterruptedException {
        String line = value.toString().trim();
        if (line.isEmpty() || line.startsWith("date")) {
            return; // skip blank lines and the header
        }
        String[] f = line.split(",");
        if (f.length < 5) {
            ctx.getCounter("Weather", "MALFORMED_ROWS").increment(1);
            return;
        }
        try {
            String month = f[0].trim().substring(0, 7);
            double temp = Double.parseDouble(f[2].trim());
            double rain = Double.parseDouble(f[3].trim());
            double hum = Double.parseDouble(f[4].trim());
            outKey.set(f[1].trim() + "," + month);
            outVal.set(temp + "," + rain + "," + hum + ",1");
            ctx.write(outKey, outVal);
        } catch (RuntimeException e) {
            ctx.getCounter("Weather", "MALFORMED_ROWS").increment(1);
        }
    }
}
