package org.comroid.cobalton.engine;

import org.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class WordStoryEngine implements MessageCreateListener {
    private final ServerTextChannel stc;

    public WordStoryEngine(ServerTextChannel stc) {
        this.stc = stc;

        stc.addTextChannelAttachableListener(this);
    }

    private final boolean autoConclude = true;

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        final String content = event.getReadableMessageContent();

        if (isStoryPart(content) && content.contains("."))
            concludeStory().join();
    }

    public CompletableFuture<Message> concludeStory() {
        stc.type();

        final List<String> yields = new ArrayList<>();

        final Optional<Message> stopship = stc.getMessagesAsStream()
                .limit(200)
                .filter(msg -> msg.getReadableContent().toLowerCase().contains("new story"))
                .findFirst();

        final List<Message> storyMessages = stopship.map(stc::getMessagesAfterAsStream)
                .orElseGet(() -> stc
                        .getMessagesAsStream()
                        .limit(100))
                .filter(msg -> {
                    String msgContent = msg.getReadableContent();

                    return !msgContent.contains("concludeStory") && isStoryPart(msgContent);
                })
                .collect(Collectors.toList());
        final String story = storyMessages.stream()
                .map(Message::getReadableContent)
                .collect(Collectors.joining(" ", "```", "```"));
        final String authors = storyMessages.stream()
                .map(Message::getUserAuthor)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .map(user -> user.getDisplayName(stc.getServer()))
                .collect(Collectors.joining("\n\t- "));

        final String title = String.format("The %s goes like this:", stopship
                .map(message -> {
                    final String readableContent = message.getReadableContent();
                    return "story named " + readableContent.substring(0, Math.min(32, readableContent.length()));
                })
                .orElse("tale of unknown name"));

        System.out.println("embed.title   = " + title);
        System.out.println("embed.story   = " + story);
        System.out.println("embed.authors = " + authors);
        System.out.println("embed.words   = " + storyMessages.size());

        final EmbedBuilder embed = DefaultEmbedFactory.create(stc.getServer())
                .setTitle(title)
                .setDescription(story)
                .addField("Authors:", authors)
                .setFooter(String.format("%d Words", storyMessages.size()));

        return stc.sendMessage(embed);
    }

    private boolean isStoryPart(String str) {
        return str.chars()
                .filter(x -> x == ' ')
                .count() == 0;
    }
}
