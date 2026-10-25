package com.ridex.driver.controller;

import com.ridex.driver.dto.BenchmarkResult;
import com.ridex.driver.service.DriverSeeder;
import com.ridex.driver.service.GeoBenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only tooling to seed fake drivers and benchmark the geospatial query.
 * Kept out of the normal driver API because it is a dev/benchmarking concern.
 */
@RestController
@RequestMapping("/api/admin/drivers")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Driver Benchmark", description = "Seed fake drivers and benchmark the 2dsphere query (ADMIN only)")
public class DriverBenchmarkController {

    private final DriverSeeder seeder;
    private final GeoBenchmarkService benchmark;

    public DriverBenchmarkController(DriverSeeder seeder, GeoBenchmarkService benchmark) {
        this.seeder = seeder;
        this.benchmark = benchmark;
    }

    @PostMapping("/seed")
    @Operation(summary = "Seed N fake AVAILABLE drivers around a center point")
    public Map<String, Object> seed(
            @RequestParam(defaultValue = "50000") int count,
            @RequestParam(defaultValue = "77.5946") double centerLng,
            @RequestParam(defaultValue = "12.9716") double centerLat,
            @RequestParam(defaultValue = "0.5") double spanDeg) {
        long inserted = seeder.seed(count, centerLng, centerLat, spanDeg);
        return Map.of("inserted", inserted);
    }

    @DeleteMapping("/seed")
    @Operation(summary = "Delete all synthetic seed drivers")
    public Map<String, Object> clearSeed() {
        long removed = seeder.clearSeedData();
        return Map.of("removed", removed);
    }

    @PostMapping("/benchmark")
    @Operation(summary = "Time the nearest-driver query with vs. without the 2dsphere index")
    public BenchmarkResult benchmark(
            @RequestParam(defaultValue = "77.5946") double longitude,
            @RequestParam(defaultValue = "12.9716") double latitude,
            @RequestParam(defaultValue = "5.0") double radiusKm,
            @RequestParam(defaultValue = "5") int runs) {
        return benchmark.run(longitude, latitude, radiusKm, runs);
    }
}
