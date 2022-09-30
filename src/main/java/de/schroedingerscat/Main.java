package de.schroedingerscat;

import de.schroedingerscat.commandhandler.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.discordbots.api.client.DiscordBotListAPI;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


/**
 * Main class for Schroder's Cat Discord Bot <br/>
 * Starts every essential instance, database connection and the JDA
 *
 * @author Joshua | KaitoKunTatsu
 * @version v2.0
 * */
public class Main {

    private static JDA JDA;
    private static DiscordBotListAPI botListAPI;

    public static void initBotAndApi()
    {

        String lBotToken = "";
        String lApiToken = "";
        Utils lUtils = null;

        try
        {
            File lTokenFile = new File("src/main/resources/tokenFile.txt");
            Scanner lReader = new Scanner(lTokenFile);
            lBotToken = lReader.nextLine();
            if (lReader.hasNext())
                lApiToken = lReader.nextLine();

            lReader.close();

            lUtils = new Utils("src/main/resources/catbot.db");
            lUtils.createTables();
        }
        catch (IOException | SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        botListAPI = new DiscordBotListAPI.Builder()
                .token(lApiToken)
                .botId("872475386620026971")
                .build();

        JDABuilder lBuilder = JDABuilder.createDefault(lBotToken, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_BANS);

        lBuilder.setMemberCachePolicy(MemberCachePolicy.ALL);

        lBuilder.addEventListeners(
                new AutoChannelHandler(lUtils),
                new AutoRoleHandler(lUtils),
                new EconomyHandler(lUtils),
                new ReactionRoleHandler(lUtils),
                new SettingsHandler(lUtils),
                new CategorylessHandler(lUtils),
                new MusicHandler(),
                new ExceptionHandler()
        );

        lBuilder.setStatus(OnlineStatus.ONLINE);
        lBuilder.setActivity(Activity.watching("/help"));

        JDA = lBuilder.build();
    }

    /**
     *
     *
     * @param pJDA - JDA on which commands will be added
     * */
    public static void addSlashCommands(JDA pJDA, String[][][] pCommands)
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

    public static void updateBotListApi() {
        if (botListAPI != null)
            botListAPI.setStats(JDA.getGuilds().size());
    }

    public static JDA getJDA() {return JDA; }

    public static void main(String[] args) throws InterruptedException {
        Main.initBotAndApi();
        Main.getJDA().awaitReady();
        Main.addSlashCommands(Main.JDA, CategorylessHandler.getCommands());
    }
}
