package net.zerotoil.dev.cyberregions.cache;

import net.zerotoil.dev.cyberregions.CyberRegions;

public class Cache {

    final CyberRegions main;

    private Config config;
    private Regions regions;

    public Cache(CyberRegions main) {
        this.main = main;
    }

    /**
     * Reload all cache.
     */
    public void reload() {
        load(true);
    }

    public void load(boolean loadCore) {

        if (loadCore) main.reloadCore();

        config = new Config(main);
        regions = new Regions(this);
        regions.reloadRegions();

        if (loadCore) main.core().loadFinish();

    }

    /**
     * Contains all values of the config.yml
     * saved to cache.
     *
     * @return Config containing values
     */
    public Config config() {
        return config;
    }

    /**
     * Contains add cache involving regions.
     *
     * @return Region cache
     */
    public Regions regions() {
        return regions;
    }

}
