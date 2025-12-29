package org.example.listeners;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.example.storage.JsonStorage;

import java.util.concurrent.TimeUnit;

public class BankEvent extends ListenerAdapter {

    private static final String ADMIN_ROLE_NAME = "Banker";

    // ===== SHOP CONFIGURATION =====
    private static final java.util.HashMap<String, Integer> SHOP_PRICES = new java.util.HashMap<>();
    private static final java.util.HashMap<String, String> SHOP_ROLES = new java.util.HashMap<>();
    private static final java.util.HashMap<String, Integer> SHOP_RANKS = new java.util.HashMap<>();

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
        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        if (message.isEmpty()) return;

        String[] args = message.split("\\s+");
        String cmd = args[0].toLowerCase();
        Member sender = event.getMember();
        if (sender == null) return;

        switch (cmd) {
            case "!top" -> handleTop(event);
            case "!addt" -> handleAddT(sender, args, event);
            case "!add" -> handleAdd(sender, args, event);
            case "!remove" -> handleRemove(sender, args, event);
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

    // ========== COMMAND HANDLERS ==========
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

    private void handleAddT(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) { event.getChannel().sendMessage("You don't have permission.").queue(); return; }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) { 
            event.getChannel().sendMessage("Usage: !addt @user amount").queue(); return; 
        }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int amount = Integer.parseInt(args[2]);
            long id = target.getIdLong();
            long taxUserID = 942818122681974804L;

