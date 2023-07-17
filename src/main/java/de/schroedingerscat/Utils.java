package de.schroedingerscat;

import de.schroedingerscat.commandhandler.EconomyHandler;
import de.schroedingerscat.entities.Pet;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.text.NumberFormat;

/**
 *
 *
 * @author KaitoKunTatsu
 * @version 3.0.0 | last edit: 17.07.2023
 * */
public class Utils {

    public static final long OWNER_ID = 1072150132583321630L;

    private static Connection conn;

    public Utils(String pDatabasePath) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:"+pDatabasePath);
        conn.setAutoCommit(false);
    }

    public void insertGuildIfAbsent(long pGuildId) throws SQLException {

        ResultSet lRs = onQuery("SELECT guild_id FROM GuildSettings WHERE guild_id = ?", pGuildId);
        if (!lRs.next()) {
            onExecute("INSERT INTO GuildSettings (guild_id) VALUES (?)", pGuildId);
        }
        conn.commit();
    }

    /**
     * Gets the bank and cash values of money from a specific user on a server
     *
     * @param pGuildId - ID of the server on which the user is whose wealth should be returned
     * @param pMemberId - ID of the user whose wealth should be returned
     * @return Returns an int array in which the first index is the bank value and the second the cash
     * */
    public long[] getWealth(long pMemberId, long pGuildId) throws SQLException {
        ResultSet rs = onQuery("SELECT bank,cash FROM Economy WHERE guild_id="+pGuildId+" AND user_id="+pMemberId);
        rs.next();

        return new long[] { rs.getLong("bank"), rs.getLong("cash") };
    }

    /**
     * Updates the bank/cash amount for a specific user on a server
     *
     * @param pGuildId - ID of the server on which the new user should be added to database
     * @param pUserId - ID of the user who should be added to database
     * @param pAmountOfMoney - Amount of money on which the bank value should be set
     * */
    public void increaseBankOrCash(long pUserId, long pGuildId, long pAmountOfMoney, String pColumn) throws SQLException {
        ResultSet lRs = onQuery("SELECT user_id FROM Economy WHERE guild_id = ? AND user_id = ?", pGuildId, pUserId);

        if (lRs.isClosed() || !lRs.next()) {
            if (pColumn.equals("bank"))
                onExecute("INSERT INTO Economy VALUES (?,?,?,?)", pGuildId, pUserId, pAmountOfMoney, 0);
            else if (pColumn.equals("cash"))
                onExecute("INSERT INTO Economy VALUES (?,?,?,?)", pGuildId, pUserId, 0, pAmountOfMoney);
        }
        else {
            onExecute("UPDATE Economy SET " + pColumn.toLowerCase() + " = " + pColumn.toLowerCase() + " + ? " +
                    "WHERE guild_id = ? AND user_id = ?", pAmountOfMoney, pGuildId, pUserId);
        }
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
        return insertVaraibles(pStatement, pSet).executeQuery();
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
        insertVaraibles(pStatement, pSet).execute();
        conn.commit();
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

    private PreparedStatement insertVaraibles(String pStatement, Object[] pSet) throws SQLException {
        PreparedStatement lStatement = conn.prepareStatement(pStatement);
        if (pSet != null) {
            for (int i=0; i < pSet.length; i++) {
                if (Blob.class.equals(pSet[i].getClass())) lStatement.setBlob(i + 1, (Blob) pSet[i]);
                else if (byte[].class.equals(pSet[i].getClass())) lStatement.setBytes(i + 1, (byte[]) pSet[i]);
                else if (byte.class.equals(pSet[i].getClass())) lStatement.setByte(i + 1, (byte) pSet[i]);
                else if (String.class.equals(pSet[i].getClass())) lStatement.setString(i + 1, (String) pSet[i]);
                else if (Integer.class.equals(pSet[i].getClass())) lStatement.setInt(i + 1, (Integer) pSet[i]);
                else if (Long.class.equals(pSet[i].getClass())) lStatement.setLong(i + 1, (Long) pSet[i]);
                else if (Boolean.class.equals(pSet[i].getClass())) lStatement.setBoolean(i + 1, (Boolean) pSet[i]);
                else if (Double.class.equals(pSet[i].getClass())) lStatement.setDouble(i + 1, (Double) pSet[i]);
                else if (Date.class.equals(pSet[i].getClass())) lStatement.setDate(i + 1, (Date) pSet[i]);
            }
        }
        return lStatement;
    }

    public static String formatPrice(long pPrice) {
        return NumberFormat.getInstance().format(pPrice) + " " + EconomyHandler.CURRENCY;
    }

    public static MessageEmbed createEmbed(@NotNull Color pColor, @NotNull String pTitle, @NotNull String pDescription, String[][] pFields, boolean inline, User pAuthor, String pImageUrl, String pFooter) {
        EmbedBuilder builder = new EmbedBuilder();

        if (!pTitle.isEmpty()) builder.setTitle(pTitle);
        if (!pDescription.isEmpty()) builder.setDescription(pDescription);
        if (pAuthor != null) builder.setAuthor(pAuthor.getName(), null, pAuthor.getAvatarUrl());
        if (pImageUrl != null) builder.setImage(pImageUrl);
        if (pFooter != null) builder.setFooter(pFooter);

        if (pFields != null) {
            for (String[] field : pFields) {
                builder.addField(field[0], field[1], inline);
            }
        }

        builder.setColor(pColor);
        return builder.build();
    }

    public static long roundUp(long num, long divisor) {
        return (num + divisor - 1) / divisor;
    }

    public static MessageEmbed createEmbed(@NotNull Color pColor, @NotNull String pDescription, User pAuthor) {
        return createEmbed(pColor, "", pDescription, null, false, pAuthor, null, null);
    }

    public static void catchAndLogError(InteractionHook pHook, Function pFunction) {
        try {
            pFunction.run();
        }
        catch (NumberFormatException numEx) {
            sendToOwner(pHook.getJDA(), ":x: An error occured:\n" + numEx.getMessage());
            pHook.editOriginalEmbeds(createEmbed(Color.red, ":x: You entered an invalid number", pHook.getInteraction().getUser())).queue();
        }
        catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
            sendToOwner(pHook.getJDA(), ":x: An error occured:\n" + sqlEx.getMessage());
            pHook.editOriginalEmbeds(createEmbed(Color.red, ":x: Database error occurred", pHook.getInteraction().getUser())).queue();
        }
        catch (NullPointerException nullEx) {
            nullEx.printStackTrace();
            sendToOwner(pHook.getJDA(), ":x: An error occured:\n" + nullEx.getMessage());
            pHook.editOriginalEmbeds(createEmbed(Color.red, ":x: An unexpected error occured and it's likely that you entered an **invalid argument**.\nMake sure you selected a valid text channel, message id, role or emoji.", pHook.getInteraction().getUser())).queue();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            sendToOwner(pHook.getJDA(), ":x: An error occured:\n" + ex.getMessage());
            pHook.editOriginalEmbeds(createEmbed(Color.red, ":x: An unknown error has occurred. This event is logged for troubleshooting purposes. If the problem persists, contact the developer", pHook.getInteraction().getUser())).queue();
        }
    }

    public static void catchAndLogError(JDA pJDA, Function pFunction) {
        try {
            pFunction.run();
        }
        catch (Exception ex) {
            sendToOwner(pJDA, ":x: An error occured:\n" + ex.getMessage());
        }
    }

    public interface Function {
        void run() throws Exception;
    }

    public static void sendToOwner(JDA pJDA, String pMessage) {
        pJDA.getUserById(OWNER_ID).openPrivateChannel().complete().sendMessage(pMessage).queue();
    }
}
