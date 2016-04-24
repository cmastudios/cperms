/*
 * Copyright (C) 2013 Connor Monahan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.cmastudios.permissions.commands;

import me.cmastudios.permissions.Group;
import me.cmastudios.permissions.Permissions;
import me.cmastudios.permissions.PermissionsPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

/**
 *
 * @author Connor Monahan
 */
public class SetGroupCommand implements CommandExecutor {

    private final Permissions plugin;

    public SetGroupCommand(Permissions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            return false;
        }
        @SuppressWarnings("deprecation") OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        Group group = plugin.getGroup(args[1]);
        if (group == null) {
            sender.sendMessage("Group not found");
            return true;
        }
        try {
            PermissionsPlayer permPlayer = plugin.getPlayer(player, null);
            if (args.length > 2) {
                permPlayer.setExpirationDate(new Timestamp(System.currentTimeMillis() + (Integer.parseInt(args[2]) * 60000)));
            }
            Group oldGroup = permPlayer.getGroup();
            permPlayer.setGroup(group);
            if (player.isOnline()) {
                plugin.updatePermissions(player.getPlayer());
            }
            Command.broadcastCommandMessage(sender, String.format("Changed group of %s from %s to %s",
                    player.getName(), oldGroup.getName(), group.getName()));
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Setting player group", ex);
        }
        return true;
    }
}
