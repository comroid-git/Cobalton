package org.comroid.cobalton.engine;

import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.ref.ReferenceMap;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.comroid.uniform.node.UniObjectNode;
import org.intellij.lang.annotations.Language;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnswersEngine implements MessageCreateListener {
    public static final AccessibleEndpoint ENDPOINT = new AccessibleEndpoint() {
        private final Pattern pattern = buildUrlPattern();

        @Override
        public String getUrlBase() {
            return "https://answers.fun.skayo.dev/";
        }

        @Override
        public String getUrlExtension() {
            return "answers.json";
        }

        @Override
        public String[] getRegExpGroups() {
            return new String[0];
        }

        @Override
        public Pattern getPattern() {
            return pattern;
        }
    };
    public static final Random RNG = new Random();
    public final Pattern questionPattern;
    public final REST<?> rest = new REST<>(DependenyObject.Adapters.HTTP_ADAPTER);
    private final ReferenceMap<String, String> responseCache = ReferenceMap.create();
    private final Pipe<? extends String> authors = responseCache.pipe().distinct();
    private final Pipe<? extends String> responses = responseCache.biPipe().drop();

    public Pipe<? extends String> getResponses() {
        return responses;
    }

    public Pipe<? extends String> getAuthors() {
        return authors;
    }

    public String getRandomResponse() {
        return responses.get(RNG.nextInt(responses.size()));
    }

    public AnswersEngine(DiscordApi api, @Language("RegExp") String questionPattern) {
        this.questionPattern = Pattern.compile(questionPattern);

        api.addMessageCreateListener(this);
    }

    public synchronized CompletableFuture<ReferenceMap<String, String>> refreshCache() {
        return rest.request()
                .endpoint(ENDPOINT)
                .method(REST.Method.GET)
                .execute$deserializeSingle()
                .thenApply(UniObjectNode::asMap)
                .thenAccept(obj -> obj.forEach((response, author) -> responseCache
                        .getReference(response, true)
                        .set(String.valueOf(author))))
                .thenApply(nil -> responseCache);
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        final Matcher matcher = questionPattern.matcher(event.getReadableMessageContent());

        if (!matcher.matches())
            return;


    }
}
