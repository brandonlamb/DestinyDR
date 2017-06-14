package net.dungeonrealms.game.item.items.functional.accessories;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dungeonrealms.game.item.PersistentItem;
import net.dungeonrealms.game.world.item.Item;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@AllArgsConstructor
public enum Trinket {
//    MULTI_LINE(Item.ItemRarity.RARE, "Multirod", new AbstractTrinketData("Cast two lines at once"), 20),
    FISH_TREASURE_FIND(Item.ItemRarity.RARE, "Lucky", new EnchantTrinketData(Item.FishingAttributeType.TREASURE_FIND, 1, 1), 10),
    FISH_TRIPLE_FISH(Item.ItemRarity.UNCOMMON, "Bountiful", new EnchantTrinketData(Item.FishingAttributeType.TRIPLE_CATCH, 1, 3), 30),
    FISH_JUNK_FIND(Item.ItemRarity.UNCOMMON, "Spelunker", new EnchantTrinketData(Item.FishingAttributeType.JUNK_FIND, 4, 8), 30),
    FISH_DURABILITY(Item.ItemRarity.COMMON, "Sturdy", new EnchantTrinketData(Item.FishingAttributeType.DURABILITY, 10, 20), 30),
    FISH_DOUBLE_FISH(Item.ItemRarity.COMMON, "Ample", new EnchantTrinketData(Item.FishingAttributeType.DOUBLE_CATCH, 1, 5), 30),
    FISH_SCALER(Item.ItemRarity.COMMON, null, new AbstractTrinketData("Cleans fish of any effects"), 10),
    FISH_DAY_SUCCESS(Item.ItemRarity.UNCOMMON, null, "of the Sun", new AbstractTrinketData("Better Fishing during Daylight"), 20),


    NO_MINING_FATIGUE(Item.ItemRarity.RARE, "Haste", new AbstractTrinketData("No Mining Fatigue"), 10),
    MINE_TREASURE_FIND(Item.ItemRarity.UNCOMMON, "Lucky", new EnchantTrinketData(Item.PickaxeAttributeType.TREASURE_FIND, 1, 1), 20),
    MINE_GEM_FIND(Item.ItemRarity.UNCOMMON, "Opulent", new EnchantTrinketData(Item.PickaxeAttributeType.GEM_FIND, 3, 5), 20),
    MINE_TRIPLE_ORE(Item.ItemRarity.UNCOMMON, "Bountiful", new EnchantTrinketData(Item.PickaxeAttributeType.TRIPLE_ORE, 1, 3), 15),
    MINE_DURABILITY(Item.ItemRarity.COMMON, "Sturdy", new EnchantTrinketData(Item.PickaxeAttributeType.DURABILITY, 10, 20), 20),
    MIN_DOUBLE_ORE(Item.ItemRarity.COMMON, "Ample", new EnchantTrinketData(Item.PickaxeAttributeType.DOUBLE_ORE, 1, 5), 20),

    //Rifts
    RIFT_LAVA_TRAIL(Item.ItemRarity.RARE, "Cooling", new AbstractTrinketData("Lava trail is now obsidian trail"), 20),
    INCREASED_RIFT(Item.ItemRarity.RARE, "Lucky", new AbstractTrinketData("2-4 Rift Shards per Rift Walker"), 10),
    UPCOMING_RIFT(Item.ItemRarity.RARE, null, "of Clairvoyance", new AbstractTrinketData("Alerted of Upcoming Rift Locations"), 5),
    RIFT_DAMAGE_INCREASE(Item.ItemRarity.UNCOMMON, "Menacing", new AbstractTrinketData("Increased Damage to Rift Enemies"), 10);

//    COMBAT(Item.ItemRarity.COMMON, null, );

    @Getter
    private Item.ItemRarity itemRarity;

    private String prefix;

    private String suffix;

    private TrinketData data;

    private double chance;

    Trinket(Item.ItemRarity rarity, String prefix, double chance) {
        this(rarity, prefix, null, null, chance);
    }

    Trinket(Item.ItemRarity rarity, String prefix, TrinketData data, double chance) {
        this(rarity, prefix, null, data, chance);
    }


    public Integer getValue() {
        if (getData() instanceof EnchantTrinketData) {
            EnchantTrinketData data = (EnchantTrinketData) getData();
            int min = data.getMin(), max = data.getMax();

            int random = max - min;
            if (random <= 0) return min;
            return ThreadLocalRandom.current().nextInt(random) + min;
        }
        return null;
    }

    //Top left
    private static final int TRINKET_SLOT = 9;

    public static boolean hasActiveTrinket(Player player, Trinket trinket) {
        Trinket active = getActiveTrinket(player);
        return active != null && active == trinket;
    }

    /**
     * Get the active trinket the player has in the TRINKET_SLOT.
     * If the item found is not a valid instanceof of @{@link TrinketItem} then it will return null
     *
     * @param player
     * @return active trinket
     */
    public static Trinket getActiveTrinket(Player player) {
        TrinketItem item = getActiveTrinketItem(player);
        return item != null ? item.getTrinket() : null;
    }

    public static TrinketItem getActiveTrinketItem(Player player) {
        ItemStack item = player.getInventory().getItem(TRINKET_SLOT);
        if (item != null && item.getType() != Material.AIR) {
            //Possible trinket?
            PersistentItem found = PersistentItem.constructItem(item);
            if (found != null && found instanceof TrinketItem) {
                return (TrinketItem) found;
            }
        }
        return null;
    }

    public static int getTrinketValue(Player player, Trinket trinket) {
        TrinketItem item = getActiveTrinketItem(player);
        if (item != null && item.getTrinket().equals(trinket)) {
            return item.getValue() != null ? item.getValue() : 0;
        }

        return 0;
    }

    public static Trinket getFromName(String name) {
        return Arrays.stream(values()).filter(t -> t.name().equals(name)).findFirst().orElse(null);
    }
}