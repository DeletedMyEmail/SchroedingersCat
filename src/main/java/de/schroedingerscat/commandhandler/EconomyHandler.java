package de.schroedingerscat.commandhandler;

import de.schroedingerscat.BotApplication;
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
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Handles slash commands related to the bot's economy
 *
 * @author KaitoKunTatsu
 * @version 3.0.0 | last edit: 17.07.2023
 * */
public class EconomyHandler extends ListenerAdapter {

    // Emoji which represents the currency
    public static final String CURRENCY = "<:CatCoin:1129421009246765106>";
    /** Default color of this category to be used for embeds */
    public static final Color ECONOMY_COLOR = new Color(234,217,25);

    // HashMap<GuildId, List<Object>{color,number,List<Object>{user,boolean,amount}, List<Channel>},
    private final HashMap<Long, Object[]> mCurrentSpins;

    // giver's id, receiver's id
    private final HashMap<Long, Long> mReceiverFromGiveCommand;
    private final BotApplication mBotApplication;
    private final Utils mUtils;

    public EconomyHandler(Utils pUtils, BotApplication pBotApplication) {
        this.mUtils = pUtils;
        this.mBotApplication = pBotApplication;

        mCurrentSpins = new HashMap<>();
        mReceiverFromGiveCommand = new HashMap<>();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                distributeIncome();
            }
        }, 5000, 21600000);
    }

    // Events

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent pEvent) {
        if (pEvent.getModalId().equals("givemodal")) {
            Utils.catchAndLogError(pEvent.getHook(), () -> {
                pEvent.deferReply().queue();
                giveCommmand(pEvent.getHook(), pEvent.getMember(), pEvent.getGuild().getMemberById(mReceiverFromGiveCommand.get(pEvent.getMember().getIdLong())), Long.parseLong(pEvent.getValue("amount").getAsString()));
            });
        }
    }

    @Override
    public void onUserContextInteraction(UserContextInteractionEvent pEvent) {
        Utils.catchAndLogError(pEvent.getHook(), () -> {
            switch (pEvent.getName()) {
                case "bal" -> balCommand(pEvent, pEvent.getTargetMember());
                case "rob" -> robCommand(pEvent, pEvent.getTarget());
                case "give" -> giveModal(pEvent);
            }
        });
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent pEvent) {
        Utils.catchAndLogError(pEvent.getHook(), () -> {
            switch (pEvent.getName()) {
                case "bal" -> balCommand(pEvent);
                case "top" -> topCommand(pEvent);
                case "crime" -> crimeCommand(pEvent);
                case "rob" -> robCommand(pEvent,pEvent.getOption("user").getAsUser());
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
        });
    }

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent pEvent) {
        Utils.catchAndLogError(pEvent.getHook(), () -> {
            switch (pEvent.getButton().getId()) {
                case "EconomyBankButton" -> topEmbed(pEvent, "Bank");
                case "EconomyCashButton" -> topEmbed(pEvent, "Cash");
            }
        });
    }

    // Commands

    private void balCommand(@NotNull InteractionHook pHook, @NotNull Member pMember, Member pUserWhoseBalance) throws SQLException {
        if (pUserWhoseBalance == null) pUserWhoseBalance = pMember;

        long[] lWealth = mUtils.getWealth(pUserWhoseBalance.getIdLong(), pMember.getGuild().getIdLong());
        String[][] lFields = {
                {"Bank", Utils.formatPrice(lWealth[0]) },
                {"Cash", Utils.formatPrice(lWealth[1])},
        };

        pHook.editOriginalEmbeds(Utils.createEmbed(ECONOMY_COLOR, "", "", lFields, false, pUserWhoseBalance.getUser(), null, null)).queue();
    }

    private void balCommand(@NotNull GenericCommandInteractionEvent pEvent, Member pOtherMember) throws SQLException {
        pEvent.deferReply().queue();
        balCommand(pEvent.getHook(), pEvent.getMember(), pOtherMember);
    }

    private void balCommand(@NotNull SlashCommandInteractionEvent pEvent) throws SQLException {
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
    private void workCommand(@NotNull SlashCommandInteractionEvent pEvent) throws SQLException {
        long lCooldown = mUtils.getCooldownFor(pEvent.getMember().getIdLong(), pEvent.getGuild().getIdLong(), "work") - System.currentTimeMillis();

        if (lCooldown > 0) {
            pEvent.replyEmbeds(Utils.createEmbed(Color.red, ":x: Cooldown for this command ends in **"+TimeUnit.MILLISECONDS.toMinutes(lCooldown)+"** minutes", pEvent.getUser())).queue();
            return;
        }

        pEvent.deferReply().queue();
        long lGuildId = pEvent.getGuild().getIdLong();
        long lMemberId = pEvent.getMember().getIdLong();

        int lGainedMoney = new Random().nextInt(2000)+1000;

        mUtils.increaseBankOrCash(lMemberId, lGuildId, lGainedMoney, "cash");
        mUtils.setCooldownFor(lMemberId, lGuildId, "work", System.currentTimeMillis() + 180000);
        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(ECONOMY_COLOR,"",":white_check_mark: You earned **"+ Utils.formatPrice(lGainedMoney)+"**", null, false,pEvent.getUser(),null,null)).queue();
    }

    /**
     * Can give a user an amount of cash between 2500 and 4500, but there is a chance of 1/3 to lose it
     *
     * @param pEvent - SlashCommandInteractionEvent triggered by member
     * */
    private void crimeCommand(@NotNull SlashCommandInteractionEvent pEvent) throws SQLException {
        long lCooldown = mUtils.getCooldownFor(pEvent.getMember().getIdLong(), pEvent.getGuild().getIdLong(), "crime") - System.currentTimeMillis();

        if (lCooldown > 0) {
            pEvent.replyEmbeds(Utils.createEmbed(Color.red, ":x: Cooldown for this command ends in **"+TimeUnit.MILLISECONDS.toMinutes(lCooldown)+"** minutes", pEvent.getUser())).queue();
            return;
        }

        pEvent.deferReply().queue();
        long lGuildId = pEvent.getGuild().getIdLong();
        long lMemberId = pEvent.getMember().getIdLong();

        Random lRandom = new Random();
        int lGainedMoney = lRandom.nextInt(2000) + 2500;
        if (lRandom.nextInt(3) == 0) {
            mUtils.increaseBankOrCash(lMemberId, lGuildId, -lGainedMoney, "cash");
            String description = ":x: You got caught while commiting a crime and paid **"+Utils.formatPrice(lGainedMoney)+"**";
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(ECONOMY_COLOR, "", description, null, false, pEvent.getUser(), null, null)).queue();
        }
        else {
            mUtils.increaseBankOrCash(lMemberId, lGuildId, lGainedMoney, "cash");
            mUtils.setCooldownFor(lMemberId, lGuildId, "crime", System.currentTimeMillis() + 360000);

            pEvent.getHook().editOriginalEmbeds(
                    Utils.createEmbed(
                            ECONOMY_COLOR,
                            "",
                            ":white_check_mark: You earned **"+Utils.formatPrice(lGainedMoney)+"**",
                            null,
                            false,
                            pEvent.getUser(),
                            null,
                            null)
            ).queue();
        }
    }

    private void topCommand(@NotNull SlashCommandInteractionEvent pEvent) {
        pEvent.deferReply().queue();
        Button[] lButtons = {
                Button.primary("EconomyBankButton", Emoji.fromUnicode("U+1F3E6").getAsReactionCode()+" Bank values"),
                Button.success("EconomyCashButton", Emoji.fromUnicode("U+1F4B5").getAsReactionCode()+" Cash values")
        };
        pEvent.getHook().
                editOriginalEmbeds(Utils.createEmbed(ECONOMY_COLOR, "Choose which amount you want to see", pEvent.getUser())).
                setActionRow(lButtons).queue();
    }

    private void topEmbed(ButtonInteractionEvent pEvent, String pCashOrBank) throws SQLException {
        pEvent.deferEdit().queue();

        StringBuilder lDescription = new StringBuilder();
        long lGuildId = pEvent.getGuild().getIdLong();
        ResultSet lRs = mUtils.onQuery("SELECT "+pCashOrBank.toLowerCase()+", user_id FROM Economy WHERE guild_id = ? ORDER BY "+pCashOrBank.toLowerCase()+" DESC LIMIT 10", lGuildId);
        int lCounter = 1;

        while(lRs.next() && lCounter < 11) {
            Member lMember = mBotApplication.getJDA().getGuildById(lGuildId).getMemberById(lRs.getLong(2));
            if (lMember == null) {
                mUtils.onExecute("DELETE FROM Economy WHERE user_id = ? AND guild_id = ?", lRs.getLong(2), lGuildId);
            }
            else {
                lDescription.append("**").append(lCounter).append("** ").append(lMember.getAsMention()).append(" **").append(Utils.formatPrice(lRs.getLong(1))).append("** ").append("\n");
                lCounter++;
            }
        }

        if (lDescription.length() == 0) {
            lDescription = new StringBuilder("There are no users with " + pCashOrBank.toLowerCase() + " money");
        }

        pEvent.getMessage().editMessageEmbeds(Utils.createEmbed(ECONOMY_COLOR, "TOP "+pCashOrBank.toUpperCase()+" VALUES", lDescription.toString(), null, false, null, null, null)).queue();
    }

    private void withCommand(SlashCommandInteraction event) throws SQLException {
        event.deferReply().queue();
        long[] lUsersWealth = mUtils.getWealth(event.getUser().getIdLong(), event.getGuild().getIdLong());
        long lAmount = lUsersWealth[0];
        if (event.getOption("amount") != null) {
            lAmount = event.getOption("amount").getAsLong();
        }

        if (lAmount > lUsersWealth[0]) {
            event.getHook().editOriginalEmbeds(Utils.createEmbed(
                    Color.red, "", ":x: The given amount is higher than your bank value",
                    null, false, event.getUser(), null, null)).queue();

        }
        else if (lAmount <= 0) {
            event.getHook().editOriginalEmbeds(Utils.createEmbed(
                    Color.red, "", ":x: You can't with less than 1",
                    null, false, event.getUser(), null, null)).queue();
        }
        else {
            mUtils.increaseBankOrCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), +lAmount, "cash");
            mUtils.increaseBankOrCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), -lAmount, "bank");
            event.getHook().editOriginalEmbeds(Utils.createEmbed(
                    ECONOMY_COLOR, "", ":white_check_mark: Withdrawed **"+Utils.formatPrice(lAmount)+"*+ from your bank",
                    null, false, event.getUser(), null, null)).queue();
        }
    }

    private void depCommand(SlashCommandInteraction event) throws SQLException {
        event.deferReply().queue();
        long[] lWealth = mUtils.getWealth(event.getUser().getIdLong(), event.getGuild().getIdLong());
        long lAmountToDep = lWealth[1];

        if (event.getOption("amount") != null) {
            lAmountToDep = event.getOption("amount").getAsLong();
        }
        if (lAmountToDep > lWealth[1]) {
            event.getHook().editOriginalEmbeds(Utils.createEmbed(
                    Color.red, "", ":x: The given amount is higher than your cash value",
                    null, false, event.getUser(), null, null)).queue();
        }
        else if (lAmountToDep < 1) {
            event.getHook().editOriginalEmbeds(Utils.createEmbed(
                    Color.red, "", ":x: You can't deposite less than 1",
                    null, false, event.getUser(), null, null)).queue();
        }
        else {
            setBankAndCash(event.getUser().getIdLong(), event.getGuild().getIdLong(), lWealth[0]+lAmountToDep, lWealth[1]-lAmountToDep);
            event.getHook().editOriginalEmbeds(Utils.createEmbed(
                    ECONOMY_COLOR, "", ":white_check_mark: Deposited **"+Utils.formatPrice(lAmountToDep)+"** to your bank",
                    null, false, event.getUser(), null, null)).queue();
        }
    }



    private void robCommand(GenericCommandInteractionEvent pEvent, User pMemberToRob) throws SQLException {
        pEvent.deferReply().queue();
        User lRobber = pEvent.getUser();
        long lGuildId = pEvent.getGuild().getIdLong();
        long lCooldown = mUtils.getCooldownFor(lRobber.getIdLong(), lGuildId, "work") - System.currentTimeMillis();

        if (lCooldown > 0) {
            pEvent.replyEmbeds(Utils.createEmbed(Color.red, ":x: Cooldown for this command ends in **"+TimeUnit.MILLISECONDS.toMinutes(lCooldown)+"** minutes", lRobber)).queue();
            return;
        }

        if (lRobber.equals(pMemberToRob)) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(
                    Color.red, "", ":x: You can't rob yourself",
                    null, false, lRobber, null, null)).queue();
            return;
        }

        long lMemberToRobCash = mUtils.getWealth(pMemberToRob.getIdLong(), lGuildId)[1];
        long[] lRobberWealth = mUtils.getWealth(lRobber.getIdLong(), lGuildId);

        if (lMemberToRobCash <= 0) {
            String lDescription = ":x: "+pMemberToRob.getAsMention()+" doesn't have enough cash to rob.";
            long lostValue = (long) ((lRobberWealth[1]+lRobberWealth[0])*0.1);
            if (lostValue > 0) {
                mUtils.increaseBankOrCash(lRobber.getIdLong(), lGuildId, -lostValue, "cash");
                mUtils.setCooldownFor(lRobber.getIdLong(), lGuildId, "rob", System.currentTimeMillis() + 7200000);
                lDescription = ":x: You got caught robbing "+lGuildId+" and paid **"+Utils.formatPrice(lostValue)+"**";
            }
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(
                    ECONOMY_COLOR, "", lDescription,
                    null, false, lRobber, null, null)).queue();
        }
        else {
            mUtils.increaseBankOrCash(lRobber.getIdLong(), pEvent.getGuild().getIdLong(), +lMemberToRobCash, "cash");
            mUtils.increaseBankOrCash(pMemberToRob.getIdLong(), pEvent.getGuild().getIdLong(), -lMemberToRobCash, "cash");
            mUtils.setCooldownFor(lRobber.getIdLong(), pEvent.getGuild().getIdLong(), "rob", System.currentTimeMillis() + 7200000);
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(
                    ECONOMY_COLOR, "", ":white_check_mark: Successfully robbed "+
                            pMemberToRob.getAsMention()+" and got **"+Utils.formatPrice(lMemberToRobCash)+"**",
                    null, false, lRobber, null, null)).queue();
        }
    }

    private void giveModal(UserContextInteractionEvent pEvent) {
        mReceiverFromGiveCommand.put(pEvent.getUser().getIdLong(), pEvent.getTargetMember().getIdLong());
        pEvent.replyModal(
                Modal.create("givemodal", "Give Command")
                        .addActionRow(TextInput.create("amount", "Amount of money to give", TextInputStyle.SHORT).build())
                        .build()
        ).queue();
    }

    private void spinCommand(SlashCommandInteraction pEvent) throws SQLException {
        pEvent.deferReply().queue();

        Guild lGuild = pEvent.getGuild();
        User lUser = pEvent.getUser();
        final long lUsersCash = mUtils.getWealth(lUser.getIdLong(),lGuild.getIdLong())[1];
        final long lAmountToBet;
        final String lBetOnTheWheel;

        OptionMapping lGuessOption = pEvent.getOption("guess");
        if (lGuessOption != null && (
                lGuessOption.getAsString().equals("red")
                || lGuessOption.getAsString().equals("black")
                || (mUtils.isInteger(lGuessOption.getAsString()) && 0 < lGuessOption.getAsInt() && lGuessOption.getAsInt() < 37))
        ) {
            lBetOnTheWheel = lGuessOption.getAsString();
        }
        else {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(
                    Color.red, "", ":x: Your guess can only be a color (red, black) or a number between 1 (inclusive) and 36 (inclusive)",
                    null, false, lUser, null, null
            )).queue();
            return;
        }

        if (pEvent.getOption("money") != null) {
            lAmountToBet = pEvent.getOption("money").getAsLong();
        }
        else {
            lAmountToBet = lUsersCash;
        }

        if (lAmountToBet > lUsersCash || lAmountToBet <= 0) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(
                    Color.red, "", ":x: You don't have enough money or the entered amount is less than 1",
                    null, false, lUser, null, null
            )).queue();
        }
        else {
            Object[] lGuildSpinData = mCurrentSpins.get(lGuild.getIdLong());
            mUtils.increaseBankOrCash(lUser.getIdLong(), lGuild.getIdLong(), -lAmountToBet, "cash");

            if (lGuildSpinData == null) {
                // [0]: when the current round started ; [1]: String[] lSpinResult ; [2]: List<String[]> lMembersAndTheirBets ; [3]: List<Long> lChannelIds
                Random lRnd = new Random();
                String lWheelnumber = Integer.toString(lRnd.nextInt(36)+1);
                String lWheelColor = "red";
                if (lRnd.nextBoolean()) lWheelColor = "black";

                mCurrentSpins.put(lGuild.getIdLong(),new Object[]{
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
            else {
                List<String[]> lUsersSpinning = (ArrayList<String[]>) mCurrentSpins.get(lGuild.getIdLong())[2];
                lUsersSpinning.add(
                    new String[]{
                        lUser.getId(),
                        lBetOnTheWheel,
                        Long.toString(lAmountToBet)
                    }
                );
                List<Long> lGamblingChannels = (ArrayList<Long>) mCurrentSpins.get(lGuild.getIdLong())[3];
                if (!lGamblingChannels.contains(pEvent.getChannel().getIdLong())) {
                    lGamblingChannels.add(pEvent.getChannel().getIdLong());
                }
            }
            int lTimeLeft = 10- (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - (Long) mCurrentSpins.get(lGuild.getIdLong())[0]);

            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(ECONOMY_COLOR, "",
                    ":white_check_mark: You bet **"+Utils.formatPrice(lAmountToBet)+"** on **"+lBetOnTheWheel+"**",
                    null, false, lUser, null, lTimeLeft+" seconds remaining")).queue();
        }

    }

    private void spinResult(Guild pGuild) {
        Object[] lSpinsOnGuild = mCurrentSpins.get(pGuild.getIdLong());
        if (lSpinsOnGuild == null) return;

        // [0]: when the current round started ; [1]: String[] lSpinResult ; [2]: List<String[]> lMembersAndTheirBets ; [3]: List<Long> lChannelIds

        List<String[]> lMembersAndTheirBets = (ArrayList<String[]>) lSpinsOnGuild[2];
        String[] lSpinResult = ((String[]) lSpinsOnGuild[1]);
        List<Long> lChannelIds = (ArrayList<Long>) lSpinsOnGuild[3];
        StringBuilder lDescription = new StringBuilder();

        mCurrentSpins.remove(pGuild.getIdLong());

        lDescription.append("The wheel landed on: **"+lSpinResult[0]+" "+lSpinResult[1]+"**\n\n");
        lMembersAndTheirBets.forEach(memberAndBet -> {
            Member lMember = pGuild.getMemberById(memberAndBet[0]);
            if (lMember == null) return;

            if (memberAndBet[1].compareTo(lSpinResult[0]) == 0)
            {
                long lAmountWonOrLost = Long.parseLong(memberAndBet[2])*2;
                try {
                    mUtils.increaseBankOrCash(Long.parseLong(memberAndBet[0]), pGuild.getIdLong(), lAmountWonOrLost, "cash");
                    lDescription.append(lMember.getAsMention()+" **won "+Utils.formatPrice(lAmountWonOrLost)+"**\n");
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
                    mUtils.increaseBankOrCash(Long.parseLong(memberAndBet[0]), pGuild.getIdLong(), lAmountWonOrLost, "cash");
                    lDescription.append(lMember.getAsMention()+" **won "+Utils.formatPrice(lAmountWonOrLost)+"**\n");
                }
                catch (SQLException sqlEx)
                {
                    lDescription.append("Database error");
                }
            }
            else {
                lDescription.append(lMember.getAsMention()+" **lost "+Utils.formatPrice(-Long.parseLong(memberAndBet[2]))+"**\n");
            }
        });

        lChannelIds.forEach(channelId -> {
            TextChannel lTextChannel = pGuild.getTextChannelById(channelId);
            if (lTextChannel != null)
                lTextChannel.sendMessageEmbeds(Utils.createEmbed(ECONOMY_COLOR, "Gambling Results", lDescription.toString(), null, false, null, null, null)).queue();
        });

    }

    private void addIncomeRoleCommand(SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        if (mUtils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        long lRoleId = pEvent.getOption("role").getAsRole().getIdLong();
        long lIncome = pEvent.getOption("income").getAsLong();
        String lDescription;
        if (mUtils.onQuery("SELECT income FROM IncomeRole WHERE role_id = ?",lRoleId).next())
        {
            mUtils.onExecute("UPDATE IncomeRole SET income = ? WHERE role_id = ?", lIncome, lRoleId);
            lDescription = ":white_check_mark: Updated income from "+pEvent.getOption("role").getAsRole().getAsMention()+" to **"
                    +lIncome+"**";
        }
        else
        {
            mUtils.onExecute("INSERT INTO IncomeRole VALUES(?,?,?)", pEvent.getGuild().getIdLong(),lRoleId,lIncome);
            lDescription = ":white_check_mark: Added Income Role "+pEvent.getOption("role").getAsRole().getAsMention()+" with **"
                    +Utils.formatPrice(lIncome)+"** income";
        }
        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(ECONOMY_COLOR, lDescription, pEvent.getUser())
        ).queue();
    }

    private void getIncomeRolesCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();
        ResultSet rs = mUtils.onQuery("SELECT * FROM IncomeRole WHERE guild_id = ? ORDER BY income DESC", pEvent.getGuild().getIdLong());
        StringBuilder lDescription = new StringBuilder();

        while(rs.next()) {
            Role lRole = pEvent.getJDA().getRoleById(rs.getLong(2));
            if (lRole == null) {
                mUtils.onExecute("DELETE FROM IncomeRole WHERE role_id = ?", rs.getLong(2));
            }
            else {
                lDescription.append(lRole.getAsMention()).append(" • **").append(Utils.formatPrice(rs.getLong(3))).append("** ").append("\n");
            }
        }

        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(ECONOMY_COLOR, "Income Roles",
                lDescription.toString(),null, false, null, null, null)).queue();
    }

    private void removeIncomeRoleCommand(SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        if (mUtils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        Role role = pEvent.getOption("role").getAsRole();

        if (mUtils.onQuery("SELECT * FROM IncomeRole where guild_id="+pEvent.getGuild().getIdLong()+
                " AND role_id="+role.getIdLong()).isClosed())
        {
            pEvent.getHook().editOriginalEmbeds(mUtils.createEmbed(Color.red, "",
                    ":x: There is no income connected with "+role.getAsMention(),null, false, pEvent.getUser(), null, null)).queue();
        }
        else
        {
            mUtils.onExecute("DELETE FROM IncomeRole WHERE guild_id="+pEvent.getGuild().getIdLong()+
                                        " AND role_id="+role.getIdLong());
            pEvent.getHook().editOriginalEmbeds(mUtils.createEmbed(ECONOMY_COLOR, "",
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

    private void giveCommmand(InteractionHook pHook, Member pGiver, Member pReciever, long pAmount) throws SQLException
    {
        long lGiversCash = mUtils.getWealth(pGiver.getIdLong(), pGiver.getGuild().getIdLong())[1];
        if (pAmount == -1) pAmount = lGiversCash;

        if (pGiver.equals(pReciever))
        {
            pHook.editOriginalEmbeds(mUtils.createEmbed(Color.red, "",
                    ":x: You can't give yourself money",null,
                    false, pGiver.getUser(), null, null)).queue();
        }
        else if (pAmount <= 0)
        {
            pHook.editOriginalEmbeds(mUtils.createEmbed(Color.red, "",
                    ":x: Can't give less than 1 "+ CURRENCY,null,
                    false, pGiver.getUser(), null, null)).queue();
        }
        else if (lGiversCash < pAmount)
        {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, "",
                    ":x: You don't have enough "+ CURRENCY,null,
                    false, pGiver.getUser(), null, null)).queue();
        }
        else
        {
            mUtils.increaseBankOrCash(pGiver.getIdLong(), pGiver.getGuild().getIdLong(), -pAmount, "cash");
            mUtils.increaseBankOrCash(pReciever.getIdLong(), pGiver.getGuild().getIdLong(), pAmount, "cash");

            pHook.editOriginalEmbeds(Utils.createEmbed(ECONOMY_COLOR, "",
                    ":white_check_mark: You gave **"+Utils.formatPrice(pAmount)+ "** to "+pReciever.getAsMention(),null,
                    false, pGiver.getUser(), null, null)).queue();
        }
    }

    private void giveAdminCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();
        if (mUtils.memberNotAuthorized(pEvent.getMember(), "admin", pEvent.getHook())) return;

        long lAmount = pEvent.getOption("amount").getAsLong();
        User lUserToGiveTo = pEvent.getOption("user").getAsUser();
        mUtils.increaseBankOrCash(lUserToGiveTo.getIdLong(), pEvent.getGuild().getIdLong(), lAmount, "cash");

        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(ECONOMY_COLOR, "",
                ":white_check_mark: You gave **"+Utils.formatPrice(lAmount)+"** to "+lUserToGiveTo.getAsMention(),null,
                false, pEvent.getUser(), null, null)).queue();
    }

    private void distributeIncome() {
        try {
            ResultSet lRs = mUtils.onQuery("SELECT * FROM IncomeRole");

            while(lRs.next()) {
                try {
                    Guild lGuild = mBotApplication.getJDA().getGuildById(lRs.getLong("guild_id"));
                    if (lGuild == null) {
                        mUtils.onExecute("DELETE FROM IncomeRole WHERE guild_id = ?", lRs.getLong("guild_id"));
                        continue;
                    }
                    Role lRole = lGuild.getRoleById(lRs.getLong("role_id"));
                    if (lRole == null) {
                        mUtils.onExecute("DELETE FROM IncomeRole WHERE guild_id = ? AND role_id = ?", lRs.getLong("guild_id"), lRs.getLong("role_id"));
                        continue;
                    }
                    List<Member> lMemberWithRole = lGuild.findMembersWithRoles(lRole).get();
                    lMemberWithRole.forEach(member -> {
                        try {
                            mUtils.increaseBankOrCash(member.getIdLong(), lGuild.getIdLong(), lRs.getLong("income"), "cash");
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
     * @param pGuildId - ID of the server on which the new user should be added to database
     * @param pUserId - ID of the user who should be added to database
     * @param pNewBankValue
     * @param pNewCashValue
     * */
    private void setBankAndCash(long pUserId, long pGuildId, long pNewBankValue, long pNewCashValue) throws SQLException {
        ResultSet lRs = mUtils.onQuery("SELECT user_id FROM Economy WHERE guild_id = ? AND user_id = ?", pGuildId, pUserId);

        if (lRs.isClosed() || !lRs.next())
            mUtils.onExecute("INSERT INTO Economy VALUES (?,?,?,?)", pGuildId, pUserId, pNewBankValue, pNewCashValue);
        else
            mUtils.onExecute(
                    "UPDATE Economy SET bank = ?, cash = ? WHERE guild_id = ? AND user_id = ?",
                    pNewBankValue, pNewCashValue, pGuildId, pUserId);

    }
}
