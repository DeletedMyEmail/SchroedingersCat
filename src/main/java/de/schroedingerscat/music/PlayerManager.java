package de.schroedingerscat.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.schroedingerscat.Main;
import de.schroedingerscat.Utils;
import de.schroedingerscat.commandhandler.MusicHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.awt.*;
import java.util.HashMap;

public class PlayerManager {

    private final HashMap<Long, GuildMusicManager> guildMusicManager;
    private final AudioPlayerManager playerManager;

    public PlayerManager() {
        this.guildMusicManager = new HashMap<>();
        playerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    public GuildMusicManager getGuildMusicManager(Guild pGuild)
    {
        return guildMusicManager.computeIfAbsent(pGuild.getIdLong(), guildId ->
        {
            final GuildMusicManager lGuildMusicManager = new GuildMusicManager(playerManager);
            pGuild.getAudioManager().setSendingHandler(lGuildMusicManager.getSendHandler());
            return lGuildMusicManager;
        });
    }

    public void loadAndPlay(InteractionHook pHook, String pTrackUrl)
    {
        GuildMusicManager lGuildMusicManager = getGuildMusicManager(pHook.getInteraction().getGuild());
        playerManager.loadItemOrdered(lGuildMusicManager, pTrackUrl, new AudioLoadResultHandler()
        {

            @Override
            public void trackLoaded(AudioTrack pTrack) {
                lGuildMusicManager.getTrackScheduler().queueTrack(pTrack);
                pHook.editOriginalEmbeds(Utils.createEmbed(
                        MusicHandler.getCategoryColor(),
                        ":white_check_mark: Adding **"+pTrack.getInfo().title+"** by **"+pTrack.getInfo().author+"** to queue",
                        Main.getJDA().getUserById("872475386620026971"))
                ).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist pAudioPlaylist) {
                if (pAudioPlaylist.getTracks().isEmpty()) return;

                AudioTrack lTrack = pAudioPlaylist.getTracks().get(0);

                lGuildMusicManager.getTrackScheduler().queueTrack(lTrack);
                pHook.editOriginalEmbeds(Utils.createEmbed(
                        MusicHandler.getCategoryColor(),
                        ":white_check_mark: Adding **"+lTrack.getInfo().title+"** by **"+lTrack.getInfo().author+"** to queue",
                        Main.getJDA().getUserById("872475386620026971"))
                ).queue();
            }

            @Override
            public void noMatches()
            {
                pHook.editOriginalEmbeds(Utils.createEmbed(
                        Color.red,
                        ":x: Track not found",
                        Main.getJDA().getUserById("872475386620026971")))
                        .queue();
            }

            @Override
            public void loadFailed(FriendlyException pEx) {

            }
        });
    }
}
