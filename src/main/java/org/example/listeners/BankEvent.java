package org.example.listeners;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.example.storage.JsonStorage;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class BankEvent extends ListenerAdapter {

    private static final String ADMIN_ROLE_NAME = "Banker";

    // ===== SHOP CONFIGURATION =====
    private static final HashMap<String, Integer> SHOP_PRICES = new HashMap<>();
    private static final HashMap<String, String> SHOP_ROLES = new HashMap<>();
    private static final HashMap<String, Integer> SHOP_RANKS = new HashMap<>();

    static {
        SHOP_PRICES.put("knight", 900); SHOP_ROLES.put("knight", "knight"); SHOP_RANKS.put("knight", 4);
        SHOP_PRICES.put("dame", 900); SHOP_ROLES.put("dame", "dame"); SHOP_RANKS.put("dame", 4);
        SHOP_PRICES.put("lord", 950); SHOP_ROLES.put("lord", "lord"); SHOP_RANKS.put("lord", 8);
        SHOP_PRICES.put("lady", 950); SHOP_ROLES.put("lady", "lady"); SHOP_RANKS.put("lady", 8);
        SHOP_PRICES.put("duke", 1250); SHOP_ROLES.put("duke", "duke"); SHOP_RANKS.put("duke", 9);
        SHOP_PRICES.put("duchess", 1250); SHOP_ROLES.put("duchess", "duchess"); SHOP_RANKS.put("duchess", 9);
        SHOP_PRICES.put("prince", 12000); SHOP_ROLES.put("prince", "prince"); SHOP_RANKS.put("prince", 13);
        SHOP_PRICES.put("princess", 12000); SHOP_ROLES.put("princess", "princess"); SHOP_RANKS.put("princess", 13);
        SHOP_PRICES.put("archduke", 20000); SHOP_ROLES.put("archduke", "archduke"); SHOP_RANKS.put("archduke", 22);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw().trim();
        if (message.isEmpty()) return;

        String[] args = message.split("\\s+");
        String cmd = args[0].toLowerCase();
        Member sender = event.getMember();
        if (sender == null) return;

        switch (cmd) {
            case "!top" -> handleTop(event);
            case "!addtm" -> handleAddT(sender, args, event);
            case "!addm" -> handleAdd(sender, args, event);
            case "!removem" -> handleRemove(sender, args, event);
            case "!addp" -> handleAddP(sender, args, event);
            case "!removep" -> handleRemoveP(sender, args, event);
            case "!give" -> handleGive(sender, args, event);
            case "!buy" -> handleBuy(sender, args, event);
            case "!info" -> handleInfo(sender, args, event);
            case "!help" -> handleHelp(event);
            case "!set" -> handleSet(sender, args, event);
            case "!fix" -> handleFix(sender, event);
            case "!reset" -> handleReset(sender, args, event);
            case "!addrole" -> handleAddRole(sender, args, event);
        }
    }

    // ========================= COMMAND HANDLERS =========================

    private void handleTop(MessageReceivedEvent event) {
        event.getGuild().loadMembers().onSuccess(members -> {
            members.removeIf(m -> m.getUser().isBot());
            members.sort((a, b) -> Integer.compare(JsonStorage.getBalance(b.getIdLong()), JsonStorage.getBalance(a.getIdLong())));
            StringBuilder sb = new StringBuilder("🏆 **Top 10 richest users** 🏆\n");
            int limit = Math.min(10, members.size());
            for (int i = 0; i < limit; i++) {
                Member m = members.get(i);
                sb.append(i + 1).append(". ").append(m.getEffectiveName())
                        .append(" — ").append(JsonStorage.getBalance(m.getIdLong())).append("\n");
            }
            event.getChannel().sendMessage(sb.toString()).queue();
        });
    }

    // ===== MONEY COMMANDS =====
    private void handleAdd(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("❌ You don't have permission.").queue();
            return;
        }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) {
            event.getChannel().sendMessage("Usage: !addM @user amount").queue();
            return;
        }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                event.getChannel().sendMessage("❌ Amount must be greater than 0").queue();
                return;
            }

            long id = target.getIdLong();
            // Ensure user exists
            JsonStorage.getBalance(id);
            JsonStorage.addBalance(id, amount);

            event.getChannel().sendMessage("✅ Added " + amount + " to " + target.getEffectiveName() +
                    " | Balance: " + JsonStorage.getBalance(id)).queue();

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Amount must be a number.").queue();
        }
    }

    private void handleAddT(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("❌ You don't have permission.").queue();
            return;
        }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) {
            event.getChannel().sendMessage("Usage: !addtM @user amount").queue();
            return;
        }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                event.getChannel().sendMessage("❌ Amount must be greater than 0").queue();
                return;
            }

            long id = target.getIdLong();
            long taxUserID = 942818122681974804L; // banker collector
            int taxAmount = (amount * 25) / 100;
            int userReceives = amount - taxAmount;
            int bonusToTaxCollector = (taxAmount * 5) / 100;

            // Ensure users exist
            JsonStorage.getBalance(id);
            JsonStorage.getBalance(taxUserID);

            JsonStorage.addBalance(id, userReceives);
            JsonStorage.addBalance(taxUserID, bonusToTaxCollector);

            event.getChannel().sendMessage("✅ Added " + userReceives + " to " + target.getEffectiveName() +
                    " (25% tax applied) | Balance: " + JsonStorage.getBalance(id) +
                    "\n💰 Added " + bonusToTaxCollector + " to the banker collector").queue();

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Amount must be a number.").queue();
        }
    }

    private void handleRemove(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("❌ You don't have permission.").queue();
            return;
        }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) {
            event.getChannel().sendMessage("Usage: !removeM @user amount").queue();
            return;
        }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                event.getChannel().sendMessage("❌ Amount must be greater than 0").queue();
                return;
            }

            long id = target.getIdLong();
            // Ensure user exists
            JsonStorage.getBalance(id);
            JsonStorage.addBalance(id, -amount);

            event.getChannel().sendMessage("✅ Removed " + amount + " from " + target.getEffectiveName() +
                    " | Balance: " + JsonStorage.getBalance(id)).queue();

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Amount must be a number.").queue();
        }
    }

    // ===== POINTS COMMANDS =====
    private void handleAddP(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("❌ You don't have permission.").queue();
            return;
        }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) {
            event.getChannel().sendMessage("Usage: !addP @user amount").queue();
            return;
        }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                event.getChannel().sendMessage("❌ Amount must be greater than 0").queue();
                return;
            }

            long id = target.getIdLong();
            // Ensure user exists
            JsonStorage.getPoints(id);
            JsonStorage.addPoints(id, amount);

            event.getChannel().sendMessage("✅ Added " + amount + " points to " + target.getEffectiveName() +
                    " | Points: " + JsonStorage.getPoints(id)).queue();

            checkRankUp(target, event);

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Amount must be a number.").queue();
        }
    }

    private void handleRemoveP(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("❌ You don't have permission.").queue();
            return;
        }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) {
            event.getChannel().sendMessage("Usage: !removeP @user amount").queue();
            return;
        }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                event.getChannel().sendMessage("❌ Amount must be greater than 0").queue();
                return;
            }

            long id = target.getIdLong();
            // Ensure user exists
            JsonStorage.getPoints(id);
            JsonStorage.addPoints(id, -amount);

            event.getChannel().sendMessage("✅ Removed " + amount + " points from " + target.getEffectiveName() +
                    " | Points: " + JsonStorage.getPoints(id)).queue();

            checkRankDown(target, event);

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Amount must be a number.").queue();
        }
    }


    private void handleGive(Member sender, String[] args, MessageReceivedEvent event) {
        if (args.length < 3 || event.getMessage().getMentions().getMembers().isEmpty()) {
            event.getChannel().sendMessage("Usage: !give @user amount").queue(); return;
        }
        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;
        try {
            int amount = Integer.parseInt(args[2]);
            long senderId = sender.getIdLong();
            long targetId = target.getIdLong();
            int senderBalance = JsonStorage.getBalance(senderId);
            if (sender.equals(target)) { event.getChannel().sendMessage("❌ Cannot give money to yourself.").queue(); return; }
            if (amount <= 0 || senderBalance < amount) {
                event.getChannel().sendMessage("❌ Invalid amount or insufficient balance").queue(); return;
            }
            JsonStorage.addBalance(senderId, -amount);
            JsonStorage.addBalance(targetId, amount);
            event.getChannel().sendMessage(sender.getEffectiveName() + " gave " + amount + " to " + target.getEffectiveName() +
                    "\nYour balance: " + JsonStorage.getBalance(senderId) +
                    "\nReceiver balance: " + JsonStorage.getBalance(targetId)).queue();
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Amount must be a number.").queue();
        }
    }

    private void handleBuy(Member sender, String[] args, MessageReceivedEvent event) {
        if (args.length < 2) { event.getChannel().sendMessage("Usage: !buy itemName").queue(); return; }
        String item = args[1].toLowerCase();
        if (!SHOP_PRICES.containsKey(item)) { event.getChannel().sendMessage("Item not found").queue(); return; }
        int price = SHOP_PRICES.get(item);
        int userBalance = JsonStorage.getBalance(sender.getIdLong());
        int userRank = JsonStorage.getRank(sender.getIdLong());
        int requiredRank = SHOP_RANKS.get(item);
        if (userRank < requiredRank) { event.getChannel().sendMessage("❌ Rank too low").queue(); return; }
        if (userBalance < price) { event.getChannel().sendMessage("❌ Insufficient balance").queue(); return; }
        JsonStorage.addBalance(sender.getIdLong(), -price);
        Role role = event.getGuild().getRolesByName(SHOP_ROLES.get(item), true).stream().findFirst().orElse(null);
        if (role != null) event.getGuild().addRoleToMember(sender, role).queue();
        event.getChannel().sendMessage(sender.getEffectiveName() + " bought " + item + "! Balance: " + JsonStorage.getBalance(sender.getIdLong())).queue();
    }

    private void handleInfo(Member sender, String[] args, MessageReceivedEvent event) {
        Member target = event.getMessage().getMentions().getMembers().isEmpty() ? sender : event.getMessage().getMentions().getMembers().get(0);
        long id = target.getIdLong();
        event.getChannel().sendMessage(target.getEffectiveName() + " | Balance: " + JsonStorage.getBalance(id) +
                " | Rank: " + toRoman(JsonStorage.getRank(id)) +
                " | Points: " + JsonStorage.getPoints(id)).queue();
    }

    private void handleHelp(MessageReceivedEvent event) {
        String text = """
                🏦 Bank Commands
                !info @user - Show balance/rank
                !give @user amount - Transfer money
                !buy itemName - Purchase item
                !top - Top 10
                !addM @user amount - Admin add money
                !addtM @user amount - Admin add taxed
                !addP @user amount - Admin add points
                !removeP @user amount - Admin remove points
                !removeM @user amount - Admin remove money
                !set @user rank - Admin set rank
                !reset @user - Admin reset user
                !fix - Admin fix nicknames
                !addrole @user RoleName - Admin add role
                """;
        event.getChannel().sendMessage(text).queue();
    }

    private void handleSet(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) { event.getChannel().sendMessage("No permission").queue(); return; }
        if (args.length < 3 || event.getMessage().getMentions().getMembers().isEmpty()) { event.getChannel().sendMessage("Usage: !set @user rank").queue(); return; }
        Member target = event.getMessage().getMentions().getMembers().get(0);
        try {
            int rank = Integer.parseInt(args[2]);
            JsonStorage.saveUser(target.getIdLong(), JsonStorage.getBalance(target.getIdLong()), rank, 0);
            if (!target.isOwner()) target.modifyNickname(getBaseName(target) + " " + toRoman(rank)).queue();
            event.getChannel().sendMessage("Set rank of " + target.getEffectiveName() + " to " + toRoman(rank)).queue();
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Rank must be a number").queue();
        }
    }

    private void handleFix(Member sender, MessageReceivedEvent event) {
        if (!isAllowed(sender)) { event.getChannel().sendMessage("No permission").queue(); return; }
        event.getGuild().loadMembers().onSuccess(members -> {
            for (Member m : members) {
                if (m.isOwner() || m.getUser().isBot()) continue;
                m.modifyNickname(getBaseName(m) + " " + toRoman(JsonStorage.getRank(m.getIdLong()))).queue();
            }
            event.getChannel().sendMessage("Nicknames fixed!").queue();
        });
    }

    private void handleReset(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) { event.getChannel().sendMessage("No permission").queue(); return; }
        if (args.length < 2 || event.getMessage().getMentions().getMembers().isEmpty()) { event.getChannel().sendMessage("Usage: !reset @user").queue(); return; }
        Member target = event.getMessage().getMentions().getMembers().get(0);
        JsonStorage.saveUser(target.getIdLong(), 0, 1, 0);
        if (!target.isOwner()) target.modifyNickname(getBaseName(target) + " I").queue();
        event.getChannel().sendMessage(target.getEffectiveName() + " reset!").queue();
    }

    private void handleAddRole(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) { event.getChannel().sendMessage("No permission").queue(); return; }
        if (args.length < 3 || event.getMessage().getMentions().getMembers().isEmpty()) { event.getChannel().sendMessage("Usage: !addrole @user RoleName").queue(); return; }
        Member target = event.getMessage().getMentions().getMembers().get(0);
        String roleName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        Role role = event.getGuild().getRolesByName(roleName, true).stream().findFirst().orElse(null);
        if (role == null) { event.getChannel().sendMessage("Role not found: " + roleName).queue(); return; }
        event.getGuild().addRoleToMember(target, role).queue(
                s -> event.getChannel().sendMessage("Added role `" + roleName + "` to " + target.getEffectiveName()).queue(),
                f -> event.getChannel().sendMessage("Failed to add role").queue()
        );
    }

    // ========================= HELPER METHODS =========================

    private boolean isAllowed(Member member) {
        return member != null && (member.isOwner() ||
                member.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(ADMIN_ROLE_NAME)));
    }

    private void checkRankUp(Member member, MessageReceivedEvent event) {
        long id = member.getIdLong();
        int points = JsonStorage.getPoints(id);
        int rank = JsonStorage.getRank(id);
        int balance = JsonStorage.getBalance(id);

        while (points >= rank * 1000) {
            points -= rank * 1000;
            rank++;
            JsonStorage.saveUser(id, balance, rank, points);

            if (!member.isOwner()) {
                member.modifyNickname(getBaseName(member) + " " + toRoman(rank)).queue();
            }

            event.getChannel().sendMessage(member.getEffectiveName() + " ranked up to " + toRoman(rank) + "! ⭐ Remaining points: " + points)
                    .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
        }
    }

    private void checkRankDown(Member member, MessageReceivedEvent event) {
        long id = member.getIdLong();
        int balance = JsonStorage.getBalance(id);
        int rank = JsonStorage.getRank(id);
        int points = JsonStorage.getPoints(id);

        while (points < 0 && rank > 1) {
            rank--;
            points += rank * 1000; // restore points for previous rank
            JsonStorage.saveUser(id, balance, rank, points);

            if (!member.isOwner()) {
                member.modifyNickname(getBaseName(member) + " " + toRoman(rank)).queue();
            }

            event.getChannel().sendMessage(member.getEffectiveName() + " ranked down to " + toRoman(rank))
                    .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
        }

        // Clamp points so they never stay negative at rank 1
        int newPoints = points < 0 ? 0 : points;
        JsonStorage.saveUser(id, balance, rank, newPoints);
    }

    private String getBaseName(Member member) {
        String nick = member.getNickname();
        if (nick == null) nick = member.getEffectiveName();

        String[] parts = nick.split(" ");
        if (parts.length > 1) {
            String lastPart = parts[parts.length - 1];
            if (lastPart.matches("(?i)M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})")) {
                return nick.substring(0, nick.lastIndexOf(" "));
            }
        }
        return nick;
    }

    private String toRoman(int num) {
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] romans = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (num >= values[i]) {
                num -= values[i];
                sb.append(romans[i]);
            }
        }
        return sb.toString();
    }
}
