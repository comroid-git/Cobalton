package de.comroid.cobalton.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.comroid.cobalton.command.skribbl.SkribblEmbed;
import de.comroid.javacord.util.commands.Command;
import de.comroid.javacord.util.commands.CommandGroup;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.CustomEmojiBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.util.logging.ExceptionLogger;

@CommandGroup(name = "Cobalton Tools", description = "Basic tools provided by Cobalton")
public enum JamesCommands {
    INSTANCE;

    @Command(aliases = "skribbl",
            convertStringResultsToEmbed = true,
            useTypingIndicator = true,
            async = true)
    public void skribbl(Server server, User user, TextChannel channel, Message command) {
        final SkribblEmbed embed = new SkribblEmbed(server, user);

        channel.sendMessage(embed.getBuilder())
                .thenRun(command::delete)
                .exceptionally(ExceptionLogger.get());
    }

    @Command(
            enablePrivateChat = false,
            convertStringResultsToEmbed = true,
            requiredDiscordPermission = PermissionType.MANAGE_EMOJIS,
            useTypingIndicator = true
    )
    public String copyEmoji(Server srv, Message msg, String[] args, ServerTextChannel stc) {
        final List<CustomEmoji> customEmojis = msg.getCustomEmojis();

        if (Arrays.binarySearch(args, "@prev") != -1) {
            stc.getMessagesBefore(1, msg)
                    .thenApply(MessageSet::getOldestMessage)
                    .thenAccept(msgOpt -> msgOpt.map(Message::getCustomEmojis)
                            .map(Collection::stream)
                            .orElseGet(Stream::of)
                            .forEachOrdered(customEmojis::add))
                    .exceptionally(ExceptionLogger.get());
        }

        Collection<CustomEmojiBuilder> builders = new ArrayList<>();

        for (CustomEmoji emoji : customEmojis) {
            final CustomEmojiBuilder builder = srv.createCustomEmojiBuilder();

            builder.setName(emoji.getName())
                    .setImage(emoji.getImage());

            builders.add(builder);
        }

        return builders.stream()
                .map(CustomEmojiBuilder::create)
                .map(CompletableFuture::join)
                .map(CustomEmoji::getMentionTag)
                .collect(Collectors.joining(" ", "Added emojis:", ""));
    }
}
