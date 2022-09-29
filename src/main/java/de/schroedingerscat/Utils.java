package de.schroedingerscat;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.*;

/**
 *
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 1.0.0 | last edit: 22.09.2022
 * */
public class Utils {

    private static Connection conn;

    public Utils(String pDatabasePath) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:"+pDatabasePath);
        conn.setAutoCommit(false);
    }

    public void createTables() throws SQLException
    {
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS 'Economy' (
                	'guild_id' INTEGER,
                	'user_id' INTEGER,
                	'bank' INTEGER,
                	'cash' INTEGER,
                	PRIMARY KEY('guild_id','user_id')
                );
                    """).executeUpdate();

            conn.prepareStatement("""
                 CREATE TABLE IF NOT EXISTS 'GuildSettings' (
                    'guild_id' INTEGER,
                    'welcome_channel_id' INTEGER,
                    'auto_role_id' INTEGER,
                    'create_channel_id' INTEGER,
                    'welcome_message' TEXT,
                    'screening'	INTEGER,
                    'log_channel_id' INTEGER,
                    'editor_role_id' INTEGER,
                    'moderator_role_id' INTEGER,
                    PRIMARY KEY('guild_id')
                )
                            """
            ).executeUpdate();

            conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS 'ReactionRole' (
                    	'guild_id' INTEGER,
                    	'message_id' INTEGER,
                    	'emoji' TEXT,
                    	'channel_id' INTEGER,
                    	'role_id' INTEGER,
                    	PRIMARY KEY('guild_id','emoji','message_id')
                    );
                    """).executeUpdate();

            conn.prepareStatement("CREATE TABLE IF NOT EXISTS 'IncomeRole' ('guild_id' INTEGER, 'role_id' INTEGER, 'income' INTEGER)").executeUpdate();

            conn.prepareStatement("CREATE TABLE IF NOT EXISTS 'AutoChannel' ('guild_id' INTEGER, 'owner_id' INTEGER, 'channel_id' INTEGER)").executeUpdate();

            conn.commit();
    }

    public MessageEmbed createEmbed(@NotNull Color pColor, @NotNull String pTitle, @NotNull String pDescription,
                                    String[][] pFields, boolean inline, User pAuthor, String pImageUrl, String pFooter)
    {
        EmbedBuilder builder = new EmbedBuilder();

        if (!pTitle.isEmpty()) builder.setTitle(pTitle);
        if (!pDescription.isEmpty()) builder.setDescription(pDescription);
        if (pAuthor != null) builder.setAuthor(pAuthor.getAsTag(), null, pAuthor.getAvatarUrl());
        if (pImageUrl != null) builder.setImage(pImageUrl);
        if (pFooter != null) builder.setFooter(pFooter);

        if (pFields != null)
            for (String[] field : pFields)
            {
                builder.addField(field[0], field[1], inline);
            }

        builder.setColor(pColor);
        return builder.build();
    }

    public MessageEmbed createEmbed(@NotNull Color pColor, @NotNull String pDescription, User pAuthor)
    {
        return createEmbed(pColor, "", pDescription, null, false, pAuthor, null, null);
    }

    public boolean authorizeMember(Member pMember, String pPermission) throws SQLException
    {
        if (pMember.hasPermission(Permission.ADMINISTRATOR)) return true;
        if (pPermission.equals("admin")) return false;

        ResultSet lRs = onQuery("SELECT "+pPermission.toLowerCase()+"_role_id FROM GuildSettings WHERE guild_id = ?", pMember.getGuild().getIdLong());
        lRs.next();

        Role lRoleNeeded = pMember.getGuild().getRoleById(lRs.getLong(pPermission+"_role_id"));
        return lRoleNeeded != null && pMember.getRoles().contains(lRoleNeeded);
    }

    public boolean memberNotAuthorized(Member pMember, String pPermission, InteractionHook pEventHook) throws SQLException
    {
        if (authorizeMember(pMember, pPermission)) return false;
        pEventHook.editOriginalEmbeds(createEmbed(Color.red, ":x: You don't have the permissions to use "+pPermission+" commands", pMember.getUser())).queue();
        return true;
    }

    public boolean memberNotAuthorized(Member pMember, String pPermission, SlashCommandInteractionEvent pEvent) throws SQLException
    {
        if (authorizeMember(pMember, pPermission)) return false;
        pEvent.replyEmbeds(createEmbed(Color.red, ":x: You don't have the permissions to use "+pPermission+" commands", pMember.getUser())).queue();
        return true;
    }

    /**
     * @param pStatement SQL statement
     * @param pSet Each ? in the statment will be replaced with the content of this array
     * @return a {@link ResultSet} containing the result of your SQL statement
     * */
    public ResultSet onQuery(String pStatement, Object... pSet) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(pStatement);
        if (pSet != null)
        {
            for (int i = 0; i < pSet.length; i++) {
                if (Blob.class.equals(pSet[i].getClass())) stmt.setBlob(i + 1, (Blob) pSet[i]);
                else if (byte[].class.equals(pSet[i].getClass())) stmt.setBytes(i + 1, (byte[]) pSet[i]);
                else if (byte.class.equals(pSet[i].getClass())) stmt.setByte(i + 1, (byte) pSet[i]);
                else if (String.class.equals(pSet[i].getClass())) stmt.setString(i + 1, (String) pSet[i]);
                else if (Integer.class.equals(pSet[i].getClass())) stmt.setInt(i + 1, (Integer) pSet[i]);
                else if (Long.class.equals(pSet[i].getClass())) stmt.setLong(i + 1, (Long) pSet[i]);
                else if (Boolean.class.equals(pSet[i].getClass())) stmt.setBoolean(i + 1, (Boolean) pSet[i]);
                else if (Double.class.equals(pSet[i].getClass())) stmt.setDouble(i + 1, (Double) pSet[i]);
                else if (Date.class.equals(pSet[i].getClass())) stmt.setDate(i + 1, (Date) pSet[i]);
            }
        }
        ResultSet rs = stmt.executeQuery();
        stmt.clearParameters();
        return rs;
    }

    /**
     * @param pStatement SQL statement
     * @return a {@link ResultSet} containing the result of your SQL statement
     * */
    public ResultSet onQuery(String pStatement) throws SQLException {
        return conn.createStatement().executeQuery(pStatement);
    }

    /**
     * @param pStatement SQL statement you want to execute
     * @param pSet Each ? in the statment will be replaced with the content of this array
     * */
    public void onExecute(String pStatement, Object... pSet) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(pStatement);
        if (pSet != null)
        {
            for (int i=0; i < pSet.length; i++)
            {
                if (Blob.class.equals(pSet[i].getClass())) stmt.setBlob(i + 1, (Blob) pSet[i]);
                else if (byte[].class.equals(pSet[i].getClass())) stmt.setBytes(i + 1, (byte[]) pSet[i]);
                else if (byte.class.equals(pSet[i].getClass())) stmt.setByte(i + 1, (byte) pSet[i]);
                else if (String.class.equals(pSet[i].getClass())) stmt.setString(i + 1, (String) pSet[i]);
                else if (Integer.class.equals(pSet[i].getClass())) stmt.setInt(i + 1, (Integer) pSet[i]);
                else if (Long.class.equals(pSet[i].getClass())) stmt.setLong(i + 1, (Long) pSet[i]);
                else if (Boolean.class.equals(pSet[i].getClass())) stmt.setBoolean(i + 1, (Boolean) pSet[i]);
                else if (Double.class.equals(pSet[i].getClass())) stmt.setDouble(i + 1, (Double) pSet[i]);
                else if (Date.class.equals(pSet[i].getClass())) stmt.setDate(i + 1, (Date) pSet[i]);
            }
        }
        stmt.execute();
        conn.commit();
        stmt.clearParameters();
    }

    /**
     * @param pStatement SQL statement you want to execute
     * */
    public void onExecute(String pStatement) throws SQLException {
        conn.createStatement().execute(pStatement);
        conn.commit();
    }

    public boolean isInteger(String pString) {
        if (pString == null || pString.isEmpty())
            return false;

        int length = pString.length();
        int i = 0;

        if (pString.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = pString.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
