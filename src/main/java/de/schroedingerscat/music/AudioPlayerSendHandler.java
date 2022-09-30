package de.schroedingerscat.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

/**
 *
 * Credits:<br>
 * <a href="https://github.com/sedmelluq/lavaplayer">Lavaplayer</a> <br>
 * <a href="https://youtu.be/1ClKoOCeeIQ">Some help for audio classes</a>
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 30.09.2022
 * */
public class AudioPlayerSendHandler implements AudioSendHandler {

    private final AudioPlayer audioPlayer;
    private final MutableAudioFrame frame;
    private final ByteBuffer buffer;

    public AudioPlayerSendHandler(AudioPlayer pPlayer) {
        audioPlayer = pPlayer;
        frame = new MutableAudioFrame();
        buffer = ByteBuffer.allocate(1024);
        frame.setBuffer(buffer);
    }

    @Override
    public boolean canProvide() {
        return audioPlayer.provide(frame);
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        buffer.flip();
        return buffer;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
