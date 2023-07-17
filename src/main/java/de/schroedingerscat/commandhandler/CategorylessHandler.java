package de.schroedingerscat.commandhandler;

import de.schroedingerscat.data.SchroedingersCatData;
import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles slash commands which can't be categorized
 *
 * @author KaitoKunTatsu
 * @version 3.0.0 | last edit: 17.07.2023
 * */
public class CategorylessHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    private static final Color CATEGORYLESS_COLOR = new Color(165, 172, 167);

    private final Utils utils;

    public CategorylessHandler(Utils pUtils) {
        this.utils = pUtils;
    }

    // Events

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent pEvent) {
        Utils.catchAndLogError(pEvent.getHook(), () -> {
            switch (pEvent.getName()) {
                case "help" -> helpCommand(pEvent);
                case "embed" -> embedCommand(pEvent);
                case "links" -> linksCommand(pEvent);
            }
        });
    }

    @Override
    public void onStringSelectInteraction(@Nonnull StringSelectInteractionEvent pEvent) {
        if (!pEvent.getComponent().getId().equals("HelpMenu")) return;

        pEvent.deferEdit().queue();

        int lCategoryAsValue = Integer.parseInt(pEvent.getValues().get(0));

        List<String[]> lFields = new ArrayList<>();
        for (String[] cmds : SchroedingersCatData.COMMANDS[lCategoryAsValue]) {
            lFields.add(new String[] { cmds[0],cmds[1] });
        }

        Color lColor = null;
        switch (lCategoryAsValue) {
            case 0 -> lColor = EconomyHandler.ECONOMY_COLOR;
            case 1 -> lColor = AutoChannelHandler.AUTOCHANNEL_COLOR;
            case 2 -> lColor = ReactionRoleHandler.REACTION_ROLE_COLOR;
            case 3 -> lColor = SettingsHandler.SERVERSETTINGS_COLOR;
            case 4 -> lColor = CatsAndPetsHandler.CATS_AND_PETS_COLOR;
            case 5 -> lColor = CATEGORYLESS_COLOR;
        }

        MessageEmbed embed = Utils.createEmbed(
                lColor, SchroedingersCatData.CATEGORIES[Integer.parseInt(pEvent.getValues().get(0))]+" Commands", "",
                lFields.toArray(new String[][]{}), true, null, null, null);
        pEvent.getHook().editOriginalEmbeds(embed).queue();
    }

    // Slash Commands

    /**
     *
     *
     * */
    private void helpCommand(SlashCommandInteractionEvent pEvent) {
        pEvent.replyEmbeds(
                Utils.createEmbed(
                        CATEGORYLESS_COLOR,
                        "",
                        "Schroedinger's Cat is **open source**! Source code and example videos can be found on https://github.com/KaitoKunTatsu/SchroedingersCat .\n\n**Select a category to see commands:**",
                        null,
                        false,
                        pEvent.getJDA().getSelfUser(),
                        null,
                        null)
        ).addActionRow(
                StringSelectMenu.create("HelpMenu").
                        addOption("Economy","0", Emoji.fromUnicode("U+1FA99")).
                        addOption("Auto Channel","1", Emoji.fromUnicode("U+2795")).
                        addOption("Reaction Roles","2", Emoji.fromUnicode("U+1F3AD")).
                        addOption("Server Settings","3", Emoji.fromUnicode("U+1F527")).
                        //addOption("Music","4", Emoji.fromUnicode("U+1F3B5")).
                        addOption("Cats And Pets","4", Emoji.fromUnicode("U+1F3B4")).
                        addOption("Others","5", Emoji.fromUnicode("U+2754")).build()
        ).queue();
    }

    private void linksCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.replyEmbeds(
                Utils.createEmbed(
                        CATEGORYLESS_COLOR,
                        "Links",
                        "**Support Server**\nhttps://www.discord.gg/XUqU4MpFFF\n\n" +
                                "**Invite Link**\nhttps://discord.com/api/oauth2/authorize?client_id=872475386620026971&permissions=1101960473814&scope=bot%20applications.commands\n\n" +
                                "**Source Code And Examples**\nhttps://github.com/KaitoKunTatsu/SchroedingersCat\n\n" +
                                "**TopGG**\nhttps://top.gg/bot/872475386620026971",
                        null,
                        false,
                        pEvent.getJDA().getSelfUser(),
                        null,
                        null)
        ).queue();
    }

    private void embedCommand(SlashCommandInteractionEvent pEvent) throws SQLException
    {
        pEvent.deferReply().queue();
        if (utils.memberNotAuthorized(pEvent.getMember(), "moderator", pEvent.getHook())) return;

        EmbedBuilder lBuilder = new EmbedBuilder();

        String lHexCode = pEvent.getOption("color").getAsString();
        if (!lHexCode.startsWith("#"))
            lHexCode = "#"+lHexCode;

        lBuilder.setColor(Color.decode(lHexCode));
        lBuilder.setTitle(pEvent.getOption("title").getAsString());

        boolean lInline = false;
        if (pEvent.getOption("inline") != null)
            lInline = pEvent.getOption("inline").getAsBoolean();
        if (pEvent.getOption("fieldtitles") != null && pEvent.getOption("fielddescriptions") != null) {

            String[] lTitles = pEvent.getOption("fieldtitles").getAsString().split(";");
            String[] lDescriptions = pEvent.getOption("fielddescriptions").getAsString().split(";");

            if (lDescriptions.length != lTitles.length) {
                pEvent.getHook()
                    .editOriginalEmbeds(Utils.createEmbed(Color.red, "",":x: Each field must have a title and description", null, false, pEvent.getUser(), null, null))
                    .queue();
                return;
            }

            for (int i = 0; i < lTitles.length; i++) {
                lBuilder.addField(
                        lTitles[i],
                        lDescriptions[i],
                        lInline
                );
            }
        }

        if (pEvent.getOption("image") != null)
            lBuilder.setImage(pEvent.getOption("image").getAsAttachment().getUrl());

        if (pEvent.getOption("description") != null)
            lBuilder.setDescription(pEvent.getOption("description").getAsString());

        pEvent.getHook().editOriginalEmbeds(lBuilder.build()).queue();
    }
}
