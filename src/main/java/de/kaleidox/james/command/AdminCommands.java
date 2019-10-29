package de.kaleidox.james.command;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.regex.Pattern;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import de.kaleidox.JamesBot;
import de.kaleidox.javacord.util.commands.Command;
import de.kaleidox.javacord.util.commands.CommandGroup;

import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.channel.ServerTextChannelUpdater;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.util.logging.ExceptionLogger;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

@CommandGroup(name = "Administration Commands", description = "Commands for handling the Server")
public enum AdminCommands {
    INSTANCE;

    private final ScriptEngineManager mgr = new ScriptEngineManager();
    private final Pattern ext = Pattern.compile("`{3}(java)?\\n(.*)\\n`{3}");
    final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(YEAR, 4)
            .toFormatter();

    @Command(usage = "shutdown", description = "Only the owner of the bot can use this", shownInHelpCommand = false)
    public void shutdown(User user) {
        if (user.isBotOwner()) System.exit(0);
    }

    @Command(aliases = "eval")
    public Object eval(User user, String[] args, Message command, TextChannel channel, Server server) throws ScriptException, ClassNotFoundException, NoSuchFieldException {
        if (!user.isBotOwner()) {
            command.delete("Unauthorized").join();
            return null;
        }

        final String argsJoin = String.join(" ", args);
        final String[] lines = argsJoin.split("\\n");

        final ScriptEngine engine = mgr.getEngineByName("JavaScript");
        final Bindings bindings = engine.createBindings();

        bindings.put("msg", command);
        bindings.put("usr", user);
        bindings.put("chl", channel);
        bindings.put("srv", server);
        bindings.put("api", JamesBot.API);

        StringBuilder code = new StringBuilder();
        boolean append;

        for (String line : lines) {
            append = !line.contains("```");

            if (line.startsWith("import ")) {
                append = false;

                String classname = line.substring("import ".length(), line.length() - ((line.lastIndexOf(';') == line.length()) ? 2 : 1));
                Class<?> aClass = Class.forName(classname);

                code.append('\n')
                        .append("var sys = Java.type('java.lang.System')\n")
                        .append("var ")
                        .append(aClass.getSimpleName())
                        .append(" = Java.type('")
                        .append(classname)
                        .append("')");
            }

            if (append) {
                code.append('\n').append(line);
            }
        }

        engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);

        return "```" + engine.eval(code.append('\n').toString().replaceAll("", "")) + "```";
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

                    String newName = "";
                    if (thisChannel & args.length == 0)
                        newName = channel.getName();
                    else if (thisChannel & args.length == 1)
                        newName = args[0];
                    else if (!thisChannel & args.length == 1)
                        newName = channel.getName();
                    else if (!thisChannel & args.length == 2)
                        newName = args[1];
                    else throw new AssertionError(String.format("Could not get new channel name [thisChannel=%b;args.length=%d]", thisChannel, args.length));

                    newName += '-' + formatter.format(ZonedDateTime.now(ZoneId.of("GMT+2")));

                    ServerTextChannelUpdater updater = channel.createUpdater()
                            .setName(newName)
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
