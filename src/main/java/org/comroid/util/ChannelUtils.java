package org.comroid.util;

import java.time.Instant;

import org.comroid.cobalton.Bot;

import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Permissionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.channel.ServerTextChannelUpdater;
import org.javacord.api.util.logging.ExceptionLogger;

public class ChannelUtils {
    @SuppressWarnings("unchecked") // fuck javacord
    public static <T extends Permissionable & DiscordEntity> void archive(boolean recreate, ServerTextChannel stc, String newName) {
        if (recreate) {
            final ServerTextChannelBuilder renewalBuilder = stc.getServer().createTextChannelBuilder()
                    .setName(stc.getName())
                    .setAuditLogReason("Archived Channel Renewal")
                    .setSlowmodeDelayInSeconds(stc.getSlowmodeDelayInSeconds());

            stc.getCategory().ifPresent(renewalBuilder::setCategory);

            renewalBuilder.create()
                    .thenCompose(chl -> chl.updateRawPosition(stc.getRawPosition()))
                    .exceptionally(ExceptionLogger.get());
        }

        Bot.API.getChannelCategoryById(Bot.Properties.ARCHIVE_CATEGORY.getValue(stc.getServer()).asLong())
                .ifPresent(cat -> {
                    final ServerTextChannelUpdater updater = stc.createUpdater()
                            .setCategory(cat)
                            .setAuditLogReason("Archived")
                            .setTopic(String.format("Archived at %s", Instant.now().toString()));

                    if (newName != null)
                        updater.setName(newName);

                    stc.getOverwrittenPermissions().keySet().forEach(permissionable -> updater.removePermissionOverwrite((T) permissionable));
                    cat.getOverwrittenPermissions().forEach((key, value) -> updater.addPermissionOverwrite((T) key, value));

                    updater.update().exceptionally(ExceptionLogger.get());
                });
    }
}
