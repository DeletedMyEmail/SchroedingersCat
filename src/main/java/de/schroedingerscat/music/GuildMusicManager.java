package de.schroedingerscat.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;

/**
 *
 * Credits:<br>
 * <a href="https://github.com/sedmelluq/lavaplayer">Lavaplayer</a> <br>
 * <a href="https://youtu.be/1ClKoOCeeIQ">Some help for audio classes</a>
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 30.09.2022
 * */
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
