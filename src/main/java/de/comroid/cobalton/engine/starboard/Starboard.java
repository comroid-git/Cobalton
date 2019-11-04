package de.comroid.cobalton.engine.starboard;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import de.kaleidox.util.interfaces.Initializable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;

public class Starboard implements Initializable, Closeable, ReactionAddListener, ReactionRemoveListener, MessageCreateListener {
    private final ArrayList<StarMap> stars;
    private final File starboardFile;
    private final String favReaction;

    public Starboard(DiscordApi api, File starboardFile, String favReaction) throws IOException {
        if (!starboardFile.exists()) starboardFile.createNewFile();
        this.starboardFile = starboardFile;
        this.stars = new ArrayList<>();
        this.favReaction = favReaction;
        init();
        
        api.addReactionAddListener(this);
        api.addReactionRemoveListener(this);
    }

    @Override
    public void init() throws IOException {
        this.readData();
    }

    @Override
    public void close() throws IOException {
        this.writeData();
    }

    @Override 
    public void onMessageCreate(MessageCreateEvent event) {
        System.out.println("event = " + event);
        System.out.println("event.getMessage() = " + event.getMessage());

        if (event.getMessage().getContent().equals("gumo test")) {
            event.getMessage().addReaction(favReaction);
        }
    }

    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        if (event.getUser().isYourself()) {
            return;
        }
        if (!event.getServer().isPresent()) return;

        if (event.getEmoji().asUnicodeEmoji().map(this.favReaction::equals).orElse(false)) {
            // test if bot reacts to configured reaction
            event.removeReaction();
            event.getChannel().sendMessage("successfully found reaction");
        } else if (event.getChannel().getId() == 639051738036568064L) {
            event.getMessage().ifPresent(message ->
                    message.addReaction("\uD83D\uDC40").join()
            );
        }
//        else {
        // DEBUG ONLY!
        // event.removeReaction()
//        }
    }

    @Override
    public void onReactionRemove(ReactionRemoveEvent event) {
    }

    private void readData() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(this.starboardFile);
        
        if (node == null) return; // nothing to serialize
        
        node.forEach(starNode -> {
                    if (starNode.isNull()) return; // must skip node

                    this.stars.add(
                            new Star(((Message) starNode.get("origin")),
                                    (Message) starNode.get("destination"),
                                    starNode.get("stars").asInt()
                            )
                    );
                }
        );
    }

    private void writeData() throws IOException {
        if (this.starboardFile.exists()) this.starboardFile.delete();
        this.starboardFile.createNewFile();
        FileOutputStream stream = new FileOutputStream(this.starboardFile);
        final ObjectMapper mapper = new ObjectMapper();
        stream.write(mapper.writeValueAsString(this.stars).getBytes());
        stream.close();
    }
}
