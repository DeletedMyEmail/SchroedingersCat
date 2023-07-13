package de.schroedingerscat;

import com.google.common.hash.Hashing;
import klibrary.utils.SystemUtils;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;

/**
 * A factory class for creating bot instances
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.1.1 | last edit: 13.07.2023
 **/
public class BotFactory {

    public static final String APP_CONFIG_FOLDER_PATH = SystemUtils.getLocalApplicationPath()+"/SchroedingersCat/";
    public static final String TOKEN_CONFIG_FILE = APP_CONFIG_FOLDER_PATH +"tokenfile.txt";

    public static BotApplication create(int pBotIndex) {
        createConfigFiles();
        return create(pBotIndex, new File(TOKEN_CONFIG_FILE));
    }

    private static BotApplication create(int pBotIndex, @NotNull File pTokenFile) {
        BotApplication lBotApp = null;
        try {
            String[] lTokens = Files.readAllLines(pTokenFile.toPath()).get(pBotIndex).split(" ");
            System.out.println("Initializing bot at index "+pBotIndex);

            if (lTokens.length >= 3) {
                lBotApp = new BotApplication(lTokens[0], getDatabasePath(lTokens[0]), lTokens[1], lTokens[2]);
            }
            else {
                lBotApp = new BotApplication(lTokens[0], getDatabasePath(lTokens[0]));
            }

            System.out.println(lBotApp.getJDA().getSelfUser().getName() + " online");
        }
        catch (SQLException sqlEx) {
            System.out.println("Could not connect to database");
        }
        catch (IOException ioEx) {
            System.out.println("Could not open or read file: "+ pTokenFile.getAbsolutePath());
        } catch (InterruptedException e) {
            System.out.println("Thread starting a bot got interupted");
        }

        return lBotApp;
    }

    public static BotApplication[] create(int[] pBotIndices) {
        createConfigFiles();
        File lTokenFile = new File(TOKEN_CONFIG_FILE);
        BotApplication[] lBots = new BotApplication[pBotIndices.length];
        for (int i = 0; i < pBotIndices.length; ++i) {
            lBots[i] = create(pBotIndices[i], lTokenFile);
        }

        return lBots;
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

    private static int[] toIntArray(String[] pStringArr) throws IllegalArgumentException {
        int[] lIntArr = new int[pStringArr.length];
        for (int i = 0; i < pStringArr.length; ++i) {
            lIntArr[i] = Integer.parseInt(pStringArr[i]);
        }

        return lIntArr;
    }

    public static void main(String[] args) {
        createConfigFiles();

        if (args.length == 0) {
            System.out.println("Give indices for tokens defined in tokenfile.txt as arguments. See https://github.com/KaitoKunTatsu/SchroedingersCat for more information");
        }
        else {
            int[] lIndices = toIntArray(args);
            create(lIndices);
        }
    }
}
