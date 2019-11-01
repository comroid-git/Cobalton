package de.kaleidox.util.skribbl;

import de.kaleidox.util.Embed;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class SkribblEmbed extends Embed {
    //    private final CompletableFuture<Object> result = new CompletableFuture<>();
    public SkribblEmbed(Server server, User user) /*throws URISyntaxException*/ {
        super(server, user);
        this.embed.setDescription("Bla bli blupp");
//        SkribblConnector connector = new SkribblConnector();
//        try {
//            Object roomResult = connector.getRoom();
//            if (roomResult instanceof Throwable) {
//                throw (Throwable) roomResult;
//            }
//            HashMap<?, ?> room = (HashMap<?, ?>) roomResult;
//            this.embed.addField("Skribbl Room", (String) room.get("url"));
//        } catch (Throwable t) {
//            //
//            this.embed.addField("Error", t.getMessage());
//        }
    }

//    public boolean complete(Message message) {
//        return this.result.complete(message);
//    }

}
