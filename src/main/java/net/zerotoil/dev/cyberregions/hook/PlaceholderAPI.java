package net.zerotoil.dev.cyberregions.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.zerotoil.dev.cyberregions.CyberRegions;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPI extends PlaceholderExpansion {

    private final CyberRegions main;

    public PlaceholderAPI(CyberRegions main) {
        this.main = main;
        register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return main.getDescription().getPrefix();
    }

    @Override
    public @NotNull String getAuthor() {
        return main.getAuthors();
    }

    @Override
    public @NotNull String getVersion() {
        return main.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        return null;
    }

}
