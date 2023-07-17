package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author KaitoKunTatsu
 * @version 3.0.0 | last edit: 17.07.2023
 * */
public class ReactionRoleHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    public static final Color REACTION_ROLE_COLOR = new Color(87, 77, 229);

    private final Utils utils;

    public ReactionRoleHandler(Utils pUtils) {
        this.utils = pUtils;
    }

    // Events

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent pEvent) {
        Utils.catchAndLogError(pEvent.getHook(), () -> {
            switch (pEvent.getName()) {
                case "add_reaction_role" -> addReactionRole((pEvent));
                case "get_reaction_roles" -> getReactionRolesCommand(pEvent);
                case "remove_reaction_role" -> removeReactionRoleCommand(pEvent);
                case "remove_all" -> removeAllReactionRolesCommand(pEvent);
            }
        });
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        if (event.getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong()) return;

        Guild lGuild = event.getGuild();
        Role lRole = getReactionRole(lGuild, event.getMessageIdLong(), event.getEmoji().getAsReactionCode());

        if (lRole != null) {
            lGuild.addRoleToMember(event.getMember(), lRole).queue();
        }
    }

    @Override
    public void onMessageReactionRemove(@Nonnull MessageReactionRemoveEvent event) {
        if (event.getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong()) return;

        Guild lGuild = event.getGuild();
        Role lRole = getReactionRole(lGuild, event.getMessageIdLong(), event.getEmoji().getAsReactionCode());

        if (lRole != null) {
            lGuild.removeRoleFromMember(event.getMember(), lRole).queue();
        }
    }

    // Slash Commands

    private void addReactionRole(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();
        if (utils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        try {
            TextChannel lChannel = pEvent.getOption("channel").getAsChannel().asTextChannel();
            Role lRole = pEvent.getOption("role").getAsRole();
            EmojiUnion lEmoji = (EmojiUnion) Emoji.fromUnicode(pEvent.getOption("emoji").getAsString());

            lChannel.retrieveMessageById(pEvent.getOption("message").getAsString()).queue(lMsg ->
            {
                try
                {
                    lMsg.addReaction(lEmoji).queue();
                    utils.onExecute("INSERT INTO ReactionRole VALUES(?,?,?,?,?)", pEvent.getGuild().getIdLong(), lMsg.getIdLong(),lEmoji.getAsReactionCode(), lChannel.getIdLong(),lRole.getIdLong());
                    pEvent.getHook().editOriginalEmbeds(
                            utils.createEmbed(
                                    REACTION_ROLE_COLOR,
                                    ":white_check_mark: Reaction role added: "+lEmoji.getAsReactionCode()+" "+lRole.getAsMention(),
                                    pEvent.getUser()
                            )
                        ).queue();
                }
                catch (SQLException sqlEx) {
                    sqlEx.printStackTrace();
                    pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
                }
            });
        }
        catch (NullPointerException | IllegalStateException ex) {
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Invalid argument. Make sure you selected a valid text channel, message id, role and emoji", pEvent.getUser())).queue();
        }
    }

    private void removeReactionRoleCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();
        if (utils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        try {
            TextChannel lChannel = getChannelForMessage(pEvent.getGuild(), pEvent.getOption("message").getAsLong());
            EmojiUnion lEmoji = (EmojiUnion) Emoji.fromUnicode(pEvent.getOption("emoji").getAsString());

            lChannel.retrieveMessageById(pEvent.getOption("message").getAsString()).queue(lMsg -> {
                Utils.catchAndLogError(pEvent.getJDA(), () -> {
                    lMsg.removeReaction(lEmoji).queue();
                    utils.onExecute("DELETE FROM ReactionRole WHERE guild_id = ? AND message_id = ? AND emoji = ?", pEvent.getGuild().getIdLong(), lMsg.getIdLong(),lEmoji.getAsReactionCode());
                    pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(REACTION_ROLE_COLOR, ":white_check_mark: Reaction role removed", pEvent.getUser())).queue();
                });
            });
        }
        catch (NullPointerException nullEx) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Invalid argument. Make sure you selected a valid text channel, message id, role and emoji", pEvent.getUser())).queue();
        }
    }

    private void removeAllReactionRolesCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();
        if (utils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        utils.onExecute("DELETE FROM ReactionRole WHERE guild_id = ?", pEvent.getGuild().getIdLong());
        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(REACTION_ROLE_COLOR,":white_check_mark: All reaction roles removed", pEvent.getUser())).queue();
    }

    private void getReactionRolesCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        if (utils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent)) return;
        pEvent.deferReply().queue();

        Guild lGuild = pEvent.getGuild();

        ResultSet lRs = utils.onQuery("SELECT * FROM ReactionRole WHERE guild_id = ?", lGuild.getIdLong());
        List<String[]> lFields = new ArrayList<>();

        while (lRs.next()) {
            Role lRole = lGuild.getRoleById(lRs.getLong("role_id"));
            String lRoleStr = "unknown role ("+lRs.getLong("role_id")+")";
            if (lRole != null) lRoleStr = lRole.getAsMention();

            TextChannel lChannel = lGuild.getTextChannelById(lRs.getLong("channel_id"));
            String lChannelStr = "unknown channel ("+lRs.getLong("channel_id")+")";
            if (lChannel != null) lChannelStr = lChannel.getAsMention();

            lFields.add(
                    new String[] {
                            lRs.getString("emoji"),
                            "**Role:** "+lRoleStr+
                            "\n**Channel:** "+lChannelStr+
                            "\n**Message ID:** "+lRs.getLong("message_id")
                    }
            );
        }

        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(REACTION_ROLE_COLOR,"Reaction Roles", "", lFields.toArray(new String[][]{}), true, null, null, null)).queue();

    }

    // Other private methods

    private TextChannel getChannelForMessage(Guild pGuild, long pMessageId) throws SQLException {
        ResultSet lRs = utils.onQuery("SELECT channel_id FROM ReactionRole WHERE guild_id = ? AND message_id = ?",pGuild.getIdLong(), pMessageId);
        lRs.next();

        return pGuild.getTextChannelById(lRs.getLong("channel_id"));

    }

    private Role getReactionRole(Guild pGuild, long pMessageId, String pEmoji) {
        try {
            ResultSet lRs = utils.onQuery("SELECT role_id FROM ReactionRole WHERE guild_id = ? AND message_id = ? AND emoji = ?", pGuild.getIdLong(), pMessageId, pEmoji);
            lRs.next();

            return pGuild.getRoleById(lRs.getLong("role_id"));
        }
        catch(SQLException sqlEx) {return null;}
    }
}
