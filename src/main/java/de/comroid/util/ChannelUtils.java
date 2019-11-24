package de.comroid.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import de.comroid.Cobalton;

import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Permissionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelUpdater;

public class ChannelUtils {
    @SuppressWarnings("unchecked") // fuck javacord
    public static <T extends Permissionable & DiscordEntity> void archive(ServerTextChannel stc, String newName) {
        Cobalton.API.getChannelCategoryById(Cobalton.Prop.ARCHIVE_CATEGORY.getValue(stc.getServer()).asLong())
                .ifPresent(cat -> {
                    final ServerTextChannelUpdater updater = stc.createUpdater()
                            .setCategory(cat)
                            .setAuditLogReason("Archived")
                            .setTopic(String.format("Archived at %s", DateTimeFormatter.ISO_DATE_TIME.format(Instant.now())));

                    if (newName != null)
                        updater.setName(newName);

                    stc.getOverwrittenPermissions().keySet().forEach(permissionable -> updater.removePermissionOverwrite((T) permissionable));
                    cat.getOverwrittenPermissions().forEach((key, value) -> updater.addPermissionOverwrite((T) key, value));

                    updater.update().join();
                });
    }
}
