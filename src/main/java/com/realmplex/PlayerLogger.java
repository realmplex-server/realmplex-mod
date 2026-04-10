package com.realmplex;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PlayerLogger {
    public static HttpClient playerLogClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    public static void logJoin(String username) {
        HttpRequest joinRequest = HttpRequest.newBuilder()
            .uri(URI.create(RealmplexMod.CONFIG.apiUrl + "/players/" + username))
            .PUT(HttpRequest.BodyPublishers.noBody())
            .setHeader("x-api-key", RealmplexMod.CONFIG.apiKey)
            .build();

        playerLogClient.sendAsync(joinRequest, HttpResponse.BodyHandlers.ofString());
    }

    public static void logLeave(String username) {
        HttpRequest leaveRequest = HttpRequest.newBuilder()
            .uri(URI.create(RealmplexMod.CONFIG.apiUrl + "/players/" + username))
            .DELETE()
            .setHeader("x-api-key", RealmplexMod.CONFIG.apiKey)
            .build();

        playerLogClient.sendAsync(leaveRequest, HttpResponse.BodyHandlers.ofString());
    }

    public static void logStartup() {
        HttpRequest startRequest = HttpRequest.newBuilder()
            .uri(URI.create(RealmplexMod.CONFIG.apiUrl + "/health/startup"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .setHeader("x-api-key", RealmplexMod.CONFIG.apiKey)
            .build();

        playerLogClient.sendAsync(startRequest, HttpResponse.BodyHandlers.ofString());
    }

    public static void logShutdown() {
        HttpRequest stopRequest = HttpRequest.newBuilder()
            .uri(URI.create(RealmplexMod.CONFIG.apiUrl + "/health/shutdown"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .setHeader("x-api-key", RealmplexMod.CONFIG.apiKey)
            .build();

        playerLogClient.sendAsync(stopRequest, HttpResponse.BodyHandlers.ofString());
    }

}
