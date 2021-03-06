package org.comroid.cobalton.command.skribbl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SkribblConnector extends SocketConnector {
    private static final int[] AVATAR = {5, 0, 2, -1};
    private static final String baseUrl = "https://skribbl.io";
    private static final String port = "5001";
    private final CompletableFuture<Object> result = new CompletableFuture<>();

    public Object getRoom() throws ExecutionException, InterruptedException {
        return this.result.get();
    }

    SkribblConnector() throws URISyntaxException {
        super(baseUrl + ":" + port);
        this.socket
                .on("lobbyConnected", this::onLobbyConnected)
                .on("lobbyPlayerConnected", this::onLobbyPlayerConnected);
    }

    @Override
    protected void onConnect(Object... args) {
        final HashMap userData = new HashMap<String, Object>() {
            {
                put("name", "Cobalton");
                put("avatar", AVATAR);
                put("language", "German");
                put("createPrivate", true);
            }
        };
        socket.emit("userData", userData);
    }

    private void onLobbyConnected(Object... args) {
        try {
            final JsonNode data = new ObjectMapper().readTree(args[0].toString());
            final String id = data.get("key").asText();
            this.result.complete(baseUrl + "/?" + id);
        } catch (Throwable t) {
            this.result.complete(new Throwable(t.getMessage() + " onLobbyConnected"));
        }
    }

    private void onLobbyPlayerConnected(Object... objects) {
        this.socket.disconnect();
    }
}