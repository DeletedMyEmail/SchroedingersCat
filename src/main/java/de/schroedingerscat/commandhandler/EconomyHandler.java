package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Main;
import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private final HashMap<Long, Object[]> currentSpins;
    // Command cooldown: HashMap<userId, guildId, HashMap<guildId, List<[when work cooldown end, when crime cooldown ends, when rob cooldown ends]>
    private final HashMap<Long, HashMap<Long, Long[]>> commandsCooldown;
    // giver's id, receiver's id
    private final HashMap<Long, Long> receiverFromGiveCommand;
    private final Utils utils;

    public EconomyHandler(Utils pUtils) {
        this.utils = pUtils;
        currentSpins = new HashMap<>();
        receiverFromGiveCommand = new HashMap<>();
        commandsCooldown = new HashMap<>();

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
    public void onModalInteraction(@Nonnull ModalInteractionEvent pEvent) {
        try {
            if (pEvent.getModalId().equals("givemodal")) {
                pEvent.deferReply().queue();
                giveCommmand(
                        pEvent.getHook(),
                        pEvent.getMember(),
                        pEvent.getGuild().getMemberById(receiverFromGiveCommand.get(pEvent.getMember().getIdLong())),
                        Long.parseLong(pEvent.getValue("amount").getAsString())
                );
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
        catch (NullPointerException nullEx) {
            nullEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Invalid argument. Make sure you selected a valid text channel, message id, role and emoji", pEvent.getUser())).queue();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Unknown error occured", pEvent.getUser())).queue();
        }
    }

    @Override
    public void onUserContextInteraction(UserContextInteractionEvent pEvent)
    {
        try
        {
            switch (pEvent.getName())
            {
                case "bal" -> balCommand(pEvent, pEvent.getTargetMember());
                case "rob" -> robCommand(pEvent, pEvent.getTargetMember());
                case "give" -> {
                    receiverFromGiveCommand.put(pEvent.getUser().getIdLong(), pEvent.getTargetMember().getIdLong());
                    pEvent.replyModal(
                            Modal.create("givemodal", "Give Command")
                                    .addActionRow(TextInput.create("amount", "Amount of money to give", TextInputStyle.SHORT).build())
                                    .build()).queue();
                }
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
        catch (NullPointerException nullEx) {
            nullEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Invalid argument. Make sure you selected a valid text channel, message id, role and emoji", pEvent.getUser())).queue();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Unknown error occured", pEvent.getUser())).queue();
        }
    }

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
                case "rob" -> robCommand(pEvent,pEvent.getOption("user").getAsMember());
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
        catch (NullPointerException nullEx) {
            nullEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Invalid argument. Make sure you selected a valid text channel, message id, role and emoji", pEvent.getUser())).queue();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Unknown error occured", pEvent.getUser())).queue();
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
        catch (NumberFormatException numEx) {
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You entered an invalid number", pEvent.getUser())).queue();
        }
        catch (SQLException sqlEx)
        {
            sqlEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
        }
        catch (NullPointerException nullEx) {
            nullEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Invalid argument. Make sure you selected a valid text channel, message id, role and emoji", pEvent.getUser())).queue();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Unknown error occured", pEvent.getUser())).queue();
        }
    }

    // Commands

    /**
     *
     *
     * @param pHook
     * */
    private void balCommand(@NotNull InteractionHook pHook, @NotNull Member pMember, Member pUserWhoseBalance) throws SQLException
    {
        if (pUserWhoseBalance == null) pUserWhoseBalance = pMember;

        long[] lWealth = getWealth(pUserWhoseBalance.getIdLong(), pMember.getGuild().getIdLong());
        String[][] lFields = {
                {"Bank",NumberFormat.getInstance()
                        .format(lWealth[0])+" "+CURRENCY },
                {"Cash",NumberFormat.getInstance()
                        .format(lWealth[1])+" "+CURRENCY},
        };

        pHook.editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "", "", lFields, false, pUserWhoseBalance.getUser(), null, null)).queue();
    }

    private void balCommand(@NotNull GenericCommandInteractionEvent pEvent, Member pOtherMember) throws SQLException
    {
        pEvent.deferReply().queue();
        balCommand(pEvent.getHook(), pEvent.getMember(), pOtherMember);
    }

    private void balCommand(@NotNull SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        if (pEvent.getOption("user") == null)
            balCommand(pEvent.getHook(), pEvent.getMember(), null);
        else balCommand(pEvent.getHook(), pEvent.getMember(), pEvent.getOption("user").getAsMember());
    }

    /**
     * Gives a user an amount of cash between 1000 and 3000
     *
     * @param pEvent - SlashCommandInteractionEvent triggered by member
     * */
    private void workCommand(@NotNull SlashCommandInteractionEvent pEvent) throws SQLException
    {
        if (hasCooldown(pEvent, 0)) return;

        pEvent.deferReply().queue();
        long lGuildId = pEvent.getGuild().getIdLong();
        long lMemberId = pEvent.getMember().getIdLong();

        int lGainedMoney = new Random().nextInt(2000)+1000;

        increaseBankOrCash(lMemberId, lGuildId, lGainedMoney, "cash");
        setCooldownInMillis(lMemberId, lGuildId, 0, 180000);
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
        if (hasCooldown(pEvent, 1)) return;

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
            setCooldownInMillis(lMemberId, lGuildId, 1, 360000);
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

        else if (lAmountToDep < 1)
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

    private void robCommand(@NotNull GenericCommandInteractionEvent pEvent, Member pMemberToRob) throws SQLException
    {
        if (hasCooldown(pEvent, 2)) return;

        pEvent.deferReply().queue();
        robCommand(pEvent.getHook(), pEvent.getMember(), pMemberToRob);
    }

    /**
     *
     *
     * */
    private void robCommand(InteractionHook pHook, Member pRobber, Member pMemberToRob) throws SQLException
    {
        if (pRobber.equals(pMemberToRob))
        {
            pHook.editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: You can't rob yourself",
                    null, false, pRobber.getUser(), null, null)).queue();
            return;
        }

        long lMemberToRobCash = getWealth(pMemberToRob.getIdLong(), pRobber.getGuild().getIdLong())[1];
        long[] lRobberWealth = getWealth(pRobber.getUser().getIdLong(), pRobber.getGuild().getIdLong());

        if (lMemberToRobCash <= 0)
        {
            String lDescription = ":x: "+pMemberToRob.getAsMention()+" doesn't have enough cash to rob.";
            long lostValue = (long) ((lRobberWealth[1]+lRobberWealth[0])*0.1);
            if (lostValue > 0)
            {
                increaseBankOrCash(pRobber.getIdLong(), pRobber.getGuild().getIdLong(), -lostValue, "cash");
                setCooldownInMillis(pRobber.getIdLong(), pRobber.getGuild().getIdLong(), 2, 7200000);
                lDescription = ":x: You got caught robbing "+pMemberToRob.getAsMention()+" and paid **"+NumberFormat.getInstance()
                        .format(lostValue)+"** "+ CURRENCY;
            }
            pHook.editOriginalEmbeds(utils.createEmbed(
                    ECONOMY_COLOR, "", lDescription,
                    null, false, pRobber.getUser(), null, null)).queue();
        }
        else
        {
            increaseBankOrCash(pRobber.getIdLong(), pRobber.getGuild().getIdLong(), +lMemberToRobCash, "cash");
            increaseBankOrCash(pMemberToRob.getIdLong(), pRobber.getGuild().getIdLong(), -lRobberWealth[1], "cash");
            setCooldownInMillis(pRobber.getIdLong(), pRobber.getGuild().getIdLong(), 2, 7200000);
            pHook.editOriginalEmbeds(utils.createEmbed(
                    ECONOMY_COLOR, "", ":white_check_mark: Successfully robbed "+
                            pMemberToRob.getAsMention()+" and got **"+NumberFormat.getInstance()
                            .format(lMemberToRobCash)+"** "+CURRENCY,
                    null, false, pRobber.getUser(), null, null)).queue();
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

        Guild lGuild = pEvent.getGuild();
        User lUser = pEvent.getUser();
        final long lUsersCash = getWealth(lUser.getIdLong(),lGuild.getIdLong())[1];
        final long lAmountToBet;
        final String lBetOnTheWheel;

        OptionMapping lGuessOption = pEvent.getOption("guess");
        if (lGuessOption != null && (
                lGuessOption.getAsString().equals("red")
                || lGuessOption.getAsString().equals("black")
                || (utils.isInteger(lGuessOption.getAsString()) && 0 < lGuessOption.getAsInt() && lGuessOption.getAsInt() < 37))
        )
            lBetOnTheWheel = lGuessOption.getAsString();
        else {
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: Your guess can only be a color (red, black) or a number between 1 (inclusive) and 36 (inclusive)",
                    null, false, lUser, null, null
            )).queue();
            return;
        }

        if (pEvent.getOption("money") != null)
            lAmountToBet = pEvent.getOption("money").getAsLong();
        else lAmountToBet = lUsersCash;

        if (lAmountToBet > lUsersCash || lAmountToBet <= 0)
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(
                    Color.red, "", ":x: You don't have enough money or the entered amount is less than 1",
                    null, false, lUser, null, null
            )).queue();

        else
        {
            Object[] lGuildSpinData = currentSpins.get(lGuild.getIdLong());
            increaseBankOrCash(lUser.getIdLong(), lGuild.getIdLong(), -lAmountToBet, "cash");

            if (lGuildSpinData == null)
            {
                // [0]: when the current round started ; [1]: String[] lSpinResult ; [2]: List<String[]> lMembersAndTheirBets ; [3]: List<Long> lChannelIds
                Random lRnd = new Random();
                String lWheelnumber = Integer.toString(lRnd.nextInt(36)+1);
                String lWheelColor = "red";
                if (lRnd.nextBoolean()) lWheelColor = "black";

                currentSpins.put(lGuild.getIdLong(),new Object[]{
                        System.currentTimeMillis(),
                        new String[] {
                                lWheelColor,
                                lWheelnumber
                        },
                        new ArrayList<String[]>(){{
                            add(new String[]{
                                lUser.getId(),
                                lBetOnTheWheel,
                                Long.toString(lAmountToBet)
                            });
                        }},
                        new ArrayList<Long>(){{
                            add(pEvent.getChannel().getIdLong());
                        }},
                });

                Timer timer = new Timer();
                timer.schedule(new TimerTask()
                {
                    @Override
                    public void run() {
                        spinResult(lGuild);
                    }
                }, 10000);
            }
            else
            {
                List<String[]> lUsersSpinning = (ArrayList<String[]>) currentSpins.get(lGuild.getIdLong())[2];
                lUsersSpinning.add(
                    new String[]{
                        lUser.getId(),
                        lBetOnTheWheel,
                        Long.toString(lAmountToBet)
                    }
                );
                List<Long> lGamblingChannels = (ArrayList<Long>) currentSpins.get(lGuild.getIdLong())[3];
                if (!lGamblingChannels.contains(pEvent.getChannel().getIdLong()))
                    lGamblingChannels.add(pEvent.getChannel().getIdLong());
            }
            int lTimeLeft = 10- (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - (Long) currentSpins.get(lGuild.getIdLong())[0]);

            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                    ":white_check_mark: You bet **"+NumberFormat.getInstance()
                            .format(lAmountToBet)+"** "+CURRENCY+ " on **"+lBetOnTheWheel+"**", null,
                    false, lUser, null, lTimeLeft+" seconds remaining")).queue();
        }

    }

    /**
     *
     *
     * */
    private void spinResult(Guild pGuild)
    {
        Object[] lSpinsOnGuild = currentSpins.get(pGuild.getIdLong());
        if (lSpinsOnGuild == null) return;

        // [0]: when the current round started ; [1]: String[] lSpinResult ; [2]: List<String[]> lMembersAndTheirBets ; [3]: List<Long> lChannelIds

        List<String[]> lMembersAndTheirBets = (ArrayList<String[]>) lSpinsOnGuild[2];
        String[] lSpinResult = ((String[]) lSpinsOnGuild[1]);
        List<Long> lChannelIds = (ArrayList<Long>) lSpinsOnGuild[3];
        StringBuilder lDescription = new StringBuilder();

        currentSpins.remove(pGuild.getIdLong());

        lDescription.append("The wheel landed on: **"+lSpinResult[0]+" "+lSpinResult[1]+"**\n\n");
        lMembersAndTheirBets.forEach(memberAndBet -> {
            Member lMember = pGuild.getMemberById(memberAndBet[0]);
            if (lMember == null) return;

            if (memberAndBet[1].compareTo(lSpinResult[0]) == 0)
            {
                long lAmountWonOrLost = Long.parseLong(memberAndBet[2])*2;
                try {
                    increaseBankOrCash(Long.parseLong(memberAndBet[0]), pGuild.getIdLong(), lAmountWonOrLost, "cash");
                    lDescription.append(lMember.getAsMention()+" **won "+NumberFormat.getInstance().format(lAmountWonOrLost)+"**\n");
                }
                catch (SQLException sqlEx)
                {
                    lDescription.append("Database error");
                }
            }
            else if (memberAndBet[1].compareTo(lSpinResult[1]) == 0)
            {
                long lAmountWonOrLost = Long.parseLong(memberAndBet[2])*20;
                try {
                    increaseBankOrCash(Long.parseLong(memberAndBet[0]), pGuild.getIdLong(), lAmountWonOrLost, "cash");
                    lDescription.append(lMember.getAsMention()+" **won "+NumberFormat.getInstance().format(lAmountWonOrLost)+"**\n");
                }
                catch (SQLException sqlEx)
                {
                    lDescription.append("Database error");
                }
            }
            else {
                lDescription.append(lMember.getAsMention()+" **lost "+NumberFormat.getInstance().format(-Long.parseLong(memberAndBet[2]))+"**\n");
            }
        });

        lChannelIds.forEach(channelId -> {
            TextChannel lTextChannel = pGuild.getTextChannelById(channelId);
            if (lTextChannel != null)
                lTextChannel.sendMessageEmbeds(utils.createEmbed(ECONOMY_COLOR, "Gambling Results", lDescription.toString(), null, false, null, null, null)).queue();
        });

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

    private void giveCommmand(@NotNull SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        long lAmount = -1;
        if (pEvent.getOption("amount") != null) lAmount = pEvent.getOption("amount").getAsLong();

        giveCommmand(pEvent.getHook(), pEvent.getMember(), pEvent.getOption("user").getAsMember(), lAmount);
    }

    /**
     *
     *
     * @param pHook
     * */
    private void giveCommmand(InteractionHook pHook, Member pGiver, Member pReciever, long pAmount) throws SQLException
    {
        long lGiversCash = getWealth(pGiver.getIdLong(), pGiver.getGuild().getIdLong())[1];
        if (pAmount == -1) pAmount = lGiversCash;

        if (pGiver.equals(pReciever))
        {
            pHook.editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    ":x: You can't give yourself money",null,
                    false, pGiver.getUser(), null, null)).queue();
        }
        else if (pAmount <= 0)
        {
            pHook.editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    ":x: Can't give less than 1 "+ CURRENCY,null,
                    false, pGiver.getUser(), null, null)).queue();
        }
        else if (lGiversCash < pAmount)
        {
            pHook.editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    ":x: You don't have enough "+ CURRENCY,null,
                    false, pGiver.getUser(), null, null)).queue();
        }
        else
        {
            increaseBankOrCash(pGiver.getIdLong(), pGiver.getGuild().getIdLong(), -pAmount, "cash");
            increaseBankOrCash(pReciever.getIdLong(), pGiver.getGuild().getIdLong(), pAmount, "cash");

            pHook.editOriginalEmbeds(utils.createEmbed(ECONOMY_COLOR, "",
                    ":white_check_mark: You gave **"+NumberFormat.getInstance()
                            .format(pAmount)+ "** "+CURRENCY +" to "+pReciever.getAsMention(),null,
                    false, pGiver.getUser(), null, null)).queue();
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
     *
     *
     * */
    private boolean hasCooldown(GenericCommandInteractionEvent pEvent, int pCommandIndex)
    {
        long lCooldown = getCooldownInMillis(pEvent.getUser().getIdLong(), pEvent.getGuild().getIdLong(), pCommandIndex);
        if (lCooldown > 0) {
            pEvent.replyEmbeds(utils.createEmbed(Color.red, ":x: Cooldown for this command ends in **"+TimeUnit.MILLISECONDS.toMinutes(lCooldown)+"** minutes", pEvent.getUser())).queue();
            return true;
        }
        return false;
    }

    /**
     *
     *
     * */
    private long getCooldownInMillis(long pUserId, long pGuildId, int pCommandIndex) {
        if (commandsCooldown.get(pUserId) == null) return 0;
        Long[] lWhenCooldownsEnd = commandsCooldown.get(pUserId).get(pGuildId);
        if (lWhenCooldownsEnd == null) return 0;

        /*if (lWhenCooldownsEnd[pCommandIndex] - System.currentTimeMillis() <= 0) {
            if (lWhenCooldownsEnd[0] - System.currentTimeMillis() <= 0 && lWhenCooldownsEnd[1]- System.currentTimeMillis() <= 0 && lWhenCooldownsEnd[2] - System.currentTimeMillis()<= 0)
                commandsCooldown.get(pUserId).remove(pGuildId);
            return 0;
        }*/
        return lWhenCooldownsEnd[pCommandIndex] - System.currentTimeMillis();
    }


    /**
     *
     *
     *
     * */
    private void setCooldownInMillis(long pUserId, long pGuildId, int pCommandIndex, long pCooldownInMillis)
    {
        commandsCooldown.putIfAbsent(pUserId, new HashMap<>());
        HashMap<Long, Long[]> lUsersGuildWithCooldowns = commandsCooldown.get(pUserId);
        if (lUsersGuildWithCooldowns.get(pGuildId) != null)
            lUsersGuildWithCooldowns.get(pGuildId)[pCommandIndex] = System.currentTimeMillis() + pCooldownInMillis;
        else {
            Long[] lCooldowns = {0L,0L,0L};
            lCooldowns[pCommandIndex] = System.currentTimeMillis() + pCooldownInMillis;
            lUsersGuildWithCooldowns.put(pGuildId, lCooldowns);
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
                    "UPDATE Economy SET bank = ?, cash = ? WHERE guild_id = ? AND user_id = ?",
                    pNewBankValue, pNewCashValue, pGuildId, pUserId);

    }

    public static Color getCategoryColor() {return ECONOMY_COLOR; }
}
