package de.schroedingerscat.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import de.schroedingerscat.Utils;
import de.schroedingerscat.commandhandler.MusicHandler;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;

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
    private final BlockingQueue<Pair<AudioTrack,String>> trackQueue;
    private TextChannel commandChannel;

    public TrackScheduler(AudioPlayer lPlayer) {
        this.player = lPlayer;
        this.trackQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        super.onPlayerPause(player);
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        super.onPlayerResume(player);
    }

    @Override
    public void onTrackStart(AudioPlayer pPlayer, AudioTrack pTrack)
    {
        commandChannel.sendMessageEmbeds(Utils.createEmbed(
                    MusicHandler.getCategoryColor(),
                    "Now Playing: `"+pTrack.getInfo().title+"` by `"+pTrack.getInfo().author+"`\n\nRequested by "+trackQueue.poll().getRight(),
                    commandChannel.getGuild().getSelfMember().getUser()))
                .queue();
    }

    @Override
    public void onTrackEnd(AudioPlayer pPlayer, AudioTrack pTrack, AudioTrackEndReason pEndReason)
    {
        if (pEndReason.mayStartNext )
            this.player.startTrack(this.trackQueue.element().getLeft(), false);
    }

    public void queueTrack(AudioTrack pTrack, String pUserWhoRequested)
    {
        if (!player.startTrack(pTrack, true))
            this.trackQueue.add(ImmutablePair.of(pTrack, pUserWhoRequested));
    }

    public void skipTracks(int pAmountOfTracks)
    {
        if (pAmountOfTracks > trackQueue.size())
            pAmountOfTracks = trackQueue.size();
        for (int i = 1; i < trackQueue.size(); ++i) {
            trackQueue.remove();
        }
        player.playTrack(this.trackQueue.element().getLeft());
    }

    public boolean isEmpty() {return this.trackQueue.isEmpty();}

    public void setTextChannel(TextChannel pTextChannel) {this.commandChannel = pTextChannel; }
}
