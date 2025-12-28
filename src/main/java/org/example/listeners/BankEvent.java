package org.example.listeners;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Role;
import org.example.storage.JsonStorage;

import java.util.concurrent.TimeUnit;

public class BankEvent extends ListenerAdapter {
    
    private static final String ADMIN_ROLE_NAME = "Banker";
    
    // ===== SHOP CONFIGURATION - EDIT HERE TO ADD ITEMS =====
    // Format: "itemName" -> price, role, rank
    // Add items with their costs, role name, and rank level
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

        SHOP_PRICES.put("lord",950);
        SHOP_ROLES.put("lord", "lord");
        SHOP_RANKS.put("lord", 8);
        SHOP_PRICES.put("lady",950);
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

            // ================= TOP =================
            if (cmd.equals("!top")) {
                event.getGuild().loadMembers().onSuccess(members -> {
                    try {
                        members.removeIf(m -> m.getUser().isBot());

                        members.sort((a, b) -> {
                            long aId = a.getIdLong();
                            long bId = b.getIdLong();
                            int aBal = JsonStorage.getBalance(aId);
                            int bBal = JsonStorage.getBalance(bId);
                            return Integer.compare(bBal, aBal);
                        });

                        StringBuilder sb = new StringBuilder("🏆 **Top 10 richess users** 🏆\n");

                        int limit = Math.min(10, members.size());
                        for (int i = 0; i < limit; i++) {
                            Member m = members.get(i);
                            long id = m.getIdLong();
                            sb.append(i + 1).append(". ")
                                    .append(m.getEffectiveName())
                                    .append(" — ").append(JsonStorage.getBalance(id))
                                    .append("\n");
                        }
                        event.getChannel().sendMessage(sb.toString()).queue();
                    } catch (Exception e) {
                        event.getChannel().sendMessage("Error loading leaderboard").queue();
                    }
                });
            }

            // ================= ADD MONEY WITH TAXES =================
            else if (cmd.equals("!addt")) {
                if (!isAllowed(sender)) {
                    event.getChannel().sendMessage("You don't have permission to use this command.").queue();
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
                        // Calculate tax: 25% of amount
                        int taxAmount = (amount * 25) / 100;
                        int userReceives = amount - taxAmount;
                        int bonusToTaxCollector = (taxAmount * 5) / 100;
                        
                        // Add money to target user
                        int newBalance = JsonStorage.getBalance(id) + userReceives;
                        int rank = JsonStorage.getRank(id);
                        JsonStorage.saveUser(id, newBalance, rank);
                        
                        // Add 5% of tax to tax collector
                        int taxUserBalance = JsonStorage.getBalance(taxUserID);
                        int taxUserNewBalance = taxUserBalance + bonusToTaxCollector;
                        int taxUserRank = JsonStorage.getRank(taxUserID);
                        JsonStorage.saveUser(taxUserID, taxUserNewBalance, taxUserRank);
                        
                        event.getChannel().sendMessage(
                                "Added " + userReceives + " to " + target.getEffectiveName() +
                                        " (25% tax applied) | Balance: " + newBalance +
                                        "\n💰 Added: +" + bonusToTaxCollector + " to the banker collector cuz of taxes"
                        ).queue();
                        
                        checkRankUp(target, event);
                    } else {
                        // No tax for amounts under 50
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
                // ================= ADD MONEY WITHOUT TAXES =================
                else if (cmd.equals("!add")) {
                    if (!isAllowed(sender)) {
                        event.getChannel().sendMessage("You don't have permission to use this command.").queue();
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
                        int rank = JsonStorage.getRank(id);

                        JsonStorage.saveUser(id, newBalance, rank);

                        event.getChannel().sendMessage(
                                "Added " + amount + " to " + target.getEffectiveName() +
                                        " | Balance: " + newBalance
                        ).queue();

                        checkRankUp(target, event);

                    } catch (NumberFormatException e) {
                        event.getChannel().sendMessage("Amount must be a number.").queue();
                    }
                }

            // ================= REMOVE MONEY =================
            else if (cmd.equals("!remove")) {
                if (!isAllowed(sender)) {
                    event.getChannel().sendMessage("You don't have permission to use this command.").queue();
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

                    int currentBalance = JsonStorage.getBalance(id);
                    int newBalance = currentBalance - amount;
                    int rank = JsonStorage.getRank(id);

                    JsonStorage.saveUser(id, newBalance, rank);

                    event.getChannel().sendMessage(
                            "Removed " + amount + " from " + target.getEffectiveName() +
                                    " | Balance: " + newBalance
                    ).queue();

                    checkRankDown(target, event);

                } catch (NumberFormatException e) {
                    event.getChannel().sendMessage("Amount must be a number.").queue();
                }
            }
            // ================= GIVE MONEY =================
            else if (cmd.equals("!give")) {
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

                    // Deduct from sender
                    int newSenderBalance = senderBalance - amount;
                    int senderRank = JsonStorage.getRank(senderId);
                    JsonStorage.saveUser(senderId, newSenderBalance, senderRank);

                    // Add to receiver
                    int receiverBalance = JsonStorage.getBalance(receiverId);
                    int newReceiverBalance = receiverBalance + amount;
                    int receiverRank = JsonStorage.getRank(receiverId);
                    JsonStorage.saveUser(receiverId, newReceiverBalance, receiverRank);

                    event.getChannel().sendMessage(
                            sender.getEffectiveName() + " gave " + amount + " to " + target.getEffectiveName() +
                                    "\n" + sender.getEffectiveName() + " balance: " + newSenderBalance +
                                    "\n" + target.getEffectiveName() + " balance: " + newReceiverBalance
                    ).queue();

                    checkRankDown(sender, event);
                    checkRankUp(target, event);

                } catch (NumberFormatException e) {
                    event.getChannel().sendMessage("Amount must be a number.").queue();
                }
            }    

            // ================= BUY ITEM =================
            else if (cmd.equals("!buy")) {
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
                        "** to buy this item.\n" +
                        "⭐ Your current rank: **" + toRoman(userRank) + "**"
                    ).queue();
                    return;
                }
                    
