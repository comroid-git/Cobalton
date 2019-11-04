package de.comroid.cobalton.engine.starboard;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

interface StarMap {
    Message origin = null;
    Message destination = null;
}

interface SerializableStarMap {
    long origin = 0L;
    long destination = 0L;
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

    private static Message getMessage(DiscordApi api, long id, long channel) throws ExecutionException, InterruptedException {
        return api.getMessageById(id, api.getChannelById(channel).get().asTextChannel().get()).get();
    }

    Star(DiscordApi api, SerializableStar star) throws ExecutionException, InterruptedException {
        this(
                getMessage(api, star.originMessage, star.originChannel),
                getMessage(api, star.destinationMessage, star.destinationChannel),
                star.count
        );
    }

    public static Consumer<? super JsonNode> map(DiscordApi api, Consumer<? super Star> consumer) {
        return jsonNode -> {
            try {
                consumer.accept(new Star(
                        api,
                        new SerializableStar(
                                jsonNode.get("originMessage").asLong(),
                                jsonNode.get("originChannel").asLong(),
                                jsonNode.get("destinationMessage").asLong(),
                                jsonNode.get("destinationChannel").asLong(),
                                jsonNode.get("count").asInt()
                        ))
                );
            } catch (Throwable t) {
                t.printStackTrace();
            }
        };
    }
//    public static Consumer<? super JsonNode> map(Runnable r) {
//        final String origin = starNode.get("origin");
//        final String destination = starnode.get("destination");
//        r.run(new SerializableStar(origin, destination));
//    }


    public Message getDestination() {
        return this.destination;
    }

    public Message getOrigin() {
        return origin;
    }

    public int getCount() {
        return count;
    }

    public Star addStar() {
        this.count++;
        return this;
    }

    public Star removeStar() {
        // ensure message always is minimum 0
        if (this.count - 1 >= 0) {
            this.count--;
        }
        return this;
    }
}

class SerializableStar implements SerializableStarMap {
    public long originMessage;
    public long destinationMessage;
    public long originChannel;
    public long destinationChannel;
    public int count;

    @JsonCreator
    public SerializableStar(@JsonProperty("originMesage") Message origin,
                            @JsonProperty("destinationMessage") Message destination,
                            @JsonProperty("count") int count) {
        this.originMessage = origin.getId();
        this.originChannel = origin.getChannel().getId();
        this.destinationMessage = destination.getId();
        this.destinationChannel = destination.getChannel().getId();
        this.count = count;
    }

    @JsonCreator
    public SerializableStar(@JsonProperty("originMesage") long origin,
                            @JsonProperty("originChannel") long originChannel,
                            @JsonProperty("destinationMessage") long destination,
                            @JsonProperty("destinationMessage") long destinationChannel,
                            int count) {
        this.originMessage = origin;
        this.originChannel = originChannel;
        this.destinationMessage = destination;
        this.destinationChannel = destinationChannel;
        this.count = count;
    }

    @JsonCreator
    public SerializableStar(Star star) {
        this(star.getOrigin(), star.getDestination(), star.getCount());
    }

}