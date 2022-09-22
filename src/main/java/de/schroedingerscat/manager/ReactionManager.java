package de.schroedingerscat.manager;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class ReactionManager extends ListenerAdapter {

    private static final Color REACTION_ROLE_COLOR = Color.cyan;

    private final Utils utils;

    public ReactionManager(Utils pUtils) {
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
}
