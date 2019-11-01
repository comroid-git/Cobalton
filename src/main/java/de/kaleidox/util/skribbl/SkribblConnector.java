package de.kaleidox.util.skribbl;


import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SkribblConnector extends SocketConnector {
    private static final int[] AVATAR = {5, 0, 2, -1};
    private final JSONObject userData = new JSONObject();
    private static final String baseUrl = "https://skribbl.io";
    private static final String port = "5001";
    private final CompletableFuture<Object> result = new CompletableFuture<>();

    SkribblConnector() throws URISyntaxException {
        super(baseUrl + ":" + port);
        this.socket
                .on("lobbyConnected", this::onLobbyConnected)
                .on("lobbyPlayerConnected", this::onLobbyPlayerConnected);
    }

    @Override
    protected void onConnect(Object... args) {
        try {
            this.userData.put("name", "Cobalton")
                    .put("avatar", AVATAR)
                    .put("language", "German")
                    .put("createPrivate", true);
            socket.emit("userData", this.userData);
        } catch (JSONException e) {
            this.result.complete("JSONException in onConnect");
        }
    }

    private void onLobbyConnected(Object... args) {
        try {
            String id = ((JSONObject) args[0]).getString("key");
            String url = baseUrl + "/?" + id;
            HashMap<String, String> result = new HashMap<String, String>() {{
                put("id", id);
                put("url", url);
            }};
            this.result.complete(result);
        } catch (JSONException e) {
            this.result.complete("JSONException in onLobbyConnected");
        }
    }

    private void onLobbyPlayerConnected(Object... objects) {
        this.socket.disconnect();
    }

    public Object getRoom() throws ExecutionException, InterruptedException {
        return this.result.get();
    }
}