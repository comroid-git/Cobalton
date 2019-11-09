package de.comroid.cobalton.engine;

import java.util.Collection;

import de.comroid.Cobalton;

import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberJoinListener;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberLeaveListener;

import static de.comroid.Cobalton.API;

public class GamescomEngine implements ServerVoiceChannelMemberJoinListener, ServerVoiceChannelMemberLeaveListener {
    public static final long GAMESCOM_VOICE = 625508431272345600L;
    public static final long GAMESCOM_ROLE = 626822066280071213L;

    @Override
    public void onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent event) {
        if (event.getChannel().getId() != GAMESCOM_VOICE) return;

        API.getRoleById(GAMESCOM_ROLE)
                .ifPresent(event.getUser()::addRole);
    }

    @Override
    public void onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) {
        final ServerVoiceChannel svc = event.getChannel();
        
        if (svc.getId() != GAMESCOM_VOICE) return;

        if (svc.getConnectedUserIds().size() == 0) {
            // trigger guna
            
            API.getRoleById(Cobalton.Prop.GAMESCOM_ROLE.getValue(event.getServer()).asLong())
                    .ifPresent(gamescom -> {
                        final Collection<User> users = gamescom.getUsers();

                        users.stream()
                                .map(member -> (Runnable) () -> member.removeRole(gamescom).join())
                                .forEachOrdered(API.getThreadPool().getExecutorService()::submit);
                    });
        } 
    }
}
