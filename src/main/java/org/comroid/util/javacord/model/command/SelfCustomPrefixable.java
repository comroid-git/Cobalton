package org.comroid.util.javacord.model.command;

import java.util.Optional;
import java.util.function.Function;

import org.comroid.util.javacord.server.properties.PropertyGroup;

import org.jetbrains.annotations.NotNull;

public interface SelfCustomPrefixable<Self extends SelfCustomPrefixable> {
    Self withCustomPrefixProvider(@NotNull Function<Long, String> customPrefixProvider);

    Optional<Function<Long, String>> getCustomPrefixProvider();

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    default Self removeCustomPrefixProvider() {
        withCustomPrefixProvider((Function) null);
        return (Self) this;
    }

    // Extensions
    default Self withCustomPrefixProvider(@NotNull PropertyGroup customPrefixPropertyGroup) {
        return withCustomPrefixProvider(customPrefixPropertyGroup.function(String.class));
    }
}
