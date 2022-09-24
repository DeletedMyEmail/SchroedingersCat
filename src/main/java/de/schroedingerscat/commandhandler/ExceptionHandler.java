package de.schroedingerscat.commandhandler;

import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

public class ExceptionHandler extends ListenerAdapter {

    @Override
    public void onException(@Nonnull ExceptionEvent event)
    {
        System.out.println(
                "Exception occurren: "+ event
        );
    }

}
