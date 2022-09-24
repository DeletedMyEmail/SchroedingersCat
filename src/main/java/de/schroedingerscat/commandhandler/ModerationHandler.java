package de.schroedingerscat.commandhandler;

import de.schroedingerscat.Utils;

import java.awt.*;

public class ModerationHandler {

    private static final Color MODERATION_COLOR = new Color(237,81,9);

    private final Utils utils;

    public ModerationHandler(Utils pUtils) {
        this.utils = pUtils;
    }

    public static Color getCategoryColor() {return MODERATION_COLOR; }
}
