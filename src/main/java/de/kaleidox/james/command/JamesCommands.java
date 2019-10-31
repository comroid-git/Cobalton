package de.kaleidox.james.command;

import de.kaleidox.javacord.util.commands.Command;

public enum JamesCommands {
    INSTANCE;
    
    @Command
    public String wtf() {
        return "```";
    }
}
