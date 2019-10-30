package de.kaleidox.james.command;

import de.kaleidox.JamesBot;
import de.kaleidox.javacord.util.commands.Command;
import de.kaleidox.javacord.util.commands.CommandGroup;
import de.kaleidox.javacord.util.ui.embed.DefaultEmbedFactory;
import de.kaleidox.util.eval.EvalFactory;
import de.kaleidox.util.eval.ExecutionFactory;
import de.kaleidox.util.polyfill.Timer;
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
import org.javacord.api.util.logging.ExceptionLogger;

import javax.script.ScriptEngineManager;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
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
        if (user.isBotOwner() || user.getId() == 292141393739251714L) System.exit(0);
        command.delete("Unauthorized").join();
        channel.sendMessage("User " + user.getDiscriminatedName() + " not authorized.");
    }

    @Command(aliases = "eval",
            convertStringResultsToEmbed = true,
            useTypingIndicator = true,
            async = true)
    public void eval(User user, String[] args, Message command, TextChannel channel, Server server) {
        if (!(user.isBotOwner() || user.getId() == 292141393739251714L)) {
            command.delete("Unauthorized").join();
            channel.sendMessage("User " + user.getDiscriminatedName() + " not authorized.");
            return;
        }

        EmbedBuilder result;
        final String argsJoin = String.join(" ", args);
        final String[] lines = argsJoin.split("\\n");

        try {
            HashMap<String, Object> bindings = new HashMap<String, Object>() {{
                put("msg", command);
                put("usr", user);
                put("chl", channel);
                put("srv", server);
                put("api", JamesBot.API);
                put("timer", new Timer());
            }};

            EvalFactory.Eval eval = new EvalFactory(bindings).prepare(lines);

            result = DefaultEmbedFactory.create()
                    .addField("Executed Code", "```javascript\n" + (eval.isVerbose() ? eval.getFullCode() : eval.getUserCode()) + "```")
                    .addField("Result", "```" + eval.run() + "```")
                    .addField("Execution Time", "```" + eval.getExecTime() + "```")
                    .setAuthor(user)
                    .setUrl("http://kaleidox.de:8111")
                    .setFooter("Evaluated by " + user.getDiscriminatedName())
                    .setColor(user.getRoleColor(server).orElse(JamesBot.THEME));
        } catch (Throwable t) {
            ExecutionFactory.Execution exec = new ExecutionFactory()._safeBuild(lines);
            result = DefaultEmbedFactory.create()
                    .addField("Executed Code", "```javascript\n" + (exec.isVerbose() ? exec.toString() : exec.getOriginalCode()) + "```")
                    .addField("Message of thrown " + t.getClass().getSimpleName(), "```" + t.getMessage() + "```")
                    .setAuthor(user)
                    .setUrl("http://kaleidox.de:8111")
                    .setFooter("Evaluated by " + user.getDiscriminatedName())
                    .setColor(user.getRoleColor(server).orElse(JamesBot.THEME));
        }

        if (result != null)
            channel.sendMessage(result).thenRun(command::delete).join();
    }

    @Command
    public String say(String[] args, User executor) {
        if (!executor.isBotOwner()) return null;

        return String.join(" ", args);
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
                .map(id -> JamesBot.Prop.ARCHIVE_CATEGORY.getValue(srv).asLong() == id)
                .orElse(false))
            throw new IllegalStateException("Channel is already archived!");

        JamesBot.API.getChannelCategoryById(JamesBot.Prop.ARCHIVE_CATEGORY
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
