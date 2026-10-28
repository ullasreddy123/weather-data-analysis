package com.weather.web;

public record MonthlyStat(String city, String month, double avgTemp, double totalRain, double avgHumidity, int days) {}
