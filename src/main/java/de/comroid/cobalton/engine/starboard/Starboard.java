package de.comroid.cobalton.engine.starboard;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.comroid.cobalton.model.Embed;
import de.kaleidox.util.interfaces.Initializable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;

public class Starboard implements ReactionAddListener, ReactionRemoveListener {
    private final StarMap stars;
    private final String favReaction;
    private final ServerTextChannel starChannel;
    private final DiscordApi api;

    public Starboard(DiscordApi api, File starboardFile, String favReaction, long starChannel) throws IOException {
        if (!starboardFile.exists()) starboardFile.createNewFile();
        this.stars = new StarMap(starboardFile);
        this.favReaction = favReaction;
        this.starChannel = api.getServerTextChannelById(starChannel).get();
        this.api = api;
        api.addListener(this);
    }

    private EmbedBuilder getBuilder(ReactionAddEvent event) {
        return new Embed(event.getServer().get(),
                event.getMessage().get().getUserAuthor().get()
        )
                .addField("Score", String.format("```1 %s```", this.favReaction))
                .getBuilder()
                .setDescription(event.getMessage().get().getContent());
    }

    private void updateEmbed(Star star) {
        this.starChannel.getMessageById(star.getDestination().id).thenAccept(destination ->
                destination.edit(
                        destination.getEmbeds()
                                .get(0)
                                .toBuilder()
                                .updateFields((embedField) -> embedField.getName().equals("Score"),
                                        editableEmbedField -> editableEmbedField.setValue(
                                                String.format("```%d %s```", star.getScore(), this.favReaction)
                                        )
                                )
                )
        ).thenAccept((Void v) -> this.stars.put(star)).join();
    }

    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        if (event.getUser().isYourself() ||
                !event.getServer().isPresent()
        ) return;
        if (event.getEmoji().asUnicodeEmoji().map(this.favReaction::equals).orElse(false)) {
            // check if event channel is starboard
            /* TODO: flip expression once feature works,
                since we want to check that reaction does not happen
                on starred messages in starboard channel
             */
            if (event.getChannel().getId() == this.starChannel.getId()) {
                final Star star = this.stars.get(event.getMessageId());
                if (star != null) {
                    star.incScore();
                    this.updateEmbed(star);
                } else {
                    // Message was not yet starred
                    event.getChannel().sendMessage(this.getBuilder(event)).thenAccept(destination -> this.stars.put(event.getMessage().get(), destination)).join();
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
            /* TODO: flip expression once feature works,
                since we want to check that reaction does not happen
                on starred messages in starboard channel
             */
            if (event.getChannel().getId() == this.starChannel.getId()) {
                final Star star = this.stars.get(event.getMessageId());
                if (star != null) {
                    star.decScore();
                    this.updateEmbed(star);
                }
            }
        }
    }
}
