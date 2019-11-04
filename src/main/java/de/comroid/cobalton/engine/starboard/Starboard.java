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
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

import static de.comroid.Cobalton.API;

public class Starboard implements Initializable, Closeable {
    private final ArrayList<StarMap> stars;
    private final File starboardFile;
    private final String favReaction;

    public Starboard(File starboardFile, String favReaction) throws IOException {
        if (!starboardFile.exists()) starboardFile.createNewFile();
        this.starboardFile = starboardFile;
        this.stars = new ArrayList<>();
        this.favReaction = favReaction;
        init();
    }

    @Override
    public void init() throws IOException {
        this.readData();
    }

    @Override
    public void close() throws IOException {
        this.writeData();
    }

    private void onAddReaction(ReactionAddEvent event) {
        if (event.getUser().isYourself()) {
            return;
        }
        if (!event.getServer().isPresent()) return;

        if (event.getEmoji().asUnicodeEmoji().map(this.favReaction::equals).orElse(false)) {
            // test if bot reacts to configured reaction
            event.removeReaction();
            event.getChannel().sendMessage("successfully found reaction");
        }
//        else {
        // DEBUG ONLY!
        // event.removeReaction()
//        }
    }

    private void onRemoveReaction(ReactionRemoveEvent event) {
    }

    private void onTestReaction(MessageCreateEvent event) {
        if (event.getMessage().getContent().equals("--starboard_test--")) {
            event.getMessage().addReaction("✅").join();
        } else if (event.getChannel().getId() == 639051738036568064L) {
            event.getMessage().addReaction("\uD83D\uDC40").join();
        }
    }

    private void readData() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(new FileInputStream(this.starboardFile));
        node.forEach(starNode ->
                this.stars.add(
                        new Star(((Message) starNode.get("origin")),
                                (Message) starNode.get("destination"),
                                starNode.get("stars").asInt()
                        )
                )
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

    public void attach() {
        API.addMessageCreateListener(this::onTestReaction);
        API.addReactionAddListener(this::onAddReaction);
        API.addReactionRemoveListener(this::onRemoveReaction);
    }

}
