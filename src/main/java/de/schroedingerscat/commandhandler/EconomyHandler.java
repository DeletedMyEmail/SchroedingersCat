package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Main;
import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/**
 * Handles slash commands considering the bot's economy
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 26.09.2022
 * */
public class EconomyHandler extends ListenerAdapter {

    // Emoji which represents the currency
    private static final String CURRENCY = "<:wiggle:935151967137832990>";

    /** Default color of this category to be used for embeds */
    private static final Color ECONOMY_COLOR = new Color(234,217,25);

    // HashMap<GuildId, List<Object>{color,number,List<Object>{user,boolean,amount}, List<Channel>},
    private final HashMap<Long, HashMap<Long, Long[]>> currentSpins;
    private final Utils utils;

    public EconomyHandler(Utils pUtils) {
        this.utils = pUtils;
        currentSpins = new HashMap<>();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                distributeIncome();
            }
        }, 1, 21600000);

    }

    // Events

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent pEvent)
    {
        try
        {
            switch (pEvent.getName())
            {
                case "bal" -> balCommand(pEvent);
                case "top" -> topCommand(pEvent);
                case "crime" -> crimeCommand(pEvent);
                case "rob" -> robCommand(pEvent);
                case "dep" -> depCommand(pEvent);
                case "with" -> withCommand(pEvent);
                case "work" -> workCommand(pEvent);
                case "spin" -> spinCommand(pEvent);
                case "get_income_roles" -> getIncomeRolesCommand(pEvent);
                case "remove_income_role" -> removeIncomeRoleCommand(pEvent);
                case "add_income_role" -> addIncomeRoleCommand(pEvent);
                case "give" -> giveCommmand(pEvent);
                case "give_admin" -> giveAdminCommand(pEvent);
            }
        }
        catch (NumberFormatException numEx) {
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You entered an invalid number", pEvent.getUser())).queue();
        }
        catch (SQLException sqlEx)
        {
            sqlEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
        }
    }

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent pEvent)
    {
        pEvent.deferEdit().queue();
        try
        {
            switch (pEvent.getButton().getId())
            {
                case "EconomyBankButton" ->
                    pEvent.getHook().editOriginalEmbeds(topEmbed(pEvent.getGuild().getIdLong(), "Bank")).queue();
                case "EconomyCashButton" ->
                    pEvent.getHook().editOriginalEmbeds(topEmbed(pEvent.getGuild().getIdLong(), "Cash")).queue();
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
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    private void balCommand(@NotNull SlashCommandInteractionEvent event) throws SQLException
    {
        event.deferReply().queue();
        User user;
        if(event.getOption("user") != null) user = event.getOption("user").getAsUser();
        else user = event.getUser();

        long[] wealth = getWealth(user.getIdLong(), event.getGuild().getIdLong());
        String[][] fields = {
                {"Bank",NumberFormat.getInstance()
                        .format(wealth[0])+" "+CURRENCY },
                {"Cash",NumberFormat.getInstance()
                        .format(wealth[1])+" "+CURRENCY},
        };

        event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "", "", fields, false, user, null, null)).queue();
    }

    /**
     * Gives a user an amount of cash between 1000 and 3000
     *
     * @param pEvent - SlashCommandInteractionEvent triggered by member
     * */
    private void workCommand(@NotNull SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        long lGuildId = pEvent.getGuild().getIdLong();
        long lMemberId = pEvent.getMember().getIdLong();

        int lGainedMoney = new Random().nextInt(2000)+1000;

        increaseBankOrCash(lMemberId, lGuildId, lGainedMoney, "cash");

        pEvent.getHook().editOriginalEmbeds(
                utils.createEmbed(
                        ECONOMY_COLOR,
                        "",
                        ":white_check_mark: You earned **"+ NumberFormat.getInstance()
                                .format(lGainedMoney)+"** "+ CURRENCY
                        , null,
                        false,
                        pEvent.getUser(),
                        null,
                        null)
        ).queue();
    }

    /**
     * Can give a user an amount of cash between 2500 and 4500, but there is a chance of 1/3 to lose it
     *
     * @param pEvent - SlashCommandInteractionEvent triggered by member
     * */
    private void crimeCommand(@NotNull SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        long lGuildId = pEvent.getGuild().getIdLong();
        long lMemberId = pEvent.getMember().getIdLong();

        Random lRandom = new Random();
        int lGainedMoney = lRandom.nextInt(2000) + 2500;
        if (lRandom.nextInt(3) == 0)
        {
            increaseBankOrCash(lMemberId, lGuildId, -lGainedMoney, "cash");
            String description = ":x: You got caught while commiting a crime and paid **"+NumberFormat.getInstance()
                    .format(lGainedMoney)+"** "+CURRENCY;
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "", description, null, false, pEvent.getUser(), null, null)).queue();
        }
        else
        {
            increaseBankOrCash(lMemberId, lGuildId, lGainedMoney, "cash");
            pEvent.getHook().editOriginalEmbeds(
                    utils.createEmbed(
                            ECONOMY_COLOR,
                            "",
                            ":white_check_mark: You earned **"+NumberFormat.getInstance()
                                    .format(lGainedMoney)+"** "+CURRENCY,
                            null,
                            false,
                            pEvent.getUser(),
                            null,
                            null)
            ).queue();
        }
    }

    /**
     * Gives the user the option to choose if bank or cash toplist should be displayed by pressing a button
     *
     * @param pEvent - SlashCommandInteractionEvent triggered by member
     * */
    private void topCommand(@NotNull SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();
        Button[] lButtons = {
                Button.primary("EconomyBankButton", Emoji.fromUnicode("U+1F3E6").getAsReactionCode()+" Bank values"),
                Button.success("EconomyCashButton", Emoji.fromUnicode("U+1F4B5").getAsReactionCode()+" Cash values")
        };
        pEvent.getHook().
                editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "Choose which amount you want to see", pEvent.getUser())).
                setActionRow(lButtons).queue();
    }
    /**
     * Creates an embed containing the richest members on server as a top list
     *
     * @param pGuildId - Server's id on which the members should be searched
     * @param  pCashOrBank - Defines the type of value with should be displayed. <br/>
     *                    If it's not "cash" or "bank", the embed will display an empty list
     * @return Returns the mentioned embed
     * */
    private MessageEmbed topEmbed(long pGuildId, String pCashOrBank) throws SQLException
    {
        MessageEmbed lEmbed;
        ResultSet lRs;
        String lDescription = "";
        int lCounter = 1;

        lRs = utils.onQuery("SELECT DISTINCT user_id, "+pCashOrBank.toLowerCase()+" FROM Economy WHERE guild_id="+pGuildId+" ORDER BY cash DESC LIMIT 10");

        while(lRs.next() && lCounter < 11)
        {
            Member member = Main.getJDA().getGuildById(pGuildId).getMemberById(lRs.getLong("user_id"));
            if (member == null)
                utils.onExecute("DELETE FROM Economy WHERE user_id = ? AND guild_id = ?", lRs.getLong("user_id"), pGuildId);

            else
            {
                lDescription += "**"+lCounter+"** "+member.getAsMention()+" **"+NumberFormat.getInstance()
                        .format(lRs.getLong(2))+"** "+CURRENCY +"\n";
                lCounter++;
            }
        }

        if (lDescription.isEmpty())
        {
            lDescription = "There aren't any users with "+pCashOrBank.toLowerCase()+" value";
        }

        lEmbed = utils.createEmbed(ECONOMY_COLOR, "TOP "+pCashOrBank.toUpperCase()+" VALUES",
                lDescription, null, false, null, null, null);
        return lEmbed;
    }

    /**
     * Withdraws money from the bank
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    private void withCommand(SlashCommandInteraction event) throws SQLException
    {
        event.deferReply().queue();
        long[] lUsersWealth = getWealth(event.getUser().getIdLong(), event.getGuild().getIdLong());
        long lAmount = lUsersWealth[0];
        if (event.getOption("amount") != null)
        {
            lAmount = event.getOption("amount").getAsLong();
        }

        if (lAmount > lUsersWealth[0])
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: The given amount is higher than your bank value",
                    null, false, event.getUser(), null, null)).queue();

        }
        else if (lAmount <= 0)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: You can't with less than 1",
                    null, false, event.getUser(), null, null)).queue();
        }
        else
        {
            increaseBankOrCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), +lAmount, "cash");
            increaseBankOrCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), -lAmount, "bank");
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    ECONOMY_COLOR, "", ":white_check_mark: Withdrawed **"+NumberFormat.getInstance()
                            .format(lAmount)+"** "+ CURRENCY +" from your bank",
                    null, false, event.getUser(), null, null)).queue();
        }
    }

    /**
     * Deposites money to the bank
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    private void depCommand(SlashCommandInteraction event) throws SQLException
    {
        event.deferReply().queue();
        long[] lWealth = getWealth(event.getUser().getIdLong(), event.getGuild().getIdLong());
        long lAmountToDep = lWealth[1];

        if (event.getOption("amount") != null)
            lAmountToDep = event.getOption("amount").getAsLong();

        if (lAmountToDep > lWealth[1])
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: The given amount is higher than your cash value",
                    null, false, event.getUser(), null, null)).queue();

        else if (lAmountToDep <= 1)
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: You can't deposite less than 1",
                    null, false, event.getUser(), null, null)).queue();

        else
        {
            setBankAndCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), lWealth[0]+lAmountToDep, lWealth[1]-lAmountToDep);
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    ECONOMY_COLOR, "", ":white_check_mark: Deposited **"+NumberFormat.getInstance()
                            .format(lAmountToDep)+ "** "+CURRENCY +" to your bank",
                    null, false, event.getUser(), null, null)).queue();
        }
    }

    /**
     *
     *
     * */
    private void robCommand(SlashCommandInteraction pEvent) throws SQLException
    {
        pEvent.deferReply().queue();

        if (pEvent.getOption("user").getAsUser().equals(pEvent.getUser()))
        {
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: You can't rob yourself",
                    null, false, pEvent.getUser(), null, null)).queue();
            return;
        }

        long lMemberToRobCash = getWealth(pEvent.getOption("user").getAsUser().getIdLong(), pEvent.getGuild().getIdLong())[1];
        long[] lRobberWealth = getWealth(pEvent.getUser().getIdLong(), pEvent.getGuild().getIdLong());

        if (lMemberToRobCash <= 0)
        {
            String lDescription = ":x: There is no cash to rob.";
            if (lRobberWealth[0] > 3)
            {
                long lostValue = (long) (lRobberWealth[0]*0.25);
                increaseBankOrCash(pEvent.getUser().getIdLong(), pEvent.getGuild().getIdLong(), -lostValue, "cash");
                lDescription += " You got caught robbing and paid **"+NumberFormat.getInstance()
                        .format(lostValue)+"** "+ CURRENCY;
            }
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(
                    ECONOMY_COLOR, "", lDescription,
                    null, false, pEvent.getUser(), null, null)).queue();
        }
        else
        {
            increaseBankOrCash(pEvent.getUser().getIdLong(), pEvent.getGuild().getIdLong(), +lMemberToRobCash, "cash");
            increaseBankOrCash(pEvent.getOption("user").getAsUser().getIdLong(), pEvent.getGuild().getIdLong(), -lRobberWealth[1], "cash");
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(
                    ECONOMY_COLOR, "", ":white_check_mark: Successfully robbed "+
                            pEvent.getOption("user").getAsMember().getAsMention()+" and got **"+NumberFormat.getInstance()
                            .format(lMemberToRobCash)+"** "+CURRENCY,
                    null, false, pEvent.getUser(), null, null)).queue();
        }
    }

    /**
     * TODO:
     *
     *
     * */
    private void spinCommand(SlashCommandInteraction pEvent) throws SQLException
    {
        pEvent.deferReply().queue();

        long lGuildId = pEvent.getGuild().getIdLong();
        long lUserId = pEvent.getUser().getIdLong();
        long lUsersCash = getWealth(lUserId,lGuildId)[1];

        long lColor = -1;
        if (pEvent.getOption("color").getAsString().equals("red")) { lColor = 0;}
        else if (pEvent.getOption("color").getAsString().equals("black")) { lColor = 1;}

        long lField = -1;
        OptionMapping fieldOption = pEvent.getOption("field");
        if (fieldOption != null)
            lField = fieldOption.getAsInt();


        long lAmountToBet = lUsersCash;
        if (pEvent.getOption("money") != null)
            lAmountToBet = pEvent.getOption("money").getAsLong();

        if (lColor == -1)
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: Please choose red or black as color",
                    null, false, pEvent.getUser(), null, null
            )).queue();

        else if (lAmountToBet > getWealth(lUserId, lGuildId)[1] || lAmountToBet <= 0)
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: You don't have enough money or the entered amount is less than 1",
                    null, false, pEvent.getUser(), null, null
            )).queue();

        else
        {
            if (currentSpins.containsKey(lGuildId))
            {
                if (currentSpins.get(lGuildId).containsKey(lUserId))
                {
                    pEvent.getHook().editOriginalEmbeds(utils.createEmbed(
                            Color.red, "", ":x: You already bet in this round",
                            null, false, pEvent.getUser(), null, null
                    )).queue();
                    return;
                }
                else
                {
                    currentSpins.get(lGuildId).put(lUserId, new Long[] { pEvent.getChannel().getIdLong(), lColor, lField, lAmountToBet});
                }
            }
            else
            {
                HashMap<Long, Long[]> guildMap = new HashMap<>();
                guildMap.put(lUserId, new Long[] { lColor, lField, lAmountToBet});
                currentSpins.put(lGuildId, guildMap);

                Timer timer = new Timer();
                timer.schedule(new TimerTask()
                {
                    @Override
                    public void run() {
                        spinResult();
                        this.cancel();
                    }
                }, 10000);

            }
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                    ":white_check_mark: You bet **"+NumberFormat.getInstance()
                            .format(lAmountToBet)+"** "+CURRENCY, null,
                    false, pEvent.getUser(), null, "10 sec remaining")).queue();


        }

    }

    /**
     *  TODO
     *
     * */
    private static void spinResult()
    {
        System.out.println("Spin Result");
    }

    /**
     *
     *
     * @param pEvent - SlashCommandInteractionEvent triggered by member
     * */
    private void addIncomeRoleCommand(SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        if (utils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        long lRoleId = pEvent.getOption("role").getAsRole().getIdLong();
        long lIncome = pEvent.getOption("income").getAsLong();
        String lDescription;
        if (utils.onQuery("SELECT income FROM IncomeRole WHERE role_id = ?",lRoleId).next())
        {
            utils.onExecute("UPDATE IncomeRole SET income = ? WHERE role_id = ?", lIncome, lRoleId);
            lDescription = ":white_check_mark: Updated income from "+pEvent.getOption("role").getAsRole().getAsMention()+" to **"
                    +lIncome+"**";
        }
        else
        {
            utils.onExecute("INSERT INTO IncomeRole VALUES(?,?,?)", pEvent.getGuild().getIdLong(),lRoleId,lIncome);
            lDescription = ":white_check_mark: Added Income Role "+pEvent.getOption("role").getAsRole().getAsMention()+" with **"
                    +NumberFormat.getInstance()
                    .format(lIncome)+"** "+CURRENCY +" income";
        }
        pEvent.getHook().editOriginalEmbeds(
                utils.createEmbed(
                        ECONOMY_COLOR,
                        lDescription,
                        pEvent.getUser())
        ).queue();
    }

    /**
     *
     *
     * @param pEvent - SlashCommandInteractionEvent triggered by member
     * */
    private void getIncomeRolesCommand(SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        ResultSet rs = utils.onQuery("SELECT * FROM IncomeRole WHERE guild_id = ? ORDER BY income DESC", pEvent.getGuild().getIdLong());
        String lDescription = "";

        while(rs.next())
        {
            lDescription += pEvent.getJDA().getRoleById(rs.getLong(2)).getAsMention() +" • **"+NumberFormat.getInstance()
                    .format(rs.getLong(3))+ "** "+CURRENCY+"\n";
        }

        pEvent.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "Income Roles",
                lDescription,null, false, null, null, null)).queue();
    }

    /**
     *
     *
     * @param pEvent - SlashCommandInteractionEvent triggered by member
     * */
    private void removeIncomeRoleCommand(SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        if (utils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        Role role = pEvent.getOption("role").getAsRole();

        if (utils.onQuery("SELECT * FROM IncomeRole where guild_id="+pEvent.getGuild().getIdLong()+
                " AND role_id="+role.getIdLong()).isClosed())
        {
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    ":x: There is no income connected with "+role.getAsMention(),null, false, pEvent.getUser(), null, null)).queue();
        }
        else
        {
            utils.onExecute("DELETE FROM IncomeRole WHERE guild_id="+pEvent.getGuild().getIdLong()+
                                        " AND role_id="+role.getIdLong());
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                    ":white_check_mark: Income Role "+role.getAsMention()+" removed",null, false, pEvent.getUser(), null, null)).queue();
        }
    }

    /**
     *
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    private void giveCommmand(SlashCommandInteractionEvent event) throws SQLException
    {
        event.deferReply().queue();

        User lUser = event.getUser();
        User lOtherUser = event.getOption("user").getAsUser();
        long lUsersCash = getWealth(lUser.getIdLong(), event.getGuild().getIdLong())[1];
        long lAmount = lUsersCash;
        if (event.getOption("amount") != null) lAmount = event.getOption("amount").getAsLong();

        if (lUser.equals(lOtherUser))
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    ":x: You can't give yourself money",null,
                    false, event.getUser(), null, null)).queue();
        }
        else if (lAmount <= 0)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    ":x: Can't give less than 1 "+ CURRENCY,null,
                    false, event.getUser(), null, null)).queue();
        }
        else if (lUsersCash < lAmount)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    ":x: You don't have enough "+ CURRENCY,null,
                    false, event.getUser(), null, null)).queue();
        }
        else
        {
            increaseBankOrCash(lUser.getIdLong(), event.getGuild().getIdLong(), -lAmount, "cash");
            increaseBankOrCash(lOtherUser.getIdLong(), event.getGuild().getIdLong(), lAmount, "cash");

            event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                    ":white_check_mark: You gave **"+NumberFormat.getInstance()
                            .format(lAmount)+ "** "+CURRENCY +" to "+lOtherUser.getAsMention(),null,
                    false, event.getUser(), null, null)).queue();
        }
    }

    /**
     *
     *
     * */
    private void giveAdminCommand(SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        if (utils.memberNotAuthorized(pEvent.getMember(), "admin", pEvent.getHook())) return;

        pEvent.deferReply().queue();
        long lAmount = pEvent.getOption("amount").getAsLong();
        User lUserToGiveTo = pEvent.getOption("user").getAsUser();
        increaseBankOrCash(lUserToGiveTo.getIdLong(), pEvent.getGuild().getIdLong(), lAmount, "cash");

        pEvent.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                ":white_check_mark: You gave **"+NumberFormat.getInstance()
                        .format(lAmount)+"** "+ CURRENCY +" to "+lUserToGiveTo.getAsMention(),null,
                false, pEvent.getUser(), null, null)).queue();
    }

    // Other private commands

    /**
     *
     *
     * */
    private void distributeIncome()
    {
        ResultSet lRs;
        try
        {
            lRs = utils.onQuery("SELECT * FROM IncomeRole");

            while(lRs.next()) {
                try
                {
                    Guild lGuild = Main.getJDA().getGuildById(lRs.getLong("guild_id"));
                    Role lRole = lGuild.getRoleById(lRs.getLong("role_id"));
                    List<Member> lMemberWithRole = lGuild.findMembersWithRoles(lRole).get();
                    lMemberWithRole.forEach(member -> {
                        try
                        {
                            increaseBankOrCash(member.getIdLong(), lGuild.getIdLong(), lRs.getLong("income"), "cash");
                        }
                        catch (SQLException sqlEx) {sqlEx.printStackTrace();}
                    });
                }
                catch (Exception ex) {ex.printStackTrace();}
            }
        }
        catch (SQLException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the bank and cash values of money from a specific user on a server
     *
     * @param pGuildId - ID of the server on which the user is whose wealth should be returned
     * @param pMemberId - ID of the user whose wealth should be returned
     * @return Returns an int array in which the first index is the bank value and the second the cash
     * */
    private long[] getWealth(long pMemberId, long pGuildId) throws SQLException
    {
            ResultSet rs = utils.onQuery("SELECT bank,cash FROM Economy WHERE guild_id="+pGuildId+" AND user_id="+pMemberId);
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
    private void increaseBankOrCash(long pUserId, long pGuildId, long pAmountOfMoney, String pColumn) throws SQLException
    {
        ResultSet lRs = utils.onQuery("SELECT user_id FROM Economy WHERE guild_id = ? AND user_id = ?", pGuildId, pUserId);

        if (lRs.isClosed() || !lRs.next())
        {
            if (pColumn.equals("bank"))
                utils.onExecute("INSERT INTO Economy VALUES (?,?,?,?)", pGuildId, pUserId, pAmountOfMoney, 0);
            else if (pColumn.equals("cash"))
                utils.onExecute("INSERT INTO Economy VALUES (?,?,?,?)", pGuildId, pUserId, 0, pAmountOfMoney);
        }
        else
            utils.onExecute("UPDATE Economy SET "+pColumn.toLowerCase()+" = "+pColumn.toLowerCase()+" + ? " +
                "WHERE guild_id = ? AND user_id = ?", pAmountOfMoney, pGuildId, pUserId);

    }

    /**
     *
     *
     * @param pGuildId - ID of the server on which the new user should be added to database
     * @param pUserId - ID of the user who should be added to database
     * @param pNewBankValue
     * @param pNewCashValue
     * */
    private void setBankAndCash(long pUserId, long pGuildId, long pNewBankValue, long pNewCashValue) throws SQLException
    {
        ResultSet lRs = utils.onQuery("SELECT user_id FROM Economy WHERE guild_id = ? AND user_id = ?", pGuildId, pUserId);

        if (lRs.isClosed() || !lRs.next())
            utils.onExecute("INSERT INTO Economy VALUES (?,?,?,?)", pGuildId, pUserId, pNewBankValue, pNewCashValue);
        else
            utils.onExecute(
                    "UPDATE Economy SET bank = ? AND cash = ? WHERE guild_id = ? AND user_id = ?",
                    pNewBankValue, pNewCashValue, pGuildId, pUserId);

    }

    public static Color getCategoryColor() {return ECONOMY_COLOR; }
}
