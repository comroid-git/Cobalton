package de.comroid.cobalton.command.eval;

import de.comroid.cobalton.model.Embed;

import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class EvalEmbed extends Embed {

    public EvalEmbed(Server server, User user) {
        super(server, user);
        this.embed
                .setUrl("http://kaleidox.de:8111")
                .setFooter("Evaluated by " + user.getDiscriminatedName());
    }
}
