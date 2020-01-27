package org.comroid.util.javacord.model.command;

public interface SelfUnknownCommandRespondable<Self extends SelfUnknownCommandRespondable> {
    Self withUnknownCommandResponseStatus(boolean status);

    boolean doesRespondToUnknownCommands();

    // Extensions
    default Self enableUnknownCommandResponse() {
        return withUnknownCommandResponseStatus(true);
    }

    default Self disableUnknownCommandResponse() {
        return withUnknownCommandResponseStatus(false);
    }
}
