package org.comroid.cobalton;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.comroid.cobalton.command.AdminCommands;
import org.comroid.cobalton.command.TextCommands;
import org.comroid.cobalton.command.ToolCommands;
import org.comroid.cobalton.engine.AntiSpam;
import org.comroid.cobalton.engine.GamescomEngine;
import org.comroid.cobalton.engine.RoleMessageEngine;
import org.comroid.util.DNSUtil;
import org.comroid.util.files.FileProvider;
import de.comroid.eval.EvalCommand;
import de.comroid.javacord.util.commands.CommandHandler;
import de.comroid.javacord.util.server.properties.PropertyGroup;
import de.comroid.javacord.util.server.properties.ServerPropertiesManager;
import de.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import de.kaleidox.botstats.BotListSettings;
import de.kaleidox.botstats.javacord.JavacordStatsClient;
import de.kaleidox.botstats.model.StatsClient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.util.logging.ExceptionLogger;

public final class Bot {
    public final static Logger logger = LogManager.getLogger();
    public final static Color THEME = new Color(0x0f7eb1);

    public static final long BOT_ID = 493055125766537236L;

    public static final DiscordApi API;
    public static final StatsClient STATS;
    public static final CommandHandler CMD;
    public static final ServerPropertiesManager PROP;
    // todo public static final Starboard STAR;
    public static final Server SRV;

    public static final List<Long> permitted = new ArrayList<>();

    static {
        try {
            try {
                API = new DiscordApiBuilder()
                        .setToken(FileProvider.readContent("login/discord.cred")[0])
                        .login()
                        .get();
            } catch (Throwable t) {
                throw new RuntimeException("Failed to login to discord servers", t);
            }

            logger.info("Successfully connected to Discord services");

            Stream.of(DNSUtil.getTxtContent("txdiad.comroid.org").split(";"))
                    .map(Long::parseLong)
                    .peek(id -> logger.info("Added " + id + " to permitted user IDs"))
                    .forEach(permitted::add);

            API.updateStatus(UserStatus.DO_NOT_DISTURB);
            API.updateActivity("Booting up...");

            SRV = API.getServerById(625494140427173889L).orElseThrow(IllegalStateException::new);

            logger.info("Initializting StatsClient...");
            STATS = new JavacordStatsClient(BotListSettings.builder()
                    .tokenFile(FileProvider.getFile("login/botlists.properties"))
                    .postStatsTester(() -> API.getYourself().getId() == BOT_ID)
                    .build(), API);

            DefaultEmbedFactory.setEmbedSupplier(() -> new EmbedBuilder().setColor(THEME));

            logger.info("Initializing command handlers");
            CMD = new CommandHandler(API);
            CMD.prefixes = new String[]{"cobalton!", "c!"};
            logger.info(String.format("Setting command prefixes: '%s'", String.join("', '", CMD.prefixes)));
            CMD.useDefaultHelp(null);
            CMD.registerCommands(TextCommands.INSTANCE, ToolCommands.INSTANCE);
            CMD.registerCommands(AdminCommands.INSTANCE);
            CMD.registerCommands(EvalCommand.INSTANCE);

            logger.info("Initialzing server properties");
            PROP = new ServerPropertiesManager(FileProvider.getFile("servers.json"));
            PROP.usePropertyCommand(null, CMD);
            Prop.init();

            logger.info("Registering prefix provider");
            CMD.withCustomPrefixProvider(Prop.PREFIX);

            logger.info("Registering runtime hooks");
            API.getThreadPool()
                    .getScheduler()
                    .scheduleAtFixedRate(Bot::storeAllData, 5, 5, TimeUnit.MINUTES);
            Runtime.getRuntime().addShutdownHook(new Thread(Bot::terminateAll));

            logger.info("Initializing Automation Core");

            logger.info("Initializing Starboard");
            // todo STAR = new Starboard(API, FileProvider.getFile("starboard.json"), "✅", 639051738036568064L);

            logger.info("Initializing AntiSpam Engine");
            API.addMessageCreateListener(AntiSpam.ENGINE);

            API.updateActivity(ActivityType.PLAYING, "Ping @Kaleidox#3902 for support");
            API.updateStatus(UserStatus.ONLINE);
            logger.info("Bot ready and listening");
        } catch (Exception e) {
            ExceptionLogger.get().apply(e);
            System.exit(1);
            throw new AssertionError();
        }
    }

