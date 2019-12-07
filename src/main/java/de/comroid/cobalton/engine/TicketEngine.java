package de.comroid.cobalton.engine;

import java.awt.Color;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import javax.annotation.processing.Completion;

import de.comroid.Cobalton;
import de.comroid.javacord.util.commands.Command;
import de.comroid.javacord.util.commands.CommandGroup;
import de.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import de.comroid.util.markers.Value;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.MessageDeleteListener;

import static de.comroid.Cobalton.API;

public class TicketEngine {
    private final DiscordApi api;

    public static final Permissions PERMISSIONS_NORMAL_USER = Permissions.fromBitmask(3263552);
    public static final Permissions PERMISSIONS_ASSIGNEE_ROLE = Permissions.fromBitmask(3263552);
    public static final Permissions PERMISSIONS_ASSIGNEE_RAISED = Permissions.fromBitmask(3271744);
    public static final Permissions PERMISSIONS_OWN = Permissions.fromBitmask(275901648);

    public TicketEngine(final DiscordApi api) {
        this.api = api;

        api.getThreadPool()
                .getExecutorService()
                .submit(() -> this.api.getServers()
                        .forEach(server -> this.api.getChannelCategoryById(Cobalton.Prop.TICKET_CATEGORY.getValue(server).asLong())
                                .ifPresent(this::initializeTicketCategory)));
    }

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
        else Cobalton.Prop.TICKET_CATEGORY.setValue(server).toLong(category.getId());

        Role role;
        try {
            role = server.createRoleBuilder()
                    .setName("ticket-assignee")
                    .setAuditLogReason(REASON)
                    .setPermissions(PERMISSIONS_ASSIGNEE_ROLE)
                    .create()
                    .thenApply(it -> {
                        Cobalton.Prop.TICKET_ROLE.setValue(server).toLong(it.getId());
                        return it;
                    })
                    .join();
        } catch (CompletionException CEx) {
            return String.format("❌ Setup Failed: Could not create Ticket Assignee Role.\n\tReason: [ %s ] %s",
                    CEx.getCause().getClass().getSimpleName(), CEx.getCause().getMessage());
        }

        ServerTextChannel mainChannel;
        try {
            mainChannel = server.createTextChannelBuilder()
                    .setName("new-ticket")
                    .setTopic("\uD83D\uDCC4 Write your request in here! A new channel about your ticket will be opened.")
                    .setCategory(category)
                    .setSlowmodeDelayInSeconds(300)
                    .setAuditLogReason(REASON)
                    .addPermissionOverwrite(API.getYourself(), PERMISSIONS_OWN)
                    .addPermissionOverwrite(role, PERMISSIONS_ASSIGNEE_RAISED)
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
                    .setTitle("Cobalton Ticket System"))
                    .join();
        } catch (CompletionException CEx) {
            return String.format("❌ Setup Failed: Could not send Hello-Message.\n\tReason: [ %s ] %s",
                    CEx.getCause().getClass().getSimpleName(), CEx.getCause().getMessage());
        }

        mainChannel.addServerTextChannelAttachableListener(new MainChannelListener());

        return "Setup Complete!";
    }

    public CompletableFuture<ServerTextChannel> openTicketChannel(final Message message) {
        if (message.isPrivateMessage())
            return CompletableFuture.failedFuture(new IllegalArgumentException("Message is private!"));
        final Server server = message.getServer()
                .orElseThrow(() -> new IllegalArgumentException("Message is private!"));
        final ServerTextChannelBuilder builder = server.createTextChannelBuilder();
        final ChannelCategory category = API.getChannelCategoryById(Cobalton.Prop.TICKET_CATEGORY.getValue(server).asLong())
                .orElseThrow(() -> new IllegalArgumentException("Message is private!"));
        final User user = message.getUserAuthor()
                .orElseThrow(() -> new IllegalArgumentException("Message was not sent by user!"));

        final int ticketId = Cobalton.Prop.TICKET_COUNTER.getValue(server).asInt() + 1;
        Cobalton.Prop.TICKET_COUNTER.setValue(server).toInt(ticketId);

        final Role role = API.getRoleById(Cobalton.Prop.TICKET_ROLE.getValue(server).asLong())
                .orElseThrow(() -> new NoSuchElementException("Could not find Ticket Role!\n" +
                        "Please adjust the ID using `c!property ticket.role.id <role id>`"));
        final String name = "ticket-" + ticketId;

        return builder.setName(name)
                .setTopic("Please state your problem here. You can change the issue title with `:<title>`")
                .setAuditLogReason("Ticket opened by " + message.getAuthor().toString())
                .setCategory(category)
                .addPermissionOverwrite(server.getEveryoneRole(), Permissions.fromBitmask(0, 1024)) // everyone: disallowed
                .addPermissionOverwrite(user, Permissions.fromBitmask(1024)) // the asking user
                .addPermissionOverwrite(role, Permissions.fromBitmask(126016)) // the supporter role
                .addPermissionOverwrite(API.getYourself(), Permissions.fromBitmask(355392)) // the bot
                .create()
                .thenApply(stc -> {
                    final EmbedBuilder embedBuilder = DefaultEmbedFactory.create()
                            .setTitle(name)
                            .setAuthor(user)
                            .addField(String.format("%s asked:", user.getDisplayName(server)),
                                    String.format("```\n%s\n```", message.getReadableContent()))
                            .setTimestampToNow()
                            .setDescription("A helper will be assigned to your request as soon as possible!")
                            .setFooter("Add more information by answering below.");

                    stc.sendMessage(embedBuilder)
                            .thenCompose(msg -> message.delete("Obsolete"))
                            .join();

                    stc.addServerTextChannelAttachableListener(new TicketChannelListener(stc));

                    return stc;
                });
    }

    private void initializeTicketCategory(ChannelCategory category) {

    }

    private static class MainChannelListener implements MessageCreateListener, MessageDeleteListener {
        @Override
        public void onMessageCreate(MessageCreateEvent event) {

        }

        @Override
        public void onMessageDelete(MessageDeleteEvent event) {

        }
    }

    private class TicketChannelListener implements MessageCreateListener {
        public TicketChannelListener(ServerTextChannel stc) {
        }
    }
}
