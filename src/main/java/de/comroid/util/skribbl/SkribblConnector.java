package de.comroid.util.skribbl;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SkribblConnector extends SocketConnector {
    private static final int[] AVATAR = {5, 0, 2, -1};
    private final ObjectNode userData = JsonNodeFactory.instance.objectNode();
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
        final ArrayNode avatar = JsonNodeFactory.instance.arrayNode(AVATAR.length);
        for (int i : AVATAR) avatar.add(AVATAR[i]);

        userData.put("name", "Cobalton");
        userData.put("avatar", avatar);
        userData.put("language", "German");
        userData.put("createPrivate", true);

        socket.emit("userData", this.userData);
    }

    private void onLobbyConnected(Object... args) {
        String id = ((ObjectNode) args[0]).get("key").asText();
        String url = baseUrl + "/?" + id;
        HashMap<String, String> result = new HashMap<String, String>() {{
            put("id", id);
            put("url", url);
        }};
        this.result.complete(result);
    }

    private void onLobbyPlayerConnected(Object... objects) {
        this.socket.disconnect();
    }

    public Object getRoom() throws ExecutionException, InterruptedException {
        return this.result.get();
    }
}