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
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.discordbots.api.client.DiscordBotListAPI;
import org.jetbrains.annotations.NotNull;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Instance of a discord bot. Should be instanciated via the {@link BotFactory}. <br/>
 * Establishes database connection, connects to discord and topgg and registers all event listeners
 *
 * @author KaitoKunTatsu
 * @version 3.0.0 | last edit: 16.07.2023
 **/
public class BotApplication {

    private DiscordBotListAPI mTopGGApi;
    private JDA mJDA;

    public BotApplication(@NotNull String pDiscordToken) throws InterruptedException {
        JDABuilder lBuilder = JDABuilder.createDefault(pDiscordToken, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_REACTIONS);

        lBuilder.disableCache(CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS, CacheFlag.EMOJI);
        lBuilder.setMemberCachePolicy(MemberCachePolicy.ALL);

        lBuilder.setStatus(OnlineStatus.ONLINE);
        lBuilder.setActivity(Activity.watching("/help"));

        mJDA = lBuilder.build();
        mJDA.awaitReady();
    }

    public BotApplication(String pDiscordToken, String pTopggToken, String pTopggBotId) throws InterruptedException {
        mTopGGApi = new DiscordBotListAPI.Builder()
                .token(pTopggToken)
                .botId(pTopggBotId)
                .build();

        new BotApplication(pDiscordToken);
    }

    public void updateBotListApi() {
        if (mTopGGApi != null)
            mTopGGApi.setStats(mJDA.getGuilds().size());
    }

    public JDA getJDA() {return mJDA; }
}
