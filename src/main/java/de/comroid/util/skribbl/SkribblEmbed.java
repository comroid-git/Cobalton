package de.comroid.util.skribbl;

import de.comroid.util.Embed;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.util.HashMap;

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
            HashMap<?, ?> room = (HashMap<?, ?>) roomResult;
            this.embed.addField("Skribbl Room", (String) room.get("url"));
        } catch (Throwable t) {
            this.embed.addField("Error", "```" + t.getMessage() + "```");
        }
    }

//    public boolean complete(Message message) {
//        return this.result.complete(message);
//    }

}
