package org.example;

import com.google.gson.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RouteService {

    private static final String ORS_API_KEY = "5b3ce3597851110001cf624821646cf58ed54dafba1ee71a8751d4d2";
    private static final String ORS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
    private static final String OSM_SEARCH_URL = "https://nominatim.openstreetmap.org/search";
    private static final Gson GSON = new Gson();

    public static RouteResult buildRoute(String addressA, String addressB) throws IOException, URISyntaxException {
        JsonObject locationA = getLocationByAddress(addressA);
        JsonObject locationB = getLocationByAddress(addressB);

        String coordinatesA = locationA.get("lon").getAsString() + "," + locationA.get("lat").getAsString();
        String coordinatesB = locationB.get("lon").getAsString() + "," + locationB.get("lat").getAsString();

        JsonObject segment = getRouteSegment(coordinatesA, coordinatesB);
        double distance = segment.get("distance").getAsDouble();
        double duration = segment.get("duration").getAsDouble();

        List<String> instructions = new ArrayList<>();
        for (JsonElement step : segment.getAsJsonArray("steps")) {
            String instr = step.getAsJsonObject().get("instruction").getAsString();
            instructions.add(instr);
        }

        String mapUrl = generateStaticMapUrl(locationA, locationB);

        return new RouteResult(distance, duration, instructions, mapUrl);
    }

    private static JsonObject getLocationByAddress(String address) throws IOException, URISyntaxException {
        JsonArray results = doGetRequest(OSM_SEARCH_URL, Map.of(
                "q", address,
                "format", "json"
        )).getAsJsonArray();
        if (results.size() == 0) throw new RuntimeException("Адрес не найден: " + address);
        return results.get(0).getAsJsonObject();
    }

    private static JsonObject getRouteSegment(String start, String end) throws IOException, URISyntaxException {
        JsonObject response = doGetRequest(ORS_URL, Map.of(
                "api_key", ORS_API_KEY,
                "start", start,
                "end", end
        )).getAsJsonObject();

        return response.getAsJsonArray("features")
                .get(0).getAsJsonObject()
                .getAsJsonObject("properties")
                .getAsJsonArray("segments")
                .get(0).getAsJsonObject();
    }

    private static JsonElement doGetRequest(String url, Map<String, String> queryParams) throws IOException, URISyntaxException {
        String queryString = buildQueryParams(queryParams);
        HttpURLConnection connection = (HttpURLConnection) new URI(url + queryString).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Java Telegram Bot");

        try (Scanner scanner = new Scanner(connection.getInputStream())) {
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) response.append(scanner.nextLine());
            return GSON.fromJson(response.toString(), JsonElement.class);
        }
    }

    private static String buildQueryParams(Map<String, String> queryParams) {
        if (queryParams.isEmpty()) return "";
        List<String> params = new ArrayList<>();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            params.add(entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return "?" + String.join("&", params);
    }

    private static String generateStaticMapUrl(JsonObject start, JsonObject end) {
        String lon1 = start.get("lon").getAsString();
        String lat1 = start.get("lat").getAsString();
        String lon2 = end.get("lon").getAsString();
        String lat2 = end.get("lat").getAsString();

        return String.format(
                "https://staticmap.openstreetmap.de/staticmap.php?size=600x400&markers=%s,%s,red&markers=%s,%s,blue&path=%s,%s|%s,%s",
                lat1, lon1, lat2, lon2, lat1, lon1, lat2, lon2
        );
    }

    public record RouteResult(double distance, double duration, List<String> instructions, String mapUrl) {}
}
