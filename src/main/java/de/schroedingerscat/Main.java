package de.schroedingerscat;

import de.schroedingerscat.manager.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
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

    public Main() {
        String lToken = "";
        try {
            File lTokenFile = new File("src/main/resources/tokenFile.txt");
            Scanner lReader = new Scanner(lTokenFile);
            lToken = lReader.nextLine();
            lReader.close();
        } catch (IOException io) {
            System.out.println("Could not read token " + io);
        }

        Utils lUtils = null;
        try {
            lUtils = new Utils("src/main/resources/catbot.db");
            lUtils.createTables();
        } catch (SQLException e) {
            System.exit(1);
        }

        JDABuilder lBuilder = JDABuilder.createDefault(lToken, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES);

        CategorylessManager lBCmdManager = new CategorylessManager(lUtils);
        lBuilder.addEventListeners(
                new AutoChannelManager(lUtils),
                new AutoRoleManager(lUtils),
                new EconomyManager(lUtils),
                new ReactionRoleManager(lUtils),
                new SettingsManager(lUtils),
                lBCmdManager
        );

        lBuilder.setStatus(OnlineStatus.ONLINE);
        lBuilder.setActivity(Activity.watching("/help"));

        try {
            JDA = lBuilder.build();
            JDA.awaitReady();
            addSlashCommands(JDA, lBCmdManager.getCommands());
        } catch (LoginException | InterruptedException loginException) {
            System.out.println("Login failed");
            System.exit(0);
        }

    }

    /**
     *
     *
     * @param pJDA - JDA on which commands will be added
     * */
    public void addSlashCommands(JDA pJDA, String[][][] pCommands)  {

        //CommandListUpdateAction new_commands = jda.getGuildById("939517124957859881").updateCommands();

        for(String[][] category : pCommands)
        {
            for (String[] cmd : category)
            {
                SlashCommandData slashCmd = Commands.slash(cmd[0], cmd[1]);
                for (int i = 2; i < cmd.length; i++)
                {
                    OptionType type = switch (cmd[i].split(",")[0]) {
                        case "int" -> OptionType.INTEGER;
                        case "number" -> OptionType.NUMBER;
                        case "string" -> OptionType.STRING;
                        case "user" -> OptionType.USER;
                        case "channel" -> OptionType.CHANNEL;
                        case "role" -> OptionType.ROLE;
                        case "bool" -> OptionType.BOOLEAN;
                        default -> OptionType.UNKNOWN;
                    };

                    String optionname = cmd[i].split(",")[1];
                    String optionDescription = cmd[i].split(",")[2];
                    boolean required = cmd[i].split(",")[3].toLowerCase().compareTo("true") == 0;

                    slashCmd.addOption(type, optionname, optionDescription, required);
                }
                pJDA.getGuildById("939517124957859881").upsertCommand(slashCmd).queue();
                //new_commands.addCommands(slashCmd).queue();
            }
        }
        //new_commands.queue();
        System.out.println("Commands added");
    }

    public static JDA getJDA() {return JDA; }

    public static void main(String[] args) {
        new Main();
    }
}
