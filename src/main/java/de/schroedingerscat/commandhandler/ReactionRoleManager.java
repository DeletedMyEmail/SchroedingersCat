package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class ReactionRoleManager extends ListenerAdapter {

    private static final Color REACTION_ROLE_COLOR = new Color(158,242,79);

    private final Utils utils;

    public ReactionRoleManager(Utils pUtils) {
        this.utils = pUtils;
    }

    /**
     * TODO
     * */
    protected void addReactionRole(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        Role role = event.getOption("role").getAsRole();

        if (event.getOption("channel") == null)
        {
            event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                    ":x: Make sure you selected an available text channel", null, false,
                    event.getUser(), null, null)).queue();

        }
        else
        {
            MessageChannel lChannel = event.getOption("channel").getAsChannel().asTextChannel();
            Message msg = lChannel.getHistory().getMessageById(event.getOption("message").getAsString());
            lChannel.retrieveMessageById(event.getOption("message").getAsString()).queue();
            if (msg != null)
            {

            }
            else
            {
                event.getHook().editOriginalEmbeds(utils.createEmbed(Color.red, "",
                        ":x: Message not found in "+lChannel.getAsMention(), null, false,
                        event.getUser(), null, null)).queue();
            }
        }

    }

    public static Color getCategoryColor() {return REACTION_ROLE_COLOR;}
}
