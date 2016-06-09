package net.dungeonrealms.game.guild.db;

import com.mongodb.client.model.Filters;
import net.dungeonrealms.game.guild.GuildDatabaseAPI;
import net.dungeonrealms.game.mastery.Utils;
import net.dungeonrealms.game.mongo.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.eq;

/**
 * Class written by APOLLOSOFTWARE.IO on 6/2/2016
 */

@SuppressWarnings("unchecked")
public class GuildDatabase implements GuildDatabaseAPI {

    private static GuildDatabaseAPI instance = null;


    public static GuildDatabaseAPI getAPI() {
        if (instance == null) instance = new GuildDatabase();
        return instance;
    }

    public void createGuild(String guildName, String displayName, String tag, UUID owner, Consumer<Boolean> callback) {
        Database.guilds.insertOne(GuildDatabaseAPI.getDocumentTemplate(owner.toString(), guildName, displayName, tag));

        Utils.log.warning("New guild created: " + guildName);
        callback.accept(true);
        setGuild(owner, guildName);
    }


    public boolean doesGuildNameExist(String guildName, Consumer<Boolean> action) {
        boolean doesGuildNameExist = get(EnumGuildData.NAME, guildName) != null;
        action.accept(doesGuildNameExist);

        return doesGuildNameExist;
    }

    private Object get(String guildName, EnumGuildData data, Class<?> clazz) {
        Document doc = (Document) get(EnumGuildData.NAME, guildName);
        if (doc == null) return null;

        return ((Document) doc.get("info")).get(data.getKey().substring(5), clazz);
    }


    private Object get(EnumGuildData data, Object value) {
        Bson query = Filters.eq(data.getKey(), value);
        return Database.guilds.find(query).first();
    }

    public EnumGuildData get(UUID uuid, String guildName) {
        if (isMember(uuid, guildName))
            return EnumGuildData.MEMBERS;

        if (isOfficer(uuid, guildName))
            return EnumGuildData.OFFICERS;

        if (isOwner(uuid, guildName))
            return EnumGuildData.OWNER;

        return null;
    }

    private List<UUID> get(String guildName, EnumGuildData data) {
        List<String> users = (List<String>) get(guildName, data, ArrayList.class);
        List<UUID> usersUUIDs = new ArrayList<>();

        if (users != null) users.stream().forEach(u -> usersUUIDs
                .add(UUID.fromString(u)));

        return usersUUIDs;
    }

    private void update(String guildName, EnumGuildData data, EnumOperators EO, Object value) {

        // INSTANTLY UPDATES THE MONGODB SERVER //
        Database.guilds.updateOne(eq("info.name", guildName), new Document(EO.getUO(), new Document(data.getKey(), value)));
    }

    public boolean doesTagExist(String tag, Consumer<Boolean> action) {
        boolean doesTagExist = get(EnumGuildData.TAG, tag) != null;
        action.accept(doesTagExist);

        return doesTagExist;
    }

    public boolean isOwner(UUID uuid, String guildName) {
        return get(guildName, EnumGuildData.OWNER, String.class) == uuid.toString();
    }

    public boolean isGuildNull(UUID uuid) {
        return getGuildOf(uuid) == null || (getGuildOf(uuid) != null && getGuildOf(uuid).equals(""));
    }

    public String getGuildOf(UUID uuid) {
        return (String) DatabaseAPI.getInstance().getData(EnumData.GUILD, uuid);
    }


    @Override
    public void deleteGuild(String guildName) {
        Database.guilds.deleteOne(eq("info.name", guildName));

        Utils.log.warning("Guild deleted: " + guildName);
    }


    public void promotePlayer(String guildName, UUID uuid) {
        if (getGuildOf(uuid) == null) return;

        modifyRank(guildName, uuid, true);
    }


    public void demotePlayer(String guildName, UUID uuid) {
        if (getGuildOf(uuid) == null) return;

        modifyRank(guildName, uuid, false);
    }


    private void modifyRank(String guildName, UUID uuid, boolean promote) {
        List<String> officers = (List<String>) get(guildName, EnumGuildData.OFFICERS, ArrayList.class);

        assert officers != null;
        if (!officers.contains(uuid.toString())) {
            if (promote) {
                //ADD TO OFFICERS
                update(guildName, EnumGuildData.OFFICERS, EnumOperators.$PUSH, uuid.toString());
                // REMOVE FROM MEMBERS
                update(guildName, EnumGuildData.MEMBERS, EnumOperators.$PULL, uuid.toString());
            } else {
                //REMOVE FROM OFFICERS
                update(guildName, EnumGuildData.OFFICERS, EnumOperators.$PULL, uuid.toString());
                // ADD TO MEMBERS
                update(guildName, EnumGuildData.MEMBERS, EnumOperators.$PUSH, uuid.toString());

            }
        }
    }


    public String getMotdOf(String guildName) {
        return (String) get(guildName, EnumGuildData.MOTD, String.class);
    }

    @Override
    public String getOwnerOf(String guildName) {
        return (String) get(guildName, EnumGuildData.OWNER, String.class);
    }

    @Override
    public void setOwner(String guildName, UUID uuid) {
        update(guildName, EnumGuildData.OWNER, EnumOperators.$SET, uuid.toString());
    }


    public void setMotdOf(String guildName, String motd) {
        update(guildName, EnumGuildData.MOTD, EnumOperators.$SET, motd);
    }

    public void removeFromGuild(String guildName, UUID uuid) {
        switch (get(uuid, guildName)) {

            case MEMBERS:
                update(guildName, EnumGuildData.MEMBERS, EnumOperators.$PULL, uuid.toString());

            case OFFICERS:
                update(guildName, EnumGuildData.OFFICERS, EnumOperators.$PULL, uuid.toString());

            case OWNER:
                update(guildName, EnumGuildData.OWNER, EnumOperators.$SET, "");
        }

        setGuild(uuid, "");
    }


    public boolean isMember(UUID uuid, String guildName) {
        return false;
    }


    public boolean isOfficer(UUID uuid, String guildName) {
        return false;
    }


    public boolean isOwnerOfGuild(UUID player) {
        return false;
    }


    public void sendAlert(String guildName, String message) {

    }

    public String getTagOf(String guildName) {
        return (String) get(guildName, EnumGuildData.TAG, String.class);
    }

    @Override
    public String getDisplayNameOf(String guildName) {
        return (String) get(guildName, EnumGuildData.DISPLAY_NAME, String.class);
    }


    public List<UUID> getAllGuildMembers(String guildName) {
        return get(guildName, EnumGuildData.MEMBERS);
    }

    @Override
    public List<UUID> getGuildOfficers(String guildName) {
        return get(guildName, EnumGuildData.OFFICERS);
    }

    @Override
    public List<UUID> getAllOfGuild(String guildName) {
        String owner = (String) get(guildName, EnumGuildData.OWNER, String.class);

        List<UUID> all = owner != null ? Collections.singletonList(UUID.fromString(owner)) : new ArrayList<>();
        all.addAll(getAllGuildMembers(guildName));
        all.addAll(getGuildOfficers(guildName));

        return all;
    }


    public void setGuild(UUID uuid, String guildName) {
        DatabaseAPI.getInstance().update(uuid, EnumOperators.$SET, EnumData.GUILD, guildName, true);
    }


}
