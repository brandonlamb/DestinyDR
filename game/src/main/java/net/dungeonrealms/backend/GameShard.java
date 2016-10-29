package net.dungeonrealms.backend;

import com.esotericsoftware.minlog.Log;
import lombok.Getter;
import net.dungeonrealms.backend.enumeration.EnumShardType;
import net.dungeonrealms.common.game.database.DatabaseAPI;
import net.dungeonrealms.common.game.database.DatabaseInstance;
import net.dungeonrealms.common.network.ShardInfo;
import net.dungeonrealms.common.network.bungeecord.BungeeUtils;
import net.dungeonrealms.network.GameClient;
import net.dungeonrealms.vgame.Game;
import org.bukkit.ChatColor;
import org.ini4j.Ini;

import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Giovanni on 29-10-2016.
 * <p>
 * This file is part of the Dungeon Realms project.
 * Copyright (c) 2016 Dungeon Realms;www.vawke.io / development@vawke.io
 */
public class GameShard
{
    @Getter
    private GameClient gameClient;

    @Getter
    private EnumShardType shardType = EnumShardType.BETA; // Default

    @Getter
    private String shardId = "Giovanni-01"; // Default

    @Getter
    private String bungeeIdentifier;

    @Getter
    private boolean instanceServer;

    @Getter
    private int realmsNumber;

    @Getter
    private int realmsPort;

    @Getter
    private int maxRealms;

    @Getter
    private int maxRealmPlayers;

    @Getter
    private ShardInfo shardInfo;

    @Getter
    private int rebootTime; // Not being randomized anymore

    public GameShard(FileReader fileReader)
    {
        try
        {
            this.loadShardData(fileReader);
            this.shardInfo = ShardInfo.getByShardID(shardId);
            this.setupDatabase();
            this.connect();
        } catch (Exception e)
        {
            e.printStackTrace();
            Game.getGame().getInstanceLogger().sendMessage(ChatColor.RED + "Failed to load the GameShard, shutting down.. (10)");
            for (int i = 0; i < 10; i++)
            {
                Game.getGame().getServer().shutdown();
            }
        }

        Game.getGame().getServer().setWhitelist(false);
    }

    public void manageSimpleStop()
    {
        // Just for instances/handlers that require a manual stop, no saving of data will take place here
        DatabaseAPI.getInstance().stopInvocation();
    }

    private void connect()
    {
        BungeeUtils.setPlugin(Game.getGame());
        this.gameClient = new GameClient();
        try
        {
            this.gameClient.connect();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private void setupDatabase()
    {
        DatabaseInstance.getInstance().startInitialization(true);
        DatabaseAPI.getInstance().startInitialization(bungeeIdentifier);
    }

    private void loadShardData(FileReader fileReader)
    {
        Ini ini = new Ini();
        try
        {
            ini.load(fileReader);

            this.instanceServer = ini.get("Backend", "instance", Boolean.class);
            this.shardId = ini.get("Backend", "shardid", String.class);
            this.bungeeIdentifier = ini.get("Backend", "bungeeId", String.class);

            this.realmsNumber = ini.get("RealmData", "number", Integer.class);
            this.realmsPort = ini.get("RealmData", "backendport", Integer.class);
            this.maxRealmPlayers = ini.get("RealmData", "maxplayers", Integer.class);
            this.maxRealms = ini.get("RealmData", "maxrealms", Integer.class);

            this.shardType = EnumShardType.valueOf(ini.get("Settings", "shard"));
            this.rebootTime = ini.get("Settings", "rebootTime", Integer.class);
        } catch (Exception e) // No multi exception catching here
        {
            e.printStackTrace();
        }
    }
}
