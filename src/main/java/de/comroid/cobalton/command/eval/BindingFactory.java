package de.comroid.cobalton.command.eval;

import java.util.HashMap;

import de.comroid.Cobalton;
import de.comroid.util.polyfill.Embed;
import de.comroid.util.polyfill.Timer;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;


public class BindingFactory {
    private final HashMap<String, Object> bindings = new HashMap<>();

    public BindingFactory(User user, Message command, TextChannel channel, Server server) {
        this.bindings.putAll(new HashMap<String, Object>() {{
            put("msg", command);
            put("usr", user);
            put("chl", channel);
            put("srv", server);
            put("api", Cobalton.API);
            put("timer", new Timer());
            put("embed", new Embed(server, user));
            channel.getMessagesBefore(1, command)
                    .join()
                    .getOldestMessage()
                    .ifPresent(prev -> put("prev", prev));
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
