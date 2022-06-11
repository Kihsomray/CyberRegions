package net.zerotoil.dev.cyberregions.hook;

import net.zerotoil.dev.cyberregions.CyberRegions;
import org.bukkit.Bukkit;

public class Hooks {

    private final CyberRegions main;
    private int counter = 0;

    private PlaceholderAPI placeholderAPI;

    public Hooks(CyberRegions main) {
        this.main = main;
        reload();
    }

    public void reload() {

        main.logger("&cLoading addons...");
        long startTime = System.currentTimeMillis();

        if (addHook("PlaceholderAPI")) placeholderAPI = new PlaceholderAPI(main);

        main.logger("&7Loaded &e" + counter + " &7addons in &a" + (System.currentTimeMillis() - startTime) + "ms&7.", "");

    }

    private boolean isEnabled(String plugin) {
        return (Bukkit.getPluginManager().getPlugin(plugin) != null);
    }

    private boolean addHook(String plugin) {
        boolean bool = isEnabled(plugin);
        if (bool) counter++;
        return bool;
    }

    public PlaceholderAPI placeholderAPI() {
        return placeholderAPI;
    }

}
