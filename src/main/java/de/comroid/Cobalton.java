package de.comroid;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import de.comroid.cobalton.command.AdminCommands;
import de.comroid.cobalton.command.JamesCommands;
import de.comroid.cobalton.engine.RoleMessageEngine;
import de.comroid.cobalton.engine.StartsWithCommandsEngine;
import de.comroid.cobalton.engine.starboard.Starboard;
import de.comroid.util.ExceptionLogger;
import de.comroid.util.files.FileProvider;
import de.kaleidox.javacord.util.commands.CommandHandler;
import de.kaleidox.javacord.util.server.properties.PropertyGroup;
import de.kaleidox.javacord.util.server.properties.ServerPropertiesManager;
import de.kaleidox.javacord.util.ui.embed.DefaultEmbedFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.UserStatus;

public final class Cobalton {
    public final static Logger logger = LogManager.getLogger();
    public final static Color THEME = new Color(0x0f7eb1);

    public static final long BOT_ID = 625651396410343424L;

    public static final DiscordApi API;
    public static final CommandHandler CMD;
    public static final ServerPropertiesManager PROP;
    public static final Starboard STAR;

    public static final Server SRV;

    static {
        try {
            File file = FileProvider.getFile("login/token.cred");
            logger.info("Looking for token file at " + file.getAbsolutePath());
            API = new DiscordApiBuilder()
                    .setToken(new BufferedReader(new FileReader(file)).readLine())
                    .login()
                    .thenApply(api -> {
                        logger.info("Successfully connected to Discord services");
                        api.getOwner()
                                .thenAccept(ExceptionLogger::addReportTarget)
                                .join();
                        api.getChannelById(639051738036568064L)
                                .flatMap(Channel::asTextChannel)
                                .ifPresent(ExceptionLogger::addReportTarget);
                        return api;
                    })
                    .exceptionally(throwable -> {
                        logger.error(throwable);
                        return null;
                    })
                    .join();

            API.updateStatus(UserStatus.DO_NOT_DISTURB);
            API.updateActivity("Booting up...");

            API.addMessageCreateListener(event -> {
                final Message message = event.getMessage();
                if (message.getReadableContent().toLowerCase().matches(".*t\\s*[o0]|(\\[])|(\\(\\))|(\\{})|(<>)\\s*[8b]\\s*[e3]\\s*r\\s*[s5].*"))
                    message.delete("Unauthorized");
            });

            DefaultEmbedFactory.setEmbedSupplier(() -> new EmbedBuilder().setColor(THEME));

            logger.info("Initializing command handlers");
            CMD = new CommandHandler(API);
            CMD.prefixes = new String[]{"cobalton!", "c!"};
            logger.info(String.format("Setting command prefixes: '%s'", String.join("', '", CMD.prefixes)));
            CMD.useDefaultHelp(null);
            CMD.registerCommands(JamesCommands.INSTANCE);
            CMD.registerCommands(AdminCommands.INSTANCE);

            logger.info("Initialzing server properties");
            PROP = new ServerPropertiesManager(FileProvider.getFile("data/serverProps.json"));
            PROP.usePropertyCommand(null, CMD);
            Prop.init();

            logger.info("Registering prefix provider");
            CMD.withCustomPrefixProvider(Prop.PREFIX);

            logger.info("Registering runtime hooks");
            API.getThreadPool()
                    .getScheduler()
                    .scheduleAtFixedRate(Cobalton::storeAllData, 5, 5, TimeUnit.MINUTES);
            Runtime.getRuntime().addShutdownHook(new Thread(Cobalton::terminateAll));

            SRV = API.getServerById(625494140427173889L).orElseThrow(IllegalStateException::new);

            logger.info("Initializing Starboard");
            STAR = new Starboard(API, FileProvider.getFile("data/starboard.json"), "✅", 639051738036568064L);

            API.updateActivity(ActivityType.LISTENING, CMD.prefixes[0] + "help");
            API.updateStatus(UserStatus.ONLINE);
            logger.info("Bot ready and listening");
        } catch (Exception e) {
            ExceptionLogger.get().apply(e);
            System.exit(1);
            throw new AssertionError();
        }
    }

    public static void main(String[] args) {
        API.getServerTextChannelById(Prop.INFO_CHANNEL.getValue(SRV).asLong())
                .ifPresent(infoChannel -> infoChannel.getMessageById(Prop.ROLE_MESSAGE.getValue(SRV).asLong())
                        .thenAcceptAsync(roleMessage -> roleMessage.addMessageAttachableListener(new RoleMessageEngine(roleMessage)))
                        .exceptionally(ExceptionLogger.get()));
        API.addMessageCreateListener(new StartsWithCommandsEngine());

        API.addServerMemberJoinListener(event -> API.getRoleById(632196120902107137L)
                .ifPresent(event.getUser()::addRole));

        API.getServerTextChannelById(625640036096016404L)
                .ifPresent(weristes -> weristes.addMessageCreateListener(event -> {
                    API.getRoleById(632196120902107137L)
                            .ifPresent(event.getMessageAuthor().asUser().get()::removeRole);
                }));

        API.getServerTextChannelById(639051738036568064L)
                .ifPresent(itcrowd -> itcrowd.sendMessage(DefaultEmbedFactory.create()
                        .setDescription("Bot restarted!")).join());
    }

    private static void terminateAll() {
        logger.info("Trying to shutdown gracefully");
        try {
            PROP.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void storeAllData() {
        logger.info("Trying to save bot properties");
        try {
            PROP.storeData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final class Prop {
        public static PropertyGroup PREFIX;

        public static PropertyGroup INFO_CHANNEL;
        public static PropertyGroup ROLE_MESSAGE;
        public static PropertyGroup ARCHIVE_CATEGORY;
        public static PropertyGroup GAMESCOM_ROLE;

        public static PropertyGroup ACCEPT_EMOJI;
        public static PropertyGroup DENY_EMOJI;

        public static PropertyGroup MAINTENANCE_CHANNEL;

        private static void init() {
            PREFIX = PROP.register("bot.customprefix", "t!");

            INFO_CHANNEL = PROP.register("info.channel.id", 625502007150641172L);
            ROLE_MESSAGE = PROP.register("role.message.id", 625645142543564822L);
            ARCHIVE_CATEGORY = PROP.register("bot.archive.id", 625498805634203648L);
            GAMESCOM_ROLE = PROP.register("role.gamescom.id", 626822066280071213L);

            ACCEPT_EMOJI = PROP.register("emoji.accept", "✅");
            DENY_EMOJI = PROP.register("emoji.deny", "❌");

            MAINTENANCE_CHANNEL = PROP.register("bot.maintenance.id", 625503716736237588L);
        }
    }
}
