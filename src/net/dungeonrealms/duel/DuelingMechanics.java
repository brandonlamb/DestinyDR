package net.dungeonrealms.duel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import net.dungeonrealms.mongo.achievements.AchievementManager;
import net.dungeonrealms.mongo.achievements.Achievements;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.dungeonrealms.DungeonRealms;
import net.dungeonrealms.mastery.Utils;
import net.dungeonrealms.mongo.DatabaseAPI;
import net.dungeonrealms.mongo.EnumData;
import net.md_5.bungee.api.ChatColor;

/**
 * Created by Chase on Nov 13, 2015
 */

public class DuelingMechanics {

	public static ArrayList<DuelOffer> duels = new ArrayList<>();
	public static HashMap<UUID, UUID> pending = new HashMap<>();
	public static ArrayList<UUID> cooldown = new ArrayList<>();

	public static void startDuel(Player p1, Player p2) {
		duels.add(new DuelOffer(p1, p2));
	}

	/**
	 * 
	 * @param sender
	 * @param requested
	 */
	public static void sendDuelRequest(Player sender, Player requested) {
		if (isOnCooldown(sender.getUniqueId())) {
			sender.sendMessage(ChatColor.RED + "You're currently on cooldown for sending duel requests!");
			return;
		}
		if (isDueling(requested.getUniqueId())){
			sender.sendMessage(ChatColor.RED + "That player is already dueling!");
			return;
		}
		
		if (isPending(requested.getUniqueId()) && getPendingPartner(requested.getUniqueId()).toString().equalsIgnoreCase(sender.getUniqueId().toString())) {
			startDuel(sender, requested);
			return;
		}

		if (Boolean.valueOf((boolean) DatabaseAPI.getInstance().getData(EnumData.TOGGLE_DUEL, requested.getUniqueId()))) {
			pending.put(sender.getUniqueId(), requested.getUniqueId());
			cooldown.add(sender.getUniqueId());
			sender.sendMessage(ChatColor.GREEN + "Duel request sent!");
			if (requested.getUniqueId().toString().equals("f8740cbf-e6c7-43ef-830a-ac3923936b3c")) {
				Achievements.getInstance().giveAchievement(sender.getUniqueId(), Achievements.EnumAchievements.U_WOT_MATE);
			}
			requested.sendMessage(ChatColor.YELLOW + "Duel request received from " + sender.getName() + "");
			requested.sendMessage(ChatColor.YELLOW + "Shift Right click the player and choose duel to accept");
			Bukkit.getScheduler().scheduleAsyncDelayedTask(DungeonRealms.getInstance(), () -> {
				if (pending.containsKey(sender.getUniqueId()))
					pending.remove(sender.getUniqueId());
				cooldown.remove(sender.getUniqueId());
			} , 100l);// Remove Pending Request after 10 seconds.
		} else {
			sender.sendMessage(ChatColor.RED + "That player has duels toggled off!");
		}
	}

	/**
	 * @param uniqueId
	 * @return UUID
	 */
	public static UUID getPendingPartner(UUID uuid) {
		if (pending.containsKey(uuid)) {
			for (UUID id : pending.keySet()) {
				if (id.toString().equalsIgnoreCase(uuid.toString()))
					return pending.get(id);
			}
		}

		if (pending.containsValue(uuid)) {
			for (UUID id : pending.values()) {
				if (id.toString().equalsIgnoreCase(uuid.toString())) {
					for (UUID uniqueId : pending.keySet()) {
						if (uniqueId.toString().equalsIgnoreCase(id.toString()))
							return uniqueId;
					}
				}
			}
		}

		return null;
	}

	/**
	 * 
	 * @param uuid
	 * @return boolean
	 */
	public static boolean isOnCooldown(UUID uuid) {
		return cooldown.contains(uuid);
	}

	/**
	 * 
	 * @param uuid
	 * @return boolean
	 */
	public static boolean isPending(UUID uuid) {
		return pending.containsKey(uuid) || pending.containsValue(uuid);
	}

	/**
	 * @param uniqueId
	 * @return boolean
	 */
	public static boolean isDueling(UUID uuid) {
		for (DuelOffer offer : duels) {
			return uuid.toString().equalsIgnoreCase(offer.player1.toString())
			        || uuid.toString().equalsIgnoreCase(offer.player2.toString());
		}
		return false;
	}

	/**
	 * @return Duel offer
	 */
	public static DuelOffer getOffer(UUID id) {
		for (DuelOffer offer : duels) {
			if (offer.player1.toString().equalsIgnoreCase(id.toString())
			        || offer.player2.toString().equalsIgnoreCase(id.toString()))
				return offer;
		}
		return null;
	}

	/**
	 * @param offer
	 */
	public static void removeOffer(DuelOffer offer) {
		if(offer.timerID != -1)
				Bukkit.getScheduler().cancelTask(offer.timerID);
		duels.remove(offer);
	}

	/**
	 * @param uniqueId
	 * @param uniqueId2
	 * @return
	 */
	public static boolean isDuelPartner(UUID uniqueId, UUID uniqueId2) {
		for(DuelOffer offer : duels){
			boolean bool = offer.player1.toString().equalsIgnoreCase(uniqueId.toString()) || offer.player2.toString().equalsIgnoreCase(uniqueId.toString()) && offer.player1.toString().equalsIgnoreCase(uniqueId2.toString()) || offer.player2.toString().equalsIgnoreCase(uniqueId2.toString());
			return bool;
		}
		return false;
	}
}
