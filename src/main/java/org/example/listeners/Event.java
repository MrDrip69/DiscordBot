package org.example.listeners;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

public class Event extends ListenerAdapter {

    private static final String ADMIN_ROLE_NAME = "Banker";

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        String[] args = message.split("\\s+");

        Member member = event.getMember();
        if (member == null) return;

        // ================= CLEAR COMMAND =================
        if (args[0].equalsIgnoreCase("!clear")) {

            boolean isAdmin = member.isOwner()
                    || member.hasPermission(Permission.MESSAGE_MANAGE)
                    || member.getRoles().stream()
                        .anyMatch(r -> r.getName().equalsIgnoreCase(ADMIN_ROLE_NAME));

            if (!isAdmin) {
                event.getChannel().sendMessage("❌ You don’t have permission.")
                        .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            if (args.length < 2) {
                event.getChannel().sendMessage("❌ Usage: `!clear <amount>`")
                        .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("❌ Amount must be a number.")
                        .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            if (amount < 1) {
                event.getChannel().sendMessage("❌ Amount must be greater then 0.")
                        .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            event.getChannel().getHistory().retrievePast(amount + 1).queue(messages -> {
                event.getChannel().purgeMessages(messages);

                event.getChannel().sendMessage("🧹 Deleted **" + amount + "** messages.")
                        .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
            });

            return;
        }

        // ================= FUN MESSAGES =================
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
            event.getChannel().sendMessage("Kys sammir🧐").queue();
        }
        else if (message.equals("L bob")) {
            event.getChannel().sendMessage("L bob fr fr").queue();
        }
        else if (message.equals("L vuk")) {
            event.getChannel().sendMessage("L vuk fr fr").queue();
        }
    }
}

