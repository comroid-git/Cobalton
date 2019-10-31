package de.kaleidox.util.eval;

import java.util.HashMap;

import org.javacord.api.entity.user.User;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import de.kaleidox.JamesBot;
import de.kaleidox.util.polyfill.Embed;
import de.kaleidox.util.polyfill.Timer;


public class BindingFactory {
    private final HashMap<String, Object> bindings = new HashMap<>();

    public BindingFactory(User user, Message command, TextChannel channel, Server server) {
        this.bindings.putAll(new HashMap<String, Object>() {{
            put("msg", command);
            put("usr", user);
            put("chl", channel);
            put("srv", server);
            put("api", JamesBot.API);
            put("timer", new Timer());
            put("embed", new Embed(server, user));
        }});
    }

    public HashMap<String, Object> getBindings() {
        return this.bindings;
    }

    public BindingFactory add(String name, Object binding) {
        this.bindings.put(name, binding);
        return this;
    }

    public BindingFactory remove(String name) {
        this.bindings.remove(name);
        return this;
    }
}
