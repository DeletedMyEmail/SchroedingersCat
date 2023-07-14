package de.schroedingerscat;

import de.schroedingerscat.commandhandler.*;
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
import org.jetbrains.annotations.NotNull;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Instance of a discord bot. Should be instanciated via the {@link BotFactory}. <br/>
 * Establishes database connection, connects to discord and topgg and registers all event listeners
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.1.1 | last edit: 13.07.2023
 **/
public class BotApplication {

    private DiscordBotListAPI mTopGGApi;
    private Utils mUtils;
    private JDA mJDA;

    public BotApplication(@NotNull String pDiscordToken, @NotNull String pDatabasePath) throws SQLException, InterruptedException {
        mUtils = new Utils(pDatabasePath);
        mUtils.createTables();

        JDABuilder lBuilder = JDABuilder.createDefault(pDiscordToken, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_BANS);

        lBuilder.setMemberCachePolicy(MemberCachePolicy.ALL);
        lBuilder.addEventListeners(
                new AutoChannelHandler(mUtils),
                new AutoRoleHandler(mUtils),
                new EconomyHandler(mUtils, this),
                new ReactionRoleHandler(mUtils),
                new SettingsHandler(mUtils, this),
                new CategorylessHandler(mUtils),
                new ExceptionHandler(),
                new CatsAndPetsHandler(mUtils)
                //new MusicHandler()
        );

        lBuilder.setStatus(OnlineStatus.ONLINE);
        lBuilder.setActivity(Activity.watching("/help"));

        mJDA = lBuilder.build();
        mJDA.awaitReady();
        insertGuildsInDBIfAbsent();
        addSlashCommands();
    }

    public BotApplication(String pDiscordToken, String pDatabasePath, String pTopggToken, String pTopggBotId) throws SQLException, InterruptedException {
        mTopGGApi = new DiscordBotListAPI.Builder()
                .token(pTopggToken)
                .botId(pTopggBotId)
                .build();

        new BotApplication(pDiscordToken, pDatabasePath);
    }

    private void insertGuildsInDBIfAbsent() throws SQLException {
        List<Guild> lGuilds =  mJDA.getGuilds();
        for (Guild guild : lGuilds) {
            mUtils.insertGuildIfAbsent(guild.getIdLong());
        }
    }

    /**
     *
     *
     * */
    private void addSlashCommands()
    {
        List<CommandData> lCommands = new ArrayList<>();
        for(String[][] commandsForCategory : BotData.COMMANDS)
        {
            for (String[] commandStructure : commandsForCategory)
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
        mJDA.updateCommands().addCommands(lCommands).queue();
        System.out.println("Commands updated");
    }

    public void updateBotListApi() {
        if (mTopGGApi != null)
            mTopGGApi.setStats(mJDA.getGuilds().size());
    }

    public JDA getJDA() {return mJDA; }
}
