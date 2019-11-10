package de.comroid.cobalton.command;

import de.comroid.javacord.util.commands.Command;
import de.comroid.javacord.util.commands.CommandGroup;

@CommandGroup(name = "TextCommands", description = "Textual fun!")
public enum TextCommands {
    INSTANCE;

    @Command(convertStringResultsToEmbed = true)
    public String codebrackets() {
        return "```";
    }

    @Command(convertStringResultsToEmbed = true)
    public String lenny() {
        return "( ͡° ͜ʖ ͡°)";
    }
}
