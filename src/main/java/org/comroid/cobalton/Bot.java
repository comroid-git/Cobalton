package org.comroid.cobalton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Provider;
import org.comroid.cobalton.command.AdminCommands;
import org.comroid.cobalton.command.TextCommands;
import org.comroid.cobalton.command.ToolCommands;
import org.comroid.cobalton.engine.*;
import org.comroid.common.io.FileHandle;
import org.comroid.javacord.util.commands.CommandHandler;
import org.comroid.javacord.util.commands.eval.EvalCommand;
import org.comroid.javacord.util.server.properties.GuildSettings;
import org.comroid.javacord.util.server.properties.Property;
import org.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.span.Span;
import org.comroid.restless.adapter.okhttp.v4.OkHttp3Adapter;
import org.comroid.status.DependenyObject.Adapters;
import org.comroid.status.StatusConnection;
import org.comroid.status.entity.Service.Status;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.util.DNSUtil;
import org.comroid.util.files.FileProvider;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.util.logging.ExceptionLogger;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.comroid.javacord.util.server.properties.Property.ANY_STRING;

public final class Bot {
    public final static Logger logger = LogManager.getLogger();
    public final static Color THEME = new Color(0x0f7eb1);

    public static final long BOT_ID = 493055125766537236L;

    public static final Pipe<? extends Provider<KnownCustomEmoji>> PING_EMOJIS;

    public static final StatusConnection STATUS;
    public static final DiscordApi API;
    public static final CommandHandler CMD;
    public static final GuildSettings PROP;
    public static final WordStoryEngine WSE;
    // todo public static final Starboard STAR;
    public static final Server SRV;

    public static final List<Long> permitted = new ArrayList<>();

