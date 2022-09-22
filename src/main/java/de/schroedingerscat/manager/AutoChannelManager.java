package de.schroedingerscat.manager;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AutoChannelManager extends ListenerAdapter {

    private final Utils utils;

    public AutoChannelManager(Utils pUtils) {
        this.utils = pUtils;
    }
}
