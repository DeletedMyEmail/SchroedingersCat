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
 * @version 1.0.0 | last edit: 25.09.2022
 * */
public class EconomyHandler extends ListenerAdapter {

    // Emoji which represents the currency
    private static final String CURRENCY = "<:wiggle:935151967137832990>";

    /** Default color for this category to be used for embeds */
    private static final Color ECONOMY_COLOR = new Color(234,217,25);

    private final Utils utils;
    private HashMap<Long, HashMap<Long, Long[]>> currentSpins;

    public EconomyHandler(Utils pUtils) {
        this.utils = pUtils;
        currentSpins = new HashMap<>();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {

        switch (event.getName())
        {
            case "bal" -> balCommand(event);
            case "top" -> topCommand(event);
            case "crime" -> crimeCommand(event);
            case "rob" -> robCommand(event);
            case "dep" -> depCommand(event);
            case "with" -> withCommand(event);
            case "work" -> workCommand(event);
            case "spin" -> spinCommand(event);
            case "get_income_role" -> getIncomeRoleCommand(event);
            case "del_income_role" -> deleteIncomeRoleCommand(event);
            case "add_income_role" -> addIncomeRoleCommand(event);
            case "give" -> giveCommmand(event);
            case "give_admin" -> giveAdminCommand(event);
        }
    }

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event)
    {
        switch (event.getButton().getId())
        {
            case "EconomyBankButton" -> {
                event.editMessageEmbeds(topEmbed(event.getGuild().getIdLong(), "Bank")).queue();
                event.editButton(event.getButton().asDisabled()).queue();
            }
            case "EconomyCashButton" -> {
                event.editMessageEmbeds(topEmbed(event.getGuild().getIdLong(), "Cash")).queue();
                event.editButton(event.getButton().asDisabled()).queue();
            }
        }
    }

    /**
     * Inserts a new user to economy on a server.<br/>
     * Bank and cash values will be 0
     *
     * @param serverId - ID of the server on which the new user should be added to database
     * @param userId - ID of the user who should be added to database
     * */
    private void insertUser(long userId, long serverId)
    {
        try {
            utils.onExecute("INSERT INTO main.economy VALUES ("+serverId+","+userId+",0,0)");
        } catch (SQLException e) { System.out.println("Error while inserting user");}
    }

    private void insertUser(long userId, long serverId, long bank, long cash)
    {
        try {
            utils.onExecute("INSERT INTO main.economy VALUES ("+serverId+","+userId+","+bank+","+cash+")");
        } catch (SQLException e) { System.out.println("Error while inserting user");}
    }

    /**
     * Gets the bank and cash values of money from a specific user on a server.<br/>
     * If associated row doesn't exist in database, {@link #insertUser(long, long)} will be executed
     *
     * @param serverId - ID of the server on which the user is whose wealth should be returned
     * @param memberId - ID of the user whose wealth should be returned
     * @return Returns an int array in which the first index is the bank value and the second the cash
     * */
    protected long[] getWealth(long memberId, long serverId)
    {
        try {
            ResultSet rs = utils.onQuery("SELECT bank,cash FROM economy WHERE server_id="+serverId+" AND user_id="+memberId);

            return new long[] { rs.getLong("bank"), rs.getLong("cash") };

        } catch (SQLException e) {
            insertUser(memberId, serverId);
            return new long[] {0,0};
        }
    }

    /**
     *
     * */
    protected boolean guildAndMemberExistanceCheck(long memberId, long guildId)
    {
        Guild guild = Main.getJDA().getGuildById(guildId);
        try {
            if (guild == null)
                utils.onExecute("DELETE FROM economy WHERE server_id="+guildId);

            else if (guild.getMemberById(memberId) == null)
                utils.onExecute("DELETE FROM economy WHERE server_id="+guildId+" AND user_id="+memberId);

            else return true;
        }
        catch (SQLException sqlEx) {sqlEx.printStackTrace();}
        return false;
    }

    /**
     * Updates the bank amount for a specific user on a server.<br/>
     * If associated row doesn't exist in database, {@link #insertUser(long, long)} will be executed
     *
     * @param serverId - ID of the server on which the new user should be added to database
     * @param userId - ID of the user who should be added to database
     * @param amount - Amount of money on which the bank value should be set
     * */
    protected void setBankOrCash(long userId, long serverId, long amount, String colum)
    {
        try {
            utils.onExecute("UPDATE economy SET "+colum.toLowerCase()+"="+amount+" " +
                    "WHERE server_id="+serverId+" AND user_id="+userId);
        } catch (SQLException e)
        {
            if (colum.equals("cash")) insertUser(userId, serverId, 0, amount);
            if (colum.equals("bank")) insertUser(userId, serverId, amount, 0);
        }
    }

