package de.comroid.cobalton.engine.starboard;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.javacord.api.entity.message.Message;

interface StarMap {
    Message origin = null;
    Message destination = null;
}

class Star implements StarMap {
    private final Message origin;
    private final Message destination;
    private int count;

    @JsonCreator
    public Star(@JsonProperty("origin") Message origin, @JsonProperty("destination") Message destination, int stars) {
        this.origin = origin;
        this.destination = destination;
        this.count = stars;
    }

    public Message getDestination() {
        return this.destination;
    }

    public Message getOrigin() {
        return origin;
    }

    public int getCount() {
        return count;
    }
}