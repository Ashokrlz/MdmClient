package com.example.mdmclient;

/*******************************************************************************
 *  Event Handler to send messages between Android components
 *  that can be published & subscribed to.
 *  Command, Value
 *******************************************************************************/

public class EventHandler {

    public final String command;
    public final String value;

    public EventHandler(String command, String value)
    {
        this.command = command;
        this.value = value;
    }

}
