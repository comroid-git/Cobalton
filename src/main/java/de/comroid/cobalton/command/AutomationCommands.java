package de.comroid.cobalton.command;

import de.comroid.javacord.util.commands.Command;
import de.comroid.javacord.util.commands.CommandGroup;

import org.javacord.api.entity.permission.PermissionType;

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
            convertStringResultsToEmbed = true
    )
    public Object roleEmoji() {
    }
}
