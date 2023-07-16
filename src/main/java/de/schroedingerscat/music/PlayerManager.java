package de.schroedingerscat.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.schroedingerscat.Utils;
import de.schroedingerscat.commandhandler.MusicHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;

/**
 *
 * Credits:<br>
 * <a href="https://github.com/sedmelluq/lavaplayer">Lavaplayer</a> <br>
 * <a href="https://youtu.be/1ClKoOCeeIQ">Some help for audio classes</a>
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 30.09.2022
 * */
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

    public void loadAndPlay(InteractionHook pHook, TextChannel pChannel, String pTrackUrl, boolean pIsSearch)
    {
        GuildMusicManager lGuildMusicManager = getGuildMusicManager(pHook.getInteraction().getGuild());
        lGuildMusicManager.getTrackScheduler().setTextChannel(pChannel);
        playerManager.loadItemOrdered(lGuildMusicManager, pTrackUrl, new AudioLoadResultHandler()
        {

            @Override
            public void trackLoaded(AudioTrack pTrack)
            {
                lGuildMusicManager.getTrackScheduler().queueTrack(pTrack, pHook.getInteraction().getUser().getAsMention());
                String lPlayingOrAdded = "Added to queue:";
                if (lGuildMusicManager.getTrackScheduler().isEmpty()) lPlayingOrAdded = "Playing:";

                pHook.editOriginalEmbeds(Utils.createEmbed(
                        MusicHandler.getCategoryColor(),
                        ":white_check_mark: "+lPlayingOrAdded+" `"+pTrack.getInfo().title+"` by `"+pTrack.getInfo().author+"`",
                        pHook.getInteraction().getUser())
                ).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist pAudioPlaylist)
            {
                if (!pIsSearch) return;

                StringSelectMenu.Builder lMenu = StringSelectMenu.create("TrackSelection");
                List<AudioTrack> lTracks = pAudioPlaylist.getTracks();
                StringBuilder lDescription = new StringBuilder("**Choose one of the following search results:**\n\n");
                for (int i = 0; i < lTracks.size() && i < 10; i++)
                {
                    lMenu.addOption(
                            lTracks.get(i).getInfo().title + " by " + lTracks.get(i).getInfo().author,
                            lTracks.get(i).getIdentifier()
                    );
                    lDescription.append("**").append(i+1).append(".** ").append(lTracks.get(i).getInfo().title).append(" by ").append(lTracks.get(i).getInfo().author).append("\n");
                }
                pHook.editOriginalEmbeds(Utils.createEmbed(MusicHandler.getCategoryColor(), lDescription.toString(), pHook.getJDA().getSelfUser())).setActionRow(lMenu.build()).queue();
            }

            @Override
            public void noMatches()
            {
                pHook.editOriginalEmbeds(Utils.createEmbed(
                        Color.red,
                        ":x: Track not found",
                                pHook.getInteraction().getUser()))
                        .queue();
            }

            @Override
            public void loadFailed(FriendlyException pEx) {
                pHook.editOriginalEmbeds(Utils.createEmbed(
                                Color.red,
                                ":x: Loading track failed",
                                pHook.getInteraction().getUser()))
                        .queue();
            }
        });
    }
}
