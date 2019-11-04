package de.comroid;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import de.comroid.james.command.AdminCommands;
import de.comroid.james.command.JamesCommands;
import de.comroid.james.engine.RoleMessageEngine;
import de.comroid.james.engine.StartsWithCommandsEngine;
import de.comroid.util.starboard.Starboard;
import de.kaleidox.javacord.util.commands.CommandHandler;
import de.kaleidox.javacord.util.server.properties.PropertyGroup;
import de.kaleidox.javacord.util.server.properties.ServerPropertiesManager;
import de.kaleidox.javacord.util.ui.embed.DefaultEmbedFactory;
import de.comroid.util.files.FileProvider;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.util.logging.ExceptionLogger;

public final class JamesBot {
    public final static Color THEME = new Color(0x0f7eb1);

    public static final long BOT_ID = 625651396410343424L;

    public static final DiscordApi API;
    public static final CommandHandler CMD;
    public static final ServerPropertiesManager PROP;

    public static final Server SRV;

    static {
        try {
            File file = FileProvider.getFile("login/token.cred");
            System.out.println("Looking for token file at " + file.getAbsolutePath());
            API = new DiscordApiBuilder()
                    .setToken(new BufferedReader(new FileReader(file)).readLine())
                    .login()
                    .exceptionally(ExceptionLogger.get())
                    .join();

            API.updateStatus(UserStatus.DO_NOT_DISTURB);
            API.updateActivity("Booting up...");

            DefaultEmbedFactory.setEmbedSupplier(() -> new EmbedBuilder().setColor(THEME));

            CMD = new CommandHandler(API);
            CMD.prefixes = new String[]{"cobalton!", "c!"};
            CMD.useDefaultHelp(null);
            CMD.registerCommands(JamesCommands.INSTANCE);
            CMD.registerCommands(AdminCommands.INSTANCE);

            PROP = new ServerPropertiesManager(FileProvider.getFile("data/serverProps.json"));
            PROP.usePropertyCommand(null, CMD);
            Prop.init();

            CMD.withCustomPrefixProvider(Prop.PREFIX);

            API.getThreadPool()
                    .getScheduler()
                    .scheduleAtFixedRate(JamesBot::storeAllData, 5, 5, TimeUnit.MINUTES);
            Runtime.getRuntime().addShutdownHook(new Thread(JamesBot::terminateAll));

            SRV = API.getServerById(625494140427173889L).orElseThrow(IllegalStateException::new);

            API.updateActivity(ActivityType.LISTENING, CMD.prefixes[0] + "help");
            API.updateStatus(UserStatus.ONLINE);
        } catch (Exception e) {
            throw new RuntimeException("Error in initializer", e);
        }
    }

    public static void main(String[] args) throws IOException {
        API.getServerTextChannelById(Prop.INFO_CHANNEL.getValue(SRV).asLong())
                .ifPresent(infoChannel -> infoChannel.getMessageById(Prop.ROLE_MESSAGE.getValue(SRV).asLong())
                        .thenAcceptAsync(roleMessage -> roleMessage.addMessageAttachableListener(new RoleMessageEngine(roleMessage))));
        API.addMessageCreateListener(new StartsWithCommandsEngine());

        API.addServerMemberJoinListener(event -> API.getRoleById(632196120902107137L).ifPresent(event.getUser()::addRole));

        API.getServerTextChannelById(625640036096016404L)
                .ifPresent(weristes -> weristes.addMessageCreateListener(event -> {
                    API.getRoleById(632196120902107137L).ifPresent(event.getMessageAuthor().asUser().get()::removeRole);
                }));

        API.getServerTextChannelById(639051738036568064L)
                .ifPresent(itcrowd -> itcrowd.sendMessage(DefaultEmbedFactory.create().setDescription("Bot restarted!")).join());


        final Starboard starboard = new Starboard(FileProvider.getFile("data/starboard.json"), "✅");
        API.addReactionAddListener(starboard::addReaction);
        API.addReactionRemoveListener(starboard::removeReaction);

        API.addMessageCreateListener(event -> {
            final Message message = event.getMessage();
            if (message.getReadableContent().toLowerCase().matches(".*t\\s*[o0]|(\\[])|(\\(\\))|(\\{})|(<>)\\s*[8b]\\s*[e3]\\s*r\\s*[s5].*"))
                message.delete("Unauthorized");
        });
    }

    private static void terminateAll() {
        try {
            PROP.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void storeAllData() {
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
