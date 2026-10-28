package com.weather.web;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WeatherController {

    private final WeatherService service;

    public WeatherController(WeatherService service) {
        this.service = service;
    }

    @GetMapping("/cities")
    public List<String> cities() {
        return service.cities();
    }

    @GetMapping("/stats")
    public List<MonthlyStat> stats(@RequestParam(name = "city", required = false) String city) {
        return service.stats(city);
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(name = "city", required = false) String city) {
        return service.summary(city);
    }

    /** Call after re-running the Hadoop job to pick up new output without restarting. */
    @PostMapping("/reload")
    public Map<String, String> reload() {
        return Map.of("source", service.reload());
    }
}