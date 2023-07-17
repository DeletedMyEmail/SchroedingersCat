package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;

/**
 * Handles slash commands related to automated custom channels
 *
 * @author KaitoKunTatsu
 * @version 3.0.0 | last edit: 17.07.2023
 **/
public class AutoChannelHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    public static final Color AUTOCHANNEL_COLOR = new Color(37, 206, 97);

    private final Utils mUtils;

    public AutoChannelHandler(Utils pUtils) {
        this.mUtils = pUtils;
    }

    @Override
    public void onUserContextInteraction(UserContextInteractionEvent pEvent) {
        Utils.catchAndLogError(pEvent.getHook(), () -> {
            switch (pEvent.getName()) {
                case "kick custom channel" -> processKickCustomRequest(pEvent, pEvent.getMember(), pEvent.getTargetMember());
                case "ban custom channel" -> processBanCustomRequest(pEvent, pEvent.getMember(), pEvent.getTargetMember());
            }
        });
    }

    @Override
    /**
     *  Redirects the event to method handling it if the slash command belongs to this category
     *
     *  @param pEvent   Event triggered by a user using a slash command
     * */
    public void onSlashCommandInteraction(SlashCommandInteractionEvent pEvent) {
        Utils.catchAndLogError(pEvent.getHook(), () -> {
            switch (pEvent.getName()) {
                case "set_create_channel" -> setCreateChannel(pEvent);
                case "vcname" -> setChannelNameCommand(pEvent);
                case "vclimit" -> setChannelLimitCommand(pEvent);
                case "vckick" -> processKickCustomRequest(pEvent, pEvent.getMember(), pEvent.getOption("user").getAsMember());
                case "vcban" -> processBanCustomRequest(pEvent, pEvent.getMember(), pEvent.getOption("user").getAsMember());
                case "claim" -> claimChannelCommand(pEvent);
            }
        });
    }

    @Override
    /**
     *  If a member connected to the create-channel for automated custom channels,
     *  a new voice will be created, added to the database and the user moved
     *
     *  @param pEvent   Event triggered by a user joining/leaving a voice channel
     * */
    public void onGuildVoiceUpdate(@Nonnull GuildVoiceUpdateEvent pEvent) {
        if (pEvent.getChannelJoined() == null) {
            deleteCustomChannel((VoiceChannel) pEvent.getChannelLeft());
        }
        else {
            createCustomChannel((VoiceChannel) pEvent.getChannelJoined(), pEvent.getMember());
        }
    }

    // Slash Commands

    /**
     * Sets the create-channel for automated custom channel to the one defined in the slash command option "channel"
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    private void setCreateChannel(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();
        if (mUtils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        Channel lChannel = pEvent.getOption("channel").getAsChannel();
        if (lChannel.getType() != ChannelType.VOICE) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Select a voice channel", pEvent.getUser())).queue();
        }
        else {
            try {
                mUtils.onExecute("UPDATE GuildSettings SET create_channel_id = ? WHERE guild_id=?",lChannel.getIdLong(), pEvent.getGuild().getIdLong());
                pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(AUTOCHANNEL_COLOR, ":white_check_mark: Successfully assigned "+lChannel.getAsMention()+" as create channel", pEvent.getUser())).queue();
            }
            catch (SQLException sqlEx) {
                pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
            }
        }
    }

    /**
     * Sets user limit of the custom channel the member who triggered the event is currently connected to the value of the slash command option "limit"
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    private void setChannelLimitCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();

        Member lMember = pEvent.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();

        if (lCurrentVoice == null) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", pEvent.getUser())).queue();
        }
        else if (!ownsCustomChannel(lMember.getIdLong(), lCurrentVoice.getIdLong(), pEvent.getGuild().getIdLong())) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You don't own your current voice channel", pEvent.getUser())).queue();
        }
        else {
            int lUserlimit = pEvent.getOption("limit").getAsInt();
            pEvent.getHook().editOriginalEmbeds(
                    Utils.createEmbed(
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
    private void setChannelNameCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();

        Member lMember = pEvent.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();

        if (lCurrentVoice == null) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", pEvent.getUser())).queue();
        }
        else if (!ownsCustomChannel(lMember.getIdLong(), lCurrentVoice.getIdLong(),pEvent.getGuild().getIdLong())) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You don't own your current voice channel", pEvent.getUser())).queue();
        }
        else {
            String lNewName = pEvent.getOption("name").getAsString();
            pEvent.getHook().editOriginalEmbeds(
                    Utils.createEmbed(
                            AUTOCHANNEL_COLOR,
                            ":white_check_mark: Changed channel name from **"+lCurrentVoice.getName()+"** to **"+ lNewName+"**",
                            pEvent.getUser()
                    )
            ).queue();
            lCurrentVoice.getManager().setName(lNewName).queue();
        }
    }

    /**
     * Changes to owner of the custom channel the member who triggered the event is currently connected to themselves
     *
     * @param pEvent    Event triggered by a user using a slash command
     * */
    private void claimChannelCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();

        Member lMember = pEvent.getMember();
        VoiceChannel lCurrentVoice = (VoiceChannel) lMember.getVoiceState().getChannel();

        if (lCurrentVoice == null) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", pEvent.getUser())).queue();
        }
        else if (!isCustomChannel(lCurrentVoice)) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: This isn't a custom channel", pEvent.getUser())).queue();
        }
        else {
            Member lVoiceOwner = getCustomChannelOwner(pEvent.getGuild(), lCurrentVoice.getIdLong());
            if (lVoiceOwner != null && lVoiceOwner.equals(lMember))
                pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You already own this voice channel", pEvent.getUser())).queue();

            else if (lVoiceOwner != null && lVoiceOwner.getVoiceState().getChannel() != null && lVoiceOwner.getVoiceState().getChannel().equals(lCurrentVoice))
                pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Channel owner is connected", pEvent.getUser())).queue();

            else
            {
                try {
                    mUtils.onExecute("UPDATE AutoChannel SET owner_id = ? WHERE guild_id = ? AND channel_id = ?", lMember.getIdLong(), pEvent.getGuild().getIdLong(), lCurrentVoice.getIdLong());
                    pEvent.getHook().editOriginalEmbeds(
                            Utils.createEmbed(
                                    AUTOCHANNEL_COLOR,
                                    ":white_check_mark: Claimed "+lCurrentVoice.getAsMention(),
                                    pEvent.getUser()
                            )
                    ).queue();
                }
                catch (SQLException sqlEx) {
                    pEvent.getHook().editOriginalEmbeds(mUtils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
                }
            }
        }
    }

    private void processKickCustomRequest(InteractionHook pHook, Member pMember, Member pMemberToKick) throws SQLException {
        VoiceChannel lCurrentVoice = (VoiceChannel) pMember.getVoiceState().getChannel();

        if (lCurrentVoice == null) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", pMember.getUser())).queue();
        }
        else if (!ownsCustomChannel(pMember.getIdLong(), lCurrentVoice.getIdLong(),pMember.getGuild().getIdLong())) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You don't own your current voice channel", pMember.getUser())).queue();
        }
        else if (pMemberToKick == null) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: User not found", pMember.getUser())).queue();
        }
        else if (pMemberToKick.equals(pMember)) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You can't kick yourself", pMember.getUser())).queue();
        }
        else if (pMemberToKick.getVoiceState().getChannel() == null || !pMemberToKick.getVoiceState().getChannel().equals(lCurrentVoice)) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: User isn't connected to your voice channel", pMember.getUser())).queue();
        }
        else {
            pMember.getGuild().kickVoiceMember(pMemberToKick).queue();
            pHook.editOriginalEmbeds(
                    mUtils.createEmbed(
                            AUTOCHANNEL_COLOR,
                            ":white_check_mark: Kicked "+pMemberToKick.getAsMention()+" from "+lCurrentVoice.getAsMention(),
                            pMember.getUser()
                    )
            ).queue();
        }
    }

    private void processKickCustomRequest(GenericCommandInteractionEvent pEvent, Member pMember, Member pMemberToKick) throws SQLException {
        pEvent.deferReply().queue();
        processKickCustomRequest(pEvent.getHook(), pMember, pMemberToKick);
    }

    private void processBanCustomRequest(InteractionHook pHook, Member pMember, Member pMemberToBan) throws SQLException {
        VoiceChannel lCurrentVoice = (VoiceChannel) pMember.getVoiceState().getChannel();
        if (lCurrentVoice == null) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You're not connected to a voice channel", pMember.getUser())).queue();
        }
        else if (!ownsCustomChannel(pMember.getIdLong(), lCurrentVoice.getIdLong(),pMember.getGuild().getIdLong())) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You don't own your current voice channel", pMember.getUser())).queue();
        }
        else if (pMemberToBan == null) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: User not found", pMember.getUser())).queue();
        }
        else if (pMemberToBan.equals(pMember)) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You can't ban yourself", pMember.getUser())).queue();
        }
        else if (pMemberToBan.getVoiceState().getChannel() == null || !pMemberToBan.getVoiceState().getChannel().equals(lCurrentVoice)) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: User isn't connected to your voice channel", pMember.getUser())).queue();
        }
        else {
            lCurrentVoice.getManager().putPermissionOverride(pMemberToBan, null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
            if (pMemberToBan.getVoiceState().getChannel() != null && pMemberToBan.getVoiceState().getChannel().equals(lCurrentVoice)) {
                pMember.getGuild().kickVoiceMember(pMemberToBan).queue();
            }
            pHook.editOriginalEmbeds(Utils.createEmbed(AUTOCHANNEL_COLOR,":white_check_mark: Banned "+pMemberToBan.getAsMention()+" from "+lCurrentVoice.getAsMention(), pMember.getUser())).queue();
        }
    }

    private void processBanCustomRequest(GenericCommandInteractionEvent pEvent, Member pMember, Member pMemberToKick) throws SQLException {
        pEvent.deferReply().queue();
        processBanCustomRequest(pEvent.getHook(), pMember, pMemberToKick);
    }

    private Member getCustomChannelOwner(Guild pGuild, long pChannelId) throws SQLException {
        ResultSet lRs = mUtils.onQuery("SELECT owner_id FROM AutoChannel WHERE guild_id = ? AND channel_id = ?", pGuild.getIdLong(), pChannelId);
        lRs.next();

        return pGuild.getMemberById(lRs.getLong("owner_id"));
    }

    private boolean ownsCustomChannel(long pMemberId, long pChannelId, long pGuildId) throws SQLException {
        ResultSet lRs = mUtils.onQuery("SELECT owner_id FROM AutoChannel WHERE guild_id = ? AND channel_id = ?", pGuildId, pChannelId);
        lRs.next();

        return pMemberId == lRs.getLong("owner_id");
    }

    private boolean isCustomChannel(VoiceChannel pVoice) throws SQLException {
        ResultSet lRs = mUtils.onQuery("SELECT channel_id FROM AutoChannel WHERE guild_id = ? AND channel_id = ?", pVoice.getGuild().getIdLong(), pVoice.getIdLong());
        lRs.next();

        return pVoice.getIdLong() == lRs.getLong("channel_id");
    }

    private VoiceChannel getAutoCreateChannelForGuild(Guild pGuild) throws SQLException {
        ResultSet lRs = mUtils.onQuery("SELECT create_channel_id FROM GuildSettings WHERE guild_id = ?", pGuild.getIdLong());
        lRs.next();

        return pGuild.getVoiceChannelById(lRs.getLong("create_channel_id"));
    }


    private void deleteCustomChannel(VoiceChannel pLeftVoiceChannel) {
        Utils.catchAndLogError(pLeftVoiceChannel.getJDA(), () -> {
            if (isCustomChannel(pLeftVoiceChannel) && pLeftVoiceChannel.getMembers().size() < 1) {
                mUtils.onExecute("DELETE FROM AutoChannel WHERE guild_id = ? AND channel_id = ?", pLeftVoiceChannel.getGuild().getIdLong(), pLeftVoiceChannel.getIdLong());
                pLeftVoiceChannel.delete().queue();
            }
        });
    }

    private void createCustomChannel(VoiceChannel pJoinedChannel, Member pMember) {
        Utils.catchAndLogError(pJoinedChannel.getJDA(), () -> {
            VoiceChannel lCreateChannel = getAutoCreateChannelForGuild(pJoinedChannel.getGuild());
            if (lCreateChannel != null && lCreateChannel.equals(pJoinedChannel)) {
                pJoinedChannel.getGuild().createVoiceChannel(pMember.getEffectiveName(), lCreateChannel.getParentCategory()).setPosition(lCreateChannel.getPosition()+1).queue(channel -> {
                    Utils.catchAndLogError(channel.getJDA(), () -> {
                        mUtils.onExecute("INSERT INTO AutoChannel VALUES(?,?,?)", pJoinedChannel.getGuild().getIdLong(), pMember.getIdLong(), channel.getIdLong());
                        pJoinedChannel.getGuild().moveVoiceMember(pMember, channel).queue();
                    });
                });
            }
        });
    }
}
