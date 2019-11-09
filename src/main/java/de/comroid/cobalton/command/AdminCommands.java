package de.comroid.cobalton.command;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.script.ScriptEngineManager;

import de.comroid.Cobalton;
import de.comroid.cobalton.command.eval.BindingFactory;
import de.comroid.cobalton.command.eval.EvalFactory;
import de.comroid.cobalton.command.eval.EvalViewer;
import de.comroid.cobalton.command.skribbl.SkribblEmbed;
import de.comroid.util.ExceptionLogger;
import de.comroid.javacord.util.commands.Command;
import de.comroid.javacord.util.commands.CommandGroup;

import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.channel.ServerTextChannelUpdater;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.CustomEmojiBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

@CommandGroup(name = "Administration Commands", description = "Commands for handling the Server")
public enum AdminCommands {
    INSTANCE;

    final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(YEAR, 4)
            .toFormatter();
    private final ScriptEngineManager mgr = new ScriptEngineManager();
    private final Pattern ext = Pattern.compile("`{3}(java)?\\n(.*)\\n`{3}");

    @Command(usage = "shutdown", description = "Only the owner of the bot can use this", shownInHelpCommand = false)
    public void shutdown(User user, String[] args, Message command, TextChannel channel) {
        if (user.isBotOwner() || user.getId() == 292141393739251714L) System.exit(1);
        command.delete("Unauthorized").join();
        channel.sendMessage("User " + user.getDiscriminatedName() + " not authorized.");
    }

    @Command(aliases = "skribbl",
            convertStringResultsToEmbed = true,
            useTypingIndicator = true,
            async = true)
    public void skribbl(Server server, User user, TextChannel channel, Message command) {
        final SkribblEmbed embed = new SkribblEmbed(server, user);
        channel.sendMessage(embed.getBuilder())
                .thenRun(command::delete)
                .join();
    }

    @Command(aliases = "eval",
            convertStringResultsToEmbed = true,
            useTypingIndicator = true,
            async = true)
    public void eval(User user, String[] args, Message command, TextChannel channel, Server server) {
        if (!(user.isBotOwner() || user.getId() == 292141393739251714L)) {
            command.delete("Unauthorized").join();
            //channel.sendMessage("User " + user.getDiscriminatedName() + " not authorized."); unfriendly :(
            return;
        }

        final String argsJoin = String.join(" ", args);
        final String[] lines = argsJoin.split("\\n");
        final BindingFactory bindings = new BindingFactory(user, command, channel, server);
        final EvalFactory eval = new EvalFactory(bindings);
        final EvalViewer viewer = new EvalViewer(eval, command, lines);

        channel
                .sendMessage(viewer.createEmbed(server, user))
                .thenAccept(viewer::complete)
                .thenRun(command::delete)
                .join();
    }

    @Command
    public String say(String[] args, User executor) {
        if (!executor.isBotOwner()) return null;

        return String.join(" ", args);
    }

    @Command(
            enablePrivateChat = false,
            convertStringResultsToEmbed = true,
            requiredDiscordPermission = PermissionType.MANAGE_EMOJIS,
            useTypingIndicator = true
    )
    public String copyEmoji(Server srv, Message msg) {
        final List<CustomEmoji> customEmojis = msg.getCustomEmojis();
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

    @Command(aliases = "archive",
            enablePrivateChat = false,
            requiredDiscordPermission = PermissionType.ADMINISTRATOR,
            usage = "archive [Channel = this] [New Topic]",
            description = "Archive a channel")
    public void archiveChannel(Command.Parameters param, String[] args, Server srv, ServerTextChannel stc) {
        final boolean thisChannel = param.getChannelMentions().size() == 0;
        final ServerTextChannel channel = thisChannel ? stc : param.getChannelMentions().get(0);
        final Permissions override = Permissions.fromBitmask(0, 0x3147840);
        final int rawPosition = channel.getRawPosition();


        if (stc.getCategory()
                .map(DiscordEntity::getId)
                .map(id -> Cobalton.Prop.ARCHIVE_CATEGORY.getValue(srv).asLong() == id)
                .orElse(false))
            throw new IllegalStateException("Channel is already archived!");

        Cobalton.API.getChannelCategoryById(Cobalton.Prop.ARCHIVE_CATEGORY
                .getValue(srv)
                .asLong())
                .ifPresent(archive -> {
                    ServerTextChannelBuilder replacementChannelBuilder = srv.createTextChannelBuilder()
                            .setName(channel.getName())
                            .setTopic(channel.getTopic());
                    channel.getCategory().ifPresent(replacementChannelBuilder::setCategory);
                    channel.getOverwrittenRolePermissions().forEach(replacementChannelBuilder::addPermissionOverwrite);

                    StringBuilder newName = new StringBuilder();
                    if (thisChannel & args.length == 0)
                        newName.append(channel.getName());
                    else if (thisChannel & args.length == 1)
                        newName.append(args[0]);
                    else if (!thisChannel & args.length == 1)
                        newName.append(channel.getName());
                    else if (!thisChannel & args.length == 2)
                        newName.append(args[1]);
                    else {
                        throw new AssertionError(String.format("Could not get new channel name [thisChannel=%b;args.length=%d]", thisChannel, args.length));
                    }
                    newName.append('-');
                    newName.append(
                            formatter.format(ZonedDateTime.now(ZoneId.of("GMT+2")))
                    );

                    ServerTextChannelUpdater updater = channel.createUpdater()
                            .setName(newName.toString())
                            .setCategory(archive)
                            .addPermissionOverwrite(srv.getEveryoneRole(), override);

                    updater.update()
                            .thenCompose(nil -> replacementChannelBuilder.create())
                            .thenCompose(newChannel -> {
                                newChannel.updateRawPosition(rawPosition)
                                        .join();
                                return newChannel.sendMessage("Replaced channel " + channel.getMentionTag() + " with this channel!");
                            })
                            .exceptionally(ExceptionLogger.get());
                });
    }
}
