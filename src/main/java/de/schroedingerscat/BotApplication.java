package de.schroedingerscat;

import com.google.common.hash.Hashing;
import de.schroedingerscat.commandhandler.*;
import klibrary.utils.SystemUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.discordbots.api.client.DiscordBotListAPI;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


/**
 * Main class for Schrödinger's Cat Discord Bot <br/>
 * Establishes database connection, connects to discord and topgg and registers all event listeners
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.1.0 | last edit: 25.02.2023
 **/
public class BotApplication {

    private final DiscordBotListAPI topggApi;
    private final Utils utils;
    private final JDA jda;

    public BotApplication(String pDiscordToken, String pTopggToken, String pTopggBotId, String pDatabasePath) throws SQLException, IOException {
        if (pDiscordToken == null || pDiscordToken.isBlank()) {
            throw new IllegalArgumentException("Invalid discord token");
        }
        if (pTopggToken == null) {
            topggApi = null;
        }
        else {
            topggApi = new DiscordBotListAPI.Builder()
                    .token(pTopggToken)
                    .botId(pTopggBotId)
                    .build();
        }

        utils = new Utils(pDatabasePath);
        utils.createTables();

        JDABuilder lBuilder = JDABuilder.createDefault(pDiscordToken, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_BANS);

        lBuilder.setMemberCachePolicy(MemberCachePolicy.ALL);
        lBuilder.addEventListeners(
                new AutoChannelHandler(utils),
                new AutoRoleHandler(utils),
                new EconomyHandler(utils, this),
                new ReactionRoleHandler(utils),
                new SettingsHandler(utils, this),
                new CategorylessHandler(utils),
                new ExceptionHandler(),
                new MusicHandler()
        );

        lBuilder.setStatus(OnlineStatus.ONLINE);
        lBuilder.setActivity(Activity.watching("/help"));

        jda = lBuilder.build();
    }

    private void insertGuildsInDBIfAbsent() throws SQLException {
        List<Guild> lGuilds =  jda.getGuilds();
        for (Guild guild : lGuilds) {
            utils.insertGuildIfAbsent(guild.getIdLong());
        }
    }

    /**
     *
     *
     * */
    private void addSlashCommands(JDA pJDA, String[][][] pCommands)
    {
        List<CommandData> lCommands = new ArrayList<>();
        for(String[][] category : pCommands)
        {
            for (String[] commandStructure : category)
            {
                SlashCommandData lSlashCommand = Commands.slash(commandStructure[0], commandStructure[1]);
                for (int i = 2; i < commandStructure.length; i++)
                {
                    String[] lOptionSettings = commandStructure[i].split(",");
                    OptionType lType = switch (lOptionSettings[0]) {
                        case "int" -> OptionType.INTEGER;
                        case "number" -> OptionType.NUMBER;
                        case "string" -> OptionType.STRING;
                        case "user" -> OptionType.USER;
                        case "channel" -> OptionType.CHANNEL;
                        case "role" -> OptionType.ROLE;
                        case "bool" -> OptionType.BOOLEAN;
                        default -> OptionType.UNKNOWN;
                    };

                    lSlashCommand.addOption(lType, lOptionSettings[1], lOptionSettings[2], lOptionSettings[3].equals("true"));
                }
                lCommands.add(lSlashCommand);
            }
        }
        lCommands.add(Commands.context(Command.Type.USER, "kick custom channel"));
        lCommands.add(Commands.context(Command.Type.USER, "ban custom channel"));
        lCommands.add(Commands.context(Command.Type.USER, "bal"));
        lCommands.add(Commands.context(Command.Type.USER, "give"));
        lCommands.add(Commands.context(Command.Type.USER, "rob"));
        pJDA.updateCommands().addCommands(lCommands).queue();
        System.out.println("Commands updated");
    }

    /**
     *
     *
     * */
    public void updateBotListApi() {
        if (topggApi != null)
            topggApi.setStats(jda.getGuilds().size());
    }

    public JDA getJDA() {return jda; }

    private static String getDatabasePath(String lDiscordToken, String pDirPath) {
        String lTokenHash = Hashing.sha256()
                .hashString(lDiscordToken, StandardCharsets.UTF_8)
                .toString();
        String pFileName = pDirPath+lTokenHash+".db";
        SystemUtils.createFileIfAbsent(pFileName);
        return pFileName;
    }

    public static void main(String[] args) {
        String lAppPath = SystemUtils.getLocalApplicationPath()+"/SchroedingersCat/";
        SystemUtils.createDirIfAbsent(lAppPath);
        SystemUtils.createFileIfAbsent(lAppPath+"tokenfile.txt");

        if (args.length == 0) {
            System.out.println("Give indices for tokens defined in tokenfile.txt as arguments. See https://github.com/KaitoKunTatsu/SchroedingersCat for more information");
            return;
        }

        try {
            File lTokenFile = new File(lAppPath+"tokenfile.txt");
            lTokenFile.createNewFile();

            Scanner lReader = new Scanner(lTokenFile);
            for (int i = 0; lReader.hasNextLine(); ++i) {
                String[] lTokens = lReader.nextLine().split(" ");
                for (String lArg : args) {
                    if (Integer.parseInt(lArg) == i) {

                        String lDiscordToken = lTokens[0];
                        String lTopggToken = null;
                        if (lTokens.length > 1)
                            lTopggToken = lTokens[1];
                        String lTopggBotId = null;
                        if (lTokens.length > 2)
                            lTopggBotId = lTokens[2];

                        System.out.println("Starting bot at index "+i);
                        BotApplication lBotApp = new BotApplication(lDiscordToken, lTopggToken, lTopggBotId, getDatabasePath(lTokens[0], lAppPath));
                        lBotApp.getJDA().awaitReady();
                        lBotApp.insertGuildsInDBIfAbsent();
                        System.out.println("Updating slash commands");
                        lBotApp.addSlashCommands(lBotApp.getJDA(), CategorylessHandler.getCommands());
                        break;
                    }
                }
            }
            lReader.close();
        }
        catch (SQLException sqlEx) {
            System.out.println("Could not connect to database");
        }
        catch (IOException ioEx) {
            System.out.println("Could not open or read file "+lAppPath+"tokenfile.txt");
        } catch (InterruptedException e) {
            System.out.println("Thread starting a bot got interupted.. skipping the bot");
        }
    }
}