    /**
     * Gives a user an amount of cash between 1500 and 2500
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void balCommand(@NotNull SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        User user;
        if(event.getOption("user") != null) user = event.getOption("user").getAsUser();
        else user = event.getUser();

        long[] wealth = getWealth(user.getIdLong(), event.getGuild().getIdLong());
        String[][] fields = {
                {"Bank",Long.toString(wealth[0])},
                {"Cash",Long.toString(wealth[1])},
        };

        event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "", "", fields, false, user, null, null)).queue();
    }

    /**
     * Gives a user an amount of cash between 1500 and 2500
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void workCommand(@NotNull SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        long serverId = event.getGuild().getIdLong();
        long memberId = event.getMember().getIdLong();

        long currentCash = getWealth(memberId, event.getGuild().getIdLong())[1];

        Random rnd = new Random();
        int gainedAmount = rnd.nextInt(2000)+1000;

        setBankOrCash(memberId, serverId, currentCash+gainedAmount, "cash");

        String description = ":white_check_mark: You earned "+ gainedAmount+ CURRENCY;

        event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "", description, null, false, event.getUser(), null, null)).queue();
    }

    /**
     * Can give a user an amount of cash between 2500 and 4500, but there is a chance of 1/3 to lose it
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void crimeCommand(@NotNull SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        long serverId = event.getGuild().getIdLong();
        long memberId = event.getMember().getIdLong();

        long currentCash = getWealth(memberId, event.getGuild().getIdLong())[1];

        Random rnd = new Random();
        int gainedAmount = rnd.nextInt(2000) + 2500;
        if (rnd.nextInt(3) == 0)
        {
            setBankOrCash(memberId, serverId, currentCash - gainedAmount, "cash");
            String description = ":x: You got caught while commiting a crime and paid " + gainedAmount + CURRENCY;
            event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "", description, null, false, event.getUser(), null, null)).queue();
        }
        else
        {
            setBankOrCash(memberId, serverId, currentCash + gainedAmount, "cash");
            String description = ":white_check_mark: You earned " + gainedAmount + CURRENCY;
            event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "", description, null, false, event.getUser(), null, null)).queue();
        }
    }

    /**
     * Gives the user the option to choose if bank or cash toplist should be displayed by pressing a button
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void topCommand(@NotNull SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        Button[] buttons = {
                Button.primary("EconomyBankButton", "Bank values"),
                Button.success("EconomyCashButton", "Cash values")
        };
        event.getHook().
                editOriginal("Choose which type of values you want to see").
                setActionRow(buttons).queue();
    }
    /**
     * Creates an embed containing the richest members on server as a top list
     *
     * @param pServerId - Server's id on which the members should be searched
     * @param  cashOrBank - Defines the type of value with should be displayed. <br/>
     *                    If it's not "cash" or "bank", the embed will display an empty list
     * @return Returns the mentioned embed
     * */
    protected MessageEmbed topEmbed(long pServerId, String cashOrBank)
    {
        MessageEmbed embed;
        ResultSet resultSet;
        StringBuilder description = new StringBuilder();
        int counter = 1;
        try
        {
            resultSet = utils.onQuery("SELECT DISTINCT user_id, "+cashOrBank.toLowerCase()+" FROM economy WHERE server_id="+pServerId+" ORDER BY cash DESC LIMIT 10");

            while(resultSet.next() && counter < 11)
            {
                Member member = Main.getJDA().getGuildById(pServerId).getMemberById(resultSet.getLong(1));
                if (member == null)
                {
                    utils.onExecute("DELETE FROM economy WHERE user_id="+resultSet.getLong(1));
                }
                else
                {
                    description.append("**").append(counter).append("** ").append(member.getAsMention()).append(" • ").append(NumberFormat.getInstance()
                            .format(resultSet.getLong(2))).append(CURRENCY +"\n");
                    counter++;
                }
            }

            if (description.toString().equals(""))
            {
                description.append("No aren't any users with ").append(cashOrBank).append(" value");
            }

            embed = utils.createEmbed(ECONOMY_COLOR, "TOP "+cashOrBank.toUpperCase()+" VALUES",
                    description.toString(), null, false, null, null, null);
        }
        catch (SQLException e)
        {
            embed = utils.createEmbed(ECONOMY_COLOR,"TOP "+cashOrBank.toUpperCase()+" VALUES",
                    "No aren't any users with "+cashOrBank+" value", null, false, null, null, null);
        }
        return embed;
    }

