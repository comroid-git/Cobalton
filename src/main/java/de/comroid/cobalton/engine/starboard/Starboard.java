package de.comroid.cobalton.engine.starboard;

import java.io.File;
import java.io.IOException;

import de.comroid.cobalton.model.Embed;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;
import org.javacord.api.util.logging.ExceptionLogger;

public class Starboard implements ReactionAddListener, ReactionRemoveListener {
    private final StarMap stars;
    private final String favReaction;
    private final ServerTextChannel starChannel;
    private final DiscordApi api;

    public Starboard(DiscordApi api, File starboardFile, String favReaction, long starChannel) throws IOException {
        if (!starboardFile.exists()) starboardFile.createNewFile();
        this.stars = new StarMap(api, starboardFile);
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
        this.starChannel
                .getMessageById(star.getDestination().id)
                .thenAccept(destination ->
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
                )
                .thenAccept((Void v) -> this.stars.put(star))
                .exceptionally(ExceptionLogger.get());
    }

    private <T extends ReactionEvent> boolean isStarboardChannel(T event) {
        if (event instanceof ReactionAddEvent) {
            return ((ReactionAddEvent) event).getEmoji().asUnicodeEmoji().map(Starboard.this.favReaction::equals).orElse(false);
        } else if (event instanceof ReactionRemoveEvent) {
            return ((ReactionRemoveEvent) event).getEmoji().asUnicodeEmoji().map(Starboard.this.favReaction::equals).orElse(false);
        }
        return false;
    }

    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        if (event.getUser().isYourself() ||
                !event.getServer().isPresent()
        ) return;
        if (this.isStarboardChannel(event)) {
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
                    event.getChannel()
                            .sendMessage(this.getBuilder(event))
                            .thenAccept(destination -> this.stars.put(event.getMessage().get(), destination))
                            .exceptionally(ExceptionLogger.get());
                }
            }
        }
    }

    @Override
    public void onReactionRemove(ReactionRemoveEvent event) {
        if (event.getUser().isYourself() ||
                !event.getServer().isPresent()
        ) return;

        if (this.isStarboardChannel(event)) {
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
