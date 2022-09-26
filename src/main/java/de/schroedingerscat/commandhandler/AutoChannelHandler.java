package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;

/**
 * Handles slash commands considering automated custom channels
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 25.09.2022
 **/
public class AutoChannelHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    private static final Color AUTOCHANNEL_COLOR = new Color(25,196,234);

    private final Utils utils;

    public AutoChannelHandler(Utils pUtils) {
        this.utils = pUtils;
    }

    // Events

    @Override
    /**
     *  Redirects the event to method handling it if the slash command belongs to this category
     *
     *  @param pEvent   Event triggered by a user using a slash command
     * */
    public void onSlashCommandInteraction(SlashCommandInteractionEvent pEvent) {
        switch (pEvent.getName())
        {
            case "set_auto_channel" -> setAutoChannelCommand(pEvent);
            case "vcname" -> setChannelNameCommand(pEvent);
            case "vclimit" -> setChannelLimitCommand(pEvent);
            case "vckick" -> kickChannelCommand(pEvent);
            case "vcban" -> banChannelCommand(pEvent);
            case "claim" -> claimChannelCommand(pEvent);
            case "clear_auto_channel_db" -> clearAutoChannelDatabaseCommand(pEvent);
        }
    }

    @Override
    /**
     *  If the voice channel the user connected to is the create-channel for automated custom channel
     *  a new voice will be created, added to the database table of custom channel and the user moved in it
     *
     *  @param pEvent   Event triggered by a user joining a voice channel
     * */
    public void onGuildVoiceJoin(@Nonnull GuildVoiceJoinEvent pEvent)
    {
        VoiceChannel lCreateChannel = getAutoCreateChannelForGuild(pEvent.getGuild());
        VoiceChannel lJoinedChannel = (VoiceChannel) pEvent.getChannelJoined();
        Member lMember = pEvent.getMember();

        // Create custom voice if joined create channel
        if (lCreateChannel != null && lCreateChannel.equals(lJoinedChannel))
        {
            pEvent.getGuild().createVoiceChannel(lMember.getEffectiveName()).queue(channel ->
                {
                    try
                    {
                        utils.onExecute(
                                "INSERT INTO AutoChannel VALUES(?,?,?)",
                                pEvent.getGuild().getIdLong(),
                                lMember.getIdLong(),
                                channel.getIdLong());
                    }
                    catch(SQLException sqlException)
                    {
                        sqlException.printStackTrace();
                        return;
                    }
                    pEvent.getGuild().moveVoiceMember(lMember, channel).queue();
                }
            );
        }
    }

    @Override
    /**
     *  If the voice channel the user left is a custom channel and empty it gets deleted
     *
     *  @param pEvent   Event triggered by a user leaving a voice channel
     * */
    public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent pEvent)
    {
        VoiceChannel lVoiceLeft = (VoiceChannel) pEvent.getChannelLeft();
        if (isCustomChannel(lVoiceLeft) && lVoiceLeft.getMembers().size() < 1)
        {
            try
            {
                utils.onExecute("DELETE FROM AutoChannel WHERE guild_id = ? AND channel_id = ?", pEvent.getGuild(), lVoiceLeft.getIdLong());
                lVoiceLeft.delete().queue();
            }
            catch (SQLException sqlEx) { sqlEx.printStackTrace();}
        }
    }

    // Slash Commands

    /**
     * Sets the create-channel for automated custom channel to the one defined in the slash command option "channel"
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    private void setAutoChannelCommand(SlashCommandInteractionEvent pEvent) {
        pEvent.deferReply().queue();
        Channel lChannel = pEvent.getOption("channel").getAsChannel();
        if (lChannel.getType() != ChannelType.VOICE)
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Select a voice channel", pEvent.getUser())).queue();
        else
        {
            try
            {
                utils.insertGuildInSettingsIfNotExist(pEvent.getGuild().getIdLong());
                utils.onExecute("UPDATE GuildSettings SET auto_channel_id = ? WHERE guild_id=?",lChannel.getIdLong(), pEvent.getGuild().getIdLong());
                pEvent.getHook().editOriginalEmbeds(utils.createEmbed(AUTOCHANNEL_COLOR, ":white_check_mark: Successfully assigned "+lChannel.getAsMention()+" as auto channel", pEvent.getUser())).queue();
            }
            catch (SQLException sqlEx) {
                pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
            }
        }
    }

    /**
     * Sets user limit of the custom channel the member who triggered the event is currently connected to the value of the slash command option "limit"
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    private void setChannelLimitCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();

        Member lMember = pEvent.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();

        if (lCurrentVoice == null)
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", pEvent.getUser())).queue();

        else if (!ownsCustomChannel(lMember.getIdLong(), lCurrentVoice.getIdLong(),pEvent.getGuild().getIdLong()))
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You don't own your current voice channel", pEvent.getUser())).queue();

        else
        {
            int lUserlimit = pEvent.getOption("limit").getAsInt();
            pEvent.getHook().editOriginalEmbeds(
                    utils.createEmbed(
                            AUTOCHANNEL_COLOR,
                            ":white_check_mark: Changed channel limit from **"+lCurrentVoice.getUserLimit()+"** to **"+ lUserlimit+"**",
                            pEvent.getUser()
                    )
            ).queue();
            lCurrentVoice.getManager().setUserLimit(lUserlimit).queue();
        }
    }

    /**
     * Sets name of the custom channel the member who triggered the event is currently connected to the value defined in the slash command option "name"
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    private void setChannelNameCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();

        Member lMember = pEvent.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();

        if (lCurrentVoice == null)
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", pEvent.getUser())).queue();

        else if (!ownsCustomChannel(lMember.getIdLong(), lCurrentVoice.getIdLong(),pEvent.getGuild().getIdLong()))
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You don't own your current voice channel", pEvent.getUser())).queue();

        else
        {
            String lNewName = pEvent.getOption("name").getAsString();
            pEvent.getHook().editOriginalEmbeds(
                    utils.createEmbed(
                            AUTOCHANNEL_COLOR,
                            ":white_check_mark: Changed channel name from **"+lCurrentVoice.getName()+"** to **"+ lNewName+"**",
                            pEvent.getUser()
                    )
            ).queue();
            lCurrentVoice.getManager().setName(lNewName).queue();
        }
    }

    /**
     * Changes to owner of the custom channel the member who triggered the event is currently connected to themself
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    private void claimChannelCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();

        Member lMember = pEvent.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();

        if (lCurrentVoice == null)
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", pEvent.getUser())).queue();

        else if (!isCustomChannel(lCurrentVoice))
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: This isn't a custom channel", pEvent.getUser())).queue();

        else
        {
            Member lVoiceOwner = getCustomChannelOwner(pEvent.getGuild(), lCurrentVoice.getIdLong());
            if (lVoiceOwner != null && lVoiceOwner.equals(lMember))
                pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You already own this voice channel", pEvent.getUser())).queue();

            else if (lVoiceOwner != null && lVoiceOwner.getVoiceState().getChannel() != null && lVoiceOwner.getVoiceState().getChannel().equals(lCurrentVoice))
                pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Channel owner is connected", pEvent.getUser())).queue();

            else
            {
                try {
                    utils.onExecute("UPDATE AutoChannel SET owner_id = ? WHERE guild_id = ? AND channel_id = ?", lMember.getIdLong(), pEvent.getGuild().getIdLong(), lCurrentVoice.getIdLong());
                    pEvent.getHook().editOriginalEmbeds(
                            utils.createEmbed(
                                    AUTOCHANNEL_COLOR,
                                    ":white_check_mark: Claimed "+lCurrentVoice.getAsMention(),
                                    pEvent.getUser()
                            )
                    ).queue();
                }
                catch (SQLException sqlEx) {
                    pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
                }
            }
        }
    }

    /**
     * Kicks the user defined in the slash command option "user" out of the current custom voice channel the user who triggered the event is connected to if they own it
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    private void kickChannelCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();

        Member lMember = pEvent.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();
        Member lMemberToKick = pEvent.getOption("user").getAsMember();

        if (lCurrentVoice == null)
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", pEvent.getUser())).queue();

        else if (!ownsCustomChannel(lMember.getIdLong(), lCurrentVoice.getIdLong(),pEvent.getGuild().getIdLong()))
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You don't own your current voice channel", pEvent.getUser())).queue();

        else if (lMemberToKick == null)
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: User not found", pEvent.getUser())).queue();

        else if (lMemberToKick.getVoiceState().getChannel() == null || !lMemberToKick.getVoiceState().getChannel().equals(lCurrentVoice))
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: User isn't connected to your voice channel", pEvent.getUser())).queue();

        else
        {
            pEvent.getGuild().kickVoiceMember(lMemberToKick).queue();
            pEvent.getHook().editOriginalEmbeds(
                    utils.createEmbed(
                            AUTOCHANNEL_COLOR,
                            ":white_check_mark: Kicked "+lMemberToKick.getAsMention()+" from "+lCurrentVoice.getAsMention(),
                            pEvent.getUser()
                    )
            ).queue();
        }
    }

    /**
     * Bans the user defined in the slash command option "user" out of the current custom voice channel the user who triggered the event is connected to if they own it
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    private void banChannelCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();

        Member lMember = pEvent.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();
        Member lMemberToBan = pEvent.getOption("user").getAsMember();

        if (lCurrentVoice == null)
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", pEvent.getUser())).queue();

        else if (!ownsCustomChannel(lMember.getIdLong(), lCurrentVoice.getIdLong(),pEvent.getGuild().getIdLong()))
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You don't own your current voice channel", pEvent.getUser())).queue();

        else if (lMemberToBan == null)
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Member not found", pEvent.getUser())).queue();

        else
        {

            lCurrentVoice.getManager().putPermissionOverride(lMemberToBan, null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
            if (lMemberToBan.getVoiceState().getChannel() != null && lMemberToBan.getVoiceState().getChannel().equals(lCurrentVoice))
                pEvent.getGuild().kickVoiceMember(lMemberToBan).queue();

            pEvent.getHook().editOriginalEmbeds(
                    utils.createEmbed(
                            AUTOCHANNEL_COLOR,
                            ":white_check_mark: Banned "+lMemberToBan.getAsMention()+" from "+lCurrentVoice.getAsMention(),
                            pEvent.getUser()
                    )
            ).queue();
        }
    }

    /**
     * Deletes all the registered custom channel in the database connected to the guild the event was triggered in
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    private void clearAutoChannelDatabaseCommand(SlashCommandInteractionEvent pEvent)
    {
        try
        {
            utils.onExecute("DELETE FROM AutoChannel WHERE guild_id = ?", pEvent.getGuild().getIdLong());
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(AUTOCHANNEL_COLOR, ":white_check_mark: Deleted all custom channel", pEvent.getUser())).queue();
        }
        catch (SQLException sqlEx) {
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
        }
    }

    // Other private methods

    /**
     *
     *
     * @param pChannelId
     * @param pGuild
     * */
    private Member getCustomChannelOwner(Guild pGuild, long pChannelId)
    {
        try
        {
            ResultSet lRs = utils.onQuery("SELECT owner_id FROM AutoChannel WHERE guild_id = ? AND channel_id = ?", pGuild.getIdLong(), pChannelId);
            lRs.next();

            return pGuild.getMemberById(lRs.getLong("owner_id"));
        }
        catch (SQLException sqlEx) { return null;}
    }

    /**
     *
     *
     * @param pMemberId
     * @param pChannelId
     * @param pGuildId
     * */
    private boolean ownsCustomChannel(long pMemberId, long pChannelId, long pGuildId)
    {
        try
        {
            ResultSet lRs = utils.onQuery("SELECT owner_id FROM AutoChannel WHERE guild_id = ? AND channel_id = ?", pGuildId, pChannelId);
            lRs.next();

            return pMemberId == lRs.getLong("owner_id");
        }
        catch (SQLException sqlEx) { return false;}
    }

    /**
     *
     *
     * @param pVoice
     * */
    private boolean isCustomChannel(VoiceChannel pVoice)
    {
        try
        {
            ResultSet lRs = utils.onQuery("SELECT channel_id FROM AutoChannel WHERE guild_id = ? AND channel_id = ?", pVoice.getGuild().getIdLong(), pVoice.getIdLong());
            lRs.next();

            return pVoice.getIdLong() == lRs.getLong("channel_id");
        }
        catch (SQLException sqlEx) { return false;}
    }

    /**
     *
     *
     * @param pGuild
     * */
    private VoiceChannel getAutoCreateChannelForGuild(Guild pGuild)
    {
        try
        {
            ResultSet lRs = utils.onQuery("SELECT auto_channel_id FROM GuildSettings WHERE guild_id = ?", pGuild.getIdLong());
            lRs.next();

            return pGuild.getVoiceChannelById(lRs.getLong("auto_channel_id"));
        }
        catch (SQLException sqlEx) { return null;}
    }

    // Getter

    public static Color getCategoryColor() {return AUTOCHANNEL_COLOR; }
}
