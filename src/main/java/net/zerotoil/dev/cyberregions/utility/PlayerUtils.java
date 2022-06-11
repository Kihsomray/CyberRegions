package net.zerotoil.dev.cyberregions.utility;

import net.zerotoil.dev.cyberregions.CyberRegions;
import net.zerotoil.dev.cyberregions.object.region.Region;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class PlayerUtils {

    /**
     * Convert an array string from config to a list of
     * existing regions.
     *
     * @param main Main instance
     * @param string Array string from config
     * @return List of regions from the array string
     */
    public static Map<String, Region> stringToRegionMap(CyberRegions main, String string) {
        Map<String, Region> list = new HashMap<>();

        // remove the surrounding brackets.
        string = string.substring(1, string.length() - 1);
        for(String s : string.split(", ")) {
            if (!main.cache().regions().isRegion(s)) continue;
            list.put(s, main.cache().regions().getRegion(s));
        }

        return list;

    }

    /**
     * Get the player's placeholders.
     *
     * @return Array of placeholders
     */
    public static String[] getPlPlaceholders() {
        return new String[]{
                "player",
                "playerDisplayName",
                "playerUUID",
                "currentRegion"
        };
    }

    /**
     * Get the player's placeholder replacements.
     *
     * @return Array of placeholder replacements
     */
    public static String[] getPlReplacements(Player player) {
        return new String[] {
                player.getName(),
                player.getDisplayName(),
                player.getUniqueId().toString(),
                getCurrentRegionString(player)
        };
    }

    /**
     * Returns a region in which the player is
     * located. Will return "None" if it is not
     * within a region.
     *
     * Warning: this should only be used if the
     * plugin is fully enabled.
     *
     * @return Region the location is in
     */
    private static String getCurrentRegionString(Player player) {
        CyberRegions main = (CyberRegions) JavaPlugin.getProvidingPlugin(CyberRegions.class);
        Region region = main.cache().regions().getRegionAt(player.getLocation());
        if (region == null) return main.core().getLangValue(player, "lang", "placeholders.null-region");
        return region.getDisplayName();
    }


}
