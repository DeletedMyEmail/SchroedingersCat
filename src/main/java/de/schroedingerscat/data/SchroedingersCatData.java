package de.schroedingerscat.data;

import de.schroedingerscat.BotApplication;
import de.schroedingerscat.Utils;
import de.schroedingerscat.commandhandler.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class SchroedingersCatData implements BotData {

    public static final String[] CATEGORIES =
            {
                    "**:coin: Economy**", "**:heavy_plus_sign: Auto Channel**", "**:performing_arts: Reaction Role**",
                    "**:wrench: Server Settings**", "**:flower_playing_cards: Cats and Pets**", //"**:musical_note: Music**",
                    "**:grey_question: Others**"
            };

    // All commands and their options
    public static final String[][][] COMMANDS =
            {
                    // Name | Description | options (type, name, description, required)
                    {
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
                            {"set_catgame_channel", "Sets the channel where users can claim cats",
                                    "channel,channel,Text Channel where cats can be claimed,true"},
                            {"set_editor_role", "Sets the role needed to edit any kind of settings with the bot", "role,role,Role needed to edit any kind of settings,true"},
                            {"set_moderator_role", "Sets the role needed to use moderation commands", "role,role,Moderator role,true"}
                    },
                    {
                            // Cats and Pets
                            {"cat", "Spawns a cute cat"},
                            {"cat_claim", "Claims the last cat spawned in the current channel"},
                            {"cat_inv", "Displays all cats you claimed"},
                            {"cat_view", "Displays one of the cats you own", "int,cat,Number of the cat card you want to display,true"},
                            {"cat_leaderboard", "Displays the top 10 cat collectors on this server"},
                            {"cat_gift", "Sends a cat card to another user", "user,user,User who will receive the cat card,true", "int,cat,Number of the cat card you want to send,true"},
                            {"shop", "Buy Cat Cards or Pets"},
                            {"buy", "Buys a cat card", "int,cat,Number of the cat card you want to buy,true"},
                            {"sell", "Sells a cat card", "int,cat,Number of the cat card you want to sell,true"},
                    },
//                {       // Music
//                    {"play_track", "Takes an url or song title, searches on YouTube and plays that song in your current voice channel", "string,track,Song name or youtube url,true"},
//                    {"disconnect", "Disconnects the bot from your current voice channel"},
//                    {"pause", "Pauses the current track playing in your voice channel"},
//                    {"resume", "Resumes stopped track"},
//                    {"skip", "Skips the next track(s)", "int,amount,Amount of tracks to skip,false"},
//                },
                    {
                            // Others
                            {"help", "Gives information about the commands"},
                            {"links", "Returns all links considering the bot"},
                            {"embed", "Creates a custom embed",
                                    "string,color,Color as hex code,true",
                                    "string,title,Embed title,true",
                                    "string,description,Embed description,false",
                                    "bool,inline,Format fields inline,false",
                                    "string,fieldtitles,Field titles seperated by semicolon,false",
                                    "string,fielddescriptions,Field descriptions seperated by semicolons,false"}
                    }
            };

    private final CommandData[] mContextCommands;

    public SchroedingersCatData() {
        mContextCommands = new CommandData[]{
                Commands.context(Command.Type.USER, "kick custom channel"),
                Commands.context(Command.Type.USER, "ban custom channel"),
                Commands.context(Command.Type.USER, "bal"),
                Commands.context(Command.Type.USER, "give"),
                Commands.context(Command.Type.USER, "rob")
        };
    }

    @Override
    public String[][][] getSlashCommands() {
        return COMMANDS;
    }

    @Override
    public CommandData[] getContextCommands() {
        return mContextCommands;
    }

    @Override
    public String[] getCommandCategories() {
        return CATEGORIES;
    }

    @Override
    public int getIndexInConfigFile() {
        return 0;
    }

    @Override
    public ListenerAdapter[] getListeners(BotApplication pBot, Utils pUtils) {
        return new ListenerAdapter[]{
                new AutoChannelHandler(pUtils),
                new ReactionRoleHandler(pUtils),
                new AutoRoleHandler(pUtils),
                new CategorylessHandler(pUtils),
                new CatsAndPetsHandler(pUtils),
                new EconomyHandler(pUtils, pBot),
                new SettingsHandler(pUtils, pBot)
        };
    }
}
