package org.comroid.cobalton.engine;

import org.comroid.cobalton.Bot;
import org.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WordStoryEngine implements MessageCreateListener {
    private final ServerTextChannel stc;
    private final boolean autoConclude = true;

    public WordStoryEngine(ServerTextChannel stc) {
        this.stc = stc;

        stc.addTextChannelAttachableListener(this);
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        if (!event.getMessage().getAuthor().isUser())
            return;

        final String content = event.getReadableMessageContent();

        if (isStoryPart(content) && (content.contains(".") || content.contains("?") || content.contains("!"))) {
            if (event.getMessageAuthor()
                    .asUser()
                    .map(User::isBot)
                    .orElse(true))
                return;

            concludeStory().thenCompose(msg -> msg.edit(generateTitle(msg
                    .getEmbeds()
                    .get(0)
                    .getDescription()
                    .orElse("no title :("))))
                    .join();
        } else if (!isStoryPart(content)) event.addReactionToMessage("⚠️");
    }

    private String generateTitle(String story) {
        final Random rng = new Random();
        final List<String> words = Stream.of(story.split(" "))
                .collect(Collectors.toUnmodifiableList());

        return String.format("New Story: %s %s %s",
                words.get(rng.nextInt(words.size())),
                words.get(rng.nextInt(words.size())),
                words.get(rng.nextInt(words.size())))
                .replace("`", "")
                .replace("\n", "");
    }

    public CompletableFuture<Message> concludeStory() {
        Bot.logger.debug("Concluding story");
        stc.type();

        final List<String> yields = new ArrayList<>();

        final Optional<Message> stopship = stc.getMessagesAsStream()
                .filter(msg -> msg.getReadableContent().matches("[^ ]+"))
                .filter(msg -> msg.getReadableContent().contains(".") || msg.getReadableContent().contains("?") || msg.getReadableContent().contains("!"))
                // latest period containing; newest end
                .findFirst()
                .map(stc::getMessagesBeforeAsStream)
                .orElseGet(Stream::empty)
                .limit(200)
                .filter(msg -> msg.getReadableContent().matches("[^ ]+"))
                .filter(msg -> msg.getReadableContent().contains(".") || msg.getReadableContent().contains("?") || msg.getReadableContent().contains("!"))
                // second latest period containing, all until 1 before here
                .findFirst();
        stopship.ifPresentOrElse(
                msg -> Bot.logger.debug("Found stopship: {}", msg),
                () -> Bot.logger.debug("Could not find stopship"));

        final List<Message> storyMessages = stopship.map(stc::getMessagesAfterAsStream)
                .orElseGet(() -> stc.getMessagesAsStream().limit(100))
                .filter(msg -> {
                    String msgContent = msg.getReadableContent();

                    return !msgContent.contains("concludeStory") && isStoryPart(msgContent);
                })
                .collect(Collectors.toList());
        Bot.logger.debug("Found {} story parts", storyMessages.size());

        final String story = storyMessages.stream()
                .map(Message::getReadableContent)
                .collect(Collectors.joining(" ", "```", "```"));
        final String authors = storyMessages.stream()
                .map(Message::getUserAuthor)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .map(user -> user.getDisplayName(stc.getServer()))
                .collect(Collectors.joining("\n\t- ", "- ", ""));
        Bot.logger.debug("New Story complete\nAuthors: {}\nStory: {}", authors, story);

        final String title = String.format("The %s goes like this:", stopship
                .map(message -> {
                    final String readableContent = message.getReadableContent();
                    return "story named " + readableContent.substring(0, Math.min(32, readableContent.length()));
                })
                .orElse("tale of unknown name"));

        final EmbedBuilder embed = DefaultEmbedFactory.create(stc.getServer())
                .setTitle(title)
                .setDescription(story)
                .addField("Authors:", authors)
                .setFooter(String.format("%d Words", storyMessages.size()));
        stopship.map(Message::getLink)
                .map(URL::toExternalForm)
                .ifPresent(embed::setUrl);

        return stc.sendMessage(embed);
    }

    private boolean isStoryPart(String str) {
        return str.chars()
                .filter(x -> x == ' ')
                .count() == 0;
    }
}
