package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import de.schroedingerscat.entities.Pet;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Handles all commands related to pets and cat cards
 *
 * @author KaitoKunTatsu
 * @version 3.0.0 | last edit: 17.07.2023
 * */
public class CatsAndPetsHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    public static final Color CATS_AND_PETS_COLOR = new Color(165, 112, 17);
    private static final long SPAWN_COOLDOWN = 180000;

    private final HashMap<Long, HashMap<Long, Long>> mCatSpawnCooldown;
    private final HashMap<Long, Integer> mLastCatSpawned;
    private final Pet[] mPetsInStock;
    private final Random mRandom;
    private final Utils mUtils;

    public CatsAndPetsHandler(Utils pUtils) {
        mRandom = new Random();
        mUtils = pUtils;
        mCatSpawnCooldown = new HashMap<>();
        mLastCatSpawned = new HashMap<>();
        mPetsInStock = new Pet[3];

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    refreshPetStock();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 86400000);
    }

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent pEvent) {
        Utils.catchAndLogError(pEvent.getJDA(), () -> {
            if (pEvent.getId().startsWith("buy_pet")) {
                buyPet(pEvent.getButton().getId().charAt(8) - '0', pEvent);
            }
        });
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent pEvent) {
        Utils.catchAndLogError(pEvent.getHook(), () -> {
            switch (pEvent.getName()) {
                case "cat" -> spawnCatCommand(pEvent);
                case "set_catsandpets_channel" -> setCatsAndPetsChannelCommand(pEvent);
                case "cat_claim" -> claimCatCommand(pEvent);
                case "cat_inv" -> catInventoryCommand(pEvent);
                case "cat_view" -> viewCatCommand(pEvent);
                case "shop" -> shopCommand(pEvent);
            }
        });
    }

    private long getCooldown(long pGuildId, long pUserId) {
        mCatSpawnCooldown.putIfAbsent(pGuildId, new HashMap<>());
        long lCurrentTime = System.currentTimeMillis();
        return mCatSpawnCooldown.get(pGuildId).getOrDefault(pUserId, lCurrentTime) - lCurrentTime;
    }

    private boolean isSpawningChannel(long pGuildId, long pChannelId) throws SQLException {
        return mUtils.onQuery("SELECT catsandpets_channel_id FROM GuildSettings WHERE guild_id = ?", pGuildId).getLong("catsandpets_channel_id") == pChannelId;
    }

    private void spawnCatCommand(SlashCommandInteractionEvent pEvent) throws FileNotFoundException, SQLException {
        pEvent.deferReply().queue();
        long lCooldown = getCooldown(pEvent.getGuild().getIdLong(), pEvent.getUser().getIdLong());

        if (!isSpawningChannel(pEvent.getGuild().getIdLong(), pEvent.getChannel().getIdLong())) {
            pEvent.getHook().
                    editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: This is not the cat game channel", pEvent.getUser())).
                    queue();
        }
        else if (lCooldown > 0) {
            pEvent.getHook().
                    editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You can spawn a new cat in **" + TimeUnit.MILLISECONDS.toSeconds(lCooldown) + " seconds**", pEvent.getUser())).
                    queue();
        }
        else {
            int lNum = mRandom.nextInt(400) == 0 ? -1 : new Random().nextInt(97);
            FileInputStream lCatInStream = new FileInputStream("src/main/resources/catpics/katze"+lNum+".png");
            MessageEmbed lEmbed = Utils.createEmbed(CATS_AND_PETS_COLOR, "Cat Card #"+lNum, "", null, false, null, "attachment://cat.png",null);

            mCatSpawnCooldown.get(pEvent.getGuild().getIdLong()).put(pEvent.getUser().getIdLong(), System.currentTimeMillis() + SPAWN_COOLDOWN);
            mLastCatSpawned.put(pEvent.getGuild().getIdLong(), lNum);
            pEvent.getHook().editOriginalEmbeds(lEmbed).setAttachments(FileUpload.fromData(lCatInStream, "cat.png")).queue();
        }
    }

    private void setCatsAndPetsChannelCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();

        if (mUtils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        Channel lChannel = pEvent.getOption("channel").getAsChannel();
        if (lChannel.getType() != ChannelType.TEXT) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Please select a proper text channel", pEvent.getUser())).queue();
            return;
        }

        mUtils.onExecute("UPDATE GuildSettings SET catsandpets_channel_id = ? WHERE guild_id = ?", lChannel.getIdLong(), pEvent.getGuild().getIdLong());
        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(CATS_AND_PETS_COLOR, ":white_check_mark: Set the cat game channel to "+lChannel.getAsMention(), pEvent.getUser())).queue();
    }

    private void claimCatCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();

        int lLastCat = mLastCatSpawned.getOrDefault(pEvent.getGuild().getIdLong(), -2);
        if (lLastCat != -2) {
            if (mUtils.onQuery("SELECT cat_number FROM CatCards WHERE guild_id = ? AND user_id = ? AND cat_number = ?", pEvent.getGuild().getIdLong(), pEvent.getUser().getIdLong(), lLastCat).next()) {
                pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You already claimed this cat", pEvent.getUser())).queue();
            }
            else {
                mLastCatSpawned.remove(pEvent.getGuild().getIdLong());
                mUtils.onExecute("INSERT INTO CatCards (guild_id, user_id, cat_number) VALUES (?, ?, ?)", pEvent.getGuild().getIdLong(), pEvent.getUser().getIdLong(), lLastCat);
                pEvent.getHook().
                        editOriginalEmbeds(Utils.createEmbed(CATS_AND_PETS_COLOR, ":white_check_mark: Congratulations, **cat number " + lLastCat + "** is yours now!", pEvent.getUser())).
                        queue();
            }
        }
        else {
            pEvent.getHook().
                    editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Looks like someone was faster and claimed your cat or no cat spawned on this server", pEvent.getUser())).
                    queue();
        }
    }

    private void catInventoryCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();

        ResultSet lCatInventory = mUtils.onQuery("SELECT cat_number FROM CatCards WHERE guild_id = ? AND user_id = ? ORDER BY cat_number ASC", pEvent.getGuild().getIdLong(), pEvent.getUser().getIdLong());
        StringBuilder lCatInventoryString = new StringBuilder();
        lCatInventoryString.append("You own the following cats:\n");

        int i = 0;
        while (lCatInventory.next()) {
            if (i == 4) {
                lCatInventoryString.append("\n");
                i = 0;
            }
            else {
                ++i;
            }
            lCatInventoryString.append("**").append(lCatInventory.getInt("cat_number")).append("**,   ");
        }
        lCatInventoryString.deleteCharAt(lCatInventoryString.lastIndexOf(","));
        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(CATS_AND_PETS_COLOR, "Inventory", lCatInventoryString.toString(), null, false, pEvent.getUser(), null, null)).queue();
    }

    private void replyWithCat(InteractionHook pHook, int pCatNumber, MessageEmbed pEmbed) throws FileNotFoundException {
        FileInputStream lCatInStream = new FileInputStream("src/main/resources/catpics/katze" + pCatNumber + ".png");
        pHook.editOriginalEmbeds(pEmbed).setAttachments(FileUpload.fromData(lCatInStream, "cat.png")).queue();
    }

    private void viewCatCommand(SlashCommandInteractionEvent pEvent) throws SQLException, FileNotFoundException {
        pEvent.deferReply().queue();

        int lCatNumber = pEvent.getOption("cat") == null ? -2 : pEvent.getOption("cat").getAsInt();
        if (lCatNumber < -1 || lCatNumber > 96) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Invalid cat number", pEvent.getUser())).queue();
        }
        else {
            if (mUtils.onQuery("SELECT cat_number FROM CatCards WHERE guild_id = ? AND user_id = ? AND cat_number = ?", pEvent.getGuild().getIdLong(), pEvent.getUser().getIdLong(), lCatNumber).next()) {
                replyWithCat(
                    pEvent.getHook(),
                    lCatNumber,
                    Utils.createEmbed(CATS_AND_PETS_COLOR, "Cat Card #"+lCatNumber, "", null, false, null, "attachment://cat.png",null)
                );
            }
            else {
                pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You don't own this cat", pEvent.getUser())).queue();
            }
        }
    }

    private void shopCommand(SlashCommandInteractionEvent pEvent) throws IOException {
        pEvent.deferReply().queue();

        Utils.mergeImages("src/main/resources/pets/pet0.png", "src/main/resources/pets/pet1.png", "src/main/resources/pets/pet2.png", "src/main/resources/shop.jpg");
        FileInputStream lCatInStream = new FileInputStream("src/main/resources/shop.jpg");
        pEvent.getHook().
                editOriginalEmbeds(new EmbedBuilder().
                        setTitle("Pet Shop").
                        setDescription("Buy yourself a pet! The following pets are in stock today:").
                        setColor(CATS_AND_PETS_COLOR).
                        setImage("attachment://shop.jpg").
                        build()
                ).
                setActionRow(
                        Button.primary("buy_pet_0", "Buy Pet 1"),
                        Button.success("buy_pet_1", "Buy Pet 2"),
                        Button.danger("buy_pet_2", "Buy Pet 3")
                ).
                setAttachments(FileUpload.fromData(lCatInStream, "shop.jpg")).
                queue();
    }

    private void buyPet(int pPetIndex, ButtonInteractionEvent pEvent) throws SQLException, FileNotFoundException {
        Pet lPet = mPetsInStock[pPetIndex];
        User lUser = pEvent.getUser();

        if (lPet.price() > mUtils.getWealth(lUser.getIdLong(), pEvent.getGuild().getIdLong())[1]) {
            pEvent.getHook().sendMessageEmbeds(Utils.createEmbed(Color.red, ":x: You don't have enough money to buy this pet.\nTipp: /with", lUser)).setEphemeral(true).queue();
        }
        else {
            mUtils.onExecute("INSERT INTO PetInventory (guild_id, user_id, pet_id) VALUES (?, ?, ?)", pEvent.getGuild().getIdLong(), lUser.getIdLong(), lPet.id());
            mUtils.increaseBankOrCash(lUser.getIdLong(), pEvent.getGuild().getIdLong(), -lPet.price(), "cash");
            FileInputStream lPetImgStream = new FileInputStream("src/main/resources/pets/pet" + lPet.id() + ".png");
            pEvent.getChannel().sendMessageEmbeds(
                        new EmbedBuilder().
                                setDescription(":white_check_mark: You bought **" + lPet.name() + "** for " + NumberFormat.getInstance().format(lPet.price()) + " " + EconomyHandler.CURRENCY + "!").
                                setColor(Color.green).
                                setThumbnail("attachment://pet.png").
                                setAuthor(lUser.getName(), null, lUser.getAvatarUrl()).
                                build())
                    .addFiles(FileUpload.fromData(lPetImgStream, "pet.png")).queue();
        }
    }

    private void refreshPetStock() throws SQLException {
        for (int i = 0; i < mPetsInStock.length; i++) {
            mPetsInStock[i] = mUtils.getPet();
        }
    }
}