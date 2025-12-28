package org.example.listeners;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Event extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();

        if (message.equals("L dani")) {
            event.getChannel().sendMessage("L dani fr fr").queue();
        }
        else if (message.equals("L luka")) {
            event.getChannel().sendMessage("L luka fr fr").queue();
        }
        else if (message.contains("💔") || message.contains("🥀")) {
            event.getChannel().sendMessage("bruh🥀").queue();
        }
        else if (message.contains("L sammir")) {
            event.getChannel().sendMessage("Kys sammir\uD83E\uDD40").queue();
        }
        else if (message.equals("L bob")) {
            event.getChannel().sendMessage("L bob fr fr").queue();
        }
        else if (message.equals("L vuk")) {
            event.getChannel().sendMessage("L vuk fr fr").queue();
        }
        /* 
        cd DiscordBot && mvn clean compile package -q -DskipTests && pkill -9 -f "mvn exec:java" && sleep 3
        */
    }
}