package uk.co.cheltenham.gtc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return jdbc.queryForList(
                    "SELECT id, display_name, year_from, year_to, body_style, drivetrain, " +
                    "mass_kg, power_hp, top_speed_kmh, accel_0_100_s, colour, category, " +
                    "subcategory, tags, is_police, physics_profile, damage_profile " +
                    "FROM vehicle_models WHERE category = ? ORDER BY display_name",
                    category);
        }
        return jdbc.queryForList(
                "SELECT id, display_name, year_from, year_to, body_style, drivetrain, " +
                "mass_kg, power_hp, top_speed_kmh, accel_0_100_s, colour, category, " +
                "subcategory, tags, is_police, physics_profile, damage_profile " +
                "FROM vehicle_models ORDER BY category, display_name");
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable String id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM vehicle_models WHERE id = ?", id);
        if (rows.isEmpty()) {
            return Map.of("error", "not found");
        }
        return rows.get(0);
    }

    @GetMapping("/catalogue/summary")
    public Map<String, Object> summary() {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM vehicle_models", Long.class);
        List<Map<String, Object>> byCat = jdbc.queryForList(
                "SELECT category, COUNT(*) AS cnt FROM vehicle_models GROUP BY category ORDER BY category");
        return Map.of(
                "total", total != null ? total : 0,
                "byCategory", byCat,
                "feature", "F16-0015 + F16-0018 skeleton"
        );
    }
}
