package de.schroedingerscat.data;

import de.schroedingerscat.BotApplication;
import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

/**
 *
 *
 * @author KaitoKunTatsu
 * @version 3.0.0 | last edit: 15.07.2023
 * */
public interface BotData {

    public String[][][] getSlashCommands();
    public CommandData[] getContextCommands();
    public String[] getCommandCategories();
    public int getIndexInConfigFile();
    public ListenerAdapter[] getListeners(BotApplication pBot, Utils pUtils);
    public String[] getDatabaseTables();
}
