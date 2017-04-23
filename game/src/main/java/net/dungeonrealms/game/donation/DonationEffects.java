package net.dungeonrealms.game.donation;

import lombok.Getter;
import lombok.Setter;
import net.dungeonrealms.DungeonRealms;
import net.dungeonrealms.GameAPI;
import net.dungeonrealms.common.game.database.DatabaseAPI;
import net.dungeonrealms.common.game.database.data.EnumData;
import net.dungeonrealms.common.game.database.data.EnumOperators;
import net.dungeonrealms.game.mastery.GamePlayer;
import net.dungeonrealms.game.mastery.Utils;
import net.dungeonrealms.game.mechanic.ParticleAPI;
import net.dungeonrealms.game.mechanic.data.EnumBuff;
import net.dungeonrealms.game.mechanic.generic.EnumPriority;
import net.dungeonrealms.game.mechanic.generic.GenericMechanic;
import net.minecraft.server.v1_9_R2.Entity;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.mongodb.BasicDBList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Kieran on 10/1/2015.
 */
@Getter
@Setter
public class DonationEffects implements GenericMechanic {

	@Getter
    private static DonationEffects instance = new DonationEffects();

    //CLOSED BETA PAYERS = RED_DUST
    //HALLOWEEN PLAYERS = SMALL_SMOKE
    //CHRISTMAS PLAYERS = SNOW_SHOVEL

    public HashMap<Player, ParticleAPI.ParticleEffect> PLAYER_PARTICLE_EFFECTS = new HashMap<>();
    public HashMap<Entity, ParticleAPI.ParticleEffect> ENTITY_PARTICLE_EFFECTS = new HashMap<>();
    public ConcurrentHashMap<Location, Material> PLAYER_GOLD_BLOCK_TRAIL_INFO = new ConcurrentHashMap<>();
    public List<Player> PLAYER_GOLD_BLOCK_TRAILS = new ArrayList<>();

    private Map<EnumBuff, LinkedList<Buff>> buffMap = new HashMap<>();

    private static Random random = new Random();

    @Override
    public EnumPriority startPriority() {
        return EnumPriority.CATHOLICS;
    }

    @SuppressWarnings("unchecked")
	@Override
    public void startInitialization() {
        Bukkit.getScheduler().runTaskTimer(DungeonRealms.getInstance(), this::spawnPlayerParticleEffects, 40L, 2L);
        Bukkit.getScheduler().runTaskTimer(DungeonRealms.getInstance(), this::spawnEntityParticleEffects, 40L, 2L);
        Bukkit.getScheduler().runTaskTimer(DungeonRealms.getInstance(), this::removeGoldBlockTrails, 40L, 4L);
        
        // Load buffs from the DB.
        for (EnumBuff buffType : EnumBuff.values()) {
        	this.buffMap.put(buffType, new LinkedList<Buff>());
        	ArrayList<String> buffs = (ArrayList<String>)DatabaseAPI.getInstance().getShardData(DungeonRealms.getInstance().bungeeName, buffType.getDatabaseTag());
        	if (buffs == null)
        		continue;
        	Queue<Buff> queue = this.buffMap.get(buffType);
        	buffs.forEach(s -> {
        		Buff buff = Buff.deserialize(s);
        		buff.setType(buffType); //This isn't serialized because it doesn't need to be.
        		queue.add(buff);
        	});
        }
        
        handleExpiry();
    }
    
    private void handleExpiry() {
    	boolean changed = false;
    	for (EnumBuff buffType : EnumBuff.values()) {
    		if (!hasBuff(buffType))
    			continue;
    		
    		Buff buff = getBuff(buffType);
    		
    		//  Expired while offline D:
    		if (System.currentTimeMillis() > buff.getTimeUntilExpiry()) {
    			buff.deactivate();
    			changed = true;
    			continue;
    		}
    		
    		//Set this buff to expire.
    		Bukkit.getScheduler().runTaskLater(DungeonRealms.getInstance(), buff::deactivate,
                    (buff.getTimeUntilExpiry() - System.currentTimeMillis()) / 50);
    	}
    	
    	if (changed)
    		saveBuffData();
    }
    
