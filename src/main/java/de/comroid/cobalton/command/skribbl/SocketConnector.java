package de.comroid.cobalton.command.skribbl;

import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;

abstract class SocketConnector {
    Socket socket;

    SocketConnector(String host) throws URISyntaxException {
        this.socket = IO.socket(host).connect();
        this.socket.on("connect", this::onConnect);
    }

    protected abstract void onConnect(Object... args);

    public SocketConnector on(String event, Runnable callback) {
        this.socket.on(event, (Object... args) -> callback.run());
        return this;
    }
}