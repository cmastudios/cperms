package me.cmastudios.permissions.commands;

import me.cmastudios.permissions.Permissions;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Miscellaneous utility commands
 * 
 * cpermissions reload: Reloads plugin config so server reboot isn't required
 * after updating config
 * 
 * @author dylanhansch
 */

public class cPermsCommand implements CommandExecutor {

    private final Permissions plugin;

    public cPermsCommand(Permissions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            
            // We need to update permissions for each player online in the event permissions
            // were edited, prefix, suffix, etc.
            for (Player playerOnline : plugin.getServer().getOnlinePlayers()) {
                plugin.updatePermissions(playerOnline);
            }
            
            sender.sendMessage(ChatColor.GOLD + "Reloaded cPermissions configuration from disk.");
            return true;
        } else {
            return false;
        }
    }
}
