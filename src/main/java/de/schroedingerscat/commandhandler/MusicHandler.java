package de.schroedingerscat.commandhandler;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import de.schroedingerscat.Utils;
import de.schroedingerscat.music.PlayerManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.annotation.Nonnull;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 2.0.0 | last edit: 30.09.2022
 * */
public class MusicHandler extends ListenerAdapter {

    /** Default color of this category to be used for embeds */
    private static final Color MUSIC_COLOR = new Color(2,140,152);

    private final PlayerManager playerManager;

    public MusicHandler() {
        playerManager = new PlayerManager();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent pEvent)
    {
        switch (pEvent.getName())
        {
            case "play_track" -> playTrackCommand(pEvent);
            case "disconnect" -> disconnectCommand(pEvent);
            case "pause" -> pauseCommand(pEvent);
            case "resume" -> resumeCommand(pEvent);
            case "skip" ->skipCommand(pEvent);
        }
    }



    @Override
    public void onStringSelectInteraction(@Nonnull StringSelectInteractionEvent pEvent)
    {
        if (!pEvent.getComponent().getId().equals("TrackSelection")) return;
        pEvent.deferReply().queue();
        playTrackCommand(pEvent.getHook(), pEvent.getValues().get(0), pEvent.getChannel().asTextChannel());
    }

    private void playTrackCommand(SlashCommandInteractionEvent pEvent) {
        pEvent.deferReply().queue();
        playTrackCommand(pEvent.getHook(), pEvent.getOption("track").getAsString(), pEvent.getChannel().asTextChannel());
    }

    private void playTrackCommand(InteractionHook pHook, String pTrackUrlOrName, TextChannel pChannel)
    {
        if (allowedToUseCommand(pHook))
        {
            AudioManager lAudioManager = pHook.getInteraction().getGuild().getAudioManager();
            VoiceChannel lVoiceChannel = (VoiceChannel) pHook.getInteraction().getMember().getVoiceState().getChannel();

            if (!isUrl(pTrackUrlOrName)) {
                pTrackUrlOrName = "ytsearch:" + pTrackUrlOrName + " audio";
            }
            if (!lAudioManager.isConnected())
                lAudioManager.openAudioConnection(lVoiceChannel);
            playerManager.loadAndPlay(pHook, pChannel, pTrackUrlOrName, true);
        }
    }

    private void disconnectCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();
        if (allowedToUseCommand(pEvent.getHook()))
        {
            pEvent.getGuild().getAudioManager().closeAudioConnection();
            playerManager.getGuildMusicManager(pEvent.getGuild()).getTrackScheduler().clearQueue();
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(MUSIC_COLOR, ":white_check_mark: Left your voice channel", pEvent.getUser())).queue();
        }
    }

    private void pauseCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();
        if (!allowedToUseCommand(pEvent.getHook())) return;

        AudioPlayer lGuildsPlayer = playerManager.getGuildMusicManager(pEvent.getGuild()).getPlayer();
        if (lGuildsPlayer.isPaused())
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Track is already paused",pEvent.getUser())).queue();
        else {
            lGuildsPlayer.setPaused(true);
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(MUSIC_COLOR, ":white_check_mark: Paused track: `"+lGuildsPlayer.getPlayingTrack().getInfo().title+"`",pEvent.getUser())).queue();
        }
    }

    private void resumeCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();
        if (!allowedToUseCommand(pEvent.getHook())) return;

        AudioPlayer lGuildsPlayer = playerManager.getGuildMusicManager(pEvent.getGuild()).getPlayer();
        if (!lGuildsPlayer.isPaused())
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: Track already playing",pEvent.getUser())).queue();
        else {
            lGuildsPlayer.setPaused(false);
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(MUSIC_COLOR, ":white_check_mark: Resumed track: `"+lGuildsPlayer.getPlayingTrack().getInfo().title+"`",pEvent.getUser())).queue();
        }
    }

    private void skipCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();
        if (!allowedToUseCommand(pEvent.getHook())) return;

        int lAmountToSkip = 1;
        if (pEvent.getOption("amount") != null)
            lAmountToSkip = pEvent.getOption("amount").getAsInt();
        if (lAmountToSkip < 1)
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x:  You can't skip less than 1 track",pEvent.getUser())).queue();
        else {
            playerManager.getGuildMusicManager(pEvent.getGuild()).getTrackScheduler().skipTracks(lAmountToSkip);
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(MUSIC_COLOR, ":white_check_mark: Skipped **"+lAmountToSkip+"** track(s)",pEvent.getUser())).queue();
        }
    }

    private boolean allowedToUseCommand(InteractionHook pHook) {
        Member lMember = pHook.getInteraction().getMember();
        if (!lMember.getVoiceState().inAudioChannel()) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You need to be in a voice channel to use this command", lMember.getUser())).queue();
            return false;
        }
        if (lMember.getGuild().getSelfMember().getVoiceState().inAudioChannel() && !lMember.getGuild().getSelfMember().getVoiceState().getChannel().equals(lMember.getVoiceState().getChannel())) {
            pHook.editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: The cat is connected to another channel", lMember.getUser())).queue();
            return false;
        }
        return true;
    }

    private boolean isUrl(String pUrl)
    {
        try {
            new URI(pUrl);
            return true;
        }
        catch (URISyntaxException e) {
            return false;
        }
    }

    public static Color getCategoryColor() {return MUSIC_COLOR; }
}