    /**
     * Withdraws money from the bank
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void withCommand(SlashCommandInteraction event)
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
                    ECONOMY_COLOR, "", ":white_check_mark: Withdrawed "+amount+ CURRENCY +" from your bank",
                    null, false, event.getUser(), null, null)).queue();
        }
    }

    /**
     * Deposites money to the bank
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void depCommand(SlashCommandInteraction event)
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
                    ECONOMY_COLOR, "", ":white_check_mark: Deposited "+amount+ CURRENCY +" to your bank",
                    null, false, event.getUser(), null, null)).queue();
        }
    }

    /**
     *
     *
     * */
    protected void robCommand(SlashCommandInteraction event)
    {
        event.deferReply().queue();

        if (event.getOption("user").getAsUser().equals(event.getUser()))
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", "You can't rob yourself",
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
                description += " You got caught robbing and paid "+lostValue+ CURRENCY;
            }
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", description,
                    null, false, event.getUser(), null, null)).queue();
        }
        else
        {
            setBankOrCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), authorsWealth[0]+membersCash, "cash");
            setBankOrCash(event.getOption("user").getAsUser().getIdLong(), event.getGuild().getIdLong(), 0, "cash");
            event.getHook().editOriginalEmbeds(utils.createEmbed(
                    ECONOMY_COLOR, "", "Successfully robbed "+
                            event.getOption("user").getAsMember().getAsMention()+" and got "+membersCash,
                    null, false, event.getUser(), null, null)).queue();
        }
    }

    /**
     * TODO:
     *
     *
     * */
    protected void spinCommand(SlashCommandInteraction event)
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
                    ":white_check_mark: You bet "+ amount+ CURRENCY, null,
                    false, event.getUser(), null, "sec remaining")).queue();


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
            if (!utils.onQuery("SELECT income FROM income_roles WHERE role_id="+role_id).isClosed())
            {
                utils.onExecute("UPDATE income_roles SET income="+income+"WHERE role_id="+role_id);
                description = "Updated income from "+event.getOption("role").getAsRole().getAsMention()+" to "
                        +income;
            }
            else
            {
                utils.onExecute("INSERT INTO income_roles VALUES("+event.getGuild().getIdLong()+","+role_id+ ","+income+")");
                description = "Added Income Role "+event.getOption("role").getAsRole().getAsMention()+" with "
                        +income+ CURRENCY +" income";
            }
        } catch (SQLException e) {
            description = "An error occurred";
            color = Color.red;
            System.out.println(e);
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
        StringBuilder builder = new StringBuilder();
        try
        {
            ResultSet rs = utils.onQuery("SELECT * FROM income_roles where server_id="+event.getGuild().getIdLong());
            while(rs.next())
            {
                builder.append(event.getJDA().getRoleById(rs.getLong(2)).getAsMention() +" • "+rs.getLong(3)+ CURRENCY);
            }
        }
        catch (SQLException e)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    "An error occurred",null, false, event.getUser(), null, null)).queue();
        }
        event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "Income Roles",
                builder.toString(),null, false, null, null, null)).queue();
    }

    /**
     *
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void deleteIncomeRoleCommand(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        Role role = event.getOption("role").getAsRole();
        try
        {
            if (utils.onQuery("SELECT * FROM income_roles where server_id="+event.getGuild().getIdLong()+
                    " AND role_id="+role.getIdLong()).isClosed())
            {
                event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                        "There is no income connected with "+role.getAsMention(),null, false, event.getUser(), null, null)).queue();
            }
            else
            {
                utils.onExecute("DELETE FROM income_roles WHERE server_id="+event.getGuild().getIdLong()+
                                            " AND role_id="+role.getIdLong());
                event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                        "Income Role "+role.getAsMention()+" deleted",null, false, event.getUser(), null, null)).queue();
            }
        }
        catch (SQLException e)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    "An error occurred",null, false, event.getUser(), null, null)).queue();
        }
    }

    /**
     *
     *
     * @param event - SlashCommandInteractionEvent triggered by member
     * */
    protected void giveCommmand(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        User user = event.getOption("user").getAsUser();
        User author = event.getUser();
        long[] wealth_user = getWealth(user.getIdLong(), event.getGuild().getIdLong());
        long[] wealth_author = getWealth(author.getIdLong(), event.getGuild().getIdLong());
        long amount = wealth_user[1];
        if (event.getOption("amount") != null) { amount = event.getOption("amount").getAsLong(); }

        if (author.equals(user))
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    "You can't give yourself money",null,
                    false, event.getUser(), null, null)).queue();
        }
        else if (amount <= 0)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    "Can't give less than 1"+ CURRENCY,null,
                    false, event.getUser(), null, null)).queue();
        }
        else if (wealth_author[1] < amount)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    "You don't have enough "+ CURRENCY,null,
                    false, event.getUser(), null, null)).queue();
        }
        else
        {
            setBankOrCash(author.getIdLong(), event.getGuild().getIdLong(), wealth_author[1]-amount, "cash");
            setBankOrCash(user.getIdLong(), event.getGuild().getIdLong(), wealth_user[1]+amount, "cash");

            event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                    "Successfully given "+amount+ CURRENCY +" to "+user.getAsMention(),null,
                    false, event.getUser(), null, null)).queue();
        }
    }

    /**
     *
     *
     * */
    protected void giveAdminCommand(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        long amount = event.getOption("amount").getAsLong();
        User user = event.getOption("user").getAsUser();
        setBankOrCash(user.getIdLong(), event.getGuild().getIdLong(),
                getWealth(user.getIdLong(), event.getGuild().getIdLong())[1]+amount, "cash");

        event.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                "Successfully given "+amount+ CURRENCY +" to "+user.getAsMention(),null,
                false, event.getUser(), null, null)).queue();
    }

    public static Color getCategoryColor() {return ECONOMY_COLOR; }
}
