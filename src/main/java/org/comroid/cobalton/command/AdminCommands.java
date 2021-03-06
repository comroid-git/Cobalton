package org.comroid.cobalton.command;

import org.comroid.cobalton.Bot;
import org.comroid.javacord.util.commands.Command;
import org.comroid.javacord.util.commands.CommandGroup;
import org.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import org.comroid.status.entity.Service;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.channel.ServerTextChannelUpdater;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.util.logging.ExceptionLogger;

import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoField.*;

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
        if (Bot.permitted.contains(user.getId()))
            System.exit(1);

        command.delete("Unauthorized").exceptionally(ExceptionLogger.get());
        channel.sendMessage("User " + user.getDiscriminatedName() + " not authorized.");
    }

    @Command(usage = "shutdown", description = "Only the owner of the bot can use this", shownInHelpCommand = false)
    public void maintenance(User user, String[] args, Message command, TextChannel channel) {
        if (Bot.permitted.contains(user.getId())) {
            Bot.STATUS.updateStatus(Service.Status.MAINTENANCE);
            Bot.API.updateStatus(UserStatus.DO_NOT_DISTURB);

            return;
        }

        command.delete("Unauthorized").exceptionally(ExceptionLogger.get());
    }

    @Command
    public String say(String[] args, User executor) {
        if (!Bot.permitted.contains(executor.getId()))
            return null;

        return String.join(" ", args);
    }

    @Command(description = "Experimental")
    public EmbedBuilder ssh(String[] args, User executor) throws IOException, InterruptedException {
        if (!Bot.permitted.contains(executor.getId()))
            return null;

        final String cmd = String.join(" ", args);

        final Process exec = Runtime.getRuntime().exec(cmd);

        while (exec.isAlive()) {
            // sleep shortly
            Thread.sleep(200);
        }

        final InputStream out = exec.getInputStream();
        final InputStream err = exec.getErrorStream();
        StringBuilder str = new StringBuilder();
        StringBuilder serr = new StringBuilder();

        int r;
        while ((r = out.read()) != -1)
            str.append((char) r);
        while ((r = err.read()) != -1)
            serr.append((char) r);

        final EmbedBuilder embedBuilder = DefaultEmbedFactory.create(executor)
                .addField(String.format("Program finished with exit code %d", exec.exitValue()), "```\n" + str.toString() + "\n```");

        if (serr.length() > 1)
            embedBuilder.addField("`stderr`:", "```\n" + serr.toString() + "\n```");

        return embedBuilder;
    }

    @Command
    public void store(User executor) throws IOException {
        if (!Bot.permitted.contains(executor.getId()))
            return;

        Bot.PROP.storeData();
    }

    @Command(aliases = "archive",
            enablePrivateChat = false,
            requiredDiscordPermissions = PermissionType.ADMINISTRATOR,
            usage = "archive [Channel = this] [New Topic]",
            description = "Archive a channel")
    public void archiveChannel(Command.Parameters param, User executor, String[] args, Server srv, ServerTextChannel stc) {
        if (!Bot.permitted.contains(executor.getId()))
            return;

        final boolean thisChannel = param.getChannelMentions().size() == 0;
        final ServerTextChannel channel = thisChannel ? stc : param.getChannelMentions().get(0);
        final Permissions override = Permissions.fromBitmask(0, 0x3147840);
        final int rawPosition = channel.getRawPosition();

        if (stc.getCategory()
                .map(DiscordEntity::getId)
                .map(id -> Bot.Properties.ARCHIVE_CATEGORY.getValue(srv).asLong() == id)
                .orElse(false))
            throw new IllegalStateException("Channel is already archived!");

        Bot.API.getChannelCategoryById(Bot.Properties.ARCHIVE_CATEGORY
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
                                        .exceptionally(ExceptionLogger.get());
                                return newChannel.sendMessage("Replaced channel " + channel.getMentionTag() + " with this channel!");
                            })
                            .exceptionally(ExceptionLogger.get());
                });
    }
}
