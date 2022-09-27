package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.sql.SQLException;

/**
 *
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 26.09.2022
 * */
public class ModerationHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    private static final Color MODERATION_COLOR = new Color(237,81,9);

    private final Utils utils;

    public ModerationHandler(Utils pUtils) {
        this.utils = pUtils;
    }

    public static Color getCategoryColor() {return MODERATION_COLOR; }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent pEvent)
    {
        try
        {
            switch (pEvent.getName()) {
                case "" -> {}
            }
        }
        catch (NumberFormatException numEx) {
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: You entered an invalid number", pEvent.getUser())).queue();
        }
        /*catch (
                SQLException sqlEx)
        {
            sqlEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
        }*/
    }

}
