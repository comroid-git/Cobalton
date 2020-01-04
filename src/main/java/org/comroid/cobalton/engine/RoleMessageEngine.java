package org.comroid.cobalton.engine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.comroid.Cobalton;

import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;
import org.javacord.api.util.logging.ExceptionLogger;

public class RoleMessageEngine implements ReactionAddListener, ReactionRemoveListener {
    private final static Map<String, Long> roles = new ConcurrentHashMap<>();
    private final Message roleMessage;

    static {
        roles.put("\uD83D\uDEAC", 625500083026133004L); // raucher
        roles.put("\uD83C\uDF77", 625500008124383264L); // trinker
        roles.put("\uD83C\uDF3F", 625495132644311042L); // kiffer
        roles.put("❄", 625500313511526400L); // pepper
        roles.put("\uD83D\uDC8A", 625500251586822174L); // chemikant
    }

    public RoleMessageEngine(Message roleMessage) {
        this.roleMessage = roleMessage;

        roleMessage.addMessageDeleteListener(event -> event.getApi()
                .getOwner()
                .exceptionally(ExceptionLogger.get())
                .thenAccept(usr -> usr.sendMessage("RoleMessage was deleted!")));

        List<Reaction> reactions = roleMessage.getReactions();
        for (Reaction reaction : reactions) {
            if (!reaction.getEmoji().isUnicodeEmoji()) {
                reaction.remove();
                continue;
            }

            String emoji = reaction.getEmoji().asUnicodeEmoji().get();

            Long roleId = roles.get(emoji);
            if (roleId == null) {
                reaction.remove().exceptionally(ExceptionLogger.get());
                continue;
            }

            Cobalton.API.getRoleById(roleId).ifPresent(role ->
                    reaction.getUsers().thenAccept(users -> users.forEach(role::addUser)));
        }

        roles.keySet().forEach(emoji -> {
            if (reactions.stream()
                    .map(Reaction::getEmoji)
                    .map(Emoji::asUnicodeEmoji)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .noneMatch(emoji::equals)) {
                roleMessage.addReaction(emoji);
            }
        });
    }

    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        if (event.getUser().isYourself()) return;
        if (!event.getEmoji().isUnicodeEmoji()) return;

        String emoji = event.getEmoji().asUnicodeEmoji().get();

        Long roleId = roles.get(emoji);
        if (roleId == null) {
            event.removeReaction().exceptionally(ExceptionLogger.get());
            return;
        }

        Cobalton.API.getRoleById(roleId).ifPresent(role -> event.getUser().addRole(role));
    }

    @Override
    public void onReactionRemove(ReactionRemoveEvent event) {
        if (event.getUser().isYourself()) return;
        if (!event.getEmoji().isUnicodeEmoji()) return;

        String emoji = event.getEmoji().asUnicodeEmoji().get();

        Long roleId = roles.get(emoji);
        if (roleId == null) return;

        Cobalton.API.getRoleById(roleId).ifPresent(role -> event.getUser().removeRole(role));
    }
}
