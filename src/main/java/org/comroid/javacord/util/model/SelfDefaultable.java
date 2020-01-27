package org.comroid.javacord.util.model;

import org.comroid.util.markers.Value;

import org.jetbrains.annotations.NotNull;

public interface SelfDefaultable<Self extends SelfDefaultable, T> {
    Self withDefaultValue(@NotNull T value);

    Value getDefaultValue();
}
