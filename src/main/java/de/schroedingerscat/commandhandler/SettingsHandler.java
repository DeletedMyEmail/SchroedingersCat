package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 1.0.0 | last edit: 25.09.2022
 * */
public class SettingsHandler extends ListenerAdapter {

    /** Default color for this category to be used for embeds */
    private static final Color SERVERSETTINGS_COLOR = new Color(252,229,159);

    private final Utils utils;

    public SettingsHandler(Utils pUtils) {
        this.utils = pUtils;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        switch (event.getName())
        {
            case "get_info" -> getInfoCommand(event);
        }
    }

    private void getInfoCommand(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();

        try
        {
            utils.insertGuildInSettingsIfNotExist(event.getGuild().getIdLong());
            ResultSet lRs = utils.onQuery("SELECT * FROM GuildSettings WHERE guild_id = ?", event.getGuild().getIdLong());
            lRs.next();

            TextChannel lWelcomeChannel = event.getGuild().getTextChannelById(lRs.getLong("welcome_channel_id"));
            String lWelcomeChannelStr = "None";
            if (lWelcomeChannel != null)
                lWelcomeChannelStr = lWelcomeChannel.getAsMention();

            TextChannel lLogChannel = event.getGuild().getTextChannelById(lRs.getLong("log_channel_id"));
            String lLogStr = "None";
            if (lLogChannel != null)
                lLogStr = lLogChannel.getAsMention();


            VoiceChannel lAutoChannel = event.getGuild().getVoiceChannelById(lRs.getLong("auto_channel_id"));
            String lAutoChannelStr = "None";
            if (lAutoChannel != null)
                lAutoChannelStr = lAutoChannel.getAsMention();

            Role lAutoRole = event.getGuild().getRoleById(lRs.getLong("auto_role_id"));
            String lAutoRoleStr = "None";
            if (lAutoRole != null)
                lAutoRoleStr = lAutoRole.getAsMention();

            String lScreeningStr = String.valueOf(lRs.getBoolean("screening"));

            String lWelcomeMessageStr = lRs.getString("welcome_message");
            if (lWelcomeMessageStr == null || lWelcomeMessageStr.isEmpty())
                lWelcomeMessageStr = "None";

            String[][] lFields = {
                    {"Welcome Channel", lWelcomeChannelStr},
                    {"Welcome Message", lWelcomeMessageStr},
                    {"Auto Role", lAutoRoleStr},
                    {"Auto-Channel", lAutoChannelStr},
                    {"Log Channel", lLogStr},
                    {"Membership Screening", lScreeningStr},
            };

            event.getHook().editOriginalEmbeds(
                    utils.createEmbed(SERVERSETTINGS_COLOR, "Server Info", "", lFields, true, null, null, null)).queue();
        }
        catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
            event.getHook().editOriginalEmbeds(
                    utils.createEmbed(Color.red, ":x: Database error occurred", event.getUser())).queue();
        }
    }

    public static Color getCategoryColor() {return SERVERSETTINGS_COLOR; }
}
