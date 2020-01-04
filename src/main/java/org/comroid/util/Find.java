package org.comroid.util;

import java.util.Optional;
import java.util.regex.Matcher;

import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.util.DiscordRegexPattern;
import org.javacord.core.entity.emoji.UnicodeEmojiImpl;

public final class Find {
    public static Optional<? extends Emoji> emoji(Server srv, String from) {
        if (!EmojiParser.parseToAliases(from).equals(from))
            return Optional.ofNullable(UnicodeEmojiImpl.fromString(from));

        if (from.matches("\\d+")) // is ID
            return srv.getCustomEmojiById(from);

        final Matcher matcher = DiscordRegexPattern.CUSTOM_EMOJI.matcher(from);
        if (matcher.matches()) // is mention tag
            return srv.getCustomEmojiById(matcher.group("id"));

        // else could be name
        return srv.getCustomEmojisByName(from)
                .stream()
                .findFirst();
    }

    public static Optional<? extends Role> role(Server srv, String from) {
        if (from.matches("\\d+")) // is ID
            return srv.getRoleById(from);

        final Matcher matcher = DiscordRegexPattern.ROLE_MENTION.matcher(from);
        if (matcher.matches()) // is mention tag
            return srv.getRoleById(matcher.group("id"));

        // else could be name
        return srv.getRolesByName(from)
                .stream()
                .findFirst();
    }

    public static Optional<? extends Message> newestMessage(TextChannel textChannel) {
        return textChannel.getMessages(1)
                .thenApply(MessageSet::getNewestMessage)
                .join();
    }
}
