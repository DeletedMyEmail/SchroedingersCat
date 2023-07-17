package de.schroedingerscat;

import com.google.common.hash.Hashing;
import de.schroedingerscat.data.BotData;
import de.schroedingerscat.data.MusicCatData;
import de.schroedingerscat.data.SchroedingersCatData;
import klibrary.utils.SystemUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A factory class for creating bot instances
 *
 * @author KaitoKunTatsu
 * @version 3.0.0 | last edit: 16.07.2023
 **/
public class BotFactory {

    public static final String APP_CONFIG_FOLDER_PATH = SystemUtils.getLocalApplicationPath()+"/SchroedingersCat/";
    public static final String TOKEN_CONFIG_FILE = APP_CONFIG_FOLDER_PATH +"tokenfile.txt";

    public static BotApplication create(BotData pBotData) throws InterruptedException, IOException, SQLException {
        String[] lTokens = getTokens(pBotData.getIndexInConfigFile());
        BotApplication lBotApplication;
        if (lTokens.length == 0) {
            throw new IllegalArgumentException("No tokens found in tokenfile.txt");
        }
        else if (lTokens.length != 3) {
            lBotApplication = new BotApplication(lTokens[0]);
        }
        else {
            lBotApplication = new BotApplication(lTokens[0], lTokens[1], lTokens[2]);
        }
        Utils lUtils = new Utils(getDatabasePath(lTokens[0]));

        // add tables to database
        String[] lTableNames = pBotData.getDatabaseTables();
        for (String lTableName : lTableNames) {
            lUtils.onExecute(lTableName);
        }

        updateSlashCommands(lBotApplication.getJDA(), pBotData.getSlashCommands(), pBotData.getContextCommands());
        addListeners(lBotApplication, pBotData.getListeners(lBotApplication, lUtils));
        insertGuildsIntoDatabaseIfAbsent(lBotApplication.getJDA(), lUtils);

        System.out.println(lBotApplication.getJDA().getSelfUser().getName() + " online!");
        return lBotApplication;
    }

    private static void updateSlashCommands(JDA pJDA, String[][][] pSlashCommands, CommandData[] pContextCommands) {
        List<CommandData> lCommands = new ArrayList<>();
        for(String[][] lCommandsForCategory : pSlashCommands) {
            for (String[] lCommandStructure : lCommandsForCategory) {
                SlashCommandData lSlashCommand = Commands.slash(lCommandStructure[0], lCommandStructure[1]);
                for (int i = 2; i < lCommandStructure.length; i++) {
                    String[] lOptionSettings = lCommandStructure[i].split(",");
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
        lCommands.addAll(Arrays.asList(pContextCommands));
        pJDA.updateCommands().addCommands(lCommands).queue();
    }

    private static String[] getTokens(int pIndex) throws IOException {
        BufferedReader lReader = new BufferedReader(new FileReader(TOKEN_CONFIG_FILE));
        int i = 0;
        while (i < pIndex) {
            lReader.readLine();
            ++i;
        }
        return lReader.readLine().split(" ");
    }

    private static void addListeners(BotApplication pBotApplication, ListenerAdapter[] pListeners) {
        for (ListenerAdapter lListener : pListeners) {
            pBotApplication.getJDA().addEventListener(lListener);
        }
    }

    private static void insertGuildsIntoDatabaseIfAbsent(JDA pJDA, Utils pUtils) throws SQLException {
        List<Guild> lGuilds =  pJDA.getGuilds();
        for (Guild lGuild : lGuilds) {
            pUtils.insertGuildIfAbsent(lGuild.getIdLong());
        }
    }

    private static void createConfigFiles() {
        SystemUtils.createDirIfAbsent(APP_CONFIG_FOLDER_PATH);
        SystemUtils.createFileIfAbsent(APP_CONFIG_FOLDER_PATH +"tokenfile.txt");
    }

    private static String getDatabasePath(String lDiscordToken) {
        String lTokenHash = Hashing.sha256()
                .hashString(lDiscordToken, StandardCharsets.UTF_8)
                .toString();
        String pFileName = APP_CONFIG_FOLDER_PATH +lTokenHash+".db";
        SystemUtils.createFileIfAbsent(pFileName);
        return pFileName;
    }

    public static void main(String[] args) throws SQLException, InterruptedException, IOException {
        createConfigFiles();

        if (args.length == 0) {
            BotFactory.create(new SchroedingersCatData());
        } else {
            BotFactory.create(new MusicCatData());
        }
    }
}