            if (amount >= 50) {
                int taxAmount = (amount * 25) / 100;
                int userReceives = amount - taxAmount;
                int bonusToTaxCollector = (taxAmount * 5) / 100;
                int newBalance = JsonStorage.getBalance(id) + userReceives;
                JsonStorage.saveUser(id, newBalance, JsonStorage.getRank(id));
                JsonStorage.saveUser(taxUserID, JsonStorage.getBalance(taxUserID) + bonusToTaxCollector, JsonStorage.getRank(taxUserID));

                event.getChannel().sendMessage(
                    "Added " + userReceives + " to " + target.getEffectiveName() +
                    " (25% tax applied) | Balance: " + newBalance +
                    "\n💰 Added: +" + bonusToTaxCollector + " to the banker collector"
                ).queue();
                checkRankUp(target, event);
            } else {
                int newBalance = JsonStorage.getBalance(id) + amount;
                JsonStorage.saveUser(id, newBalance, JsonStorage.getRank(id));
                event.getChannel().sendMessage("Added " + amount + " to " + target.getEffectiveName() + " | Balance: " + newBalance).queue();
                checkRankUp(target, event);
            }
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Amount must be a number.").queue();
        }
    }

    private void handleAdd(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) { event.getChannel().sendMessage("You don't have permission.").queue(); return; }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) { 
            event.getChannel().sendMessage("Usage: !add @user amount").queue(); return; 
        }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int amount = Integer.parseInt(args[2]);
            long id = target.getIdLong();
            int newBalance = JsonStorage.getBalance(id) + amount;
            JsonStorage.saveUser(id, newBalance, JsonStorage.getRank(id));
            event.getChannel().sendMessage("Added " + amount + " to " + target.getEffectiveName() + " | Balance: " + newBalance).queue();
            checkRankUp(target, event);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Amount must be a number.").queue();
        }
    }

    private void handleRemove(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) { event.getChannel().sendMessage("You don't have permission.").queue(); return; }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) { 
            event.getChannel().sendMessage("Usage: !remove @user amount").queue(); return; 
        }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int amount = Integer.parseInt(args[2]);
            long id = target.getIdLong();
            int newBalance = JsonStorage.getBalance(id) - amount;
            JsonStorage.saveUser(id, newBalance, JsonStorage.getRank(id));
            event.getChannel().sendMessage("Removed " + amount + " from " + target.getEffectiveName() + " | Balance: " + newBalance).queue();
            checkRankDown(target, event);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Amount must be a number.").queue();
        }
    }

    private void handleGive(Member sender, String[] args, MessageReceivedEvent event) {
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) { 
            event.getChannel().sendMessage("Usage: !give @user amount").queue(); return; 
        }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int amount = Integer.parseInt(args[2]);
            long senderId = sender.getIdLong();
            long receiverId = target.getIdLong();
            int senderBalance = JsonStorage.getBalance(senderId);

            if (senderBalance < amount) { 
                event.getChannel().sendMessage("❌ Insufficient balance. You have " + senderBalance).queue(); 
                return; 
            }

            JsonStorage.saveUser(senderId, senderBalance - amount, JsonStorage.getRank(senderId));
            JsonStorage.saveUser(receiverId, JsonStorage.getBalance(receiverId) + amount, JsonStorage.getRank(receiverId));

            event.getChannel().sendMessage(sender.getEffectiveName() + " gave " + amount + " to " + target.getEffectiveName() +
                    "\nYour balance: " + JsonStorage.getBalance(senderId) +
                    "\nReceiver balance: " + JsonStorage.getBalance(receiverId)).queue();

            checkRankDown(sender, event);
            checkRankUp(target, event);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Amount must be a number.").queue();
        }
    }

    private void handleBuy(Member sender, String[] args, MessageReceivedEvent event) {
        if (args.length < 2) { event.getChannel().sendMessage("Usage: !buy itemName").queue(); return; }
        String itemName = args[1].toLowerCase();
        if (!SHOP_PRICES.containsKey(itemName)) { 
            event.getChannel().sendMessage("Item not found. Available: " + SHOP_PRICES.keySet()).queue(); 
            return; 
        }

        int price = SHOP_PRICES.get(itemName);
        String roleName = SHOP_ROLES.get(itemName);
        long userId = sender.getIdLong();
        int balance = JsonStorage.getBalance(userId);
        int userRank = JsonStorage.getRank(userId);
        int requiredRank = SHOP_RANKS.get(itemName);

        if (userRank < requiredRank) {
            event.getChannel().sendMessage("❌ Need rank " + toRoman(requiredRank) + ", you have " + toRoman(userRank)).queue();
            return;
        }
        if (balance < price) {
            event.getChannel().sendMessage("❌ Insufficient balance: " + balance + ", need " + price).queue();
            return;
        }

        int newBalance = balance - price;
        JsonStorage.saveUser(userId, newBalance, userRank);

        // Give 25% to owners
        long owner1 = 942818122681974804L;
        long owner2 = 1396926205881483354L;
        int ownerShare = price / 4;
        JsonStorage.saveUser(owner1, JsonStorage.getBalance(owner1) + ownerShare, JsonStorage.getRank(owner1));
        JsonStorage.saveUser(owner2, JsonStorage.getBalance(owner2) + ownerShare, JsonStorage.getRank(owner2));

        checkRankUp(sender, event);
        Member o1 = event.getGuild().getMemberById(owner1); if (o1 != null) checkRankUp(o1, event);
        Member o2 = event.getGuild().getMemberById(owner2); if (o2 != null) checkRankUp(o2, event);

        Role role = event.getGuild().getRolesByName(roleName, true).stream().findFirst().orElse(null);
        if (role != null) event.getGuild().addRoleToMember(sender, role).queue();

        event.getChannel().sendMessage(sender.getEffectiveName() + " bought " + itemName + "! New balance: " + newBalance).queue();
    }

    private void handleInfo(Member sender, String[] args, MessageReceivedEvent event) {
        Member target = event.getMessage().getMentions().getMembers().isEmpty() ? sender : event.getMessage().getMentions().getMembers().get(0);
        long id = target.getIdLong();
        int balance = JsonStorage.getBalance(id);
        int rank = JsonStorage.getRank(id);
        int needed = (rank + 1) * 1000;
        event.getChannel().sendMessage(target.getEffectiveName() + " | Balance: " + balance + " | Rank: " + toRoman(rank) + " | Next: " + needed).queue();
    }

    private void handleHelp(MessageReceivedEvent event) {
        String helpText = """
            🏦 Bank Commands
            !info @user - Show balance/rank
            !give @user amount - Transfer money
            !buy itemName - Purchase item
            !top - Top 10
            !add @user amount - Admin add money
            !addt @user amount - Admin add taxed
            !remove @user amount - Admin remove money
            !set @user rankNumber - Admin set rank
            !reset @user - Admin reset user
            !fix - Admin fix nicknames
            !addrole @user RoleName - Admin add role
            """;
        event.getChannel().sendMessage(helpText).queue();
    }

    private void handleSet(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) { event.getChannel().sendMessage("No permission").queue(); return; }
        if (args.length < 3 || event.getMessage().getMentions().getMembers().isEmpty()) { 
            event.getChannel().sendMessage("Usage: !set @user rank").queue(); return; 
        }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        try {
            int rank = Math.max(1, Integer.parseInt(args[2]));
            long id = target.getIdLong();
            int balance = JsonStorage.getBalance(id);
            int minBalance = (rank - 1) * 100;
            if (balance < minBalance) balance = minBalance;
            JsonStorage.saveUser(id, balance, rank);
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
                long id = m.getIdLong();
                String base = getBaseName(m);
                m.modifyNickname(base + " " + toRoman(JsonStorage.getRank(id))).queue();
            }
            event.getChannel().sendMessage("Nicknames fixed!").queue();
        });
    }

    private void handleReset(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) { event.getChannel().sendMessage("No permission").queue(); return; }
        if (event.getMessage().getMentions().getMembers().isEmpty()) { event.getChannel().sendMessage("Usage: !reset @user").queue(); return; }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        long id = target.getIdLong();
        JsonStorage.saveUser(id, 0, 1);
        if (!target.isOwner()) target.modifyNickname(getBaseName(target) + " I").queue();
        event.getChannel().sendMessage(target.getEffectiveName() + " reset to Rank I, Balance 0").queue();
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

    // ========== HELPERS ==========
    private boolean isAllowed(Member m) {
        return m != null && (m.isOwner() || m.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(ADMIN_ROLE_NAME)));
    }

    private String getBaseName(Member m) {
        String nick = m.getNickname() != null ? m.getNickname() : m.getEffectiveName();
        String[] parts = nick.split(" ");
        String last = parts;
        String last = parts[parts.length - 1];
        if (last.matches("(?i)M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})")) {
            return nick.substring(0, nick.lastIndexOf(" "));
        }
        return nick;
    }

    // ========== RANK MANAGEMENT ==========
    private void checkRankUp(Member member, MessageReceivedEvent event) {
        long id = member.getIdLong();
        int balance = JsonStorage.getBalance(id);
        int rank = JsonStorage.getRank(id);

        while (balance >= rank * 1000) {
            int requirement = rank * 1000;
            balance -= requirement;
            rank++;
            JsonStorage.saveUser(id, balance, rank);

            if (!member.isOwner()) {
                member.modifyNickname(getBaseName(member) + " " + toRoman(rank)).queue();
            }

            event.getChannel().sendMessage(member.getEffectiveName() + " ranked up to " + toRoman(rank) + "! 💰 Remaining balance: " + balance)
                    .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
        }
    }

    private void checkRankDown(Member member, MessageReceivedEvent event) {
        long id = member.getIdLong();
        int balance = JsonStorage.getBalance(id);
        int rank = JsonStorage.getRank(id);

        while (balance < 0 && rank > 1) {
            rank--;
            balance += rank * 1000; // restore balance for previous rank
            JsonStorage.saveUser(id, balance, rank);

            if (!member.isOwner()) {
                member.modifyNickname(getBaseName(member) + " " + toRoman(rank)).queue();
            }

            event.getChannel().sendMessage(member.getEffectiveName() + " ranked down to " + toRoman(rank))
                    .queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
        }
    }

    private String toRoman(int num) {
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] roman = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (num >= values[i]) {
                num -= values[i];
                sb.append(roman[i]);
            }
        }
        return sb.toString();
    }
}