    public void saveBuffData() {
    	for (EnumBuff buffType : EnumBuff.values()) {
    		//Remove existing buffs.
    		DatabaseAPI.getInstance().updateShardCollection(DungeonRealms.getInstance().bungeeName, EnumOperators.$SET,
					buffType.getDatabaseTag(), new BasicDBList(), true);
    		//Add queued buffs.
    		for (Buff buff : getQueuedBuffs(buffType))
    			DatabaseAPI.getInstance().updateShardCollection(DungeonRealms.getInstance().bungeeName, EnumOperators.$PUSH,
    					buffType.getDatabaseTag(), buff.serialize(), true);
    	}
    }

    @Override
    public void stopInvocation() {
        saveBuffData();
    }
    
    /**
     * Gets all the queued buffs.
     */
    public Queue<Buff> getQueuedBuffs(EnumBuff type) {
    	return buffMap.get(type);
    }
    
    /**
     * Gets the active buff of this type.
     */
    public Buff getBuff(EnumBuff buffType) {
    	return hasBuff(buffType) ? buffMap.get(buffType).getFirst() : null;
    }
    
    /**
     * Returns if there is at least one buff of this type queued / active.
     */
    public boolean hasBuff(EnumBuff buffType) {
    	return !buffMap.get(buffType).isEmpty();
    }

    public void spawnPlayerParticleEffects(Location location) {
        if (PLAYER_PARTICLE_EFFECTS.isEmpty()) return;
        for (Player player : PLAYER_PARTICLE_EFFECTS.keySet()) {
            if (!player.isOnline()) {
                PLAYER_PARTICLE_EFFECTS.remove(player);
                continue;
            }
            float moveSpeed = 0.02F;
            ParticleAPI.ParticleEffect particleEffect = PLAYER_PARTICLE_EFFECTS.get(player);
            if (particleEffect == ParticleAPI.ParticleEffect.RED_DUST || particleEffect == ParticleAPI.ParticleEffect.NOTE) {
                moveSpeed = -1F;
            }
            try {
                ParticleAPI.sendParticleToLocation(particleEffect, location.clone().add(0, 0.22, 0), (random.nextFloat()) - 0.4F, (random.nextFloat()) - 0.5F, (random.nextFloat()) - 0.5F, moveSpeed, 6);
            } catch (Exception e) {
                e.printStackTrace();
                Utils.log.warning("[Donations] Could not spawn donation particle " + particleEffect.name() + " for player " + player.getName());
            }
        }
    }

