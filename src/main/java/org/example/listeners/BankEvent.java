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
        SHOP_PRICES.put("knight", 900);
        SHOP_ROLES.put("knight", "knight");
        SHOP_RANKS.put("knight", 4);

        SHOP_PRICES.put("dame", 900);
        SHOP_ROLES.put("dame", "dame");
        SHOP_RANKS.put("dame", 4);

        SHOP_PRICES.put("lord", 950);
        SHOP_ROLES.put("lord", "lord");
        SHOP_RANKS.put("lord", 8);

        SHOP_PRICES.put("lady", 950);
        SHOP_ROLES.put("lady", "lady");
        SHOP_RANKS.put("lady", 8);

        SHOP_PRICES.put("duke", 1250);
        SHOP_ROLES.put("duke", "duke");
        SHOP_RANKS.put("duke", 9);

        SHOP_PRICES.put("duchess", 1250);
        SHOP_ROLES.put("duchess", "duchess");
        SHOP_RANKS.put("duchess", 9);

        SHOP_PRICES.put("prince", 12000);
        SHOP_ROLES.put("prince", "prince");
        SHOP_RANKS.put("prince", 13);

        SHOP_PRICES.put("princess", 12000);
        SHOP_ROLES.put("princess", "princess");
        SHOP_RANKS.put("princess", 13);

        SHOP_PRICES.put("archduke", 20000);
        SHOP_ROLES.put("archduke", "archduke");
        SHOP_RANKS.put("archduke", 22);
    }
    // ========================================================

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            if (!event.isFromGuild()) return;
            if (event.getAuthor().isBot()) return;

            String message = event.getMessage().getContentRaw();
            if (message.isEmpty()) return;

            String[] args = message.split("\\s+");
            if (args.length == 0) return;

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

        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    // ================= HANDLER METHODS =================

    private void handleTop(MessageReceivedEvent event) {
        event.getGuild().loadMembers().onSuccess(members -> {
            try {
                members.removeIf(m -> m.getUser().isBot());
                members.sort((a, b) -> Integer.compare(JsonStorage.getBalance(b.getIdLong()), JsonStorage.getBalance(a.getIdLong())));
                StringBuilder sb = new StringBuilder("🏆 **Top 10 richest users** 🏆\n");
                int limit = Math.min(10, members.size());
                for (int i = 0; i < limit; i++) {
                    Member m = members.get(i);
                    sb.append(i + 1).append(". ")
                            .append(m.getEffectiveName())
                            .append(" — ").append(JsonStorage.getBalance(m.getIdLong()))
                            .append("\n");
                }
                event.getChannel().sendMessage(sb.toString()).queue();
            } catch (Exception e) {
                event.getChannel().sendMessage("Error loading leaderboard").queue();
            }
        });
    }

    private void handleAddT(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("You don't have permission.").queue();
            return;
        }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) {
            event.getChannel().sendMessage("Usage: !add @user amount").queue();
            return;
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
                int rank = JsonStorage.getRank(id);
                JsonStorage.saveUser(id, newBalance, rank);

                int taxUserBalance = JsonStorage.getBalance(taxUserID);
                JsonStorage.saveUser(taxUserID, taxUserBalance + bonusToTaxCollector, JsonStorage.getRank(taxUserID));

                event.getChannel().sendMessage(
                        "Added " + userReceives + " to " + target.getEffectiveName() +
                                " (25% tax applied) | Balance: " + newBalance +
                                "\n💰 Added: +" + bonusToTaxCollector + " to the banker collector"
                ).queue();

                checkRankUp(target, event);
            } else {
                int newBalance = JsonStorage.getBalance(id) + amount;
                int rank = JsonStorage.getRank(id);
                JsonStorage.saveUser(id, newBalance, rank);
                event.getChannel().sendMessage(
                        "Added " + amount + " to " + target.getEffectiveName() +
                                " | Balance: " + newBalance
                ).queue();
                checkRankUp(target, event);
            }
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Amount must be a number.").queue();
        }
    }

    private void handleAdd(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("You don't have permission.").queue();
            return;
        }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) {
            event.getChannel().sendMessage("Usage: !add @user amount").queue();
            return;
        }
        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int amount = Integer.parseInt(args[2]);
            long id = target.getIdLong();
            int newBalance = JsonStorage.getBalance(id) + amount;
            JsonStorage.saveUser(id, newBalance, JsonStorage.getRank(id));
            event.getChannel().sendMessage(
                    "Added " + amount + " to " + target.getEffectiveName() +
                            " | Balance: " + newBalance
            ).queue();
            checkRankUp(target, event);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Amount must be a number.").queue();
        }
    }

    private void handleRemove(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("You don't have permission.").queue();
            return;
        }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) {
            event.getChannel().sendMessage("Usage: !remove @user amount").queue();
            return;
        }
        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int amount = Integer.parseInt(args[2]);
            long id = target.getIdLong();
            int newBalance = JsonStorage.getBalance(id) - amount;
            JsonStorage.saveUser(id, newBalance, JsonStorage.getRank(id));
            event.getChannel().sendMessage(
                    "Removed " + amount + " from " + target.getEffectiveName() +
                            " | Balance: " + newBalance
            ).queue();
            checkRankDown(target, event);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Amount must be a number.").queue();
        }
    }

    private void handleGive(Member sender, String[] args, MessageReceivedEvent event) {
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) {
            event.getChannel().sendMessage("Usage: !give @user amount").queue();
            return;
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

            event.getChannel().sendMessage(
                    sender.getEffectiveName() + " gave " + amount + " to " + target.getEffectiveName() +
                            "\n" + sender.getEffectiveName() + " balance: " + JsonStorage.getBalance(senderId) +
                            "\n" + target.getEffectiveName() + " balance: " + JsonStorage.getBalance(receiverId)
            ).queue();

            checkRankDown(sender, event);
            checkRankUp(target, event);

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Amount must be a number.").queue();
        }
    }

    private void handleBuy(Member sender, String[] args, MessageReceivedEvent event) {
        if (args.length < 2) {
            event.getChannel().sendMessage("Usage: !buy itemName").queue();
            return;
        }

        String itemName = args[1].toLowerCase();
        if (!SHOP_PRICES.containsKey(itemName)) {
            event.getChannel().sendMessage("❌ Item not found. Available items: " + SHOP_PRICES.keySet()).queue();
            return;
        }

        int price = SHOP_PRICES.get(itemName);
        String roleName = SHOP_ROLES.get(itemName);
        long userId = sender.getIdLong();
        int balance = JsonStorage.getBalance(userId);
        int userRank = JsonStorage.getRank(userId);
        int requiredRank = SHOP_RANKS.get(itemName);

        if (userRank < requiredRank) {
            event.getChannel().sendMessage(
                    "❌ You need rank **" + toRoman(requiredRank) +
                            "** to buy this item.\n⭐ Your current rank: **" + toRoman(userRank) + "**"
            ).queue();
            return;
        } else if (balance < price) {
            event.getChannel().sendMessage("❌ Insufficient balance. You have " + balance + ", need " + price).queue();
            return;
        }

        try {
            int newBalance = balance - price;
            JsonStorage.saveUser(userId, newBalance, SHOP_RANKS.get(itemName));
            
            // Give 25% each to two owners
            long ownerID1 = 942818122681974804L;
            long ownerID2 = 1396926205881483354L;
            int ownerShare = price / 4;
            JsonStorage.saveUser(ownerID1, JsonStorage.getBalance(ownerID1) + ownerShare, JsonStorage.getRank(ownerID1));
            JsonStorage.saveUser(ownerID2, JsonStorage.getBalance(ownerID2) + ownerShare, JsonStorage.getRank(ownerID2));

            checkRankUp(sender, event);
            Member owner1 = event.getGuild().getMemberById(ownerID1);
            if (owner1 != null) checkRankUp(owner1, event);
            Member owner2 = event.getGuild().getMemberById(ownerID2);
            if (owner2 != null) checkRankUp(owner2, event);

            Role role = (roleName != null && !roleName.isEmpty()) ?
                    event.getGuild().getRolesByName(roleName, true).stream().findFirst().orElse(null) : null;

            if (role != null) event.getGuild().addRoleToMember(sender, role).queue();

            event.getChannel().sendMessage("✅ " + sender.getEffectiveName() + " bought **" + itemName + "**!\n💰 New balance: " + newBalance + "\n🏦 Owners received: +" + ownerShare + " each").queue();

        } catch (Exception e) {
            event.getChannel().sendMessage("Error processing purchase: " + e.getMessage()).queue();
        }
    }

    private void handleInfo(Member sender, String[] args, MessageReceivedEvent event) {
        Member target = event.getMessage().getMentions().getMembers().isEmpty() ? sender : event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        long id = target.getIdLong();
        int balance = JsonStorage.getBalance(id);
        int rank = JsonStorage.getRank(id);
        int neededForNext = (rank + 1) * 1000;

        event.getChannel().sendMessage(
                "\n| User: " + target.getEffectiveName() + " |" +
                        "\n| Balance: " + balance + " |" +
                        "\n| Rank: " + toRoman(rank) + " |" +
                        "\n| Needed for next rank: " + neededForNext + " |"
        ).queue();
    }

    private void handleHelp(MessageReceivedEvent event) {
        String helpText = "\uD83D\uDCDC Bank System Commands\n" +
                "Public Commands:\n" +
                "!info @user — Shows display name, rank, balance.\n" +
                "!give @user amount — Transfer money.\n" +
                "!buy itemName — Purchase item.\n" +
                "!top — Top 10 richest users.\n" +
                "Owner Commands:\n" +
                "!add @user amount — Adds money.\n" +
                "!addt @user amount — Adds money after tax.\n" +
                "!remove @user amount — Removes money.\n" +
                "!set @user rankNumber — Set rank.\n" +
                "!reset @user — Reset balance and rank.\n" +
                "!fix — Fix nicknames.\n" +
                "!addrole @user RoleName — Add role.\n";
        event.getChannel().sendMessage(helpText).queue();
    }

    private void handleSet(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("You don't have permission.").queue();
            return;
        }
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) {
            event.getChannel().sendMessage("Usage: !set @user rankNumber").queue();
            return;
        }
        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        try {
            int newRank = Math.max(1, Integer.parseInt(args[2]));
            long id = target.getIdLong();
            int currentBalance = JsonStorage.getBalance(id);
            int requiredBalance = (newRank - 1) * 100;
            if (currentBalance < requiredBalance) currentBalance = requiredBalance;
            JsonStorage.saveUser(id, currentBalance, newRank);

            if (!target.isOwner()) {
                target.modifyNickname(getBaseName(target) + " " + toRoman(newRank)).queue();
            }

            event.getChannel().sendMessage("Set " + target.getEffectiveName() + " to rank " + toRoman(newRank)).queue();
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Rank must be a number.").queue();
        }
    }

    private void handleFix(Member sender, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("You don't have permission.").queue();
            return;
        }
        event.getGuild().loadMembers().onSuccess(members -> {
            int fixed = 0;
            for (Member m : members) {
                if (m.getUser().isBot() || m.isOwner()) continue;
                long id = m.getIdLong();
                String baseName = getBaseName(m);
                String expected = baseName + " " + toRoman(JsonStorage.getRank(id));
                if (m.getNickname() == null || !m.getNickname().equals(expected)) {
                    m.modifyNickname(expected).queue();
                    fixed++;
                }
            }
            event.getChannel().sendMessage("Fixed nicknames for " + fixed + " members.").queue();
        });
    }

    private void handleReset(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("You don't have permission.").queue();
            return;
        }
        if (event.getMessage().getMentions().getMembers().isEmpty()) {
            event.getChannel().sendMessage("Usage: !reset @user").queue();
            return;
        }
        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        long id = target.getIdLong();
        JsonStorage.saveUser(id, 0, 1);
        if (!target.isOwner()) target.modifyNickname(getBaseName(target) + " I").queue();
        event.getChannel().sendMessage("Reset " + target.getEffectiveName() + " | Rank: I | Balance: 0").queue();
    }

    private void handleAddRole(Member sender, String[] args, MessageReceivedEvent event) {
        if (!isAllowed(sender)) {
            event.getChannel().sendMessage("You don't have permission.").queue();
            return;
        }
        if (args.length < 2 || event.getMessage().getMentions().getMembers().isEmpty()) {
            event.getChannel().sendMessage("Usage: !addrole @user RoleName").queue();
            return;
        }
        Member target = event.getMessage().getMentions().getMembers().get(0);
        if (target.getUser().isBot()) return;

        String roleName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        Role role = event.getGuild().getRolesByName(roleName, true).stream().findFirst().orElse(null);

        if (role == null) {
            event.getChannel().sendMessage("Role not found: " + roleName).queue();
            return;
        }

        event.getGuild().addRoleToMember(target, role).queue(
                success -> event.getChannel().sendMessage("Added role `" + roleName + "` to " + target.getEffectiveName()).queue(),
                error -> event.getChannel().sendMessage("Failed to add role. Make sure I have permissions.").queue()
        );
    }

    // ================= HELPER METHODS =================

    private String getBaseName(Member member) {
        String nickname = member.getNickname();
        if (nickname == null) nickname = member.getEffectiveName();
        String[] parts = nickname.split(" ");
        if (parts.length > 1) {
            String last = parts[parts.length - 1];
            if (last.matches("(?i)M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})"))
                return nickname.substring(0, nickname.lastIndexOf(" "));
        }
        return nickname;
    }

    private void checkRankUp(Member member, MessageReceivedEvent event) {
        long id = member.getIdLong();
        int balance = JsonStorage.getBalance(id);
        int rank = JsonStorage.getRank(id);

        while (balance >= rank * 1000) {
            int requirement = rank * 1000;
            balance -= requirement;
            rank++;
            JsonStorage.saveUser(id, balance, rank);

            if (!member.isOwner()) member.modifyNickname(getBaseName(member) + " " + toRoman(rank)).queue();

            event.getChannel().sendMessage(
                    member.getEffectiveName() + " ranked up to " + toRoman(rank) +
                            "! 💰 Balance remaining: " + balance
            ).queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
        }
    }

    private void checkRankDown(Member member, MessageReceivedEvent event) {
        long id = member.getIdLong();
        int balance = JsonStorage.getBalance(id);
        int currentRank = JsonStorage.getRank(id);

        if (balance < 0 && currentRank > 1) {
            int newRank = currentRank - 1;
            int newBalance = (newRank * 1000) + balance;
            JsonStorage.saveUser(id, newBalance, newRank);

            if (!member.isOwner()) member.modifyNickname(getBaseName(member) + " " + toRoman(newRank)).queue();
        }
    }

    private boolean isAllowed(Member member) {
        if (member == null) return false;
        if (member.isOwner()) return true;
        return member.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(ADMIN_ROLE_NAME));
    }

    private String toRoman(int num) {
        int[] values = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
        String[] roman = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++)
            while (num >= values[i]) { num -= values[i]; sb.append(roman[i]); }
        return sb.toString();
    }
}
