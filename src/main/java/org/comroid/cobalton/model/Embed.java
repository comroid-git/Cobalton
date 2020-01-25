package org.comroid.cobalton.model;

import org.comroid.cobalton.Bot;
import de.comroid.javacord.util.ui.embed.DefaultEmbedFactory;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class Embed {
    protected EmbedBuilder embed;

    public Embed(Server server, User user) {
        this.embed = DefaultEmbedFactory.create()
                .setAuthor(user)
                .setColor(user.getRoleColor(server).orElse(Bot.THEME));
    }

    public Embed addField(String name, String value) {
        this.embed.addField(name, value);
        return this;
    }

    public Embed addField(String name, String value, boolean inline) {
        this.embed.addField(name, value, inline);
        return this;
    }

    public EmbedBuilder getBuilder() {
        return this.embed;
    }
}
