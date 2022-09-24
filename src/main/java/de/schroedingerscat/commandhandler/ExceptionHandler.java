package de.schroedingerscat.commandhandler;

import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

/**
 *
 *
 * @author Joshua H. | KaitoKunTatsu
 * @version 1.0.0 | last edit: 25.09.2022
 * */
public class ExceptionHandler extends ListenerAdapter {

    @Override
    public void onException(@Nonnull ExceptionEvent event)
    {
        System.out.println(
                "Exception occurren: "+ event
        );
    }

}