    public void doLogin(Player p) {
    	for (EnumBuff buffType : EnumBuff.values()) {
    		if (!hasBuff(buffType))
    			continue;
    		Buff buff = getBuff(buffType);
    		int minutesLeft = (int) (((buff.getTimeUntilExpiry() - System.currentTimeMillis()) / 1000.0D) / 60.0D);
    		
    		p.sendMessage("");
    		p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + ">> " + buff.getActivatingPlayer() + "'s " + ChatColor.GOLD.toString() + ChatColor.UNDERLINE + "+" + buff.getBonusAmount() + "% "
    				+ ChatColor.stripColor(buff.getType().getDescription()) + ChatColor.GOLD + " is active for " + ChatColor.UNDERLINE + minutesLeft + ChatColor.RESET + ChatColor.GOLD + " more minute(s)!");
    		p.sendMessage("");
    	}
    }
    
    public void activateLocalBuff(Buff buff) {
    	boolean existingBuff = hasBuff(buff.getType());
    	this.buffMap.get(buff.getType()).add(buff);
    	saveBuffData();
    	
    	if (existingBuff) {
    		Bukkit.broadcastMessage(ChatColor.GOLD + ">> Player " + buff.getActivatingPlayer() + ChatColor
                    .GOLD + " has queued a " + buff.getType().getItemName() + " set for activation after the current one expires.");
            Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_EGG_THROW, 1f, 1f));
    		return;
    	}
    	
    	buff.activate();
    }

    private void spawnPlayerParticleEffects() {
        if (PLAYER_PARTICLE_EFFECTS.isEmpty()) return;
        for (Player player : PLAYER_PARTICLE_EFFECTS.keySet()) {
            if (!player.isOnline()) {
                PLAYER_PARTICLE_EFFECTS.remove(player);
                continue;
            }
            float moveSpeed = 0.02F;
            ParticleAPI.ParticleEffect particleEffect = PLAYER_PARTICLE_EFFECTS.get(player);
            if (particleEffect == ParticleAPI.ParticleEffect.RED_DUST || particleEffect == ParticleAPI.ParticleEffect.NOTE) {
                moveSpeed = -1F;
            }
            try {
                ParticleAPI.sendParticleToLocation(particleEffect, player.getLocation().add(0, 0.22, 0), (random.nextFloat()) - 0.4F, (random.nextFloat()) - 0.5F, (random.nextFloat()) - 0.5F, moveSpeed, 6);
            } catch (Exception e) {
                e.printStackTrace();
                Utils.log.warning("[Donations] Could not spawn donation particle " + particleEffect.name() + " for player " + player.getName());
            }
        }
    }

    private void removeGoldBlockTrails() {
        if (PLAYER_GOLD_BLOCK_TRAIL_INFO.isEmpty()) return;
        for (Map.Entry<Location, Material> goldTrails : PLAYER_GOLD_BLOCK_TRAIL_INFO.entrySet()) {
            Location location = goldTrails.getKey();
            int timeRemaining = location.getBlock().getMetadata("time").get(0).asInt();
            timeRemaining--;
            if (timeRemaining <= 0) {
                Material material = goldTrails.getValue();
                location.getBlock().setType(material);
                PLAYER_GOLD_BLOCK_TRAIL_INFO.remove(location);
            } else {
                location.getBlock().setMetadata("time", new FixedMetadataValue(DungeonRealms.getInstance(), timeRemaining));
            }
        }
    }

    private void spawnEntityParticleEffects() {
        if (ENTITY_PARTICLE_EFFECTS.isEmpty()) return;
        for (Entity entity : ENTITY_PARTICLE_EFFECTS.keySet()) {
            if (!entity.isAlive()) {
                ENTITY_PARTICLE_EFFECTS.remove(entity);
                continue;
            }
            float moveSpeed = 0.02F;
            ParticleAPI.ParticleEffect particleEffect = ENTITY_PARTICLE_EFFECTS.get(entity);
            if (particleEffect == ParticleAPI.ParticleEffect.RED_DUST || particleEffect == ParticleAPI.ParticleEffect.NOTE) {
                moveSpeed = -1F;
            }
            Location location = new Location(Bukkit.getWorlds().get(0), entity.locX, entity.locY, entity.locZ);
            try {
                ParticleAPI.sendParticleToLocation(particleEffect, location.add(0, 0.22, 0), (random.nextFloat()) - 0.4F, (random.nextFloat()) - 0.5F, (random.nextFloat()) - 0.5F, moveSpeed, 6);
            } catch (Exception e) {
                e.printStackTrace();
                Utils.log.warning("[Donations] Could not spawn donation particle " + particleEffect.name() + " for entity " + entity.getName());
            }
        }
    }

    public boolean removeECashFromPlayer(Player player, int amount) {
        if (amount <= 0) {
            return true;
            //Someone done fucked up and made it remove a negative amount. Probably Chase.
        }
        int playerEcash = (int) DatabaseAPI.getInstance().getData(EnumData.ECASH, player.getUniqueId());
        if (playerEcash <= 0) {
            return false;
        }
        if (playerEcash - amount >= 0) {
            GamePlayer gamePlayer = GameAPI.getGamePlayer(player);
            if (gamePlayer == null) return false;
            gamePlayer.getPlayerStatistics().setEcashSpent(gamePlayer.getPlayerStatistics().getEcashSpent() + amount);
            DatabaseAPI.getInstance().update(player.getUniqueId(), EnumOperators.$INC, EnumData.ECASH, (amount * -1), true);
            return true;
        } else {
            return false;
        }
    }
}
