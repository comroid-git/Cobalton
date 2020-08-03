package org.comroid.cobalton.engine;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.activity.Activity;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.user.UserChangeActivityEvent;
import org.javacord.api.listener.user.UserChangeActivityListener;

import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

// jumpy exclusive
public final class GameRoleEngine implements UserChangeActivityListener {
    private static final String playingPrefix = "Playing ";
    private final DiscordApi api;
    private final long srvId;
    private final Map<String, Long> gameRolesByName = new ConcurrentHashMap<>();

    public GameRoleEngine(Server srv) {
        this.api = srv.getApi();
        this.srvId = srv.getId();

        api.addListener(this);
    }

    public static GameRoleEngine init(DiscordApi api) {
        final GameRoleEngine engine = api.getServerById(371328017340825610L)
                .map(GameRoleEngine::new)
                .orElseThrow(() -> new NoSuchElementException("Jumpy Missing"));
        System.out.println("GameRole Engine initialized; members: " + Arrays.toString(engine.server().getMembers().toArray()));
        return engine;
    }

    private static String gameRoleString(String gameName) {
        return playingPrefix + gameName;
    }

    private Server server() {
        return api.getServerById(srvId).orElseThrow(NoSuchElementException::new);
    }

    private CompletableFuture<Role> roleForGame(String gameName) {
        if (gameRolesByName.containsKey(gameName))
            return server().getRoleById(gameRolesByName.get(gameName))
                    .map(CompletableFuture::completedFuture)
                    .orElseThrow(NoSuchElementException::new);

        return server().getRoles()
                .stream()
                .filter(role -> role.getName().startsWith(playingPrefix))
                .peek(role -> gameRolesByName.put(role.getName().substring(playingPrefix.length()), role.getId()))
                .findFirst()
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> server().createRoleBuilder()
                        .setDisplaySeparately(true)
                        .setName(gameRoleString(gameName))
                        .setAuditLogReason("Cobaltoin GameRoleEngine")
                        .setMentionable(true)
                        .create());
    }

    @Override
    public void onUserChangeActivity(UserChangeActivityEvent event) {
        final User user = event.getUser();
        System.out.printf("%s updated activity from %s to %s\n",
                user,
                event.getOldActivity().orElse(null),
                event.getNewActivity().orElse(null)
        );

        if (user.isBot()) return;

        event.getOldActivity()
                .ifPresent(old -> roleForGame(old.getName()).thenCompose(role -> {
                    System.out.printf("Removing GameRole %s from %s\n", role, user);
                    return user.removeRole(role);
                }));

        final Optional<Activity> opt = event.getNewActivity();
        if (opt.isEmpty()) return;
        final Activity activityN = opt.get();
        if (activityN.getType() != ActivityType.PLAYING) return;

        roleForGame(activityN.getName()).thenCompose(role -> {
            System.out.printf("Adding GameRole %s to %s\n", role, user);
            return user.addRole(role);
        });
    }
}
