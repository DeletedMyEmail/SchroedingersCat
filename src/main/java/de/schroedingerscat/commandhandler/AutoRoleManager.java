package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

public class AutoRoleManager extends ListenerAdapter {

    private static final Color AUTOROLE_COLOR = new Color(25,234,130);

    private final Utils utils;

    public AutoRoleManager(Utils pUtils) {
        this.utils = pUtils;
    }

    public static Color getCategoryColor() {return AUTOROLE_COLOR; }
}
