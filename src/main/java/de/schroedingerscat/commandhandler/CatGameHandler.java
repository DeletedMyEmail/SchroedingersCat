package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

public class CatGameHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    private static final Color CATGAME_COLOR = new Color(165, 172, 167);
    private final Random mRandom;

    public CatGameHandler() {
        mRandom = new Random();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent pEvent) {
        try {
            switch (pEvent.getName()) {
                case "cat" -> catCommand(pEvent);
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

    private void catCommand(SlashCommandInteractionEvent pEvent) throws FileNotFoundException {
        pEvent.deferReply().queue();

        int lNum = mRandom.nextInt(400) == 0 ? -1 : new Random().nextInt(97);
        FileInputStream lCatInStream = new FileInputStream("src/main/resources/catpics/katze"+lNum+".png");

        MessageEmbed lEmbed = Utils.createEmbed(CATGAME_COLOR, "Cat Card #"+lNum, "", null, false, null, "attachment://cat.png",null);
        pEvent.getHook().editOriginalEmbeds(lEmbed).setAttachments(FileUpload.fromData(lCatInStream, "cat.png")).queue();
    }
}
