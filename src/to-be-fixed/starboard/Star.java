package org.comroid.cobalton.engine.starboard;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.javacord.api.entity.message.Message;

class StarMessage {
    public long id;
    public long channel;

    @JsonCreator StarMessage(@JsonProperty long id, @JsonProperty long channel) {
        this.id = id;
        this.channel = channel;
    }
}

public class Star {
    private int score;
    private StarMessage origin;
    private StarMessage destination;

    @JsonCreator
    public Star(@JsonProperty int score, @JsonProperty StarMessage origin, @JsonProperty StarMessage destination) {
        this.score = score;
        this.origin = origin;
        this.destination = destination;
    }

    public Star(int score, Message origin, Message destination) {
        this.score = score;
        this.setOrigin(origin);
        this.setDestination(destination);
    }

    @JsonGetter("score")
    public int getScore() {
        return this.score;
    }

    @JsonProperty
    public void setScore(int score) {
        this.score = score;
    }

    @JsonGetter("origin")
    public StarMessage getOrigin() {
        return this.origin;
    }

    @JsonProperty
    public void setOrigin(Message origin) {
        this.origin = new StarMessage(origin.getId(), origin.getChannel().getId());
    }

    @JsonGetter("destination")
    public StarMessage getDestination() {
        return this.destination;
    }

    @JsonProperty
    public void setDestination(Message destination) {
        this.destination = new StarMessage(destination.getId(), destination.getChannel().getId());
    }

    public int incScore() {
        this.score++;
        return this.score;
    }

    public int decScore() {
        this.score--;
        return this.score;
    }
}