package de.comroid.cobalton.command.skribbl;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

abstract class SocketConnector {
    Socket socket;

    SocketConnector(String host) throws URISyntaxException {
        this.socket = IO.socket(host).connect();
        this.socket.on("connect", this::onConnect);
    }

    public SocketConnector on(String event, Runnable callback) {
        this.socket.on(event, (Object... args) -> callback.run());
        return this;
    }

    protected abstract void onConnect(Object... args);
}