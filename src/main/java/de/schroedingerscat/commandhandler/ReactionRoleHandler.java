package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
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
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 26.09.2022
 * */
public class ReactionRoleHandler extends ListenerAdapter {

    /** Default color for this category to be used for embeds */
    private static final Color REACTION_ROLE_COLOR = new Color(158,242,79);

    private final Utils utils;

    public ReactionRoleHandler(Utils pUtils) {
        this.utils = pUtils;
    }

    // Events

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        switch (event.getName())
        {
            case "add_reaction_role" -> addReactionRole((event));
            case "get_reaction_roles" -> getReactionRolesCommand(event);
        }
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event)
    {
        if (event.getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong()) return;

        Guild lGuild = event.getGuild();
        Role lRole = getReactionRole(lGuild, event.getMessageIdLong(), event.getEmoji().getAsReactionCode());

        if (lRole != null) {
            lGuild.addRoleToMember(event.getMember(), lRole).queue();
        }
    }

    @Override
    public void onMessageReactionRemove(@Nonnull MessageReactionRemoveEvent event)
    {
        if (event.getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong()) return;

        Guild lGuild = event.getGuild();
        Role lRole = getReactionRole(lGuild, event.getMessageIdLong(), event.getEmoji().getAsReactionCode());

        if (lRole != null) {
            lGuild.removeRoleFromMember(event.getMember(), lRole).queue();
        }
    }

    // Slash Commands

    /**
     *
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    protected void addReactionRole(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();
        try
        {
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
        catch (NullPointerException nullEx) {
            nullEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Invalid argument. Make sure you selected a valid text channel, message, role and emoji", pEvent.getUser())).queue();
        }
        //lChannel.retrieveMessageById(event.getOption("message").getAsString()).queue();
    }

    /**
     *
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    protected void getReactionRolesCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();
        Guild lGuild = pEvent.getGuild();
        try
        {
            ResultSet lRs = utils.onQuery("SELECT * FROM ReactionRole WHERE guild_id = ?", lGuild.getIdLong());
            List<String[]> lFields = new ArrayList<>();

            while (lRs.next())
            {
                Role lRole = lGuild.getRoleById(lRs.getLong("role_id"));
                String lRoleStr = "";

                if (lRole == null) lRoleStr = "unknown role ("+lRs.getLong("role_id")+")";
                else lRoleStr = lRole.getAsMention();

                TextChannel lChannel = lGuild.getTextChannelById(lRs.getLong("channel_id"));
                String lChannelStr = "";

                if (lChannel == null) lChannelStr = "unknown channel ("+lRs.getLong("channel_id")+")";
                else lChannelStr = lRole.getAsMention();

                lFields.add(
                        new String[] {
                                lRs.getString("emoji"),
                                "**Role:** "+lRoleStr+
                                "\n**Message ID:** "+lRs.getLong("message_id")+
                                "\n**Channel:** "+lChannelStr
                        }
                );
            }

            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(REACTION_ROLE_COLOR,"", "", lFields.toArray(new String[][]{}), true, pEvent.getUser(), null, null)).queue();
        }
        catch (SQLException sqlEx) {
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Invalid argument. Make sure you selected a valid text channel, message, role and emoji", pEvent.getUser())).queue();
        }
    }

    // Other private methods

    private Role getReactionRole(Guild pGuild, long pMessageId, String pEmoji)
    {
        try
        {
            ResultSet lRs = utils.onQuery("SELECT role_id FROM ReactionRole WHERE guild_id = ? AND message_id = ? AND emoji = ?",pGuild.getIdLong(), pMessageId, pEmoji);
            lRs.next();

            return pGuild.getRoleById(lRs.getLong("role_id"));
        }
        catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
            return null;
        }
    }



    // Getter

    public static Color getCategoryColor() {return REACTION_ROLE_COLOR;}
}
