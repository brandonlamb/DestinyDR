package net.dungeonrealms.game.guild.db;

import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.Setter;
import net.dungeonrealms.game.guild.GuildDatabaseAPI;
import net.dungeonrealms.game.mastery.Utils;
import net.dungeonrealms.game.mongo.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Class written by APOLLOSOFTWARE.IO on 6/2/2016
 */

@SuppressWarnings("unchecked")
public class GuildDatabase implements GuildDatabaseAPI {

    private static GuildDatabaseAPI instance = null;

    @Setter
    @Getter
    private static Logger logger = null;

    public volatile ConcurrentHashMap<String, Document> CACHED_GUILD = new ConcurrentHashMap<>();

    public static GuildDatabaseAPI getAPI() {
        if (instance == null) instance = new GuildDatabase();
        return instance;
    }

    public void updateCache(String guildName) {
        Document doc = Database.guilds.find(Filters.eq("info.name", guildName)).first();
        CACHED_GUILD.put(guildName, doc);
    }

    @Override
    public void removeFromCache(String guildName) {
        CACHED_GUILD.remove(guildName);
    }

    @Override
    public boolean isGuildCached(String guildName) {
        return CACHED_GUILD.containsKey(guildName);
    }

    public void createGuild(String guildName, String displayName, String tag, UUID owner, String banner, Consumer<Boolean> callback) {
        Document template = GuildDatabaseAPI.getDocumentTemplate(owner.toString(), guildName, displayName, tag, banner);

        Database.guilds.insertOne(template);
        CACHED_GUILD.put(guildName, template);

        Utils.log.warning("New guild created: " + guildName);

        if (callback != null)
            callback.accept(true);
        setGuild(owner, guildName);
    }


    public boolean doesGuildNameExist(String guildName, Consumer<Boolean> action) {
        boolean doesGuildNameExist = get(EnumGuildData.NAME, guildName) != null;

        if (action != null)
            action.accept(doesGuildNameExist);

        return doesGuildNameExist;
    }

    private Object get(String guildName, EnumGuildData data, Class<?> clazz) {
        Document doc;

        // GRABBED CACHED DATA
        if (CACHED_GUILD.containsKey(guildName)) doc = CACHED_GUILD.get(guildName);
        else doc = (Document) get(EnumGuildData.NAME, guildName);

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

    private List<UUID> getList(String guildName, EnumGuildData data) {
        List<String> users = (List<String>) get(guildName, data, ArrayList.class);
        List<UUID> usersUUIDs = new ArrayList<>();

        if (users != null) users.stream().forEach(u -> usersUUIDs
                .add(UUID.fromString(u)));

        return usersUUIDs;
    }

    private void update(String guildName, EnumGuildData data, EnumOperators EO, Object value) {

        // INSTANTLY UPDATES THE MONGODB SERVER //
        Database.guilds.updateOne(Filters.eq("info.name", guildName), new Document(EO.getUO(), new Document(data.getKey(), value)));
    }

    public boolean doesTagExist(String tag, Consumer<Boolean> action) {
        boolean doesTagExist = get(EnumGuildData.TAG, tag) != null;

        if (action != null)
            action.accept(doesTagExist);

        return doesTagExist;
    }

    public boolean isOwner(UUID uuid, String guildName) {
        String owner = (String) get(guildName, EnumGuildData.OWNER, String.class);
        return owner != null && owner.equals(uuid.toString());
    }

    public boolean isGuildNull(UUID uuid) {
        return getGuildOf(uuid) == null || getGuildOf(uuid).equals("");
    }

    public String getGuildOf(UUID uuid) {
        return (String) DatabaseAPI.getInstance().getData(EnumData.GUILD, uuid);
    }


    public void deleteGuild(String guildName) {
        Database.guilds.deleteOne(Filters.eq("info.name", guildName));
        removeFromCache(guildName);
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

    @Override
    public void addPlayer(String guildName, UUID uuid) {
        update(guildName, EnumGuildData.MEMBERS, EnumOperators.$PUSH, uuid.toString());
        setGuild(uuid, guildName);
    }

    private void modifyRank(String guildName, UUID uuid, boolean promote) {
        List<String> officers = (List<String>) get(guildName, EnumGuildData.OFFICERS, ArrayList.class);

        assert officers != null;
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

        updateCache(guildName);
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
        switch (get(uuid, guildName)) {
            case OFFICERS:
                update(guildName, EnumGuildData.OFFICERS, EnumOperators.$PULL, uuid.toString());

            case MEMBERS:
                update(guildName, EnumGuildData.OFFICERS, EnumOperators.$PULL, uuid.toString());
        }
        update(guildName, EnumGuildData.OWNER, EnumOperators.$SET, uuid.toString());
        updateCache(guildName);
    }


    public void setMotdOf(String guildName, String motd) {
        update(guildName, EnumGuildData.MOTD, EnumOperators.$SET, motd);
        updateCache(guildName);
    }

    public void removeFromGuild(String guildName, UUID uuid) {
        switch (get(uuid, guildName)) {

            case MEMBERS:
                update(guildName, EnumGuildData.MEMBERS, EnumOperators.$PULL, uuid.toString());
                break;

            case OFFICERS:
                update(guildName, EnumGuildData.OFFICERS, EnumOperators.$PULL, uuid.toString());
                break;

            case OWNER:
                update(guildName, EnumGuildData.OWNER, EnumOperators.$SET, "");
                break;

        }

        setGuild(uuid, "");
        updateCache(guildName);
    }


    public boolean isMember(UUID uuid, String guildName) {
        return getList(guildName, EnumGuildData.MEMBERS).contains(uuid);
    }


    public boolean isOfficer(UUID uuid, String guildName) {
        return getList(guildName, EnumGuildData.OFFICERS).contains(uuid);
    }

    public boolean isOwnerOfGuild(UUID player) {
        return get(EnumGuildData.OWNER, player) != null;
    }


    public String getTagOf(String guildName) {
        return (String) get(guildName, EnumGuildData.TAG, String.class);
    }

    @Override
    public String getDisplayNameOf(String guildName) {
        return (String) get(guildName, EnumGuildData.DISPLAY_NAME, String.class);
    }

    @Override
    public String getBannerOf(String guildName) {
        return (String) get(guildName, EnumGuildData.BANNER, String.class);
    }


    public List<UUID> getAllGuildMembers(String guildName) {
        return getList(guildName, EnumGuildData.MEMBERS);
    }

    @Override
    public List<UUID> getGuildOfficers(String guildName) {
        return getList(guildName, EnumGuildData.OFFICERS);
    }

    @Override
    public List<UUID> getAllOfGuild(String guildName) {
        String owner = getOwnerOf(guildName);

        List<UUID> all = owner != null && !owner.equals("") ? new ArrayList<>(Collections.singletonList(UUID.fromString(owner))) : new ArrayList<>();
        all.addAll(getAllGuildMembers(guildName));
        all.addAll(getGuildOfficers(guildName));

        return all;
    }


    public void setGuild(UUID uuid, String guildName) {
        DatabaseAPI.getInstance().update(uuid, EnumOperators.$SET, EnumData.GUILD, guildName, true);
    }

    public boolean areInSameGuild(UUID uuid1, UUID uuid2) {
        String p1Guild = (String) DatabaseAPI.getInstance().getData(EnumData.GUILD, uuid1);
        if (p1Guild.equals("")) return false;
        String p2Guild = (String) DatabaseAPI.getInstance().getData(EnumData.GUILD, uuid2);
        return !(p2Guild.equals("")) && p1Guild.equals(p2Guild);
    }


}