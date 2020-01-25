package org.comroid.cobalton.engine;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.comroid.Cobalton;
import de.comroid.javacord.util.ui.embed.DefaultEmbedFactory;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

public enum AntiSpam implements MessageCreateListener {
    ENGINE;

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        if (event.isServerMessage() && !Cobalton.Prop.ENABLE_ANTISPAM.getValue(event.getServer().get()).asBoolean())
            return;

        final Message message = event.getMessage();

        for (SingleMessageScanner scanner : SingleMessageScanner.values()) {
            if (!scanner.test(message)) {
                // replace message
                message.delete("Antispam")
                        .thenCompose(nil -> event.getChannel().sendMessage(scanner.apply(message)))
                        .exceptionally(ExceptionLogger.get());
            }
        }
    }

    private enum SingleMessageScanner implements Predicate<Message>, Function<Message, EmbedBuilder> {
        CAPSLOCK(message -> {
            // count upper- and lowercase characters
            final String content = message.getReadableContent();

            long uppercaseCount = content.chars()
                    .filter(Character::isUpperCase)
                    .count();
            long lowercaseCount = content.chars()
                    .filter(Character::isLowerCase)
                    .count();

            return uppercaseCount < (lowercaseCount * 2);
        }, (embed, message) -> embed.setDescription(message.getReadableContent().toLowerCase()));

        private final Predicate<Message> messagePredicate;
        private final BiFunction<EmbedBuilder, Message, EmbedBuilder> cleaner;

        SingleMessageScanner(Predicate<Message> messagePredicate, BiFunction<EmbedBuilder, Message, EmbedBuilder> cleaner) {
            this.messagePredicate = messagePredicate;
            this.cleaner = cleaner;
        }

        @Override
        public boolean test(Message message) {
            return messagePredicate.test(message);
        }

        @Override
        public EmbedBuilder apply(Message message) {
            final EmbedBuilder embed = DefaultEmbedFactory.create()
                    .setAuthor(message.getAuthor())
                    .setTimestamp(message.getCreationTimestamp());

            return cleaner.apply(embed, message);
        }
    }
}
