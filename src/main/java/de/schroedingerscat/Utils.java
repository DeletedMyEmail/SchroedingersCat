package de.schroedingerscat;

import de.schroedingerscat.entities.Pet;
import net.dv8tion.jda.api.EmbedBuilder;
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
import java.io.File;
import java.io.IOException;
import java.sql.*;

/**
 *
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.2.0 | last edit: 14.07.2022
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
                'catsandpets_channel_id' INTEGER,
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

        conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS 'CatCards' (
                'guild_id' INTEGER,
                'user_id' INTEGER,
                'cat_number' INTEGER)
        """).executeUpdate();

        conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS 'Pet' (
                'id' INTEGER,
                'name' TEXT,
                'price' INTEGER,
                'description' TEXT)
        """).executeUpdate();

        conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS 'PetInventory' (
                'guild_id' INTEGER,
                'user_id' INTEGER,
                'pet_id' INTEGER,
                'amount' INTEGER)
        """).executeUpdate();

        conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS 'CommandCooldown' (
                'guild_id' INTEGER,
                'user_id' INTEGER,
                'command' TEXT,
                'cooldown_until' INTEGER)
        """).executeUpdate();

        conn.prepareStatement("CREATE TABLE IF NOT EXISTS 'IncomeRole' (" +
                "'guild_id' INTEGER, " +
                "'role_id' INTEGER, " +
                "'income' INTEGER)"
        ).executeUpdate();

        conn.prepareStatement("CREATE TABLE IF NOT EXISTS 'AutoChannel' (" +
                "'guild_id' INTEGER, " +
                "'owner_id' INTEGER, " +
                "'channel_id' INTEGER)"
        ).executeUpdate();

        conn.commit();
    }

    public void insertGuildIfAbsent(long pGuildId) throws SQLException {

        ResultSet lRs = onQuery("SELECT guild_id FROM GuildSettings WHERE guild_id = ?", pGuildId);
        if (!lRs.next()) {
            onExecute("INSERT INTO GuildSettings (guild_id) VALUES (?)", pGuildId);
        }
        conn.commit();
    }

    public Pet getPet(int pPetId) throws SQLException {
        ResultSet lRs = onQuery("SELECT * FROM Pet WHERE id = ?", pPetId);
        if (lRs.next()) {
            return new Pet(lRs.getInt("id"), lRs.getString("name"), lRs.getInt("price"), lRs.getString("description"));
        }
        return null;
    }

    public Pet getPet() throws SQLException {
        ResultSet lRs = onQuery("SELECT * FROM Pet ORDER BY RANDOM() LIMIT 1");
        if (lRs.next()) {
            return new Pet(lRs.getInt("id"), lRs.getString("name"), lRs.getInt("price"), lRs.getString("description"));
        }
        return null;
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

    public static void mergeImages(String pImg1, String pImg2, String pImg3, String pOutput) throws IOException {
        BufferedImage lImg1 = ImageIO.read(new File(pImg1));
        BufferedImage lImg2 = ImageIO.read(new File(pImg2));
        BufferedImage lImg3 = ImageIO.read(new File(pImg3));
        BufferedImage lMergedImg = new BufferedImage(lImg1.getWidth() + lImg2.getWidth() + lImg3.getWidth() + 160, Math.max(lImg1.getHeight(), Math.max(lImg2.getHeight(), lImg3.getHeight()))+80, BufferedImage.TYPE_INT_RGB);

        Graphics2D lMergedImgGraphic = lMergedImg.createGraphics();
        lMergedImgGraphic.drawImage(lImg1, 40, 40, null);
        lMergedImgGraphic.drawImage(lImg2, lImg1.getWidth() + 80, 40, null);
        lMergedImgGraphic.drawImage(lImg3, lImg1.getWidth() + 120 + lImg2.getWidth(), 40, null);

        ImageIO.write(lMergedImg, "jpg", new File(pOutput));
        lMergedImgGraphic.dispose();
    }

    public static MessageEmbed createEmbed(@NotNull Color pColor, @NotNull String pTitle, @NotNull String pDescription, String[][] pFields, boolean inline, User pAuthor, String pImageUrl, String pFooter) {
        EmbedBuilder builder = new EmbedBuilder();

        if (!pTitle.isEmpty()) builder.setTitle(pTitle);
        if (!pDescription.isEmpty()) builder.setDescription(pDescription);
        if (pAuthor != null) builder.setAuthor(pAuthor.getName(), null, pAuthor.getAvatarUrl());
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

    public static MessageEmbed createEmbed(@NotNull Color pColor, @NotNull String pDescription, User pAuthor) {
        return createEmbed(pColor, "", pDescription, null, false, pAuthor, null, null);
    }
}
