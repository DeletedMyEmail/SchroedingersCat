package de.schroedingerscat.data;

import de.schroedingerscat.BotApplication;
import de.schroedingerscat.Utils;
import de.schroedingerscat.commandhandler.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class MusicCatData implements BotData {

    // Command categories
    public final String[] mCategories = {
            "**:musical_note: Music**",
    };

    // All commands and their options
    public final String[][][] mCommands = {
                {
                    {"play_track", "Takes an url or song title, searches on YouTube and plays that song in your current voice channel", "string,track,Song name or youtube url,true"},
                    {"disconnect", "Disconnects the bot from your current voice channel"},
                    {"pause", "Pauses the current track playing in your voice channel"},
                    {"resume", "Resumes stopped track"},
                    {"skip", "Skips the next track(s)", "int,amount,Amount of tracks to skip,false"},
                }
    };

    private final String[] mTables = new String[0];
    private final CommandData[] mContextCommands = new CommandData[0];

    @Override
    public int getIndexInConfigFile() {
        return 0;
    }

    @Override
    public String[][][] getSlashCommands() {
        return mCommands;
    }

    @Override
    public CommandData[] getContextCommands() {
        return mContextCommands;
    }

    @Override
    public String[] getCommandCategories() {
        return mCategories;
    }

    @Override
    public ListenerAdapter[] getListeners(BotApplication pBot, Utils pUtils) {
        return new ListenerAdapter[]{
                new MusicHandler()
        };
    }

    @Override
    public String[] getDatabaseTables() {
        return mTables;
    }
}
