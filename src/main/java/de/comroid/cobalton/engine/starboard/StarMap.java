package de.comroid.cobalton.engine.starboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kaleidox.util.interfaces.Initializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class StarMap implements Initializable, Closeable {
    public final Logger logger = LogManager.getLogger();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HashMap<Long, Star> stars = new HashMap<>();
    private final DiscordApi api;
    private final File starboardFile;

    public StarMap(DiscordApi api, File file) {
        this.api = api;
        this.starboardFile = file;
    }

    private void _init() throws IOException {
        if (!this.starboardFile.exists())
            this.starboardFile.createNewFile();
        this.mapper.writeValue(this.starboardFile, this.stars.values());
    }

    @Override
    public void init() {
        logger.info("initializing starboard store");
        try {
            if (!this.starboardFile.exists()) this._init();

            Star[] stars = this.mapper.readerFor(Star[].class).readValue(this.starboardFile);
            if (stars.length == 0) return;
            for (Star star : stars) {
                if (star.getScore() == 0 ||
                        star.getOrigin() == null ||
                        star.getDestination() == null
                ) break;
                this.stars.put(star.getOrigin().id, star);
            }
        } catch (IOException e) {
            throw new RuntimeException("error initializing starboard store", e);
        }
    }

    @Override
    public void close() {
        try {
            this.mapper.writeValue(this.starboardFile, this.stars.values());
        } catch (IOException e) {
            throw new RuntimeException("error closing starboard store", e);
        }
    }

    public Star get(long id) {
        return this.stars.get(id);
    }

    public void put(Star star) {
        this.stars.put(star.getOrigin().id, star);
        if (star.getScore() <= 0) {
            api.getChannelById(star.getDestination().channel).ifPresent(destination ->
                    {
                        try {
                            destination
                                    .asTextChannel().get()
                                    .getMessageById(star.getDestination().id).get()
                                    .delete().thenAccept((Void v) ->
                                    this.stars.remove(star.getOrigin().id))
                                    .join();
                        } catch (ExecutionException | InterruptedException e) {
                            this.logger.error("error unstarring message", e);
                        }
                    }
            );
        } else {
            this.stars.put(star.getOrigin().id, star);
        }
    }

    public void put(Message origin, Message destination) {
        this.put(1, origin, destination);
    }

    public void put(int score, Message origin, Message destination) {
        if (score <= 0) {
            destination.delete().thenAccept((Void v) -> this.stars.remove(origin.getId())).join();
        } else {
            this.stars.put(origin.getId(), new Star(score, origin, destination));
        }
    }
}