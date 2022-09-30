package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import de.schroedingerscat.music.GuildMusicManager;
import de.schroedingerscat.music.PlayerManager;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

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
            case "play_track" -> playTrackHandlerCommand(pEvent);
            case "disconnect" -> disconnectCommand(pEvent);
        }
    }

    private void playTrackHandlerCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();
        if (!pEvent.getMember().getVoiceState().inAudioChannel())
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You need to be in a voice channel to play music", pEvent.getUser())).queue();
        else if (pEvent.getGuild().getSelfMember().getVoiceState().inAudioChannel())
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: The cat is already connected to a channel", pEvent.getUser())).queue();
        else
        {
            AudioManager lAudioManager = pEvent.getGuild().getAudioManager();
            VoiceChannel lVoiceChannel = (VoiceChannel) pEvent.getMember().getVoiceState().getChannel();

            lAudioManager.openAudioConnection(lVoiceChannel);

            String lTrackUrl = pEvent.getOption("track").getAsString();

            if (!isUrl(lTrackUrl))
                lTrackUrl = "ytsearch:"+lTrackUrl+" audio";

            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(MUSIC_COLOR, ":white_check_mark: Searching for your track...", pEvent.getUser())).queue();
            playerManager.loadAndPlay(pEvent.getHook(), lTrackUrl);
        }
    }

    private void disconnectCommand(SlashCommandInteractionEvent pEvent)
    {
        pEvent.deferReply().queue();
        if (!pEvent.getGuild().getSelfMember().getVoiceState().inAudioChannel())
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: The cat is not connected to a channel", pEvent.getUser())).queue();

        else if (!pEvent.getMember().getVoiceState().inAudioChannel() || !pEvent.getMember().getVoiceState().getChannel().equals(pEvent.getGuild().getSelfMember().getVoiceState().getChannel()))
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(Color.red, ":x: You need to be connect to the same voice channel as the cat", pEvent.getUser())).queue();

        else
        {
            pEvent.getGuild().getAudioManager().closeAudioConnection();
            pEvent.getHook().editOriginalEmbeds(Utils.createEmbed(MUSIC_COLOR, ":white_check_mark: Left your voice channel", pEvent.getUser())).queue();
        }
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
