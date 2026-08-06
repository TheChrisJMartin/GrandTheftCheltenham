package uk.co.cheltenham.gtc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        boolean dbOk = false;
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            dbOk = true;
        } catch (Exception ignored) {}
        return Map.of(
                "status", dbOk ? "UP" : "DEGRADED",
                "app", "Grand Theft Cheltenham",
                "version", "0.12.1",
                "db", dbOk
        );
    }
}
