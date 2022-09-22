package de.schroedingerscat.manager;

import de.schroedingerscat.Utils;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AutoRoleManager extends ListenerAdapter {

    private final Utils utils;

    public AutoRoleManager(Utils pUtils) {
        this.utils = pUtils;
    }
}
