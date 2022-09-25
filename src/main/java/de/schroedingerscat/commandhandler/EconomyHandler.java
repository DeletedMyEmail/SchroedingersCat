package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Main;
import de.schroedingerscat.Utils;
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

    private final HashMap<Long, HashMap<Long, Long[]>> currentSpins;
    private final Utils utils;

    public EconomyHandler(Utils pUtils) {
        this.utils = pUtils;
        currentSpins = new HashMap<>();
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
                case "get_income_role" -> getIncomeRoleCommand(pEvent);
                case "del_income_role" -> deleteIncomeRoleCommand(pEvent);
                case "add_income_role" -> addIncomeRoleCommand(pEvent);
                case "give" -> giveCommmand(pEvent);
                case "give_admin" -> giveAdminCommand(pEvent);
            }
        }
        catch (SQLException sqlEx) {
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
        catch (SQLException sqlEx) {
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
        }
    }

    // Slash Commands

    /**
     *
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void balCommand(@NotNull SlashCommandInteractionEvent event) throws SQLException
    {
        event.deferReply().queue();
        User user;
        if(event.getOption("user") != null) user = event.getOption("user").getAsUser();
        else user = event.getUser();

        long[] wealth = getWealth(user.getIdLong(), event.getGuild().getIdLong());
        String[][] fields = {
                {"Bank",wealth[0]+" "+CURRENCY },
                {"Cash",wealth[1]+" "+CURRENCY},
        };

        event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "", "", fields, false, user, null, null)).queue();
    }

    /**
     * Gives a user an amount of cash between 1000 and 3000
     *
     * @param pEvent - SlashCommandInteractionEvent triggered by member
     * */
    protected void workCommand(@NotNull SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        long lGuildId = pEvent.getGuild().getIdLong();
        long lMemberId = pEvent.getMember().getIdLong();

        long lCurrentCash = getWealth(lMemberId, pEvent.getGuild().getIdLong())[1];

        int lGainedAmount = new Random().nextInt(2000)+1000;

        setBankOrCash(lMemberId, lGuildId, lCurrentCash+lGainedAmount, "cash");

        pEvent.getHook().editOriginalEmbeds(
                utils.createEmbed(
                        ECONOMY_COLOR,
                        "",
                        ":white_check_mark: You earned **"+ lGainedAmount+"** "+ CURRENCY
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
    protected void crimeCommand(@NotNull SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        long lGuildId = pEvent.getGuild().getIdLong();
        long lMemberId = pEvent.getMember().getIdLong();

        long lCurrentCash = getWealth(lMemberId, pEvent.getGuild().getIdLong())[1];

        Random lRandom = new Random();
        int lGainedMoney = lRandom.nextInt(2000) + 2500;
        if (lRandom.nextInt(3) == 0)
        {
            setBankOrCash(lMemberId, lGuildId, lCurrentCash - lGainedMoney, "cash");
            String description = ":x: You got caught while commiting a crime and paid **"+lGainedMoney+"** "+CURRENCY;
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "", description, null, false, pEvent.getUser(), null, null)).queue();
        }
        else
        {
            setBankOrCash(lMemberId, lGuildId, lCurrentCash + lGainedMoney, "cash");
            pEvent.getHook().editOriginalEmbeds(
                    utils.createEmbed(
                            ECONOMY_COLOR,
                            "",
                            ":white_check_mark: You earned **"+lGainedMoney+"** "+CURRENCY,
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
    protected void topCommand(@NotNull SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();
        Button[] lButtons = {
                Button.primary("EconomyBankButton", "Bank values"),
                Button.success("EconomyCashButton", "Cash values")
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
    protected MessageEmbed topEmbed(long pGuildId, String pCashOrBank) throws SQLException
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
            lDescription = "There aren't any users with "+pCashOrBank+" value";
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
    protected void withCommand(SlashCommandInteraction event) throws SQLException
    {
        event.deferReply().queue();
        long[] wealth = getWealth(event.getUser().getIdLong(), event.getGuild().getIdLong());
        long amount = wealth[0];
        if (event.getOption("amount") != null)
        {
            amount = event.getOption("amount").getAsLong();
        }

        if (amount > wealth[0])
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: The given amount is higher than your bank value",
                    null, false, event.getUser(), null, null)).queue();

        }
        else if (amount <= 0)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: You can't with less than 1",
                    null, false, event.getUser(), null, null)).queue();
        }
        else
        {
            setBankOrCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), wealth[1]+amount, "cash");
            setBankOrCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), wealth[0]-amount, "bank");
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    ECONOMY_COLOR, "", ":white_check_mark: Withdrawed **"+amount+"** "+ CURRENCY +" from your bank",
                    null, false, event.getUser(), null, null)).queue();
        }
    }

    /**
     * Deposites money to the bank
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void depCommand(SlashCommandInteraction event) throws SQLException
    {
        event.deferReply().queue();
        long[] wealth = getWealth(event.getUser().getIdLong(), event.getGuild().getIdLong());
        long amount = wealth[1];
        if (event.getOption("amount") != null)
        {
            amount = event.getOption("amount").getAsLong();
        }

        if (amount > wealth[1])
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: The given amount is higher than your cash value",
                    null, false, event.getUser(), null, null)).queue();

        }
        else if (amount <= 1)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: You can't deposite less than 1",
                    null, false, event.getUser(), null, null)).queue();
        }
        else
        {
            setBankOrCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), wealth[1]-amount, "cash");
            setBankOrCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), wealth[0]+amount, "bank");
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    ECONOMY_COLOR, "", ":white_check_mark: Deposited **"+amount+ "** "+CURRENCY +" to your bank",
                    null, false, event.getUser(), null, null)).queue();
        }
    }

    /**
     *
     *
     * */
    protected void robCommand(SlashCommandInteraction event) throws SQLException
    {
        event.deferReply().queue();

        if (event.getOption("user").getAsUser().equals(event.getUser()))
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: You can't rob yourself",
                    null, false, event.getUser(), null, null)).queue();
            return;
        }

        long membersCash = getWealth(event.getOption("user").getAsUser().getIdLong(), event.getGuild().getIdLong())[1];
        long[] authorsWealth = getWealth(event.getUser().getIdLong(), event.getGuild().getIdLong());

        if (membersCash <= 0)
        {
            String description = ":x: There is no cash to rob.";
            if (authorsWealth[0] > 3)
            {
                long lostValue = (long) (authorsWealth[0]*0.25);
                setBankOrCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), authorsWealth[1]-lostValue, "cash");
                description += " You got caught robbing and paid **"+lostValue+"** "+ CURRENCY;
            }
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    ECONOMY_COLOR, "", description,
                    null, false, event.getUser(), null, null)).queue();
        }
        else
        {
            setBankOrCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), authorsWealth[0]+membersCash, "cash");
            setBankOrCash(event.getOption("user").getAsUser().getIdLong(), event.getGuild().getIdLong(), 0, "cash");
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    ECONOMY_COLOR, "", ":white_check_mark: Successfully robbed "+
                            event.getOption("user").getAsMember().getAsMention()+" and got **"+membersCash+"** "+CURRENCY,
                    null, false, event.getUser(), null, null)).queue();
        }
    }

    /**
     * TODO:
     *
     *
     * */
    protected void spinCommand(SlashCommandInteraction event) throws SQLException
    {
        event.deferReply().queue();
        long guildId = event.getGuild().getIdLong();
        long userId = event.getUser().getIdLong();
        long usersCash = getWealth(userId,guildId)[1];

        // command options
        // color option
        long color = -1;
        if (event.getOption("color").getAsString().equals("red")) { color = 0;}
        else if (event.getOption("color").getAsString().equals("black")) { color = 1;}

        // field option
        long field = -1;
        OptionMapping fieldOption = event.getOption("field");
        if (fieldOption != null && fieldOption.getAsInt() > 0 && fieldOption.getAsInt() < 37)
        {
            field = fieldOption.getAsInt();
        }

        // amount option
        long amount = usersCash;
        if (event.getOption("money") != null)
        {
            amount = event.getOption("money").getAsLong();
        }

        if (color == -1)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: Please choose red or black as color",
                    null, false, event.getUser(), null, null
            )).queue();
        }
        else if (amount > getWealth(userId, guildId)[1] || amount <= 0)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: You don't have enough money or the entered amount is less than 1",
                    null, false, event.getUser(), null, null
            )).queue();
        }
        else
        {
            if (currentSpins.containsKey(guildId))
            {
                if (currentSpins.get(guildId).containsKey(userId))
                {
                    event.getHook().editOriginalEmbeds(utils.createEmbed(
                            Color.red, "", ":x: You already bet in this round",
                            null, false, event.getUser(), null, null
                    )).queue();
                    return;
                }
                else
                {
                    currentSpins.get(guildId).put(userId, new Long[] { event.getChannel().getIdLong(), color, field, amount});
                }
            }
            else
            {
                HashMap<Long, Long[]> guildMap = new HashMap<>();
                guildMap.put(userId, new Long[] { color, field, amount});
                currentSpins.put(guildId, guildMap);

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
            event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                    ":white_check_mark: You bet **"+amount+"** "+CURRENCY, null,
                    false, event.getUser(), null, "10 sec remaining")).queue();


        }

    }

    /**
     *  TODO
     *
     * */
    protected static void spinResult()
    {
        System.out.println("Spin Result");
    }

    /**
     *
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void addIncomeRoleCommand(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        long role_id = event.getOption("role").getAsRole().getIdLong();
        long income = event.getOption("income").getAsLong();
        Color color = ECONOMY_COLOR;
        String description;
        try {
            if (!utils.onQuery("SELECT income FROM IncomeRole WHERE role_id="+role_id).isClosed())
            {
                utils.onExecute("UPDATE IncomeRole SET income="+income+"WHERE role_id="+role_id);
                description = ":white_check_mark: Updated income from "+event.getOption("role").getAsRole().getAsMention()+" to "
                        +income;
            }
            else
            {
                utils.onExecute("INSERT INTO IncomeRole VALUES("+event.getGuild().getIdLong()+","+role_id+ ","+income+")");
                description = ":white_check_mark: Added Income Role "+event.getOption("role").getAsRole().getAsMention()+" with **"
                        +income+"** "+CURRENCY +" income";
            }
        } catch (SQLException e) {
            description = "An error occurred";
            color = Color.red;
        }
        event.getHook().editOriginalEmbeds(utils.createEmbed(color, "",
                description,null, false, event.getUser(), null, null)).queue();
    }

    /**
     *
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void getIncomeRoleCommand(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        String lDescription = "";
        try
        {
            ResultSet rs = utils.onQuery("SELECT * FROM IncomeRole WHERE guild_id="+event.getGuild().getIdLong());
            while(rs.next())
            {
                lDescription += event.getJDA().getRoleById(rs.getLong(2)).getAsMention() +" • **"+rs.getLong(3)+ "** "+CURRENCY;
            }
        }
        catch (SQLException e)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    "An error occurred",null, false, event.getUser(), null, null)).queue();
        }
        event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "Income Roles",
                lDescription,null, false, null, null, null)).queue();
    }

    /**
     *
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void deleteIncomeRoleCommand(SlashCommandInteractionEvent event) throws SQLException
    {
        event.deferReply().queue();
        Role role = event.getOption("role").getAsRole();

        if (utils.onQuery("SELECT * FROM IncomeRole where guild_id="+event.getGuild().getIdLong()+
                " AND role_id="+role.getIdLong()).isClosed())
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    ":x: There is no income connected with "+role.getAsMention(),null, false, event.getUser(), null, null)).queue();
        }
        else
        {
            utils.onExecute("DELETE FROM IncomeRole WHERE guild_id="+event.getGuild().getIdLong()+
                                        " AND role_id="+role.getIdLong());
            event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                    ":white_check_mark: Income Role "+role.getAsMention()+" deleted",null, false, event.getUser(), null, null)).queue();
        }
    }

    /**
     *
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void giveCommmand(SlashCommandInteractionEvent event) throws SQLException
    {
        event.deferReply().queue();
        User lUser = event.getUser();
        User lOtherUser = event.getOption("user").getAsUser();
        long lUsersCash = getWealth(lUser.getIdLong(), event.getGuild().getIdLong())[1];
        long lOtherUsersCash = getWealth(lOtherUser.getIdLong(), event.getGuild().getIdLong())[1];

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
            setBankOrCash(lUser.getIdLong(), event.getGuild().getIdLong(), lUsersCash-lAmount, "cash");
            setBankOrCash(lOtherUser.getIdLong(), event.getGuild().getIdLong(), lOtherUsersCash+lAmount, "cash");

            event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                    ":white_check_mark: You gave **"+lAmount+ "** "+CURRENCY +" to "+lOtherUser.getAsMention(),null,
                    false, event.getUser(), null, null)).queue();
        }
    }

    /**
     *
     *
     * */
    protected void giveAdminCommand(SlashCommandInteractionEvent event) throws SQLException
    {
        event.deferReply().queue();
        long amount = event.getOption("amount").getAsLong();
        User user = event.getOption("user").getAsUser();
        setBankOrCash(user.getIdLong(), event.getGuild().getIdLong(),
                getWealth(user.getIdLong(), event.getGuild().getIdLong())[1]+amount, "cash");

        event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                ":white_check_mark: You gave **"+amount+"** "+ CURRENCY +" to "+user.getAsMention(),null,
                false, event.getUser(), null, null)).queue();
    }

    // Other private commands

    /**
     * Gets the bank and cash values of money from a specific user on a server
     *
     * @param pGuildId - ID of the server on which the user is whose wealth should be returned
     * @param pMemberId - ID of the user whose wealth should be returned
     * @return Returns an int array in which the first index is the bank value and the second the cash
     * */
    protected long[] getWealth(long pMemberId, long pGuildId) throws SQLException
    {
            ResultSet rs = utils.onQuery("SELECT bank,cash FROM Economy WHERE guild_id="+pGuildId+" AND user_id="+pMemberId);
            rs.next();

            return new long[] { rs.getLong("bank"), rs.getLong("cash") };
    }

    /**
     * Updates the bank amount for a specific user on a server
     *
     * @param pGuildId - ID of the server on which the new user should be added to database
     * @param pUserId - ID of the user who should be added to database
     * @param pAmountOfMoney - Amount of money on which the bank value should be set
     * */
    protected void setBankOrCash(long pUserId, long pGuildId, long pAmountOfMoney, String pColumn) throws SQLException
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
            utils.onExecute("UPDATE Economy SET "+pColumn.toLowerCase()+" = ? " +
                "WHERE guild_id = ? AND user_id = ?", pAmountOfMoney, pGuildId, pUserId);

    }

    public static Color getCategoryColor() {return ECONOMY_COLOR; }
}