    static {
        try {
            Adapters.SERIALIZATION_ADAPTER = FastJSONLib.fastJsonLib;
            Adapters.HTTP_ADAPTER = new OkHttp3Adapter();

            STATUS = new StatusConnection("cobalton", new FileHandle("login/status.cred"));
            STATUS.updateStatus(Status.MAINTENANCE);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to login to Status Server", t);
        }

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

            PING_EMOJIS = Span.immutable(
                    Provider.of(() -> API.getCustomEmojiById(582591035016871937L).orElse(null)),
                    Provider.of(() -> API.getCustomEmojiById(539194032870522889L).orElse(null))
            ).pipe();

            SRV = API.getServerById(711318785889665127L).orElseThrow(IllegalStateException::new);

            logger.info("Initializting StatsClient...");

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
            PROP = GuildSettings.using(FileProvider.getFile("data/guildSettings.json"));
            CMD.registerCommandTarget(PROP);
            Properties.init();

            logger.info("Initializing WordStoryEngine");
            WordStoryEngine[] wseLocal = new WordStoryEngine[1];
            API.getServerTextChannelById(Properties.WSE_CHANNEL
                    .getDefaultValue()
                    .asLong(""))
                    .ifPresent(stc -> wseLocal[0] = new WordStoryEngine(stc));
            WSE = wseLocal[0];

            logger.info("Registering prefix provider");
            //todo Causes NPEs CMD.withCustomPrefixProvider(Properties.PREFIX);

            logger.info("Registering runtime hooks");
            API.getThreadPool()
                    .getScheduler()
                    .scheduleAtFixedRate(Bot::storeAllData, 5, 5, TimeUnit.MINUTES);
            Runtime.getRuntime().addShutdownHook(new Thread(Bot::terminateAll));

            logger.info("Initializing Automation Core");

            logger.info("Initializing Starboard");
            // todo STAR = new Starboard(API, FileProvider.getFile("starboard.json"), "✅", 639051738036568064L);

            logger.info("Initializing AntiSpam Engine");
            logger.log(AntiSpam.SpamRule.INCIDENT, "AntiSpam incidents are logged at this level");
            API.addMessageCreateListener(AntiSpam.ENGINE);

            API.updateActivity(ActivityType.PLAYING, "Ping @Kaleidox#3902 for support");
            API.updateStatus(UserStatus.ONLINE);
            STATUS.updateStatus(Status.ONLINE);
            logger.info("Bot ready and listening");
        } catch (Exception e) {
            STATUS.updateStatus(Status.OFFLINE);
            ExceptionLogger.get().apply(e);
            System.exit(1);
            throw new AssertionError();
        }
    }

    private static <T> Comparator<T> randomComparator() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int x = r.nextInt(), y = r.nextInt();
        boolean b = r.nextBoolean();

        return Comparator.<T>comparingInt(t -> t.toString().hashCode() ^ x)
                .thenComparingInt(t -> t.toString().length() ^ y)
                .thenComparingInt(t -> b ? 1 : -1);
    }

    public static void main(String[] args) {

        API.addMessageCreateListener(event -> {
            if (event.getMessage().getMentionedUsers().size() > 0)
                PING_EMOJIS.sorted(randomComparator())
                        .map(Provider::now)
                        .findAny()
                        .into(event::addReactionsToMessage)
                        .exceptionally(ExceptionLogger.get());
        });

        API.getServerTextChannelById(Properties.INFO_CHANNEL.getValue(SRV).asLong())
                .ifPresent(infoChannel -> infoChannel.getMessageById(Properties.ROLE_MESSAGE.getValue(SRV).asLong())
                        .thenAcceptAsync(roleMessage -> roleMessage.addMessageAttachableListener(new RoleMessageEngine(roleMessage)))
                        .exceptionally(ExceptionLogger.get()));

        API.addMessageCreateListener(AntiSpam.ENGINE);

        API.getServerTextChannelById(644211429599346708L)
                .ifPresent(itcrowd -> itcrowd.sendMessage(DefaultEmbedFactory.create(SRV)
                        .setDescription("Bot restarted!")).exceptionally(ExceptionLogger.get()));
    }

    private static void terminateAll() {
        logger.info("Trying to shutdown gracefully");

        Bot.STATUS.updateStatus(Status.MAINTENANCE);

        for (ThrowingRunnable exec : new ThrowingRunnable[]{PROP::close, API::disconnect}) {
            try {
                exec.run();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        Bot.STATUS.updateStatus(Status.OFFLINE);

        logger.info("Shutdown complete!");
    }

    private static void storeAllData() {
        logger.info("Trying to save bot properties");
        try {
            PROP.storeData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Throwable;
    }

    public static final class Properties {
        public static Property PREFIX;

        public static Property INFO_CHANNEL;
        public static Property ROLE_MESSAGE;
        public static Property ARCHIVE_CATEGORY;
        public static Property GAMESCOM_ROLE;

        public static Property ACCEPT_EMOJI;
        public static Property DENY_EMOJI;

        public static Property WSE_CHANNEL;

        //region AntiSpam
        public static Property ENABLE_ANTISPAM;
        public static Property ANTISPAM_NOCAPS;
        public static Property ANTISPAM_NOURLS;
        //endregion

        public static Property MAINTENANCE_CHANNEL;

        private static void init() {
            PREFIX = PROP.registerProperty(prop -> prop.setName("bot.customprefix")
                    .setDefaultValue("t!")
                    .setType(String.class)
                    .setPattern(ANY_STRING)
                    .setDescription("Custom Command prefix"))
                    .property("bot.customprefix")
                    .orElseThrow();

            INFO_CHANNEL = PROP.registerProperty(prop -> prop.setName("channels.info.id")
                    .setDefaultValue("625502007150641172")
                    .setType(Long.class)
                    .setPattern(Property.DEFAULT_PATTERNS.get(Long.class))
                    .setDescription("Information Channel ID"))
                    .property("channels.info.id")
                    .orElseThrow();
            MAINTENANCE_CHANNEL = PROP.registerProperty(prop -> prop.setName("channels.maintenance.id")
                    .setDefaultValue("625503716736237588")
                    .setType(Long.class)
                    .setPattern(Property.DEFAULT_PATTERNS.get(Long.class))
                    .setDescription("Maintenance Channel ID"))
                    .property("channels.maintenance.id")
                    .orElseThrow();
            ARCHIVE_CATEGORY = PROP.registerProperty(prop -> prop.setName("channels.archive.id")
                    .setDefaultValue("625498805634203648")
                    .setType(Long.class)
                    .setPattern(Property.DEFAULT_PATTERNS.get(Long.class))
                    .setDescription("Archival Category ID"))
                    .property("channels.archive.id")
                    .orElseThrow();

            ROLE_MESSAGE = PROP.registerProperty(prop -> prop.setName("messages.autorole.id")
                    .setDefaultValue("625645142543564822")
                    .setType(Long.class)
                    .setPattern(Property.DEFAULT_PATTERNS.get(Long.class))
                    .setDescription("AutoRole Message ID"))
                    .property("messages.autorole.id")
                    .orElseThrow();
            GAMESCOM_ROLE = PROP.registerProperty(prop -> prop.setName("roles.gamescom.id")
                    .setDefaultValue("626822066280071213")
                    .setType(Long.class)
                    .setPattern(Property.DEFAULT_PATTERNS.get(Long.class))
                    .setDescription("Gamescom Role ID"))
                    .property("roles.gamescom.id")
                    .orElseThrow();

            ACCEPT_EMOJI = PROP.registerProperty(prop -> prop.setName("emojis.accept")
                    .setDefaultValue("✅")
                    .setType(String.class)
                    .setPattern(ANY_STRING)
                    .setDescription("Acceptance Emoji"))
                    .property("emojis.accept")
                    .orElseThrow();
            DENY_EMOJI = PROP.registerProperty(prop -> prop.setName("emojis.deny")
                    .setDefaultValue("❌")
                    .setType(String.class)
                    .setPattern(ANY_STRING)
                    .setDescription("Denial Emoji"))
                    .property("emojis.deny")
                    .orElseThrow();

            WSE_CHANNEL = PROP.registerProperty(prop -> prop.setName("wse.channel.id")
                    .setDefaultValue("640965171782746165")
                    .setType(Long.class)
                    .setPattern(Property.DEFAULT_PATTERNS.get(Long.class))
                    .setDescription("WordStory Channel"))
                    .property("wse.channel.id")
                    .orElseThrow();

            ENABLE_ANTISPAM = PROP.registerProperty(prop -> prop.setName("antispam.enable")
                    .setDefaultValue("false")
                    .setType(Boolean.class)
                    .setPattern(Property.DEFAULT_PATTERNS.get(Boolean.class))
                    .setDescription("Whether AntiSpam should be enabled"))
                    .property("antispam.enable")
                    .orElseThrow();
            ANTISPAM_NOCAPS = PROP.registerProperty(prop -> prop.setName("antispam.filter.nocaps")
                    .setDefaultValue("true")
                    .setType(Boolean.class)
                    .setPattern(Property.DEFAULT_PATTERNS.get(Boolean.class))
                    .setDescription("Whether the AntiSpam NoCaps filter should be enabled"))
                    .property("antispam.filter.nocaps")
                    .orElseThrow();
            ANTISPAM_NOURLS = PROP.registerProperty(prop -> prop.setName("antispam.filter.nourls")
                    .setDefaultValue("true")
                    .setType(Boolean.class)
                    .setPattern(Property.DEFAULT_PATTERNS.get(Boolean.class))
                    .setDescription("Whether the AntiSpam NoURLs filter should be enabled"))
                    .property("antispam.filter.nourls")
                    .orElseThrow();
        }
    }
}
