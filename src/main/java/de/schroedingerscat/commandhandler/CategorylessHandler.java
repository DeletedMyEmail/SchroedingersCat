package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Handles slash commands which can't be categorized
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 26.09.2022
 * */
public class CategorylessHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    private static final Color CATEGORYLESS_COLOR = new Color(192,214,203);

    // Command categories
    private static final String[] categories =
            {
                    "**» Economy «**", "**» Auto Channel «**", "**» Reaction Roles «**",
                    "**» Server Settings «**", "**» Moderation «**", "**» Others «**"
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
                    {"spin", "Spins a wheel. You can bet on a color or a specific number", "string,color,Color to bet on (red or black),true","int,field,Your guess on the wheel,false","int,money,Amount of money to bet (leave out to bet all),false"},
                    {"give_admin", "Creates money out of nothing and gives it to a user on your server","user,user,User who will earn the money,true","int,amount,Amount of money,true"},
                    {"get_income_roles","Displays all Income Roles"},
                    {"add_income_role", "Updates or adds an Income Role", "role,role,Income Role,true", "int,income,Amount which is granted every 6h,true"},
                    {"del_income_role","Deletes an Income Role", "role,role,Income Role which will be deleted,true"}
                },
                {
                    // Auto Channel
                    {"set_auto_channel","Sets the Voice Channel for Auto Channel creation", "channel,channel,Voice Channel which creates a new one if someone joins,true"},
                    {"clear_auto_channel_db", "Resets the Auto Channel database for your server"},
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
                    //**Note:** The `Schroedinger's Cat` role has to be higher than normal user to manage roles, and you have to be an admin to use this commands
                    {"get_info", "Displays the server settings"},
                    {"set_welcome", "Sets the channel and text for Welcome Messages",
                            "channel,channel,Channel where the messages will be send,true",
                            "string,message,Welcome Message,true"},
                    {"set_auto_role", "Sets the role which will be added to every new member",
                            "role,role,Auto Role,true",
                            "bool,screening,Is Rule Screening enabled,true"},
                    {"clear_settings", "Resets all settings"},
                    {"set_log", "Sets the channel where all important commands will be logged",
                        "channel,channel,Text Channel which will be the log,true"}
                },
                {
                    // Moderation
                    {"clear", "Deletes a specified amount of messages in your text channel",
                        "int,amount, Amount of messages to delete,true"},
                    {"kick", "Kicks a user from the server and sends him a private message with the reason",
                        "user,user,User you want to kick,true",
                        "string,reason,Reason for this action,false"},
                    {"ban", "Bans a user from the server and sends him a private message with the reason",
                                "user,user,User you want to ban,true",
                                "string,reason,Reason for this action,false"}
                },
                {
                    // Others
                    {"help", "Gives information about the commands"},
                    {"links", "Returns all links considering the bot"},
                    {"cat", "Returns a cute cat pic"},
                    {"embed", "Creates a custom embed",
                        "int,color,Color as decimal,true",
                        "string,title,Embed title,true",
                        "string,description,Embed description,false",
                        "bool,inline,Format fields inline,false",
                        "string,fieldtitles,Field titles seperated by semicolon,false",
                        "string,fielddescriptions,Field descriptions seperated by semicolons,false"},
                    {"ping", "Mentions a user x times",
                            "user,user,User to mention,true",
                            "int,amount,How often should the user be mentioned,true"},
                    {"meow", "Meows in your current Voice Channel"},
                    {"box", "Dead or alive?"}
                }
            };

    private final Utils utils;

    public CategorylessHandler(Utils pUtils) {
        this.utils = pUtils;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        switch (event.getName())
        {
            case "help" -> helpCommand(event);
            case "embed" -> embedCommand(event);
        }
    }

    @Override
    public void onSelectMenuInteraction(@Nonnull SelectMenuInteractionEvent event)
    {
        event.deferEdit().queue();

        int lCategoryAsValue = Integer.parseInt(event.getValues().get(0));

        List<String[]> fields = new ArrayList<>();
        for (String[] cmds : commands[lCategoryAsValue]) {
            fields.add(new String[] { cmds[0],cmds[1] });
        }

        Color lColor = null;
        switch (lCategoryAsValue) {
            case 0 -> lColor = EconomyHandler.getCategoryColor();
            case 1 -> lColor = AutoChannelHandler.getCategoryColor();
            case 2 -> lColor = ReactionRoleHandler.getCategoryColor();
            case 3 -> lColor = SettingsHandler.getCategoryColor();
            case 4 -> lColor = ModerationHandler.getCategoryColor();
            case 5 -> lColor = CATEGORYLESS_COLOR;
        }

        MessageEmbed embed = utils.createEmbed(
                lColor, categories[Integer.parseInt(event.getValues().get(0))]+" Commands", "",
                fields.toArray(new String[][]{}), true, null, null, null);
        event.getHook().editOriginalEmbeds(embed).queue();
    }

    /**
     * TODO:
     *
     * */
    private void helpCommand(SlashCommandInteractionEvent event)
    {
        event.replyEmbeds(utils.createEmbed(CATEGORYLESS_COLOR, "Select a category","", null, false, null, null, null)).addActionRow(
                SelectMenu.create("HelpMenu").
                        addOption("Economy","0").
                        addOption("AutoChannel","1").
                        addOption("ServerSettings","2").
                        addOption("ReactionRoles","3").
                        addOption("Moderation","4").
                        addOption("Others","5").build()
        ).queue();
    }

    private void embedCommand(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        EmbedBuilder lBuilder = new EmbedBuilder();

        lBuilder.setTitle(event.getOption("title").getAsString());

        boolean lInline = false;
        if (event.getOption("inline") != null)
            lInline = event.getOption("inline").getAsBoolean();
        if (event.getOption("fieldtitles") != null && event.getOption("fielddescriptions") != null) {

            String[] lTitles = event.getOption("fieldtitles").getAsString().split(";");
            String[] lDescriptions = event.getOption("fielddescriptions").getAsString().split(";");

            if (lDescriptions.length != lTitles.length)
            {
                event.getHook()
                    .editOriginalEmbeds(utils.createEmbed(Color.red, "",":x: Each field must have a title and description", null, false, event.getUser(), null, null))
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

        if (event.getOption("image") != null)
            lBuilder.setImage(event.getOption("image").getAsAttachment().getUrl());

        if (event.getOption("description") != null)
            lBuilder.setDescription(event.getOption("description").getAsString());

        event.getHook().editOriginalEmbeds(lBuilder.build()).queue();
    }

    public String[] getCategories() {return categories;}

    public static String[][][] getCommands() {return commands; }
}
