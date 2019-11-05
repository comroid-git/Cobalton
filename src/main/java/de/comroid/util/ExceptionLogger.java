package de.comroid.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import de.comroid.Cobalton;
import de.kaleidox.javacord.util.ui.embed.DefaultEmbedFactory;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;

public class ExceptionLogger {
    private static Collection<Long> reportTo = new ArrayList<>();

    public static void addReportTarget(Mentionable reportTo) {
        if (reportTo instanceof User) {
            ((User) reportTo).openPrivateChannel()
                    .thenApply(DiscordEntity::getId)
                    .thenAccept(ExceptionLogger.reportTo::add)
                    .join();
        } else if (reportTo instanceof TextChannel) {
            ExceptionLogger.reportTo.add(((TextChannel) reportTo).getId());
        } else throw new IllegalArgumentException("Unexpected State");
    }

    public static boolean removeReportTarget(long id) {
        return reportTo.remove(id);
    }
    
    public static <T> Function<Throwable, T> get() {
        return throwable -> {
            class StringOutputStream extends OutputStream {
                String str = "";

                @Override
                public void write(int b) {
                    str += (char) b;
                }
            }
            
            final StringOutputStream out = new StringOutputStream();
            throwable.printStackTrace(new PrintStream(out));

            final EmbedBuilder embedBuilder = DefaultEmbedFactory.create()
                    .addField(String.format("An [%s] was thrown in a future:", throwable.getClass().getSimpleName()),
                            String.format("```\n%s\n```", out.str));

            reportTo.stream()
                    .map(Cobalton.API::getTextChannelById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(tc -> tc.sendMessage(embedBuilder))
                    .map(CompletableFuture::join);

            return null;
        };
    }

    @Plugin(
            name = "CobaltonLog4J2Appender",
            category = Core.CATEGORY_NAME,
            elementType = Appender.ELEMENT_TYPE)
    public static class CobaltonLog4J2Appender extends AbstractAppender {
        private final Function<Throwable, ?> logger;

        protected CobaltonLog4J2Appender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
            super(name, filter, layout, ignoreExceptions, properties);

            //noinspection RedundantTypeArguments
            this.logger = ExceptionLogger.<Object>get();
        }

        @Override
        public void append(LogEvent event) {
            final Throwable throwable = event.getThrown();
            
            if (throwable != null) 
                logger.apply(throwable);
        }
    }
}
