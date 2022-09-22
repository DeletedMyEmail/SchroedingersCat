package de.schroedingerscat.manager;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SettingsManager extends ListenerAdapter {

    private final Utils utils;

    public SettingsManager(Utils pUtils) {
        this.utils = pUtils;
    }
}
