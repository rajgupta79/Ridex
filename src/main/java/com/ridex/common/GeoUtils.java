package com.ridex.common;

/**
 * Small geospatial helpers. MongoDB does the heavy lifting for nearby queries,
 * but we still need distance math for fares, pooling detours, and tests.
 */
public final class GeoUtils {

    private GeoUtils() {}

    /** Mean radius of the Earth in kilometers. */
    public static final double EARTH_RADIUS_KM = 6371.0088;

    /**
     * Great-circle distance between two lon/lat points using the haversine formula.
     * Note the argument order: longitude first, then latitude, to match GeoJSON
     * ([lng, lat]) which is what MongoDB uses.
     *
     * @return distance in kilometers
     */
    public static double haversineKm(double lon1, double lat1, double lon2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /** Convert kilometers to radians, used for legacy $centerSphere-style queries. */
    public static double kmToRadians(double km) {
        return km / EARTH_RADIUS_KM;
    }

    public static boolean isValidLongitude(double lon) {
        return lon >= -180.0 && lon <= 180.0;
    }

    public static boolean isValidLatitude(double lat) {
        return lat >= -90.0 && lat <= 90.0;
    }
}
