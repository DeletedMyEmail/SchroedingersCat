package de.schroedingerscat.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * Credits:<br>
 * <a href="https://github.com/sedmelluq/lavaplayer">Lavaplayer</a> <br>
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 30.09.2022
 * */
public class TrackScheduler extends AudioEventAdapter {

    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> trackQueue;

    public TrackScheduler(AudioPlayer lPlayer) {
        this.player = lPlayer;
        this.trackQueue = new LinkedBlockingQueue<>();
    }

    public void queueTrack(AudioTrack pTrack)
    {
        if (!player.startTrack(pTrack, true))
            trackQueue.offer(pTrack);
    }

    @Override
    public void onTrackEnd(AudioPlayer pPlayer, AudioTrack pTrack, AudioTrackEndReason pEndReason) {
        if (pEndReason.mayStartNext)
            this.player.startTrack(trackQueue.poll(), false);
    }
}
