package de.kaleidox.util.eval;

import de.kaleidox.JamesBot;
import de.kaleidox.javacord.util.ui.embed.DefaultEmbedFactory;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class EvalEmbed {
    private EmbedBuilder embed;

    public EvalEmbed(Server server, User user) {
        this.embed = DefaultEmbedFactory.create()
                .setAuthor(user)
                .setUrl("http://kaleidox.de:8111")
                .setFooter("Evaluated by " + user.getDiscriminatedName())
                .setColor(user.getRoleColor(server).orElse(JamesBot.THEME));
    }

    public EvalEmbed addField(String name, String value) {
        this.embed.addField(name, value);
        return this;
    }

    public EvalEmbed addField(String name, String value, boolean inline) {
        this.embed.addField(name, value, inline);
        return this;
    }

    public EmbedBuilder getBuilder() {
        return this.embed;
    }
}
