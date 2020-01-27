package org.comroid.util.javacord.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Optional;

import org.comroid.util.javacord.ui.embed.DefaultEmbedFactory;
import org.comroid.util.javacord.ui.messages.InformationMessage;
import org.comroid.util.javacord.ui.messages.RefreshableMessage;
import org.comroid.util.javacord.ui.messages.paging.PagedEmbed;
import org.comroid.util.javacord.ui.messages.paging.PagedMessage;
import org.comroid.util.javacord.ui.reactions.InfoReaction;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageEditEvent;

/**
 * Marks a method as a command.
 * <p>
 * The return value of such a command method is used as the response to the actual command.
 * The response is deleted when the command message is deleted.
 * Command methods can return one of the following:
 * <table summary="All possible return values.">
 * <tr>
 * <th>{@link String}</th>
 * </tr>
 * <tr>
 * <td>{@link EmbedBuilder}</td>
 * </tr>
 * <tr>
 * <td>{@link MessageBuilder}</td>
 * </tr>
 * <tr>
 * <td>{@link InformationMessage}</td>
 * </tr>
 * <tr>
 * <td>{@link PagedEmbed}</td>
 * </tr>
 * <tr>
 * <td>{@link PagedMessage}</td>
 * </tr>
 * <tr>
 * <td>{@link RefreshableMessage}</td>
 * </tr>
 * <tr>
 * <td>{@link InfoReaction}</td>
 * </tr>
 * </table>
 * Anything not listed here will be converted to a string using {@link String#valueOf(Object)}.
 * <p>
 * A command method can have different types of parameters, any of which will be set to their respective value:
 * <table summary="A command method can have different types of parameters, any of which will be set to their
 * respective
 * value.">
 * <tr>
 * <th>Class<br></th>
 * <th>Respective value<br></th>
 * <th>Notes</th>
 * </tr>
 * <tr>
 * <td>{@link CommandRepresentation}</td>
 * <td>The representation of the command that was used.</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link DiscordApi}</td>
 * <td>The API that caught the command message's event.</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link MessageCreateEvent}<br></td>
 * <td>The MessageCreateEvent that triggered the command.</td>
 * <td>{@code null} if the command was triggered by a {@link MessageEditEvent}.</td>
 * </tr>
 * <tr>
 * <td>{@link MessageEditEvent}<br></td>
 * <td>The MessageEditEvent that triggered the command.</td>
 * <td>{@code null} if the command was triggered by a {@link MessageCreateEvent}.</td>
 * </tr>
 * <tr>
 * <td>{@link Server}</td>
 * <td>The server that the command was sent in.</td>
 * <td>{@code null} if the command was sent in a {@link PrivateChannel}.</td>
 * </tr>
 * <tr>
 * <td>{@link Boolean}<br></td>
 * <td>Whether the command was sent in a private chat.</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link TextChannel}</td>
 * <td>The {@link TextChannel} that the command was sent in.</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link ServerTextChannel}</td>
 * <td>The ServerTextChannel that the command was sent in.</td>
 * <td>{@code null} if the command was sent in a {@link PrivateChannel}.</td>
 * </tr>
 * <tr>
 * <td>{@link PrivateChannel}<br></td>
 * <td>The PrivateChannel that the command was sent in.</td>
 * <td>{@code null} if the command was sent in a {@link ServerTextChannel}.</td>
 * </tr>
 * <tr>
 * <td>{@link Message}</td>
 * <td>The command message.</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link User}</td>
 * <td>The user that authored the command message.</td>
 * <td>{@code null} if there is no {@link User}. See {@link MessageEditEvent#getMessageAuthor()} for more
 * information.</td>
 * </tr>
 * <tr>
 * <td>{@link MessageAuthor}</td>
 * <td>The author of the command message.</td>
 * <td>{@code null} if there is no {@link MessageAuthor}. See {@link MessageEditEvent#getMessageAuthor()} for more
 * information.</td>
 * </tr>
 * <tr>
 * <td>{@code String[]}</td>
 * <td>The arguments of the command.</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link Command.Parameters}</td>
 * <td>A parameter object that carries all command parameters.</td>
 * <td></td>
 * </tr>
 * </table>
 * Any type that is not listed here will get {@code null} as a parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    /**
     * Defines aliases for the command. Must not contain {@code null}.
     *
     * @return The aliases of the command.
     */
    String[] aliases() default {};

    /**
     * Defines the description that is shown in a help command.
     *
     * @return The description of the command.
     */
    String description() default "No description provided.";

    /**
     * Defines the usage that is shown in a help command.
     *
     * @return The usage of the command.
     */
    String usage() default "No usage provided.";

    /**
     * Defines the ordinal index of the command in the group, as listed in a help command.
     *
     * @return The ordinal of the command in the group.
     */
    int ordinal() default Integer.MAX_VALUE;

    /**
     * Defines whether the command is shown in the default help command. Default value is {@code TRUE}.
     *
     * @return Whether to list this command in the default help command.
     */
    boolean shownInHelpCommand() default true;

    /**
     * Defines whether this command should be usable in a private chat. Default value is {@code true}.
     *
     * @return Whether this command is available in private chat.
     */
    boolean enablePrivateChat() default true;

    /**
     * Defines whether this command should be usable in a server chat. Default value is {@code true}.
     *
     * @return Whether this command is available in server chat.
     */
    boolean enableServerChat() default true;

    /**
     * Defines a permission that the user is required to have be in the executing context; e.g. the ServerTextChannel.
     * Default value is {@link PermissionType#SEND_MESSAGES}.
     *
     * @return The required permission to execute this command.
     */
    PermissionType[] requiredDiscordPermissions() default PermissionType.SEND_MESSAGES;

    /**
     * Defines the minimum amount of arguments required for the command to run. Default value is {@code 0}.
     *
     * @return The minimum required amount of arguments.
     */
    int minimumArguments() default 0;

    /**
     * Defines the maximum amount of arguments allowed for the command to run. Default value is {@link Integer#MAX_VALUE}.
     *
     * @return The maximum allowed amount of arguments.
     */
    int maximumArguments() default Integer.MAX_VALUE;

    /**
     * Defines the minimum amount of channel mentions required for the command to run. Default value is {@code 0}.
     *
     * @return The minimum required amount of channel mentions.
     */
    int requiredChannelMentions() default 0;

    /**
     * Defines the minimum amount of user mentions required for the command to run. Default value is {@code 0}.
     *
     * @return The minimum required amount of user mentions.
     */
    int requiredUserMentions() default 0;

    /**
     * Defines the minimum amount of role mentions required for the command to run. Default value is {@code 0}.
     *
     * @return The minimum required amount of role mentions.
     */
    int requiredRoleMentions() default 0;

    /**
     * Defines whether a command should only be usable in an NSFW channel.
     *
     * @return Whether to run this command in NSFW channels only.
     */
    boolean runInNSFWChannelOnly() default false;

    /**
     * Defines whether all returned Strings should be converted to an embed using {@link DefaultEmbedFactory}.
     *
     * @return Whether to convert all {@code String}-type results to embeds.
     */
    boolean convertStringResultsToEmbed() default false;

    /**
     * Defines whether the bot should send a typing indicator in the command channel while processing the command.
     * <p>
     * This value should only be set to {@code TRUE} if the command evaluation is expected to take a long time.
     * <p>
     * Note that this will not work well when not responding with a message, as a typing indicator will
     * last until either a message is sent or for 5 seconds.
     * This will also send a typing indicator to the channel the command has been invoked in and, if open,
     * the private channel to the user who sent the command.
     * <p>
     * Read more on the documentation: https://discordapp.com/developers/docs/resources/channel#trigger-typing-indicator
     *
     * @return Whether to use a typing indicator.
     */
    boolean useTypingIndicator() default false;

    /**
     * Defines whether a command should be executed async.
     *
     * @return Whether to run the command method async.
     */
    boolean async() default false;

    /**
     * Command parameter structure
     */
    interface Parameters {
        CommandRepresentation getCommand();

        DiscordApi getDiscord();

        Optional<MessageCreateEvent> getMessageCreateEvent();

        Optional<MessageEditEvent> getMessageEditEvent();

        Optional<Server> getServer();

        default boolean isPrivate() {
            return !getServer().isPresent();
        }

        TextChannel getTextChannel();

        default Optional<ServerTextChannel> getServerTextChannel() {
            return getTextChannel().asServerTextChannel();
        }

        default Optional<PrivateChannel> getPrivateChannel() {
            return getTextChannel().asPrivateChannel();
        }

        Message getCommandMessage();

        default List<ServerTextChannel> getChannelMentions() {
            return getCommandMessage().getMentionedChannels();
        }

        default List<User> getUserMentions() {
            return getCommandMessage().getMentionedUsers();
        }

        default List<Role> getRoleMentions() {
            return getCommandMessage().getMentionedRoles();
        }

        Optional<MessageAuthor> getCommandExecutor();

        default Optional<User> getUser() {
            return getCommandExecutor().flatMap(MessageAuthor::asUser);
        }

        String[] getArguments();
    }
}