    public static void main(String[] args) {
        API.addMessageCreateListener(event -> {
            if (event.getReadableMessageContent().matches(".*[gG]+\\s*[uU]+\\s*[nN]+\\s*[aA4].*"))
                event.addReactionsToMessage("\uD83C\uDDEC", "\uD83C\uDDFA", "\uD83C\uDDF3", "\uD83C\uDDE6")
                        .exceptionally(ExceptionLogger.get());
        });

        API.getServerTextChannelById(Bot.Prop.INFO_CHANNEL.getValue(SRV).asLong())
                .ifPresent(infoChannel -> infoChannel.getMessageById(Bot.Prop.ROLE_MESSAGE.getValue(SRV).asLong())
                        .thenAcceptAsync(roleMessage -> roleMessage.addMessageAttachableListener(new RoleMessageEngine(roleMessage)))
                        .exceptionally(ExceptionLogger.get()));

        SRV.addServerMemberJoinListener(event -> {
            SRV.getSystemChannel()
                    .ifPresent(stc -> stc.sendMessage(DefaultEmbedFactory.create().addField("Willkommen zum Abriss, " + event.getUser().getName() + "!",
                            "Bitte stell dich doch kurz in <#625640036096016404> mit ein paar Zeilen vor, dann kannst du alle Channel benutzen!")));
        });

        // init gamescom engine
        new GamescomEngine(API);

        API.addMessageCreateListener(AntiSpam.ENGINE);

        API.addServerMemberJoinListener(event -> API.getRoleById(632196120902107137L)
                .ifPresent(event.getUser()::addRole));

        API.getServerTextChannelById(625640036096016404L)
                .ifPresent(weristes -> weristes.addMessageCreateListener(event -> {
                    API.getRoleById(632196120902107137L)
                            .ifPresent(event.getMessageAuthor().asUser().get()::removeRole);
                }));

        API.getServerTextChannelById(644220645814566912L)
                .ifPresent(itcrowd -> itcrowd.sendMessage(DefaultEmbedFactory.create()
                        .setDescription("Bot restarted!")).exceptionally(ExceptionLogger.get()));
    }

    private static void terminateAll() {
        logger.info("Trying to shutdown gracefully");
        try {
            PROP.close();
            API.disconnect();
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

        public static PropertyGroup ENABLE_ANTISPAM;

        public static PropertyGroup MAINTENANCE_CHANNEL;

        private static void init() {
            PREFIX = PROP.register("bot.customprefix", "t!");

            INFO_CHANNEL = PROP.register("info.channel.id", 625502007150641172L);
            ROLE_MESSAGE = PROP.register("role.message.id", 625645142543564822L);
            ARCHIVE_CATEGORY = PROP.register("bot.archive.id", 625498805634203648L);
            GAMESCOM_ROLE = PROP.register("role.gamescom.id", 626822066280071213L);

            ACCEPT_EMOJI = PROP.register("emoji.accept", "✅");
            DENY_EMOJI = PROP.register("emoji.deny", "❌");

            ENABLE_ANTISPAM = PROP.register("bot.antispam.enable", true)
                    .withDisplayName("Enable AntiSpam")
                    .withDescription("Enables the AntiSpam system. Boolean values only.");

            MAINTENANCE_CHANNEL = PROP.register("bot.maintenance.id", 625503716736237588L);
        }
    }
}
