package de.comroid.cobalton.engine;

import java.awt.Color;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import de.comroid.Cobalton;
import de.comroid.javacord.util.commands.Command;
import de.comroid.javacord.util.commands.CommandGroup;

import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import static de.comroid.Cobalton.API;

public enum TicketEngine {
    INSTANCE;

    @Command(
            aliases = "setup-tickets",
            description = "Sets up Tickets for your Guild",
            usage = "ticket [Category = new]",
            enablePrivateChat = false,
            requiredDiscordPermission = PermissionType.ADMINISTRATOR,
            maximumArguments = 1,
            convertStringResultsToEmbed = true,
            useTypingIndicator = true,
            async = true
    )
    @CommandGroup(name = "Setup", description = "Commands used for Setup")
    public String setup(Server server, ServerTextChannel channel, String[] args) {
        final String REASON = "Ticket System Setup";

        ChannelCategory category;

        try {
            if (args.length == 1) {
                if (args[0].matches("\\d+")) // is ID
                    category = API.getChannelCategoryById(args[0]).orElseThrow(() -> new NoSuchElementException(String.format("Category with ID [ %s ] not found!", args[0])));
                else // is desired name
                    category = server.getChannelCategoriesByName(args[0])
                            .stream().findAny()
                            .or(() -> server.getChannelCategoriesByName(args[0].substring(1) /* possible # delimiter */)
                                    .stream().findAny())
                            .orElseGet(() -> server.createChannelCategoryBuilder()
                                    .setName(args[0])
                                    .setAuditLogReason(REASON)
                                    .create()
                                    .join());
            } else category = server.createChannelCategoryBuilder()
                    .setName("\uD83D\uDCC4 Tickets")
                    .setAuditLogReason(REASON)
                    .create()
                    .join();
        } catch (NoSuchElementException NSEEx) { // tried to find by name
            return String.format("❌ Setup Failed: Could not find Ticket Category by ID '%s'.\n\tReason: [ %s ] %s",
                    args[0], NSEEx.getClass().getSimpleName(), NSEEx.getMessage());
        } catch (CompletionException CEx) { // tried to create category
            return String.format("❌ Setup Failed: Could not create Ticket Category.\n\tReason: [ %s ] %s",
                    CEx.getCause().getClass().getSimpleName(), CEx.getCause().getMessage());
        }

        if (category == null)
            return String.format("❌ Setup Failed: Could not guess Category.\n\tArguments: %s", Arrays.toString(args));

        ServerTextChannel mainChannel;
        try {
            mainChannel = server.createTextChannelBuilder()
                    .setName("new-ticket")
                    .setTopic("\uD83D\uDCC4 Write your request in here! A new channel about your ticket will be opened.")
                    .setCategory(category)
                    .setSlowmodeDelayInSeconds(300)
                    .setAuditLogReason(REASON)
                    .addPermissionOverwrite(API.getYourself(), Permissions.fromBitmask(109648))
                    .create()
                    .join();
        } catch (CompletionException CEx) {
            return String.format("⚠️ Setup Stopped: Could not create Main Text Channel.\n\tReason: [ %s ] %s",
                    CEx.getCause().getClass().getSimpleName(), CEx.getCause().getMessage());
        }

        Message infoMessage;
        try {
            infoMessage = mainChannel.sendMessage(new EmbedBuilder()
                    .addField("Ticket Channel!", "Just write your request in here. It will be moved to a new Channel.")
                    .setColor(new Color(0x12B32C))
                    .setAuthor(API.getYourself())
                    .setTitle("Cobalton Ticket System")
                    .setFooter())
                    .join();
        } catch (CompletionException CEx) {
            return String.format("❌ Setup Failed: Could not send Hello-Message.\n\tReason: [ %s ] %s",
                    CEx.getCause().getClass().getSimpleName(), CEx.getCause().getMessage());
        }

        try {
            infoMessage.addReaction("\uD83D\uDCE7")
                    .join();
            infoMessage.addReactionAddListener(this::reaction);
            infoMessage.addReactionRemoveListener(this::reaction);
        } catch (CompletionException CEx) {
            return String.format("Setup Failed: Error in Reaction.\n\tReason: [ %s ] %s",
                    CEx.getCause().getClass().getSimpleName(), CEx.getCause().getMessage());
        }

        mainChannel.addServerTextChannelAttachableListener(new MainChannelListener());

        return "Setup Complete!";
    }

    private void reaction(SingleReactionEvent reactionEvent) {
        // todo [Open Ticket]
    }
    
    public CompletableFuture<Message> openTicket(Message message) {
        return Cobalton.Prop.TICKET_CATEGORY.
    }

    private static class MainChannelListener implements MessageCreateListener {
        @Override
        public void onMessageCreate(MessageCreateEvent event) {

        }
    }
}
