package net.dungeonrealms.spawning;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import net.dungeonrealms.DungeonRealms;
import net.dungeonrealms.mastery.NMSUtils;
import net.dungeonrealms.mastery.Utils;
import net.minecraft.server.v1_8_R3.EntityArmorStand;

/**
 * Created by Chase on Sep 28, 2015
 */
public class SpawningMechanics {
	private static ArrayList<MobSpawner> spawners = new ArrayList<>();

	public static void updateSpawners() {
		if (spawners.size() > 0)
			for (MobSpawner current : spawners) {
			if (current.playersAround()) {
				current.spawnIn();
			}
			}
	}

	public static void add(MobSpawner spawner) {
		spawners.add(spawner);
	}

	public static void loadSpawners() {
		Bukkit.broadcastMessage("spawners loading");
		List<Entity> list = Bukkit.getWorlds().get(0).getEntities();
		for (Entity aList : list) {
			if (aList instanceof EntityArmorStand || aList instanceof ArmorStand) {
			net.minecraft.server.v1_8_R3.Entity nms = NMSUtils.getNMSEntity(aList);
			if (nms.getNBTTag() != null && nms.getNBTTag().hasKey("type")) {
				if (nms.getNBTTag().getString("type").equalsIgnoreCase("spawner")) {
					spawners.add(new MobSpawner((EntityArmorStand) nms));
				}
			}
			} else {
			Utils.log.info(aList.getClass().getSimpleName());
			}
		}
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(DungeonRealms.getInstance(), () -> updateSpawners(),
			0, 6 * 20L);
	}
}
