package de.comroid.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import de.comroid.Cobalton;
import de.kaleidox.javacord.util.ui.embed.DefaultEmbedFactory;

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
}