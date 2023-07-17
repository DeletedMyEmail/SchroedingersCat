package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 *
 * @author KaitoKunTatsu
 * @version 3.0.0 | last edit: 17.07.2023
 * */
public class AutoRoleHandler extends ListenerAdapter {

    private final Utils mUtils;

    public AutoRoleHandler(Utils pUtils) {
        this.mUtils = pUtils;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent pEvent) {
        if ("set_auto_role".equals(pEvent.getName())) {
            Utils.catchAndLogError(pEvent.getHook(), () -> {
                setAutoRole(pEvent);
            });
        }
    }

    @Override
    public void onGuildMemberUpdatePending(@Nonnull GuildMemberUpdatePendingEvent pEvent) {
        if (!pEvent.getNewPending())
            addRoleToMember(pEvent.getGuild(), pEvent.getMember(), true);

    }

    private void addRoleToMember(Guild pGuild, Member pMember, boolean pShouldScreeningBeEnabled) {
        try {
            ResultSet lRs = mUtils.onQuery("SELECT screening,auto_role_id FROM GuildSettings WHERE guild_id = ?", pGuild.getIdLong());
            lRs.next();

            if (lRs.getBoolean("screening") != pShouldScreeningBeEnabled) return;

            Role lAutoRole = pGuild.getRoleById(lRs.getLong("auto_role_id"));
            if (lAutoRole != null)
                pGuild.addRoleToMember(pMember, lAutoRole).queue();
        }
        catch (SQLException sqlEx) {
            Utils.sendToOwner(pGuild.getJDA(), sqlEx.getMessage());
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        addRoleToMember(event.getGuild(), event.getMember(), false);
    }

    private void setAutoRole(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();
        if (mUtils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        Role lRole = pEvent.getOption("role").getAsRole();
        boolean lScreeningEnabled = pEvent.getOption("screening").getAsBoolean();

        try {
            mUtils.onExecute("UPDATE GuildSettings SET auto_role_id = ? , screening = ? WHERE guild_id=?",lRole.getIdLong(), lScreeningEnabled, pEvent.getGuild().getIdLong());
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(SettingsHandler.SERVERSETTINGS_COLOR, ":white_check_mark: Successfully assigned "+lRole.getAsMention()+" as auto role. Each new member will receive it.", pEvent.getUser())).queue();
        }
        catch (SQLException sqlEx) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
        }
    }
}
