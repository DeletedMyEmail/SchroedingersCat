package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


/**
 * Handles slash commands which can't be categorized
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 30.09.2022
 * */
public class CategorylessHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    private static final Color CATEGORYLESS_COLOR = new Color(165, 172, 167);

    // Command categories
    private static final String[] categories =
            {
                "**:coin: Economy**", "**:heavy_plus_sign: Auto Channel**", "**:performing_arts: Reaction Role**",
                "**:wrench: Server Settings**", "**:musical_note: Music**", "**:grey_question: Others**"
            };

    // All commands and their options
    private static final String[][][] commands =
            {
                {
                    // Name | Description | options (type, name, description, required)

                    // Economy
                    {"bal", "Displays balance of specific user", "user,user,User whose wealth you want to see,false"},
                    {"dep", "Deposites your money to your bank", "int,amount,Amount of money you want to deposite,false"},
                    {"with", "Debits your money from the bank", "int,amount,Amount of money you want to debit,false"},
                    {"crime", "If you commit a crime, you risk losing money, but you can also earn a lot"},
                    {"give", "Gives a user a certain amount of your money", "user,user,User who will receive your money,true","int,amount,Amount of money you want to give,false"},
                    {"rob", "Robs a user's cash", "user,user,User whose cash you  want to steal,true"},
                    {"top", "Displays the richest people on current server"},
                    {"work", "You earn 1500-2500 wiggles"},
                    {"spin", "Spins a wheel. You can bet on a color (2x) or a specific number (20x)", "string,guess,Color (red or black) or number (1-36) to bet on,true","int,money,Amount of money to bet (leave out to bet all),false"},
                    {"give_admin", "Creates money out of nothing and gives it to a user on your server","user,user,User who will earn the money,true","int,amount,Amount of money,true"},
                    {"get_income_roles","Displays all Income Roles"},
                    {"add_income_role", "Updates or adds an income role", "role,role,Income role,true", "int,income,Amount which is distributed every 6h,true"},
                    {"remove_income_role","Deletes an income role", "role,role,Income role which will be deleted,true"}
                },
                {
                    // Auto Channel
                    {"set_create_channel","Sets the Voice Channel for Auto Channel creation", "channel,channel,Voice Channel which creates a new one if someone joins,true"},
                    {"claim","Get admin in your current Voice Channel if the old admin isn't connected"},
                    {"vcname", "Changes the name of your current Voice Channel if you own it", "string,name,New name for your Voice Channel,true"},
                    {"vckick", "Kicks a user from your current Voice Channel", "user,user,User who will be kicked,true"},
                    {"vcban", "Bans a user from your current Voice Channel", "user,user,User who will be banned,true"},
                    {"vcunban","Unbans a user from your current Voice Channel","user,user,User who will be unbanned,true"},
                    {"vclimit", "Sets the userlimit in your current Voice Channel. 0 will remove the userlimit.", "int,limit,Maximum amount of possible users in your Voice Channel,true"}
                },
                {
                    // Reaction Roles
                    {"add_reaction_role", "Sets a role which each member adding the specific emoji will get",
                                "role,role,Reaction Role,true",
                                "channel,channel,Channel in which the message was sent,true",
                                "string,message,ID of the message for Reaction Role,true",
                                "string,emoji,Reacting with this will grant the role,true"},
                    {"remove_reaction_role", "Deletes a Reaction Role",
                            "string,message,ID of the message with emotji,true",
                            "string,emoji,Reation which grants a role,true"},
                    {"get_reaction_roles", "Displays all Reaction Roles"},
                    {"remove_all", "Deletes all Reaction Roles"}
                },
                {
                    // Server Settings
                    {"get_info", "Displays the server settings"},
                    {"set_welcome", "Sets the channel and text for Welcome Messages",
                            "channel,channel,Channel where the messages will be send,true",
                            "string,message,Welcome Message,true"},
                    {"set_auto_role", "Sets the role which will be added to every new member",
                            "role,role,Auto Role,true",
                            "bool,screening,Is Rule Screening enabled,true"},
                    {"reset_settings", "Resets all settings"},
                    {"set_log", "Sets the channel where all important commands will be logged",
                        "channel,channel,Text Channel which will be the log,true"},
                    {"set_editor_role", "Sets the role needed to edit any kind of settings with the bot", "role,role,Role needed to edit any kind of settings,true"},
                    {"set_moderator_role", "Sets the role needed to use moderation commands", "role,role,Moderator role,true"}
                },
                {
                    // Music
                    {"play_track", "Takes an url or song title, searches on youtube an plays that song in your current voice channel", "string,track,Song name or youtube url,true"},
                    {"disconnect", "Disconnects the bot from your current voice channel"},
                    {"pause", "Pauses the current track playing in your voice channel"},
                    {"resume", "Resumes stopped track"},
                    {"skip", "Skips the next track(s)", "int,amount,Amount of tracks to skip,false"},
                },
                {
                    // Others
                    {"help", "Gives information about the commands"},
                    {"links", "Returns all links considering the bot"},
                    {"cat", "Returns a cute cat pic"},
                    {"embed", "Creates a custom embed",
                        "string,color,Color as hex code,true",
                        "string,title,Embed title,true",
                        "string,description,Embed description,false",
                        "bool,inline,Format fields inline,false",
                        "string,fieldtitles,Field titles seperated by semicolon,false",
                        "string,fielddescriptions,Field descriptions seperated by semicolons,false"}
                }
            };

    private final Utils utils;

    public CategorylessHandler(Utils pUtils) {
        this.utils = pUtils;
    }

    // Events

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent pEvent)
    {
        try
        {
            switch (pEvent.getName()) {
                case "help" -> helpCommand(pEvent);
                case "embed" -> embedCommand(pEvent);
                case "links" -> linksCommand(pEvent);
            }
        }
        catch (NumberFormatException numEx) {
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You entered an invalid number", pEvent.getUser())).queue();
        }
        catch (SQLException sqlEx)
        {
            sqlEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Database error occurred", pEvent.getUser())).queue();
        }
        catch (NullPointerException nullEx) {
            nullEx.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Invalid argument. Make sure you selected a valid text channel, message id, role and emoji", pEvent.getUser())).queue();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Unknown error occured", pEvent.getUser())).queue();
        }
    }

    @Override
    public void onSelectMenuInteraction(@Nonnull SelectMenuInteractionEvent pEvent)
    {
        if (!pEvent.getComponent().getId().equals("HelpMenu")) return;

        pEvent.deferEdit().queue();

        int lCategoryAsValue = Integer.parseInt(pEvent.getValues().get(0));

        List<String[]> lFields = new ArrayList<>();
        for (String[] cmds : commands[lCategoryAsValue]) {
            lFields.add(new String[] { cmds[0],cmds[1] });
        }

        Color lColor = null;
        switch (lCategoryAsValue) {
            case 0 -> lColor = EconomyHandler.getCategoryColor();
            case 1 -> lColor = AutoChannelHandler.getCategoryColor();
            case 2 -> lColor = ReactionRoleHandler.getCategoryColor();
            case 3 -> lColor = SettingsHandler.getCategoryColor();
            case 4 -> lColor = MusicHandler.getCategoryColor();
            case 5 -> lColor = CATEGORYLESS_COLOR;
        }

        MessageEmbed embed = Utils.createEmbed(
                lColor, categories[Integer.parseInt(pEvent.getValues().get(0))]+" Commands", "",
                lFields.toArray(new String[][]{}), true, null, null, null);
        pEvent.getHook().editOriginalEmbeds(embed).queue();
    }

    // Slash Commands

    /**
     *
     *
     * */
    private void helpCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.replyEmbeds(
                Utils.createEmbed(
                        CATEGORYLESS_COLOR,
                        "",
                        "Schroedinger's Cat is **open source**! Source code and example videos can be found on https://github.com/KaitoKunTatsu/SchroedingersCat .\n\n**Select a category to see commands:**",
                        null,
                        false,
                        pEvent.getJDA().getUserById("872475386620026971"),
                        null,
                        null)
        ).addActionRow(
                SelectMenu.create("HelpMenu").
                        addOption("Economy","0", Emoji.fromUnicode("U+1FA99")).
                        addOption("AutoChannel","1", Emoji.fromUnicode("U+2795")).
                        addOption("ReactionRoles","2", Emoji.fromUnicode("U+1F3AD")).
                        addOption("ServerSettings","3", Emoji.fromUnicode("U+1F527")).
                        addOption("Music","4", Emoji.fromUnicode("U+1F3B5")).
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
                        pEvent.getJDA().getUserById("872475386620026971"),
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

            if (lDescriptions.length != lTitles.length)
            {
                pEvent.getHook()
                    .editOriginalEmbeds(Utils.createEmbed(Color.red, "",":x: Each field must have a title and description", null, false, pEvent.getUser(), null, null))
                    .queue();
                return;
            }

            for (int i = 0; i < lTitles.length; i++)
            {
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

    private void catCommand(SlashCommandInteractionEvent pEvent)
    {

    }

    public String[] getCategories() {return categories;}

    public static String[][][] getCommands() {return commands; }
}
