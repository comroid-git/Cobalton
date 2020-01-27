package org.comroid.util.javacord.model.command;

public interface SelfBotOwnerIgnorable<Self extends SelfBotOwnerIgnorable> {
    Self ignoreBotOwnerPermissions(boolean status);

    boolean doesIgnoreBotOwnerPermissions();

    // Extensions
    default Self enableIgnoringBotOwnerPermissions() {
        return ignoreBotOwnerPermissions(true);
    }

    default Self disableIgnoringBotOwnerPermissions() {
        return ignoreBotOwnerPermissions(false);
    }
}
