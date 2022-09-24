package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AutoRoleHandler extends ListenerAdapter {

    private final Utils utils;

    public AutoRoleHandler(Utils pUtils) {
        this.utils = pUtils;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        if ("set_auto_role".equals(event.getName()))
            setAutoRole(event);
    }

    @Override
    public void onGuildMemberUpdatePending(@Nonnull GuildMemberUpdatePendingEvent event)
    {
        System.out.println(event.getNewPending() + " yeet " + event.getOldPending());
        if (!event.getNewPending())
            addRoleToMember(event.getGuild(), event.getMember(), true);

    }

    private void addRoleToMember(Guild pGuild, Member pMember, boolean pShouldScreeningBeEnabled)
    {
        try {
            ResultSet lRs = utils.onQuery("SELECT screening,auto_role_id FROM GuildSettings WHERE guild_id = ?", pGuild.getIdLong());
            lRs.next();

            if (lRs.getBoolean("screening") != pShouldScreeningBeEnabled) return;

            Role lAutoRole = pGuild.getRoleById(lRs.getLong("auto_role_id"));
            if (lAutoRole != null)
                pGuild.addRoleToMember(pMember, lAutoRole).queue();
        }
        catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event)
    {
        addRoleToMember(event.getGuild(), event.getMember(), false);
    }

    private void setAutoRole(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();

        Role lRole = event.getOption("role").getAsRole();
        boolean lScreeningEnabled = event.getOption("screening").getAsBoolean();

        try
        {
            utils.insertGuildInSettingsIfNotExist(event.getGuild().getIdLong());
            utils.onExecute("UPDATE GuildSettings SET auto_role_id = ? , screening = ? WHERE guild_id=?",lRole.getIdLong(), lScreeningEnabled, event.getGuild().getIdLong());
            event.getHook().editOriginalEmbeds(utils.createEmbed(SettingsHandler.getCategoryColor(), ":white_check_mark: Successfully assigned "+lRole.getAsMention()+" as auto role. Each new member will receive it.", event.getUser())).queue();
        }
        catch (SQLException sqlEx) {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Database error occurred", event.getUser())).queue();
        }
    }
}
