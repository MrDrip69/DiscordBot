package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.example.listeners.BankEvent;
import org.example.listeners.Event;
import org.example.storage.JsonStorage;
import javax.security.auth.login.LoginException;

public class Main {
    private final ShardManager shardManager;
    private final Dotenv config;

    public Main() throws LoginException {
        config = Dotenv.configure().ignoreIfMissing().load();
        String token = config.get("TOKEN");
        if (token == null) {
            token = System.getenv("DISCORD_BOT_TOKEN");
        }
        if (token == null) {
            throw new LoginException("Token not found in .env or DISCORD_BOT_TOKEN environment variable");
        }
        
        // Initialize JSON storage
        JsonStorage.initialize();
        
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.playing("GTA VI\nType !help for black bananas"));
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT);
        shardManager = builder.build();
        shardManager.addEventListener(new Event());
        shardManager.addEventListener(new BankEvent());
    }
    public Dotenv getConfig() {
        return config;
    }
    public ShardManager getShardManager() {
        return shardManager;
    }
    public static void main(String[] args) {
        try {
            new Main();
        } catch (LoginException e) {
            System.out.println("Token invalid");
        }
    }
}
