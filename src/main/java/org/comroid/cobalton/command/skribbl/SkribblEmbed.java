package org.comroid.cobalton.command.skribbl;

import org.comroid.cobalton.model.Embed;

import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class SkribblEmbed extends Embed {
    //    private final CompletableFuture<Object> result = new CompletableFuture<>();
    public SkribblEmbed(Server server, User user) /*throws URISyntaxException*/ {
        super(server, user);
        this.embed.setDescription("Lass uns skribbl spielen!");
        try {
            SkribblConnector connector = new SkribblConnector();
            Object roomResult = connector.getRoom();
            if (roomResult instanceof Throwable) {
                throw (Throwable) roomResult;
            }
            this.embed.addField("Skribbl Room", (String) roomResult);
        } catch (Throwable t) {
            this.embed.addField("Error", "```" + t.getMessage() + "```");
        }
    }

//    public boolean complete(Message message) {
//        return this.result.complete(message);
//    }

}
