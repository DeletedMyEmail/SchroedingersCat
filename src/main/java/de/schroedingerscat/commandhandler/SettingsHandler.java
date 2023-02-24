package de.schroedingerscat.commandhandler;

import de.schroedingerscat.BotApplication;
import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 26.09.2022
 * */
public class SettingsHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    private static final Color SERVERSETTINGS_COLOR = new Color(243, 214, 93);

    private final BotApplication botApplication;
    private final Utils utils;

    public SettingsHandler(Utils pUtils, BotApplication pBotApplication) {
        this.utils = pUtils;
        this.botApplication = pBotApplication;
    }

    // Events

    @Override
    public void onGuildJoin(GuildJoinEvent pEvent) {
        try {
            utils.insertGuildIfAbsent(pEvent.getGuild().getIdLong());
        }
        catch (SQLException sqlEx) {
            System.out.println("Failed to insert guild ("+pEvent.getGuild().getId()+")into database");
        }
        botApplication.updateBotListApi();
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent pEvent)
    {
        try {
            ResultSet lRs = utils.onQuery("SELECT welcome_channel_id, welcome_message FROM GuildSettings WHERE guild_id = ?", pEvent.getGuild().getIdLong());
            lRs.next();

            String lMessage = lRs.getString("welcome_message");
            TextChannel lChannel = pEvent.getGuild().getTextChannelById(lRs.getLong("welcome_channel_id"));
            if (lChannel != null)
                lChannel.
                        sendMessage(
                            pEvent.getUser().getAsMention()
                        )
                        .addEmbeds(
                                Utils.createEmbed(
                                        SERVERSETTINGS_COLOR,
                                        "",
                                        lMessage,
                                        null,
                                        false,
                                        pEvent.getUser(),
                                        null,
                                        OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE).replace("-",".")
                                        )
                        ).queue();
        }
        catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent pEvent)
    {
        try
        {
            switch (pEvent.getName())
            {
                case "get_info" -> getInfoCommand(pEvent);
                case "set_editor_role" -> setRoleCommand(pEvent, "editor");
                case "set_moderator_role" -> setRoleCommand(pEvent, "moderator");
                case "set_welcome" -> setWelcomeCommand(pEvent);
                case "reset_settings" -> resetSettingsCommand(pEvent);
            }
        }
        catch (NumberFormatException numEx) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You entered an invalid number", pEvent.getUser())).queue();
        }
        catch (SQLException sqlEx)
        {
            sqlEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
        }
        catch (NullPointerException nullEx) {
            nullEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Invalid argument. Make sure you selected a valid text channel, message id, role and emoji", pEvent.getUser())).queue();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Unknown error occured", pEvent.getUser())).queue();
        }
    }

    // Slash Commands

    /**
     *
     *
     * */
    private void getInfoCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();

        ResultSet lRs = utils.onQuery("SELECT * FROM GuildSettings WHERE guild_id = ?", pEvent.getGuild().getIdLong());
        lRs.next();

        TextChannel lWelcomeChannel = pEvent.getGuild().getTextChannelById(lRs.getLong("welcome_channel_id"));
        String lWelcomeChannelStr = "None";
        if (lWelcomeChannel != null)
            lWelcomeChannelStr = lWelcomeChannel.getAsMention();

        TextChannel lLogChannel = pEvent.getGuild().getTextChannelById(lRs.getLong("log_channel_id"));
        String lLogStr = "None";
        if (lLogChannel != null)
            lLogStr = lLogChannel.getAsMention();

        VoiceChannel lAutoChannel = pEvent.getGuild().getVoiceChannelById(lRs.getLong("create_channel_id"));
        String lAutoChannelStr = "None";
        if (lAutoChannel != null)
            lAutoChannelStr = lAutoChannel.getAsMention();

        Role lAutoRole = pEvent.getGuild().getRoleById(lRs.getLong("auto_role_id"));
        String lAutoRoleStr = "None";
        if (lAutoRole != null)
            lAutoRoleStr = lAutoRole.getAsMention();

        Role lModRole = pEvent.getGuild().getRoleById(lRs.getLong("moderator_role_id"));
        String lModRoleStr = "None";
        if (lModRole != null)
            lModRoleStr = lModRole.getAsMention();

        Role lEditorRole = pEvent.getGuild().getRoleById(lRs.getLong("editor_role_id"));
        String lEditorRoleStr = "None";
        if (lEditorRole != null)
            lEditorRoleStr = lEditorRole.getAsMention();

        String lWelcomeMessageStr = lRs.getString("welcome_message");
        if (lWelcomeMessageStr == null || lWelcomeMessageStr.isEmpty())
            lWelcomeMessageStr = "None";

        String lScreeningStr = String.valueOf(lRs.getBoolean("screening"));

        String[][] lFields = {
                {"Welcome Message", lWelcomeMessageStr},
                {"Welcome Channel", lWelcomeChannelStr},
                {"Create Custom Channel", lAutoChannelStr},
                {"Log Channel", lLogStr},
                {"Auto Role", lAutoRoleStr},
                {"Editor Role", lEditorRoleStr},
                {"Moderator Role", lModRoleStr},
                {"Rules Screening", lScreeningStr},
                {"Place Holder", "Holding Place"}
        };

        pEvent.getHook().editOriginalEmbeds(
                Utils.createEmbed(SERVERSETTINGS_COLOR, ":wrench: Server Info", "", lFields, true , null, "https://discord.com/api/oauth2/authorize?client_id=872475386620026971&permissions=1101960473814&scope=bot%20applications.commands", null)).queue();

    }

    /**
     *
     *
     * */
    private void setRoleCommand(SlashCommandInteractionEvent pEvent, String pRole) throws SQLException {
        pEvent.deferReply().queue();
        if (utils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        Role lRole = pEvent.getOption("role").getAsRole();

        utils.onExecute("UPDATE GuildSettings SET "+pRole+"_role_id = ? WHERE guild_id = ?", lRole.getIdLong(), pEvent.getGuild().getIdLong());
        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(SERVERSETTINGS_COLOR, ":white_check_mark: Set the "+pRole+" role to "+lRole.getAsMention(), pEvent.getUser())).queue();
    }

    private void setWelcomeCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();
        if (utils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        Channel lChannel = pEvent.getOption("channel").getAsChannel();
        if (lChannel.getType() != ChannelType.TEXT) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Please select a proper text channel", pEvent.getUser())).queue();
            return;
        }

        String lMessage = pEvent.getOption("message").getAsString();

        utils.onExecute("UPDATE GuildSettings SET welcome_channel_id = ?, welcome_message = ? WHERE guild_id = ?", lChannel.getIdLong(), lMessage, pEvent.getGuild().getIdLong());
        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(SERVERSETTINGS_COLOR, ":white_check_mark: Set the welcome channel to "+lChannel.getAsMention()+" and the message: **"+lMessage+"**", pEvent.getUser())).queue();
    }

    /**
     *
     *
     * */
    private void resetSettingsCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();
        if (utils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        utils.onExecute(
                "UPDATE GuildSettings SET" +
                        " editor_role_id = null, moderator_role_id = null, welcome_message = null, welcome_channel_id = null , log_channel_id = null, screening = false, create_channel_id = null, auto_role_id = null" +
                        " WHERE guild_id = ?",
                pEvent.getGuild().getIdLong());
        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(SERVERSETTINGS_COLOR, ":white_check_mark: Server settings reset", pEvent.getUser())).queue();
    }

    public static Color getCategoryColor() {return SERVERSETTINGS_COLOR; }
}
