package de.comroid.cobalton.engine.starboard;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import de.comroid.cobalton.model.Embed;
import de.kaleidox.util.interfaces.Initializable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;

public class Starboard implements Initializable, Closeable, ReactionAddListener, ReactionRemoveListener {
    private final HashMap<Long, Star> stars;
    private final File starboardFile;
    private final String favReaction;
    private final ServerTextChannel starChannel;

    public Starboard(DiscordApi api, File starboardFile, String favReaction, long starChannel) throws IOException {
        if (!starboardFile.exists()) starboardFile.createNewFile();
        this.starboardFile = starboardFile;
        this.stars = new HashMap<>();
        this.favReaction = favReaction;
        this.starChannel = api.getServerTextChannelById(starChannel).get();
        init();

        api.addListener(this);
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
    public void onReactionAdd(ReactionAddEvent event) {
        if (event.getUser().isYourself() ||
                !event.getServer().isPresent()
        ) return;


        if (event.getEmoji().asUnicodeEmoji().map(this.favReaction::equals).orElse(false)) {
            // check if event channel is starboard
            if (event.getChannel().getId() == this.starChannel.getId()) {
                final long id = event.getMessageId();
                Star star = this.stars.get(id);
                if (star != null) {
                    // message was already starred
                    this.stars.replace(id, star.addStar());
                    this.starChannel.getMessageById(star.getDestination().getId()).thenAccept(starEmbed -> starEmbed.edit(
                            starEmbed.getEmbeds()
                                    .get(0)
                                    .toBuilder()
                                    .updateFields((embedField) -> embedField.getName().equals("Score"),
                                            editableEmbedField -> editableEmbedField.setValue(String.format("```%d Stars```", star.getCount())))
                    ).join());
                } else {
                    // Message was not yet starred
                    Message destination = this.starChannel.sendMessage(
                            new Embed(event.getServer().get(), event.getUser())
                                    .addField("Message", event.getMessage().get().getContent())
                                    .addField("Score", String.valueOf(this.stars.get(id).getCount()))
                                    .getBuilder()
                    ).join();
                    this.stars.put(id, new Star(event.getMessage().get(), destination, 1));
                }
            }
        }
    }

    @Override
    public void onReactionRemove(ReactionRemoveEvent event) {
        if (event.getUser().isYourself() ||
                !event.getServer().isPresent()
        ) return;


        if (event.getEmoji().asUnicodeEmoji().map(this.favReaction::equals).orElse(false)) {
            // check if event channel is starboard
            if (event.getChannel().getId() == this.starChannel.getId()) {
                final long id = event.getMessageId();
                Star star = this.stars.get(id);
                if (star != null) {
                    if (star.removeStar().getCount() == 0) {
                        this.stars.remove(id, star);
                        this.starChannel.deleteMessages(star.getDestination()).join();
                    } else {
                        this.starChannel.getMessageById(star.getDestination().getId()).thenAccept(starEmbed -> {
                            star.removeStar();
                            this.stars.replace(id, star);
                            starEmbed.edit(
                                    starEmbed.getEmbeds()
                                            .get(0)
                                            .toBuilder()
                                            .updateFields((embedField) -> embedField.getName().equals("Score"),
                                                    editableEmbedField -> editableEmbedField.setValue(String.format("```%d Stars```", star.getCount())))
                            ).join();
                        }).join();
                    }
                }
            }
        }
    }

    private void readData() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(this.starboardFile);

        if (node == null) return; // nothing to serialize

        node.forEach((starNode) -> {
                    if (starNode.isNull()) return; // must skip node
                    final Message origin = ((Message) starNode.get("origin"));
                    this.stars.put(origin.getId(),
                            new Star(origin,
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
        stream.write(mapper.writeValueAsString(this.stars.values()).getBytes());
        stream.close();
    }
}
