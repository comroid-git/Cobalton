package org.comroid.cobalton.command;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult;
import org.comroid.cobalton.command.skribbl.SkribblEmbed;
import org.comroid.javacord.util.commands.Command;
import org.comroid.javacord.util.commands.CommandGroup;
import org.javacord.api.entity.Nameable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.CustomEmojiBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@CommandGroup(name = "Cobalton Tools", description = "Basic tools provided by Cobalton")
public enum ToolCommands {
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
            requiredDiscordPermissions = PermissionType.MANAGE_EMOJIS,
            useTypingIndicator = true
    )
    public String copyEmoji(Server srv, Message msg, String[] args, ServerTextChannel stc) {
        final List<CustomEmoji> customEmojis = msg.getCustomEmojis();

        if (Arrays.binarySearch(args, "@prev") != -1) {
            stc.getMessagesBefore(1, msg)
                    .thenApply(MessageSet::getOldestMessage)
                    .thenAccept(msgOpt -> msgOpt.map(Message::getCustomEmojis)
                            .stream()
                            .flatMap(Collection::stream)
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

    @Command(
            usage = "gameping <game>",
            enablePrivateChat = false,
            minimumArguments = 1,
            convertStringResultsToEmbed = true,
            useTypingIndicator = true
    )
    public String gameping(User user, Server server, String[] args) {
        final String gameName = String.join(" ", args);
        final BoundExtractedResult<Role> result = FuzzySearch.extractOne(gameName, server.getRoles(), Nameable::getName);
        final Role role = result.getReferent();

        return server.getMembers()
                .stream()
                .filter(member -> server.getRoles(member).contains(role))
                .filter(member -> member.getActivity()
                        .map(activity -> activity.getName().equalsIgnoreCase(gameName))
                        .orElse(false))
                .map(User::getNicknameMentionTag)
                .collect(Collectors.joining(
                        ", ",
                        String.format("%s wants to play %s\n", user.getNicknameMentionTag(), role),
                        ""
                ));
    }
}
