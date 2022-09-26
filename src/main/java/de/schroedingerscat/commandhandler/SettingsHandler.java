package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 26.09.2022
 * */
public class SettingsHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    private static final Color SERVERSETTINGS_COLOR = new Color(252,229,159);

    private final Utils utils;

    public SettingsHandler(Utils pUtils) {
        this.utils = pUtils;
    }

    // Events

    @Override
    public void onGuildJoin(GuildJoinEvent pEvent)
    {
        try {
            utils.onExecute("INSERT INTO GuildSettings (guild_id) VALUES (?)", pEvent.getGuild().getIdLong());
        }
        catch (SQLException ignored) {}
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent pEvent)
    {
        try
        {
            switch (pEvent.getName())
            {
                case "get_info" -> getInfoCommand(pEvent);
                case "set_editor_role" -> setRole(pEvent, "editor");
                case "set_moderator_role" -> setRole(pEvent, "moderator");
            }
        }
        catch (SQLException sqlEx)
        {
            sqlEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
        }
    }

    // Slash Commands

    /**
     *
     *
     * */
    private void getInfoCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();

        try
        {
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

            VoiceChannel lAutoChannel = pEvent.getGuild().getVoiceChannelById(lRs.getLong("auto_channel_id"));
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
                    utils.createEmbed(SERVERSETTINGS_COLOR, ":wrench: Server Info", "", lFields, true , null, "https://discord.com/api/oauth2/authorize?client_id=872475386620026971&permissions=1101960473814&scope=bot%20applications.commands", null)).queue();
        }
        catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(
                    utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
        }
    }

    /**
     *
     *
     * */
    private void setRole(SlashCommandInteractionEvent pEvent, String pRole) throws SQLException
    {
        Role lRole = pEvent.getOption("role").getAsRole();

        utils.onExecute("UPDATE GuildSettings SET "+pRole+"_role_id = ? WHERE guild_id = ?", lRole.getIdLong(), pEvent.getGuild().getIdLong());
        pEvent.replyEmbeds(utils.createEmbed(SERVERSETTINGS_COLOR, ":white_check_mark: Set the "+pRole+" role to "+lRole.getAsMention(), pEvent.getUser())).queue();
    }


    public static Color getCategoryColor() {return SERVERSETTINGS_COLOR; }
}
