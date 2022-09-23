package de.schroedingerscat.manager;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

public class AutoChannelManager extends ListenerAdapter {

    private static final Color AUTOCHANNEL_COLOR = new Color(25,196,234);

    private final Utils utils;

    public AutoChannelManager(Utils pUtils) {
        this.utils = pUtils;
    }


    public static Color getCategoryColor() {return AUTOCHANNEL_COLOR; }
}
