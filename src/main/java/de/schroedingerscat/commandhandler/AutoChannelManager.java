package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;

public class AutoChannelManager extends ListenerAdapter {

    private static final Color AUTOCHANNEL_COLOR = new Color(25,196,234);

    private final Utils utils;

    public AutoChannelManager(Utils pUtils) {
        this.utils = pUtils;
    }

    // Events

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName())
        {
            case "set_auto_channel" -> setAutoChannelCommand(event);
            case "vcname" -> setChannelNameCommand(event);
            case "vclimit" -> setChannelLimitCommand(event);
            case "vckick" -> kickChannelCommand(event);
            case "vcban" -> banChannelCommand(event);
        }
    }

    @Override
    public void onGuildVoiceJoin(@Nonnull GuildVoiceJoinEvent event)
    {
        VoiceChannel lCreateChannel = getAutoCreateChannelForGuild(event.getGuild());
        VoiceChannel lJoinedChannel = (VoiceChannel) event.getChannelJoined();
        Member lMember = event.getMember();

        // Create custom voice if joined create channel
        if (lCreateChannel != null && lCreateChannel.equals(lJoinedChannel))
        {
            event.getGuild().createVoiceChannel(lMember.getEffectiveName()).queue(channel ->
                {
                    try
                    {
                        utils.onExecute(
                                "INSERT INTO AutoChannel VALUES(?,?,?)",
                                event.getGuild().getIdLong(),
                                lMember.getIdLong(),
                                channel.getIdLong());
                    }
                    catch(SQLException sqlException)
                    {
                        sqlException.printStackTrace();
                        return;
                    }
                    event.getGuild().moveVoiceMember(lMember, channel).queue();
                }
            );
        }
    }

    @Override
    public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event)
    {
        VoiceChannel lVoiceLeft = (VoiceChannel) event.getChannelLeft();
        if (isCustomChannel(lVoiceLeft) && lVoiceLeft.getMembers().size() < 1)
        {
            try
            {
                utils.onExecute("DELETE FROM AutoChannel WHERE guild_id = ? AND channel_id = ?", event.getGuild(), lVoiceLeft.getIdLong());
                lVoiceLeft.delete().queue();
            }
            catch (SQLException sqlEx) { sqlEx.printStackTrace();}
        }
    }

    // Slash Commands

    private void setAutoChannelCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        Channel lChannel = event.getOption("channel").getAsChannel();
        if (lChannel.getType() != ChannelType.VOICE)
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Select a voice channel", event.getUser())).queue();
        else
        {
            try
            {
                utils.insertGuildInSettingsIfNotExist(event.getGuild().getIdLong());
                utils.onExecute("UPDATE GuildSettings SET auto_channel_id = ? WHERE guild_id=?",lChannel.getIdLong(), event.getGuild().getIdLong());
                event.getHook().editOriginalEmbeds(utils.createEmbed(AUTOCHANNEL_COLOR, ":white_check_mark: Successfully assigned "+lChannel.getAsMention()+" as auto channel", event.getUser())).queue();
            }
            catch (SQLException sqlEx) {
                event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Database error occurred", event.getUser())).queue();
            }
        }
    }

    private void setChannelLimitCommand(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();

        Member lMember = event.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();

        if (lCurrentVoice == null)
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", event.getUser())).queue();

        else if (!ownsCustomChannel(lMember.getIdLong(), lCurrentVoice.getIdLong(),event.getGuild().getIdLong()))
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You don't own your current voice channel", event.getUser())).queue();

        else
        {
            int lUserlimit = event.getOption("limit").getAsInt();
            event.getHook().editOriginalEmbeds(
                    utils.createEmbed(
                            AUTOCHANNEL_COLOR,
                            ":white_check_mark: Changed channel limit from **"+lCurrentVoice.getUserLimit()+"** to **"+ lUserlimit+"**",
                            event.getUser()
                    )
            ).queue();
            lCurrentVoice.getManager().setUserLimit(lUserlimit).queue();
        }
    }

    private void setChannelNameCommand(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();

        Member lMember = event.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();

        if (lCurrentVoice == null)
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", event.getUser())).queue();

        else if (!ownsCustomChannel(lMember.getIdLong(), lCurrentVoice.getIdLong(),event.getGuild().getIdLong()))
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You don't own your current voice channel", event.getUser())).queue();

        else
        {
            String lNewName = event.getOption("name").getAsString();
            event.getHook().editOriginalEmbeds(
                    utils.createEmbed(
                            AUTOCHANNEL_COLOR,
                            ":white_check_mark: Changed channel name from **"+lCurrentVoice.getName()+"** to **"+ lNewName+"**",
                            event.getUser()
                    )
            ).queue();
            lCurrentVoice.getManager().setName(lNewName).queue();
        }
    }

    private void claimChannelCommand(SlashCommandInteractionEvent event) {}

    private void kickChannelCommand(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();

        Member lMember = event.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();
        Member lMemberToKick = event.getOption("user").getAsMember();

        if (lCurrentVoice == null)
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", event.getUser())).queue();

        else if (!ownsCustomChannel(lMember.getIdLong(), lCurrentVoice.getIdLong(),event.getGuild().getIdLong()))
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You don't own your current voice channel", event.getUser())).queue();

        else if (lMemberToKick == null)
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: User not found", event.getUser())).queue();

        else if (lMemberToKick.getVoiceState().getChannel() == null || !lMemberToKick.getVoiceState().getChannel().equals(lCurrentVoice))
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: User isn't connected to your voice channel", event.getUser())).queue();

        else
        {
            event.getGuild().kickVoiceMember(lMemberToKick).queue();
            event.getHook().editOriginalEmbeds(
                    utils.createEmbed(
                            AUTOCHANNEL_COLOR,
                            ":white_check_mark: Kicked "+lMemberToKick.getAsMention()+" from "+lCurrentVoice.getAsMention(),
                            event.getUser()
                    )
            ).queue();
        }
    }

    private void banChannelCommand(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();

        Member lMember = event.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();
        Member lMemberToBan = event.getOption("user").getAsMember();

        if (lCurrentVoice == null)
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", event.getUser())).queue();

        else if (!ownsCustomChannel(lMember.getIdLong(), lCurrentVoice.getIdLong(),event.getGuild().getIdLong()))
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You don't own your current voice channel", event.getUser())).queue();

        else if (lMemberToBan == null)
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Member not found", event.getUser())).queue();

        else
        {

            lCurrentVoice.getManager().putPermissionOverride(lMemberToBan, null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
            if (lMemberToBan.getVoiceState().getChannel() != null && lMemberToBan.getVoiceState().getChannel().equals(lCurrentVoice))
                event.getGuild().kickVoiceMember(lMemberToBan).queue();

            event.getHook().editOriginalEmbeds(
                    utils.createEmbed(
                            AUTOCHANNEL_COLOR,
                            ":white_check_mark: Banned "+lMemberToBan.getAsMention()+" from "+lCurrentVoice.getAsMention(),
                            event.getUser()
                    )
            ).queue();
        }
    }

    private void clearAutoChannelDatabaseCommand(SlashCommandInteractionEvent event) {}

    // Other private methods

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