                else if (balance < price) {
                    event.getChannel().sendMessage("❌ Insufficient balance. You have " + balance + ", need " + price).queue();
                    return;
                }
                
                try {
                    // Deduct balance from buyer
                    int newBalance = balance - price;
                    Integer newRank = SHOP_RANKS.getOrDefault(itemName, null);
                    int finalRank = (newRank != null) ? newRank : JsonStorage.getRank(userId);
                    JsonStorage.saveUser(userId, newBalance, finalRank);
                    
                    // Give 50% to two specific owners
                    long ownerID1 = 942818122681974804L;
                    int ownerShare = price / 2;
                    int ownerBalance1 = JsonStorage.getBalance(ownerID1);
                    int ownerRank1 = JsonStorage.getRank(ownerID1);
                    JsonStorage.saveUser(ownerID1, ownerBalance1 + ownerShare, ownerRank1);

                    // Rank up owner1 if needed
                    Member owner1 = event.getGuild().getMemberById(ownerID1);
                    if (owner1 != null) checkRankUp(owner1, event);
                    
                    long ownerID2 = 1396926205881483354L;
                    int ownerBalance2 = JsonStorage.getBalance(ownerID2);
                    int ownerRank2 = JsonStorage.getRank(ownerID2);
                    JsonStorage.saveUser(ownerID2, ownerBalance2 + ownerShare, ownerRank2);
                    
                    // Rank up owner2 if needed
                    Member owner2 = event.getGuild().getMemberById(ownerID2);
                    try {
                        checkRankUp(owner2, event);
                    } catch (Exception ignored) {}

                    // Try to assign role
                    Role role = (roleName != null && !roleName.isEmpty())
                            ? (event.getGuild().getRolesByName(roleName, true).isEmpty() 
                                ? null 
                                : event.getGuild().getRolesByName(roleName, true).get(0))
                            : null;
                    
                    if (role != null) {
                        event.getGuild().addRoleToMember(sender, role).queue();
                    }
                    
                    String buyMessage = "✅ " + sender.getEffectiveName() + " bought **" + itemName + "**!";
                    if (role != null) {
                        buyMessage += "\n🎖️ Role assigned";
                    }
                    buyMessage += "\n💰 New balance: " + newBalance;
                    buyMessage += "\n🏦 Owners received: +" + ownerShare + " each";
                    
                    event.getChannel().sendMessage(buyMessage).queue();
                    
                } catch (Exception e) {
                    event.getChannel().sendMessage("Error processing purchase: " + e.getMessage()).queue();
                }
            }

            // ================= INFO =================
            else if (cmd.equals("!info")) {
                Member target = event.getMessage().getMentions().getMembers().isEmpty()
                        ? sender
                        : event.getMessage().getMentions().getMembers().get(0);

                if (target.getUser().isBot()) return;

                long id = target.getIdLong();
                int balance = JsonStorage.getBalance(id);
                int rank = JsonStorage.getRank(id);
                int neededToMaintain = rank * 1000;
                int neededForNext = (rank + 1) * 1000;
                int nextRank = rank+1;
                int nnRank = nextRank+1;

                event.getChannel().sendMessage(
                        "\n| User: " + target.getEffectiveName() + " |" +
                                "\n| Balance: " + balance +" |" +
                                "\n| Rank: " + toRoman(rank) + " |" +
                                "\n| Needed to maintain: " + neededToMaintain + " (Currently: " + balance + ") |" +
                                "\n| Needed for rank " + toRoman(nextRank) + " to rank " + toRoman(nnRank) + " :" + neededForNext + " money |"
                ).queue();
            }

            // ================= HELP =================
            else if (cmd.equals("!help")) {
                String helpText = "\uD83D\uDCDC Bank System Commands\n" +
                        "\uD83D\uDC64 Public Commands\n" +
                        "\n" +
                        "!info @user — Shows display name, rank, balance, and money needed for next rank.\n\n" +
                        "!give @user amount — Transfer money to another user.\n\n" +
                        "!buy itemName — Purchase an item from the shop.\n\n" +
                        "!top — Shows Top 10 richest users with display name, rank, and balance.\n" +
                        "\n" +
                        "\uD83D\uDD12 Owner/Special Role Commands\n" +
                        "\n" +
                        "!add @user amount — Adds money to a user and updates rank/nickname if needed.\n\n" +
                        "!addt @user amount — Adds money to a user after taxes and updates rank/nickname if needed.\n\n" +
                        "!remove @user amount — Removes money from a user and updates rank/nickname if needed.\n\n" +
                        "!set @user rankNumber — Sets a user's rank directly and updates their nickname.\n\n" +
                        "!reset @user — Resets a user's balance to 0 and rank to I, updates nickname.\n\n" +
                        "!fix — Fixes all nicknames to match current ranks, skips bots, includes owner.\n\n" +
                        "!addrole @user RoleName — Adds a role to a user (Owner or BankAdmin role)\n\n";

                event.getChannel().sendMessage(helpText).queue();
            }

            // ================= SET RANK =================
            else if (cmd.equals("!set")) {
                if (!isAllowed(sender)) {
                    event.getChannel().sendMessage("You don't have permission to use this command.").queue();
                    return;
                }

                if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 3) {
                    event.getChannel().sendMessage("Usage: !set @user rankNumber").queue();
                    return;
                }

                Member target = event.getMessage().getMentions().getMembers().get(0);
                if (target.getUser().isBot()) return;

                try {
                    int newRank = Integer.parseInt(args[2]);
                    if (newRank < 1) newRank = 1;
                    long id = target.getIdLong();

                    int currentBalance = JsonStorage.getBalance(id);
                    int requiredBalance = (newRank - 1) * 100;
                    if (currentBalance < requiredBalance) {
                        currentBalance = requiredBalance;
                    }

                    JsonStorage.saveUser(id, currentBalance, newRank);

                    String baseName = getBaseName(target);
                    target.modifyNickname(baseName + " " + toRoman(newRank)).queue(
                            success -> {},
                            failure -> {}
                    );

                    event.getChannel().sendMessage(
                            "Set " + target.getEffectiveName() + " to rank " + toRoman(newRank)
                    ).queue();

                } catch (NumberFormatException e) {
                    event.getChannel().sendMessage("Rank must be a number.").queue();
                }
            }

            // ================= FIX NAMES =================
            else if (cmd.equals("!fix")) {
                if (!isAllowed(sender)) {
                    event.getChannel().sendMessage("You don't have permission to use this command.").queue();
                    return;
                }

                event.getGuild().loadMembers().onSuccess(members -> {
                    try {
                        int fixed = 0;

                        for (Member m : members) {
                            if (m.getUser().isBot()) continue;
                            if (m.isOwner()) continue;

                            long id = m.getIdLong();
                            String baseName = getBaseName(target);
                            String expected = baseName + " " + toRoman(JsonStorage.getRank(id));

                            String current = m.getNickname();
                            if (current == null || !current.equals(expected)) {
                                m.modifyNickname(expected).queue();
                                fixed++;
                            }
                        }

                        event.getChannel().sendMessage("Fixed nicknames for " + fixed + " members.").queue();
                    } catch (Exception e) {
                        event.getChannel().sendMessage("Error fixing nicknames").queue();
                    }
                });
            }

            // ================= RESET USER =================
            else if (cmd.equals("!reset")) {
                if (!isAllowed(sender)) {
                    event.getChannel().sendMessage("You don't have permission to use this command.").queue();
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

                if (!target.isOwner()) {
                    target.modifyNickname(getBaseName(target) + " I").queue();
                }

                event.getChannel().sendMessage(
                        "Reset " + target.getEffectiveName() +
                                " | Rank: I | Balance: 0"
                ).queue();
            }

            // ================= ADD ROLE =================
            else if (cmd.equals("!addrole")) {
                if (!isAllowed(sender)) {
                    event.getChannel().sendMessage("You don't have permission to use this command.").queue();
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
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }
    // ================= GET BASE NAME =================
    private String getBaseName(Member member) {
        String nickname = member.getNickname(); // fetch nickname if exists
        if (nickname == null) nickname = member.getEffectiveName(); // fallback to username
        // Remove last word if it looks like a Roman numeral
        String[] parts = nickname.split(" ");
        if (parts.length > 1) {
            String last = parts[parts.length - 1];
            if (last.matches("(?i)M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})")) {
                // last word is Roman numeral, remove it
                return nickname.substring(0, nickname.lastIndexOf(" "));
            }
        }
        return nickname;
    }

    // ================= RANK UP =================
    private void checkRankUp(Member member, MessageReceivedEvent event) {
    long id = member.getIdLong();
    int balance = JsonStorage.getBalance(id);
    int rank = JsonStorage.getRank(id);

    while (balance >= rank * 1000) {
        int requirement = rank * 1000;
        balance -= requirement;  // Lose only the requirement amount
        rank++;
        JsonStorage.saveUser(id, balance, rank);

        // Only change nickname if not owner
        if (!member.isOwner()) {
            String baseName = getBaseName(member);
            member.modifyNickname(baseName + " " + toRoman(rank)).queue();
        }

        event.getChannel().sendMessage(
            member.getEffectiveName() + " ranked up to " + toRoman(rank) + "! 💰 Balance remaining: " + balance
        ).queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
    }


    // ================= RANK DOWN =================
    private void checkRankDown(Member member, MessageReceivedEvent event) {
        long id = member.getIdLong();
        int balance = JsonStorage.getBalance(id);
        int currentRank = JsonStorage.getRank(id);
        
        // If balance goes negative and rank > 1, demote and recalculate balance
        if (balance < 0 && currentRank > 1) {
            int newRank = currentRank - 1;
            int newBalance = (newRank * 1000) + balance; // balance is negative, so this subtracts
            JsonStorage.saveUser(id, newBalance, newRank);
            
            String baseName = getBaseName(member);
            member.modifyNickname(baseName + " " + toRoman(newRank)).queue();

            event.getChannel().sendMessage(
                    member.getEffectiveName() + " ranked down to " + toRoman(newRank)
            ).queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS, null, failure -> {}));
        }
    }

    // ================= PERMISSION CHECK =================
    private boolean isAllowed(Member member) {
        if (member == null) return false;
        if (member.isOwner()) return true;
        return member.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(ADMIN_ROLE_NAME));
    }

    // ================= INT → ROMAN =================
    private String toRoman(int num) {
        int[] values = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
        String[] roman = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
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
