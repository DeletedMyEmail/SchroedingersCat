package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Handles all commands related to the cat game
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.2.0 | last edit: 13.07.2023
 * */
public class CatGameHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    public static final Color CATGAME_COLOR = new Color(165, 112, 17);
    private static final long SPAWN_COOLDOWN = 180000;

    private final HashMap<Long, HashMap<Long, Long>> mCatSpawnCooldown;
    private final HashMap<Long, Integer> mLastCatSpawned;
    private final Random mRandom;
    private final Utils mUtils;

    public CatGameHandler(Utils pUtils) {
        mRandom = new Random();
        mUtils = pUtils;
        mCatSpawnCooldown = new HashMap<>();
        mLastCatSpawned = new HashMap<>();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent pEvent) {
        try {
            switch (pEvent.getName()) {
                case "cat" -> spawnCatCommand(pEvent);
                case "set_catgame_channel" -> setCatGameChannelCommand(pEvent);
                case "cat_claim" -> claimCatCommand(pEvent);
                case "cat_inv" -> catInventoryCommand(pEvent);
                case "cat_view" -> viewCatCommand(pEvent);
            }
        }
        catch (NumberFormatException numEx) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You entered an invalid number", pEvent.getUser())).queue();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Unknown error occured", pEvent.getUser())).queue();
        }
    }

    private long getCooldown(long pGuildId, long pUserId) {
        mCatSpawnCooldown.putIfAbsent(pGuildId, new HashMap<>());
        long lCurrentTime = System.currentTimeMillis();
        return mCatSpawnCooldown.get(pGuildId).getOrDefault(pUserId, lCurrentTime) - lCurrentTime;
    }

    private boolean isSpawningChannel(long pGuildId, long pChannelId) throws SQLException {
        return mUtils.onQuery("SELECT catgame_channel_id FROM GuildSettings WHERE guild_id = ?", pGuildId).getLong("catgame_channel_id") == pChannelId;
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
            MessageEmbed lEmbed = Utils.createEmbed(CATGAME_COLOR, "Cat Card #"+lNum, "", null, false, null, "attachment://cat.png",null);

            mCatSpawnCooldown.get(pEvent.getGuild().getIdLong()).put(pEvent.getUser().getIdLong(), System.currentTimeMillis() + SPAWN_COOLDOWN);
            mLastCatSpawned.put(pEvent.getGuild().getIdLong(), lNum);
            pEvent.getHook().editOriginalEmbeds(lEmbed).setAttachments(FileUpload.fromData(lCatInStream, "cat.png")).queue();
        }
    }

    private void setCatGameChannelCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();

        if (mUtils.memberNotAuthorized(pEvent.getMember(), "editor", pEvent.getHook())) return;

        Channel lChannel = pEvent.getOption("channel").getAsChannel();
        if (lChannel.getType() != ChannelType.TEXT) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Please select a proper text channel", pEvent.getUser())).queue();
            return;
        }

        mUtils.onExecute("UPDATE GuildSettings SET catgame_channel_id = ? WHERE guild_id = ?", lChannel.getIdLong(), pEvent.getGuild().getIdLong());
        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(CATGAME_COLOR, ":white_check_mark: Set the cat game channel to "+lChannel.getAsMention(), pEvent.getUser())).queue();
    }

    private void claimCatCommand(SlashCommandInteractionEvent pEvent) throws SQLException {
        pEvent.deferReply().queue();

        int lLastCat = mLastCatSpawned.getOrDefault(pEvent.getGuild().getIdLong(), -2);
        if (lLastCat != -2) {
            if (mUtils.onQuery("SELECT cat_number FROM CatGame WHERE guild_id = ? AND user_id = ? AND cat_number = ?", pEvent.getGuild().getIdLong(), pEvent.getUser().getIdLong(), lLastCat).next()) {
                pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You already claimed this cat", pEvent.getUser())).queue();
            }
            else {
                mLastCatSpawned.remove(pEvent.getGuild().getIdLong());
                mUtils.onExecute("INSERT INTO CatGame (guild_id, user_id, cat_number) VALUES (?, ?, ?)", pEvent.getGuild().getIdLong(), pEvent.getUser().getIdLong(), lLastCat);
                pEvent.getHook().
                        editOriginalEmbeds(Utils.createEmbed(CATGAME_COLOR, ":white_check_mark: Congratulations, **cat number " + lLastCat + "** is yours now!", pEvent.getUser())).
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

        ResultSet lCatInventory = mUtils.onQuery("SELECT cat_number FROM CatGame WHERE guild_id = ? AND user_id = ? ORDER BY cat_number ASC", pEvent.getGuild().getIdLong(), pEvent.getUser().getIdLong());
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
        pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(CATGAME_COLOR, "Inventory", lCatInventoryString.toString(), null, false, pEvent.getUser(), null, null)).queue();
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
            if (mUtils.onQuery("SELECT cat_number FROM CatGame WHERE guild_id = ? AND user_id = ? AND cat_number = ?", pEvent.getGuild().getIdLong(), pEvent.getUser().getIdLong(), lCatNumber).next()) {
                replyWithCat(
                    pEvent.getHook(),
                    lCatNumber,
                    Utils.createEmbed(CATGAME_COLOR, "Cat Card #"+lCatNumber, "", null, false, null, "attachment://cat.png",null)
                );
            }
            else {
                pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You don't own this cat", pEvent.getUser())).queue();
            }
        }
    }
}