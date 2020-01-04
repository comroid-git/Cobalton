package org.comroid.cobalton.command;

import java.util.NoSuchElementException;

import org.comroid.Cobalton;
import org.comroid.cobalton.engine.AutomationCore;
import org.comroid.util.Find;
import de.comroid.javacord.util.commands.Command;
import de.comroid.javacord.util.commands.CommandGroup;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;

@CommandGroup(name = "Automation Tools", description = "Commands to help you automate your Discord Guild!")
public enum AutomationCommands {
    INSTANCE;

    @Command(
            aliases = "roleEmoji",
            description = "Create selfrole Emojis for the previous message!",
            usage = "roleEmoji <Emoji> <Role>",
            ordinal = 1,
            enablePrivateChat = false,
            requiredDiscordPermission = PermissionType.MANAGE_ROLES,
            minimumArguments = 2,
            maximumArguments = 2,
            convertStringResultsToEmbed = true,
            async = true
    )
    public Object roleEmoji(Server srv, ServerTextChannel stc, String[] args) {
        final Message message = Find.newestMessage(stc).orElseThrow(() -> new NoSuchElementException("Could not find latest message!"));
        final Emoji emoji = Find.emoji(srv, args[0]).orElseThrow(() -> new NoSuchElementException(String.format("Could not find emoji: %s", args[0])));
        final Role role = Find.role(srv, args[1]).orElseThrow(() -> new NoSuchElementException(String.format("Could not find role: %s", args[1])));

        message.addReaction(emoji).thenRun(() -> Cobalton.AUTOMATION_CORE.roleEmoji(message, emoji, role));
    }
}
