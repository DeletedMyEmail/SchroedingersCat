package de.schroedingerscat.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;

public class GuildMusicManager extends DefaultAudioPlayerManager {

    private final AudioPlayer player;
    private final TrackScheduler trackScheduler;
    private final AudioPlayerSendHandler sendHandler;

    public GuildMusicManager(AudioPlayerManager pPlayerManager) {
        this(pPlayerManager.createPlayer());
    }

    public GuildMusicManager(AudioPlayer pPlayer)
    {
        player = pPlayer;
        trackScheduler = new TrackScheduler(player);
        player.addListener(trackScheduler);
        sendHandler = new AudioPlayerSendHandler(player);
    }

    public AudioPlayer getPlayer() {return player;}

    public TrackScheduler getTrackScheduler() {return trackScheduler;}

    public AudioPlayerSendHandler getSendHandler() {return sendHandler;}
}
