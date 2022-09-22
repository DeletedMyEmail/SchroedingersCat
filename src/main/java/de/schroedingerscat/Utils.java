package de.schroedingerscat;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
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

    public void createTables()
    {
        try
        {
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS economy (server_id INTEGER, user_id INTEGER, bank INTEGER, cash INTEGER)").executeUpdate();

            conn.prepareStatement("CREATE TABLE IF NOT EXISTS guildsettings (server_id INTEGER, channel_id INTEGER," +
                    " autorole_id LONG, ac_id INTEGER, welcome_message TEXT, screening TEXT, log_id INTEGER)").executeUpdate();

            conn.prepareStatement("CREATE TABLE IF NOT EXISTS income_roles (server_id INTEGER, role_id INTEGER, income INTEGER)").executeUpdate();

            conn.prepareStatement("CREATE TABLE IF NOT EXISTS reactions (server_id INTEGER, reaction TEXT, role_id INTEGER, msg_id INTEGER)").executeUpdate();

            conn.prepareStatement("CREATE TABLE IF NOT EXISTS autochannel (server_id INTEGER, owner_id INTEGER, channel_id INTEGER)").executeUpdate();
        }
        catch (SQLException e) {e.printStackTrace();}
    }

    public MessageEmbed createEmbed(@NotNull Color pColor, @NotNull String pTitle, @NotNull String pDescription,
                                    String[][] pFields, boolean inline, User pAuthor, String pUrl, String pFooter)
    {
        EmbedBuilder builder = new EmbedBuilder();
        if (!pTitle.isEmpty()) builder.setTitle(pTitle);
        if (!pDescription.isEmpty()) builder.setDescription(pDescription);
        if (pAuthor != null) builder.setAuthor(pAuthor.getAsTag(), null, pAuthor.getAvatarUrl());
        if (pUrl != null) builder.setImage(pUrl);
        if (pFooter != null) builder.setFooter(pFooter);

        if (pFields != null)
            for (String[] field : pFields)
            {
                builder.addField(field[0], field[1], inline);
            }

        builder.setColor(pColor);
        return builder.build();
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
}
