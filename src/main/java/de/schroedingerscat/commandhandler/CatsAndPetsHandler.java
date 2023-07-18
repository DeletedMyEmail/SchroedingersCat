package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import de.schroedingerscat.entities.Pet;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 86400000);
    }

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent pEvent) {
        Utils.catchAndLogError(pEvent.getJDA(), () -> {
            if (pEvent.getButton().getId().startsWith("buy_pet")) {
                buyPet(pEvent.getButton().getId().charAt(8) - '0', pEvent);
            }
            else if (pEvent.getButton().getId().startsWith("left")) {
                cyclePetInv(pEvent, false);
            }
            else if (pEvent.getButton().getId().startsWith("right")) {
                cyclePetInv(pEvent, true);
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
                case "pet_shop" -> shopCommand(pEvent);
                case "pets" -> petInvCommand(pEvent);
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

    private void petInvCommand(SlashCommandInteractionEvent pEvent) throws SQLException, IOException {
        pEvent.deferReply().queue();

        if (pEvent.getOption("user") != null) {
            displayPetInv(pEvent.getGuild().getIdLong(), pEvent.getOption("user").getAsUser(), 0, pEvent.getHook());
        }
        else {
            displayPetInv(pEvent.getGuild().getIdLong(), pEvent.getUser(), 0, pEvent.getHook());
        }
    }

    private void displayPetInv(long pGuildId, User pUser, int pPage, InteractionHook pHook) throws SQLException, IOException {
        Pet[] lPets = getOnePageOfPetsFor(pUser.getIdLong(), pGuildId, pPage);

        if (lPets.length == 0 && pPage == 0) {
            pHook.editOriginalEmbeds(Utils.createEmbed(CATS_AND_PETS_COLOR, pUser.getEffectiveName() +"'s Pet Inventory", "An empty pet inventory is a bad pet inventory :(\n\n**Tipp:** /pet_shop", null, true, null, null, "1/1")).queue();
            return;
        }
        if (lPets.length == 0) {
            return;
        }

        String[][] lPetNames = new String[lPets.length][2];
        for (int i = 0; i < 3 &&  i < lPets.length; ++i) {
            lPetNames[i][0] = lPets[i].name();
            lPetNames[i][1] = "Level " + lPets[i].level()+"\nStrength: "+lPets[i].strength()+"\nHealth: "+lPets[i].health()+"\nSpeed: "+lPets[i].speed()+"\nPrice: "+Utils.formatPrice(lPets[i].price());
        }

        EmbedBuilder lEmbedBuilder = new EmbedBuilder();
        lEmbedBuilder.setFooter(pPage+1+"/"+Utils.roundUp(getNumberOfPetsFor(pUser.getIdLong(), pGuildId),3));
        lEmbedBuilder.setColor(CATS_AND_PETS_COLOR);
        lEmbedBuilder.setTitle(pUser.getName() +"'s Pet Inventory");
        lEmbedBuilder.setDescription("Your pets:");
        lEmbedBuilder.setImage("attachment://pets.png");

        for (int i = 0; i < 3 &&  i < lPets.length; ++i) {
            lEmbedBuilder.addField(lPetNames[i][0], lPetNames[i][1], true);
        }

        lEmbedBuilder.setThumbnail(pUser.getEffectiveAvatarUrl());
        pHook.editOriginalEmbeds(lEmbedBuilder.build()).
                setAttachments(FileUpload.fromData(mergePetImages(lPets), "pets.png")).
                setActionRow(
                        Button.primary("left_"+pUser.getId()+"_"+pPage, Emoji.fromUnicode("U+21E6").getAsReactionCode()),
                        Button.primary("right_"+pUser.getId()+"_"+pPage, Emoji.fromUnicode("U+21E8").getAsReactionCode())
                ).
                queue();
    }

    private void cyclePetInv(ButtonInteractionEvent pEvent, boolean pNext) throws SQLException, IOException {
        pEvent.deferEdit().queue();

        String[] lData = pEvent.getButton().getId().split("_");
        long lUserId = Long.parseLong(lData[1]);
        int lPage = Integer.parseInt(lData[2]);

        if (lPage == 0 && !pNext) {
            return;
        }
        else if (pNext) {
            ++lPage;
        }
        else {
            --lPage;
        }

        displayPetInv(pEvent.getGuild().getIdLong(), pEvent.getJDA().getUserById(lUserId), lPage, pEvent.getHook());
    }

    private void createShopImage(String pPetPath1, String pPetPath2, String pPetPath3, String pOutput) throws IOException {
        BufferedImage lImg1 = ImageIO.read(new File(pPetPath1));
        BufferedImage lImg2 = ImageIO.read(new File(pPetPath2));
        BufferedImage lImg3 = ImageIO.read(new File(pPetPath3));
        BufferedImage lMergedImg = new BufferedImage(lImg1.getWidth() + lImg2.getWidth() + lImg3.getWidth() + 160, Math.max(lImg1.getHeight(), Math.max(lImg2.getHeight(), lImg3.getHeight()))+80, BufferedImage.TYPE_INT_ARGB);

        Graphics2D lMergedImgGraphic = lMergedImg.createGraphics();
        lMergedImgGraphic.drawImage(lImg1, 0, 0, null);
        lMergedImgGraphic.drawImage(lImg2, lImg1.getWidth(), 0, null);
        lMergedImgGraphic.drawImage(lImg3, lImg1.getWidth() + lImg2.getWidth(), 0, null);

        ImageIO.write(lMergedImg, "png", new File(pOutput));
        lMergedImgGraphic.dispose();
    }

    private byte[] mergePetImages(Pet... pPets) throws IOException {
        BufferedImage[] lImageFiles = new BufferedImage[pPets.length];
        int lWidth = 0;
        int lHeight = 0;

        for (int i = 0; i < pPets.length; ++i) {
            lImageFiles[i] = ImageIO.read(new File(getPetPath(pPets[i].id())));
            lWidth += lImageFiles[i].getWidth();
            lHeight = Math.max(lHeight, lImageFiles[i].getHeight());
        }

        BufferedImage lMergedImg = new BufferedImage(lWidth, lHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D lMergedImgGraphic = lMergedImg.createGraphics();

        int lX = 0;
        for (int i = 0; i < lImageFiles.length; ++i) {
            lMergedImgGraphic.drawImage(lImageFiles[i], lX, 40, null);
            lX += lImageFiles[i].getWidth();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(lMergedImg, "png", baos);
        return baos.toByteArray();
    }

    private String getPetPath(int pPetId) {
        return "src/main/resources/pets/pet" + pPetId + ".png";
    }

    private int getNumberOfPetsFor(long pUserId, long pGuildId) throws SQLException {
        ResultSet lRs = mUtils.onQuery("SELECT COUNT(*) FROM PetInventory WHERE guild_id = ? AND user_id = ?", pGuildId, pUserId);
        lRs.next();
        return lRs.getInt(1);
    }

    private Pet[] getOnePageOfPetsFor(long pUserId, long pGuildId, int pPage) throws SQLException {
        ArrayList<Pet> lPets = new ArrayList<>();
        ResultSet lRs = mUtils.onQuery("SELECT id, name, description, price, strength, health, speed, level FROM Pet JOIN PetInventory ON pet_id = id WHERE guild_id = ? AND user_id = ? ORDER BY pet_id ASC LIMIT ?, 3", pGuildId, pUserId, pPage*3);
        for (int i = 0; i < 3 && lRs.next(); ++i) {
            lPets.add(new Pet(lRs.getInt("id"), lRs.getString("name"), lRs.getString("description"), lRs.getInt("price"), lRs.getInt("level"), lRs.getInt("strength"), lRs.getInt("health"), lRs.getInt("speed")));
        }

        return lPets.toArray(new Pet[lPets.size()]);
    }

    private void shopCommand(SlashCommandInteractionEvent pEvent) throws IOException {
        pEvent.deferReply().queue();

        FileInputStream lCatInStream = new FileInputStream("src/main/resources/shop.png");
        pEvent.getHook().
                editOriginalEmbeds(new EmbedBuilder().
                        setTitle("Pet Shop").
                        setDescription("Buy yourself a pet! The following pets are in stock today:").
                        setColor(CATS_AND_PETS_COLOR).
                        addField(mPetsInStock[0].name(), Utils.formatPrice(mPetsInStock[0].price()), true).
                        addField(mPetsInStock[1].name(), Utils.formatPrice(mPetsInStock[1].price()), true).
                        addField(mPetsInStock[2].name(), Utils.formatPrice(mPetsInStock[2].price()), true).
                        setImage("attachment://shop.png").
                        build()
                ).
                setActionRow(
                        Button.primary("buy_pet_0", "Buy Pet 1"),
                        Button.success("buy_pet_1", "Buy Pet 2"),
                        Button.danger("buy_pet_2", "Buy Pet 3")
                ).
                setAttachments(FileUpload.fromData(lCatInStream, "shop.png")).
                queue();
    }

    private void buyPet(int pPetIndex, ButtonInteractionEvent pEvent) throws SQLException, FileNotFoundException {
        pEvent.deferReply().queue();
        Pet lPet = mPetsInStock[pPetIndex];
        User lUser = pEvent.getUser();

        if (lPet.price() > mUtils.getWealth(lUser.getIdLong(), pEvent.getGuild().getIdLong())[1]) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You don't have enough money to buy this pet.\n**Tipp:** /with", lUser)).queue();
        }
        else if (ownsPet(lUser.getIdLong(), pEvent.getGuild().getIdLong(), lPet.id())) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You already own this pet", lUser)).queue();
        }
        else {
            mUtils.onExecute("INSERT INTO PetInventory (guild_id, user_id, pet_id, level) VALUES (?, ?, ?, 1)", pEvent.getGuild().getIdLong(), lUser.getIdLong(), lPet.id());
            mUtils.increaseBankOrCash(lUser.getIdLong(), pEvent.getGuild().getIdLong(), -lPet.price(), "cash");
            FileInputStream lPetImgStream = new FileInputStream(getPetPath(lPet.id()));
            pEvent.getHook().editOriginalEmbeds(
                        new EmbedBuilder().
                                setDescription(":white_check_mark: You bought **" + lPet.name() + "** for " + Utils.formatPrice(lPet.price()) + "!").
                                setColor(Color.green).
                                setThumbnail("attachment://pet.png").
                                setAuthor(lUser.getName(), null, lUser.getAvatarUrl()).
                                build()).
                    setAttachments(FileUpload.fromData(lPetImgStream, "pet.png")).
                    queue();
        }
    }

    private boolean ownsPet(long pUserId, long pGuildId, int pPetId) throws SQLException {
        ResultSet lRs = mUtils.onQuery("SELECT pet_id FROM PetInventory WHERE guild_id = ? AND user_id = ? AND pet_id = ?", pGuildId, pUserId, pPetId);
        return lRs.next();
    }

    private void refreshPetStock() throws SQLException, IOException {
        ResultSet lRs = mUtils.onQuery("SELECT * FROM Pet ORDER BY RANDOM() LIMIT 3");
        for (int i = 0; i < 3; ++i) {
            lRs.next();
            mPetsInStock[i] = new Pet(lRs.getInt("id"), lRs.getString("name"), lRs.getString("description"), lRs.getInt("price"), 1, lRs.getInt("strength"), lRs.getInt("health"), lRs.getInt("speed"));
        }
        createShopImage(getPetPath(mPetsInStock[0].id()), getPetPath(mPetsInStock[1].id()), getPetPath(mPetsInStock[2].id()), "src/main/resources/shop.png");
    }
}