package net.zerotoil.cybertravel;

import net.zerotoil.cybertravel.cache.FileCache;
import net.zerotoil.cybertravel.cache.PlayerCache;
import net.zerotoil.cybertravel.utilities.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.*;

import static java.lang.System.currentTimeMillis;

public class CTPCommand implements CommandExecutor {

    public CTPCommand(CyberTravel main) {
        main.getCommand("fastteleport").setExecutor(this);
        this.main = main;
    }

    private CyberTravel main;
    private HashMap<String, HashMap<String, Long>> regionCooldown = new HashMap<>();
    private HashMap<String, Long> globalCooldown = new HashMap<>();
    private Map<Player, BukkitTask> cmdCountdown = new HashMap<>();

    public HashMap<String, HashMap<String, Long>> getRegionCooldown() {
        return regionCooldown;
    }
    public HashMap<String, Long> getGlobalCooldown() {
        return globalCooldown;
    }
    public Map<Player, BukkitTask> getCmdCountdown() {
        return cmdCountdown;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(!(sender instanceof Player)) return true;

        // basic info
        String fl = "regions.";
        String pl = "players.";
        Player player = (Player) sender;
        Configuration dataConfig = main.getFileCache().getStoredFiles().get("data").getConfig();

        // 0 arguments
        if (args.length ==  0) {
            sendHelpMessage(sender);
            return true;
        }

        // 1 argument
        if (args.length == 1) {

            // /ftp help
            if (args[0].equalsIgnoreCase("help")) {
                sendHelpMessage(sender);
                return true;
            }

            // /ftp reload
            if (args[0].equalsIgnoreCase("reload")) {

                if (!sender.hasPermission("CyberTravel.admin")) {
                    main.getMessageUtils().noPermission(player);
                    return true;
                }

                main.getMessageUtils().sendMessage("lang", "messages.reloading", "&7Reloading...", sender);
                main.getFileCache().initializeFiles();
                main.getPlayerCache().refreshRegionData(false);
                main.getPlayerCache().initializePlayerData();
                main.getMessageUtils().sendMessage("lang", "messages.reloaded", "&aReloaded!", sender);
                return true;
            }

            if (args[0].equalsIgnoreCase("regions")) {

                if (!sender.hasPermission("CyberTravel.player")) {
                    main.getMessageUtils().noPermission(player);
                    return true;
                }

                if (!main.getPlayerCache().getRegions().isEmpty()) {
                    main.getMessageUtils().sendMessage("lang", "messages.unlocked-regions-header",
                            "&8&m―――――&8<&b&l Unlocked &f&lRegions: &8>&8&m―――――", player, "none", "none", false);
                    for (String i : main.getPlayerCache().getRegions().keySet()) {

                        if (main.getPlayerCache().getPlayerRegions().containsKey(player.getUniqueId().toString())) {
                            if (main.getPlayerCache().getPlayerRegions().get(player.getUniqueId().toString()).contains(i)) {
                                main.getMessageUtils().sendMessage("lang", "messages.unlocked-region", "&8➼ &7 Region &f\"" + i + "\" &a&o(unlocked)", player, "region", i, false);
                            } else {
                                main.getMessageUtils().sendMessage("lang", "messages.locked-region", "&8➼ &7 Region &f\"" + i + "\" &c&o(locked)", player, "region", i, false);
                            }
                        } else {
                            main.getMessageUtils().sendMessage("lang", "messages.locked-region", "&8➼ &7 Region &f\"" + i + "\" &c&o(locked)", player, "region", i, false);
                        }

                    }
                    main.getMessageUtils().sendMessage("lang", "messages.unlocked-regions-footer",
                            "&8&m――――――――――――――――――――――――――――――――――", player, "none", "none", false);

                    return true;
                }
            }

            // send help message
            sendHelpMessage(sender);
            return true;
        }

        // 2 arguments
        if (args.length == 2) {

            // /ftp create <region>
            if (args[0].equalsIgnoreCase("create")) {

                if (!sender.hasPermission("CyberTravel.admin")) {
                    main.getMessageUtils().noPermission(player);
                    return true;
                }

                // if region exists
                if (dataConfig.isConfigurationSection(fl + args[1])) {
                    main.getMessageUtils().sendMessage("lang", "messages.region-exists", "&cThe name " + args[1] +
                            " already exists. Please use another name!", sender, "region", args[1]);
                } else {
                    try {

                        // creates region and stores name
                        dataConfig.set(fl + args[1] + ".world", player.getWorld().getName());
                        main.getFileCache().getStoredFiles().get("data").saveConfig();
                        main.getMessageUtils().sendMessage("lang", "messages.region-created", "&aRegion " + args[1] +
                                " has successfully been created!", sender, "region", args[1]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }

            // /ftp [pos1|pos2|settp] <region>
            if (args[0].equalsIgnoreCase("pos1") || args[0].equalsIgnoreCase("pos2") || args[0].equalsIgnoreCase("settp")) {

                if (!sender.hasPermission("CyberTravel.admin")) {
                    main.getMessageUtils().noPermission(player);
                    return true;
                }

                // region does not exist
                if (!dataConfig.isConfigurationSection(fl + args[1])) {
                    main.getMessageUtils().sendMessage("lang", "messages.unknown-region", "&c" + args[1] +
                            " is not a region!", sender, "region", args[1]);
                    return true;
                }

                // is there no world registered?
                if (!dataConfig.isSet(fl + args[1] + ".world")) {
                    dataConfig.set(fl + args[1] + ".world", player.getWorld().toString());

                // is the player in a different world?
                } else if (!(player.getWorld().getName().equalsIgnoreCase(dataConfig.getString(fl + args[1] + ".world")))) {
                    main.getMessageUtils().sendMessage("lang", "messages.incorrect-world", "&cThis region is registered in the world \"" +
                            dataConfig.getString(fl + args[1] + ".world") + "!", sender, "world", dataConfig.getString(fl + args[1] + ".world"));
                    return true;
                }

                // gets player location and saves it
                String location = (0.5 + player.getLocation().getBlockX()) + ", " + player.getLocation().getBlockY() + ", " + (0.5 + player.getLocation().getBlockZ());
                dataConfig.set(fl + args[1] + "." + args[0], location);

                // save changes
                try {
                    main.getFileCache().getStoredFiles().get("data").saveConfig();
                    sender.sendMessage(main.getMessageUtils().getColor("&aLocation set to (" + location + "&a) in world \"" + dataConfig.get(fl + args[1] + ".world") + ".\"", true));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // adds it to memory
                if (dataConfig.isSet(fl + args[1] + ".pos1") && dataConfig.isSet(fl + args[1] + ".pos2") && dataConfig.isSet(fl + args[1] + ".settp")) {
                    main.getPlayerCache().refreshRegionData(true);
                    main.getMessageUtils().sendMessage("lang", "messages.auto-ready", "&aLooks like you have finished setting up region " + args[1] +
                            ". This region is now active!", sender, "region", args[1]);
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("teleport")) {

                if (!sender.hasPermission("CyberTravel.player")) {
                    main.getMessageUtils().noPermission(player);
                    return true;
                }

                // invalid location
                if (!dataConfig.isConfigurationSection(fl + args[1])) {
                    main.getMessageUtils().sendMessage("lang", "messages.invalid-location", "&cThat is not a valid location!", sender);
                    return true;
                }

                // no checkpoints saved
                if (!dataConfig.isSet(pl + player.getUniqueId().toString())) {
                    main.getMessageUtils().sendMessage("lang", "messages.no-checkpoints", "&cYou do not have any checkpoints saved!", sender);
                    return true;
                }

                // has the player discovered this region yet?
                if (main.getPlayerCache().getPlayerRegions().containsKey(player.getUniqueId().toString())) {
                    if (!main.getPlayerCache().getPlayerRegions().get(player.getUniqueId().toString()).contains(args[1])) {
                        main.getMessageUtils().sendMessage("lang", "messages.not-discovered", "&cYou still haven't been to this region!", sender);
                        return true;
                    }
                }

                boolean cooldownEnabled = main.getFileCache().getStoredFiles().get("config").getConfig().getBoolean("config.region-cooldown.enabled");
                boolean globalCooldownEnabled = main.getFileCache().getStoredFiles().get("config").getConfig().getBoolean("config.global-cooldown.enabled");

                if (globalCooldownEnabled) {
                    if (globalCooldown.containsKey(player.getUniqueId().toString())) {

                        int gcdTime = main.getFileCache().getStoredFiles().get("config").getConfig().getInt("config.global-cooldown.seconds");

                        if (globalCooldown.containsKey(player.getUniqueId().toString())) {
                            long globalTimeRemaining = ((globalCooldown.get(player.getUniqueId().toString()) / 1000) + gcdTime) - (currentTimeMillis() / 1000);
                            if (globalTimeRemaining > 0) {

                                // player still on a cooldown
                                main.getMessageUtils().sendMessage("lang", "messages.global-cooldown", "&cYou cant teleport to that region for another " +
                                        globalTimeRemaining + " seconds!", sender, "time", globalTimeRemaining + "");
                                return true;
                            }
                        }
                    }
                }


                for (String i : dataConfig.getStringList(pl + player.getUniqueId().toString())) {
                    if (i.equalsIgnoreCase(args[1])) {



                        // cool-down module
                        if (cooldownEnabled) {

                            int cdTime = main.getFileCache().getStoredFiles().get("config").getConfig().getInt("config.region-cooldown.seconds");

                            if (regionCooldown.containsKey(player.getUniqueId().toString())) {
                                if (regionCooldown.get(player.getUniqueId().toString()).containsKey(args[1])) {
                                    long timeRemaining = ((regionCooldown.get(player.getUniqueId().toString()).get(args[1]) / 1000) + cdTime) - (currentTimeMillis() / 1000);
                                    if (timeRemaining > 0) {

                                        // player still on a cooldown
                                        main.getMessageUtils().sendMessage("lang", "messages.region-cooldown", "&cYou cannot teleport to any region for another " +
                                                timeRemaining + " seconds!", sender, "time", timeRemaining + "");
                                        return true;
                                    }
                                }
                            }
                            // creates player hashmap if not present
                            if (!regionCooldown.containsKey(player.getUniqueId().toString())) regionCooldown.put(player.getUniqueId().toString(), new HashMap<>());

                            // deletes prior time for this region
                            if (regionCooldown.get(player.getUniqueId().toString()).containsKey(args[1])) regionCooldown.get(player.getUniqueId().toString()).remove(args[1]);

                        }

                        //count-down module
                        String[] teleportLocation = dataConfig.getString(fl + args[1] + ".settp").split(", ");;

                        if (main.getFileCache().getStoredFiles().get("config").getConfig().getBoolean("config.countdown.enabled")) {
                            int cndTime = main.getFileCache().getStoredFiles().get("config").getConfig().getInt("config.countdown.seconds");
                            if (!this.cmdCountdown.containsKey(player)) {
                                main.getMessageUtils().sendMessage("lang", "messages.countdown", "&7Teleporting in " +
                                        cndTime + " seconds...", sender, "time", cndTime + "");
                                this.cmdCountdown.put(player, (new BukkitRunnable() {

                                    @Override
                                    public void run() {
                                        teleportPlayer(player, args[1], teleportLocation);
                                        cmdCountdown.remove(player);
                                        if (cooldownEnabled) regionCooldown.get(player.getUniqueId().toString()).put(args[1], System.currentTimeMillis());
                                        if (globalCooldownEnabled) globalCooldown.put(player.getUniqueId().toString(), System.currentTimeMillis());
                                    }

                                }).runTaskLater(main, 20L * cndTime));
                            } else {
                                main.getMessageUtils().sendMessage("lang", "messages.already-teleporting", "&cYou are already being sent to " + args[1] +
                                        "! Please wait before you can teleport elsewhere.", sender, "region", args[1]);
                            }
                        } else {

                            teleportPlayer(player, args[1], teleportLocation);
                            if (cooldownEnabled) regionCooldown.get(player.getUniqueId().toString()).put(args[1], System.currentTimeMillis());
                            if (globalCooldownEnabled) globalCooldown.put(player.getUniqueId().toString(), System.currentTimeMillis());
                            // teleport to this location
                        }
                        return true;
                    }
                }
            }

            if (args[0].equalsIgnoreCase("delete")) {

                if (!sender.hasPermission("CyberTravel.admin")) {
                    main.getMessageUtils().noPermission(player);
                    return true;
                }

                // region does not exist
                if (!dataConfig.isConfigurationSection(fl + args[1])) {
                    main.getMessageUtils().sendMessage("lang", "messages.unknown-region", "&c" + args[1] +
                            " is not a region!", sender, "region", args[1]);
                    return true;
                }

                //
                try {
                    dataConfig.set(fl + args[1], null);
                    main.getPlayerCache().refreshRegionData(false);
                    for (String i : dataConfig.getConfigurationSection("players").getKeys(false)) {
                        List playerRegions = dataConfig.getList(pl + i);
                        if (playerRegions.contains(args[1])) {
                            playerRegions.remove(args[1]);
                            dataConfig.set(pl + i, playerRegions);
                            main.getPlayerCache().initializePlayerData();
                            main.getFileCache().getStoredFiles().get("data").saveConfig();
                        }
                    }
                    main.getMessageUtils().sendMessage("lang", "messages.delete-region", "&aYou have successfully deleted " + args[1] +
                            "!", sender, "region", args[1]);
                } catch (Exception e) {
                    main.getMessageUtils().sendMessage("lang", "messages.delete-failed", "&cThe region " + args[1] +
                            " cannot be deleted!", sender, "region", args[1]);
                }
                return true;

            }


            sendHelpMessage(sender);
            return true;
        }

        // 3 arguments
        if (args.length == 3) {


            // /ftp addregion <player> <region>
            if (args[0].equalsIgnoreCase("addregion")) {


                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.getName().equalsIgnoreCase(args[1])) {

                        // invalid location
                        if (!dataConfig.isConfigurationSection(fl + args[2])) {
                            main.getMessageUtils().sendMessage("lang", "messages.invalid-location", "&cThat is not a valid location!", sender);
                            return true;
                        }

                        // has the player discovered this region yet?
                        if (main.getPlayerCache().getPlayerRegions().containsKey(onlinePlayer.getUniqueId().toString())) {
                            if (main.getPlayerCache().getPlayerRegions().get(onlinePlayer.getUniqueId().toString()).contains(args[2])) {
                                main.getMessageUtils().sendMessage("lang", "messages.already-discovered", "&cThe player " +
                                        onlinePlayer.getName() + " has already found this region!", sender, "player", onlinePlayer.getName());
                                return true;

                            } else {

                                main.getPlayerCache().getPlayerRegions().get(onlinePlayer.getUniqueId().toString()).add(args[2]);
                                List playerRegions = dataConfig.getList(pl + onlinePlayer.getUniqueId().toString());
                                if (!playerRegions.contains(args[2])) {
                                    playerRegions.add(args[2]);
                                    dataConfig.set(pl + onlinePlayer.getUniqueId().toString(), playerRegions);
                                    try {
                                        main.getFileCache().getStoredFiles().get("data").saveConfig();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                main.getMessageUtils().sendMessage("lang", "messages.added-player-region", "&aSuccessfully added " +
                                        args[2] + " to that player's discovered regions.", sender, "region", args[2]);
                                return true;

                            }

                        }

                    }

                }

                main.getMessageUtils().sendMessage("lang", "messages.not-online", "&cThe player " +
                        args[1] + "is not currently online!", player, "player", args[1]);
                return true;


            }



            // /ftp delregion <player> <region>
            if (args[0].equalsIgnoreCase("delregion")) {


                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.getName().equalsIgnoreCase(args[1])) {

                        // invalid location
                        if (!dataConfig.isConfigurationSection(fl + args[2])) {
                            main.getMessageUtils().sendMessage("lang", "messages.invalid-location", "&cThat is not a valid location!", sender);
                            return true;
                        }

                        // no checkpoints saved
                        if (!dataConfig.isSet(pl + onlinePlayer.getUniqueId().toString())) {
                            main.getMessageUtils().sendMessage("lang", "messages.has-no-checkpoints", "&cThe player " +
                                    onlinePlayer.getName() + " has no checkpoints saved!", sender, "player", onlinePlayer.getName());
                            return true;
                        }

                        // has the player discovered this region yet?
                        if (main.getPlayerCache().getPlayerRegions().containsKey(onlinePlayer.getUniqueId().toString())) {
                            if (!main.getPlayerCache().getPlayerRegions().get(onlinePlayer.getUniqueId().toString()).contains(args[2])) {
                                main.getMessageUtils().sendMessage("lang", "messages.has-not-discovered", "&cThe player " +
                                        onlinePlayer.getName() + " has not yet discovered this region!", sender, "player", onlinePlayer.getName());
                                return true;

                            } else {

                                main.getPlayerCache().getPlayerRegions().get(onlinePlayer.getUniqueId().toString()).remove(args[2]);
                                List playerRegions = dataConfig.getList(pl + onlinePlayer.getUniqueId().toString());
                                if (playerRegions.contains(args[2])) {
                                    playerRegions.remove(args[2]);
                                    dataConfig.set(pl + onlinePlayer.getUniqueId().toString(), playerRegions);
                                    try {
                                        main.getFileCache().getStoredFiles().get("data").saveConfig();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                main.getMessageUtils().sendMessage("lang", "messages.deleted-player-region", "&aSuccessfully deleted " +
                                        args[2] + " from that player's discovered regions.", sender, "region", args[2]);
                                return true;

                            }

                        }

                    }

                }

                main.getMessageUtils().sendMessage("lang", "messages.not-online", "&cThe player " +
                        args[1] + "is not currently online!", player, "player", args[1]);
                return true;


            }

        }

        sendHelpMessage(sender);
        return true;

    }

    private void teleportPlayer(Player player, String region, String[] arrayLocation) {
        player.teleport(new Location(Bukkit.getWorld(main.getFileUtils().dataFile().getString("regions." + region + ".world")), Double.parseDouble(arrayLocation[0]),
                Double.parseDouble(arrayLocation[1]), Double.parseDouble(arrayLocation[2]), 0, 0));
        main.getMessageUtils().sendMessage("lang", "messages.teleport", "&aSuccessfully teleported to " +
                region + "!", player, "region", region);

        if (main.getFileUtils().configFile().getBoolean("config.global-cooldown")) {

        }
    }
    private void sendHelpMessage(CommandSender sender) {
        if(sender.hasPermission("CyberTravel.admin") && !main.getFileCache().getStoredFiles().get("lang").getConfig().isSet("messages.help-admin")){
            if(main.getFileCache().getStoredFiles().get("lang").getConfig().getString("messages.help-admin").equalsIgnoreCase("")){
                return;
            }

            sender.sendMessage(main.getMessageUtils().getColor("&8&m―――――&8<&c&l Cyber&f&lTravel &8>&8&m―――――", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp help &fSee the help menu.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp reload &fReload the plugin.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp create <region> &fCreate a region in your world.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp delete <region> &fDelete an existing region.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp (pos1 | pos2) <region> &fSet cuboid boundaries of region.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp settp <region> &fSet teleport location.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp (tp | teleport) <region> &fTeleport to location.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp regions &fSee all locked/unlocked regions.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp addregion <player> <region> &fAdd a region to a player's list.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp delregion <player> <region> &fRemove a region from a player's list.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8&m――――――――――――――――――――――――――――――", false));
            return;

        } else if (sender.hasPermission("CyberTravel.admin")) {
            for (String x : main.getFileCache().getStoredFiles().get("lang").getConfig().getStringList("messages.help-admin")) {
                sender.sendMessage(main.getMessageUtils().getColor(x, false));

            }

        } else if(sender.hasPermission("CyberTravel.player") && !main.getFileCache().getStoredFiles().get("lang").getConfig().isSet("messages.help-player")){

            if(main.getFileCache().getStoredFiles().get("lang").getConfig().getString("messages.help-admin").equalsIgnoreCase("")){
                return;
            }

            sender.sendMessage(main.getMessageUtils().getColor("&8&m―――――&8<&c&l Cyber&f&lTravel &8>&8&m―――――", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp help &fSee the help menu.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp (tp | teleport) <region> &fTeleport to location.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8➼ &7/ctp regions &fSee all locked/unlocked regions.", false));
            sender.sendMessage(main.getMessageUtils().getColor("&8&m――――――――――――――――――――――――――――――", false));
            return;
        } else if (sender.hasPermission("CyberTravel.player")) {
            for (String x : main.getFileCache().getStoredFiles().get("lang").getConfig().getStringList("messages.help-player")) {
                sender.sendMessage(main.getMessageUtils().getColor(x, false));

            }

        } else {
            main.getMessageUtils().noPermission((Player) sender);
        }

        return;
    }

}
