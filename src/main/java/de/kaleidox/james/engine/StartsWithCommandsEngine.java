package de.kaleidox.james.engine;

import java.util.Collection;
import java.util.function.Predicate;

import de.kaleidox.JamesBot.Prop;

import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveAllEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveAllListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;

import static de.kaleidox.JamesBot.API;

public class StartsWithCommandsEngine implements MessageCreateListener {
    private static long activeMessage;

    @Override
    public void onMessageCreate(final MessageCreateEvent event) {
        if (!event.isServerMessage()) return;

        final Server server = event.getServer().get();
        final Message message = event.getMessage();
        final String content = message.getContent();
        final Predicate<User> effectiveMentions = getEffectiveMentionTester(message);

        if (content.startsWith("bald gamescom oder")) {
            final String acceptanceEmoji = Prop.ACCEPT_EMOJI.getValue(server).asString();

            final BaldGamescomListener listener = new BaldGamescomListener(acceptanceEmoji, effectiveMentions);
            listener.unregistering = message::delete;

            message.addReaction(acceptanceEmoji);
            message.addMessageAttachableListener(listener);

            activeMessage = message.getId();

            return;
        }

        if (content.startsWith("guna gamescom")) {
            API.getRoleById(Prop.GAMESCOM_ROLE.getValue(server).asLong())
                    .ifPresent(gamescom -> {
                        final Collection<User> users = gamescom.getUsers();

                        users.stream()
                                .map(member -> (Runnable) () -> member.removeRole(gamescom).join())
                                .forEachOrdered(API.getThreadPool().getExecutorService()::submit);
                    });
        }
    }

    private Predicate<User> getEffectiveMentionTester(Message message) {
        if (message.mentionsEveryone())
            return any -> true;

        return user -> message.getUserAuthor().map(user::equals).orElse(false)
                || message.getMentionedUsers().contains(user)
                || message.getMentionedRoles()
                .stream()
                .map(Role::getUsers)
                .flatMap(Collection::stream)
                .anyMatch(user::equals);
    }

    private static class BaldGamescomListener implements ReactionAddListener, ReactionRemoveListener, ReactionRemoveAllListener {
        private final String acceptanceEmoji;
        private final Predicate<User> invited;

        private Runnable unregistering;

        public BaldGamescomListener(String acceptanceEmoji, Predicate<User> effectiveMentions) {
            this.acceptanceEmoji = acceptanceEmoji;
            invited = effectiveMentions;
        }

        @Override
        public void onReactionAdd(ReactionAddEvent event) {
            if (event.getUser().isYourself()) return;
            if (!event.getServer().isPresent()) return;

            if (event.getEmoji().asUnicodeEmoji().map("❌"::equals).orElse(false)) {
                unregistering.run();
            }

            if (notInvited(event.getUser())) {
                event.removeReaction().join();
                return;
            }
            if (notEmoji(event.getEmoji())) return;

            final Server server = event.getServer().get();

            API.getRoleById(Prop.GAMESCOM_ROLE.getValue(server).asLong())
                    .ifPresent(event.getUser()::addRole);
        }

        @Override
        public void onReactionRemove(ReactionRemoveEvent event) {
            if (event.getUser().isYourself()) return;
            if (!event.getServer().isPresent()) return;

            if (notInvited(event.getUser())) return;
            if (notEmoji(event.getEmoji())) return;

            final Server server = event.getServer().get();

            API.getRoleById(Prop.GAMESCOM_ROLE.getValue(server).asLong())
                    .ifPresent(event.getUser()::removeRole);
        }

        @Override
        public void onReactionRemoveAll(ReactionRemoveAllEvent event) {
            if (!event.getServer().isPresent()) return;

            final Server server = event.getServer().get();

            API.getRoleById(Prop.GAMESCOM_ROLE.getValue(server).asLong())
                    .ifPresent(gamescom -> {
                        final Collection<User> users = gamescom.getUsers();

                        users.stream()
                                .map(member -> (Runnable) () -> member.removeRole(gamescom).join())
                                .forEachOrdered(API.getThreadPool().getExecutorService()::submit);
                    });
        }

        private boolean notInvited(User user) {
            return !invited.test(user);
        }

        private boolean notEmoji(Emoji emoji) {
            return !emoji.asUnicodeEmoji()
                    .map(acceptanceEmoji::equals)
                    .orElse(true);
        }
    }
}
