package com.weather.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/** Usage: hadoop jar weather-hadoop-job.jar com.weather.hadoop.WeatherJob <input> <output> */
public class WeatherJob {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: WeatherJob <input path> <output path>");
            System.exit(2);
        }
        Configuration conf = new Configuration();
        // Write output as CSV: city,month,avgTemp,totalRain,avgHumidity,days
        conf.set("mapreduce.output.textoutputformat.separator", ",");

        Job job = Job.getInstance(conf, "weather-data-analysis");
        job.setJarByClass(WeatherJob.class);
        job.setMapperClass(WeatherMapper.class);
        job.setCombinerClass(WeatherReducer.SumCombiner.class);
        job.setReducerClass(WeatherReducer.StatsReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
