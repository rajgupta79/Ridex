package com.ridex.tracking;

import com.ridex.tracking.dto.LocationMessage;
import com.ridex.tracking.dto.TrackingBroadcast;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manual latency harness (not a unit test — run against a LIVE server on :8080
 * with Mongo up). It:
 *   1. logs in rider + driver over REST, requests a ride, has the driver accept it
 *   2. opens two STOMP sessions (rider subscribes, driver publishes)
 *   3. sends N location pings and records receive-time minus send-time
 *   4. prints min / avg / p95 / max latency
 *
 * Run with:  mvn -q exec or as a plain main(). Kept out of the normal test run
 * because it needs a running server.
 */
public class TrackingLatencyManualTest {

    private static final String BASE = "http://localhost:8080";
    private static final String WS = "ws://localhost:8080/ws";
    private static final int PINGS = 50;

    public static void main(String[] args) throws Exception {
        HttpClient http = HttpClient.newHttpClient();

        String riderToken = login(http, "rider1@test.com", "password123");
        String driverToken = login(http, "driver1@test.com", "password123");
        String driverId = subjectUserId(http, driverToken);

        // Make sure the driver is online and request+accept a ride so tracking is authorized.
        putLocation(http, driverToken, 77.60, 12.97);
        String rideId = requestRide(http, riderToken);
        acceptRide(http, driverToken, rideId);
        System.out.println("Ride " + rideId + " accepted by driver " + driverId);

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());

        List<Long> latencies = new CopyOnWriteArrayList<>();

        // Rider subscribes.
        StompSession riderSession = connect(client, riderToken);
        riderSession.subscribe("/topic/ride/" + rideId, new StompSessionHandlerAdapter() {
            @Override public Type getPayloadType(StompHeaders headers) { return TrackingBroadcast.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                TrackingBroadcast b = (TrackingBroadcast) payload;
                long now = System.currentTimeMillis();
                latencies.add(now - b.clientSentAt());
            }
        });

        // Driver publishes.
        StompSession driverSession = connect(client, driverToken);
        for (int i = 0; i < PINGS; i++) {
            LocationMessage msg = new LocationMessage(
                    77.60 + i * 0.0001, 12.97 + i * 0.0001, System.currentTimeMillis());
            driverSession.send("/app/ride/" + rideId + "/location", msg);
            Thread.sleep(100);
        }

        Thread.sleep(1000); // allow stragglers
        report(latencies);

        driverSession.disconnect();
        riderSession.disconnect();
        client.stop();
        System.exit(0);
    }

    private static void report(List<Long> l) {
        if (l.isEmpty()) { System.out.println("No messages received!"); return; }
        l.sort(Long::compareTo);
        long min = l.get(0);
        long max = l.get(l.size() - 1);
        double avg = l.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95 = l.get((int) Math.min(l.size() - 1, Math.round(l.size() * 0.95) - 1));
        System.out.println("===== WebSocket tracking latency (" + l.size() + " msgs) =====");
        System.out.printf("min=%d ms  avg=%.1f ms  p95=%d ms  max=%d ms%n", min, avg, p95, max);
    }

    private static StompSession connect(WebSocketStompClient client, String token) throws Exception {
        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.add("Authorization", "Bearer " + token);
        WebSocketHttpHeaders handshake = new WebSocketHttpHeaders();
        return client.connectAsync(WS, handshake, stompHeaders,
                new StompSessionHandlerAdapter() {}).get();
    }

    // --- tiny REST helpers ---

    private static String login(HttpClient http, String email, String pw) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + pw + "\"}";
        HttpResponse<String> r = post(http, "/api/auth/login", body, null);
        return extract(r.body(), "token");
    }

    private static String subjectUserId(HttpClient http, String token) throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/api/auth/me"))
                .header("Authorization", "Bearer " + token).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        return extract(r.body(), "id");
    }

    private static void putLocation(HttpClient http, String token, double lng, double lat) throws Exception {
        String body = "{\"longitude\":" + lng + ",\"latitude\":" + lat + "}";
        http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/api/drivers/me/location"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String requestRide(HttpClient http, String token) throws Exception {
        String body = "{\"pickupLng\":77.5946,\"pickupLat\":12.9716,\"dropoffLng\":77.64,\"dropoffLat\":12.95}";
        HttpResponse<String> r = post(http, "/api/rides", body, token);
        return extract(r.body(), "id");
    }

    private static void acceptRide(HttpClient http, String token, String rideId) throws Exception {
        post(http, "/api/rides/" + rideId + "/accept", "", token);
    }

    private static HttpResponse<String> post(HttpClient http, String path, String body, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Crude JSON field extractor to avoid pulling a JSON lib into the harness. */
    private static String extract(String json, String field) {
        String key = "\"" + field + "\":\"";
        int i = json.indexOf(key);
        if (i < 0) return null;
        int start = i + key.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
