package de.comroid.util.starboard;

import java.io.*;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kaleidox.util.interfaces.Initializable;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

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

    public void addReaction(ReactionAddEvent event) {
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

    public void removeReaction(ReactionRemoveEvent event) {
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
}
