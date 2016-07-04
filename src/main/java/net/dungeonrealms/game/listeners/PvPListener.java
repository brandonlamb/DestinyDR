package net.dungeonrealms.game.listeners;

import net.dungeonrealms.API;
import net.dungeonrealms.game.handlers.EnergyHandler;
import net.dungeonrealms.game.handlers.HealthHandler;
import net.dungeonrealms.game.handlers.KarmaHandler;
import net.dungeonrealms.game.mongo.DatabaseAPI;
import net.dungeonrealms.game.mongo.EnumData;
import net.dungeonrealms.game.player.combat.CombatLog;
import net.dungeonrealms.game.world.items.Attribute;
import net.dungeonrealms.game.world.items.DamageAPI;
import net.dungeonrealms.game.world.items.Item;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Created by Kieran Quigley (Proxying) on 03-Jul-16.
 */
public class PvPListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void playerMeleePlayer(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!API.isPlayer(event.getDamager())) return;
        if (!API.isPlayer(event.getEntity())) return;

        Player damager = (Player) event.getDamager();
        Player receiver = (Player) event.getEntity();

        event.setDamage(0);

        double finalDamage;

        if (CombatLog.isInCombat(damager)) {
            CombatLog.updateCombat(damager);
        } else {
            CombatLog.addToCombat(damager);
        }

        EnergyHandler.removeEnergyFromPlayerAndUpdate(damager.getUniqueId(), EnergyHandler.getWeaponSwingEnergyCost(damager.getEquipment().getItemInMainHand()));

        if (!API.isWeapon(damager.getEquipment().getItemInMainHand())) {
            HealthHandler.getInstance().handlePlayerBeingDamaged(receiver, damager, 1, 0, 0);
            return;
        }

        Item.ItemType weaponType = new Attribute(damager.getInventory().getItemInMainHand()).getItemType();
        Item.ItemTier tier = new Attribute(damager.getInventory().getItemInMainHand()).getItemTier();

        switch (weaponType) {
            case BOW:
                switch (tier) {
                    case TIER_1:
                        DamageAPI.knockbackEntity(damager, receiver, 1.2);
                        break;
                    case TIER_2:
                        DamageAPI.knockbackEntity(damager, receiver, 1.5);
                        break;
                    case TIER_3:
                        DamageAPI.knockbackEntity(damager, receiver, 1.8);
                        break;
                    case TIER_4:
                        DamageAPI.knockbackEntity(damager, receiver, 2.0);
                        break;
                    case TIER_5:
                        DamageAPI.knockbackEntity(damager, receiver, 2.2);
                        break;
                    default:
                        break;
                }
            case STAFF:
                event.setDamage(0);
                event.setCancelled(true);
                damager.updateInventory();
                receiver.updateInventory();
                return;
            default:
                break;
        }

        finalDamage = DamageAPI.calculateWeaponDamage(damager, receiver);

        if (API.getGamePlayer(receiver) != null && API.getGamePlayer(damager) != null) {
            if (API.getGamePlayer(receiver).getPlayerAlignment() == KarmaHandler.EnumPlayerAlignments.LAWFUL) {
                if (API.getGamePlayer(damager).getPlayerAlignment() != KarmaHandler.EnumPlayerAlignments.CHAOTIC) {
                    if (Boolean.valueOf(DatabaseAPI.getInstance().getData(EnumData.TOGGLE_CHAOTIC_PREVENTION, damager.getUniqueId()).toString())) {
                        if (finalDamage >= HealthHandler.getInstance().getPlayerHPLive(receiver)) {
                            event.setCancelled(true);
                            event.setDamage(0);
                            damager.updateInventory();
                            receiver.updateInventory();
                            event.getDamager().sendMessage(ChatColor.YELLOW + "Your Chaotic Prevention Toggle has activated preventing the death of " + receiver.getName() + "!");
                            event.getEntity().sendMessage(ChatColor.YELLOW + damager.getName() + " has their Chaotic Prevention Toggle ON, your life has been spared!");
                            return;
                        }
                    }
                }
            }
        }

        double[] armorCalculation =DamageAPI.calculateArmorReduction(damager, receiver, finalDamage, null);
        finalDamage = finalDamage - armorCalculation[0];
        HealthHandler.getInstance().handlePlayerBeingDamaged(receiver, damager, finalDamage, armorCalculation[0], armorCalculation[1]);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void playerRangedPlayer(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!DamageAPI.isBowProjectile(event.getDamager()) && !DamageAPI.isStaffProjectile(event.getDamager())) return;
        if (!API.isPlayer(event.getEntity())) return;
        Projectile projectile = (Projectile) event.getDamager();
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }

        event.setDamage(0);

        Player damager = (Player) projectile.getShooter();
        Player receiver = (Player) event.getEntity();

        if (CombatLog.isInCombat(damager)) {
            CombatLog.updateCombat(damager);
        } else {
            CombatLog.addToCombat(damager);
        }

        double finalDamage = DamageAPI.calculateProjectileDamage(damager, receiver, projectile);

        if (API.getGamePlayer(receiver).getPlayerAlignment() == KarmaHandler.EnumPlayerAlignments.LAWFUL) {
            if (API.getGamePlayer(damager).getPlayerAlignment() != KarmaHandler.EnumPlayerAlignments.CHAOTIC) {
                if (Boolean.valueOf(DatabaseAPI.getInstance().getData(EnumData.TOGGLE_CHAOTIC_PREVENTION, damager.getUniqueId()).toString())) {
                    if (finalDamage >= HealthHandler.getInstance().getPlayerHPLive(receiver)) {
                        event.setCancelled(true);
                        event.setDamage(0);
                        damager.updateInventory();
                        receiver.updateInventory();
                        event.getDamager().sendMessage(ChatColor.YELLOW + "Your Chaotic Prevention Toggle has activated preventing the death of " + receiver.getName() + "!");
                        event.getEntity().sendMessage(ChatColor.YELLOW + damager.getName() + " has their Chaotic Prevention Toggle ON, your life has been spared!");
                        return;
                    }
                }
            }
        }

        double[] armorCalculation =DamageAPI.calculateArmorReduction(damager, receiver, finalDamage, null);
        finalDamage = finalDamage - armorCalculation[0];
        HealthHandler.getInstance().handlePlayerBeingDamaged(receiver, damager, finalDamage, armorCalculation[0], armorCalculation[1]);
        event.setDamage(0);
    }
}
