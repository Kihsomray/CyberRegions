package net.zerotoil.dev.cyberregions.listener;

import net.zerotoil.dev.cyberregions.CyberRegions;

public class Events {

    private final CyberRegions main;
    private final int counter = 0;

    // event classes here

    public Events(CyberRegions main) {
        this.main = main;
        load();
    }

    private void load() {

        main.logger("&cLoading events...");
        long startTime = System.currentTimeMillis();

        // events go here

        main.logger("&7Loaded &e" + counter + " &7events in &a" + (System.currentTimeMillis() - startTime) + "ms&7.", "");

    }

}
