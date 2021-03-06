package org.comroid.cobalton.engine;

import org.comroid.cobalton.Bot;
import org.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import org.comroid.util.ChannelUtils;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.activity.Activity;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberJoinListener;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberLeaveListener;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class GamescomEngine implements ServerVoiceChannelMemberJoinListener, ServerVoiceChannelMemberLeaveListener {
    public static final long GAMESCOM_VOICE = 625508431272345600L;
    public static final long GAMESCOM_ROLE = 626822066280071213L;
    private final DiscordApi api;
    private boolean active;

    public GamescomEngine(DiscordApi api) {
        this.api = api;
        api.getServerVoiceChannelById(GAMESCOM_VOICE)
                .ifPresent(svc -> {
                    svc.addServerVoiceChannelMemberJoinListener(this);
                    svc.addServerVoiceChannelMemberLeaveListener(this);
                });
        Bot.SRV.addUserChangeActivityListener(event -> {
            if (event.getNewActivity()
                    .map(Activity::getType)
                    .filter(type -> type == ActivityType.PLAYING)
                    .isEmpty()) return;
            if (event.getUser()
                    .getRoles(Bot.SRV)
                    .stream()
                    .map(DiscordEntity::getId)
                    .noneMatch(id -> id == GAMESCOM_ROLE)) return;
            if (event.getNewActivity().isPresent() && event.getOldActivity().isPresent()) {
                final Activity oldActivity = event.getOldActivity().get();
                final Activity newActivity = event.getNewActivity().get();

                if (oldActivity.getName().equalsIgnoreCase(newActivity.getName())) return;
            }
            final EmbedBuilder embed = DefaultEmbedFactory.create()
                    .setAuthor(event.getUser())
                    .setFooter("Activity Changed");

            event.getNewActivity()
                    .ifPresentOrElse(activity -> {
                        embed.addField(String.format("Now %s:", activity.getType().name().toLowerCase()), activity.getName());
                        activity.getStreamingUrl().ifPresent(embed::setUrl);
                    }, () -> embed.setDescription("Not doing anything."));

            gamescomText().ifPresent(gc -> gc.sendMessage(embed));
        });

        active = false;
    }

    @Override
    public void onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent event) {
        if (event.getUser().isBot()) return;

        Bot.API.getRoleById(GAMESCOM_ROLE)
                .ifPresent(event.getUser()::addRole);

        final Collection<User> current = currentUsers();

        if (current.size() >= 2) {
            final ServerTextChannel gamescom = gamescomText().orElseThrow(AssertionError::new);

            if (!gamescom.getTopic().startsWith("has started"))
                gamescom.sendMessage(new EmbedBuilder()
                        .setColor(Bot.THEME)
                        .setDescription("Los geht die Gamescom!"))
                        .thenApply(msg -> gamescom.createUpdater()
                                .setTopic("has started" + (gamescom.getTopic().isBlank() ? "" : " - " + gamescom.getTopic()))
                                .update())
                        .exceptionally(ExceptionLogger.get());

            active = true;
        }
    }

    @Override
    public void onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) {
        if (event.getUser().isBot()) return;

        final ServerVoiceChannel svc = event.getChannel();

        if (active && svc.getConnectedUserIds().size() == 0) {
            // trigger guna

            Bot.API.getRoleById(Bot.Properties.GAMESCOM_ROLE.getValue(event.getServer()).asLong())
                    .ifPresent(gamescom -> {
                        final Collection<User> users = gamescom.getUsers();

                        final EmbedBuilder embed = DefaultEmbedFactory.create()
                                .setDescription("GuNa!");
                        StringBuilder joined = new StringBuilder();
                        for (User user : users)
                            joined.append("\n - ")
                                    .append(user.getDisplayName(event.getServer()));

                        embed.addField("Mit dabei waren:", joined.substring(1));

                        final ServerTextChannel stc = event.getServer().getChannelsByName("gamescom")
                                .iterator()
                                .next()
                                .asServerTextChannel()
                                .orElseThrow(NullPointerException::new);

                        stc.sendMessage(embed).exceptionally(ExceptionLogger.get());

                        ChannelUtils.archive(true, stc, "gamescom-" + users.stream()
                                .map(usr -> usr.getDisplayName(event.getServer()))
                                .map(str -> str.charAt(0))
                                .map(String::valueOf)
                                .collect(Collectors.joining()));

                        users.stream()
                                .map(member -> (Runnable) () -> member.removeRole(gamescom).exceptionally(ExceptionLogger.get()))
                                .forEachOrdered(Bot.API.getThreadPool().getExecutorService()::submit);
                    });

            active = false;
        }
    }

    private Collection<User> currentUsers() {
        return api.getRoleById(GAMESCOM_ROLE)
                .map(Role::getUsers)
                .orElseThrow(AssertionError::new);
    }

    private Optional<ServerTextChannel> gamescomText() {
        return api.getServerById(625494140427173889L)
                .flatMap(srv -> srv
                        .getChannelsByName("gamescom")
                        .stream()
                        .findAny()
                        .flatMap(Channel::asServerTextChannel));
    }
}
