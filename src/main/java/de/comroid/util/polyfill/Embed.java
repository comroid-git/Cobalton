package de.comroid.util.polyfill;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class Embed {
    Server server;
    User user;

    public Embed(Server server, User user) {
        this.server = server;
        this.user = user;
    }

    public EmbedBuilder create() {
        return new de.comroid.util.Embed(this.server, this.user).getBuilder();
    }
}
