package de.schroedingerscat.manager;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

public class SettingsManager extends ListenerAdapter {

    private static final Color SERVERSETTINGS_COLOR = new Color(83,80,84);

    private final Utils utils;

    public SettingsManager(Utils pUtils) {
        this.utils = pUtils;
    }


    public static Color getCategoryColor() {return SERVERSETTINGS_COLOR; }
}
