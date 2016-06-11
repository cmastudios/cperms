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
package me.cmastudios.permissions;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;

/**
 * Permissions groups define the permissions and other attributes (e.g name
 * color) for their members. Groups are defined in the plugin configuration to
 * have set permissions for themselves, in specific worlds, and inherited from
 * other groups in the file.
 * <p>
 * Groups interact with other groups in the following ways:
 * <ul>
 * <li>Inheriting permissions
 * <li>Rank expiration fallback
 * </ul>
 * <p>
 * Please do not store copies of this class after a server reload. This class
 * contains a copy of the plugin's configuration.
 *
 * @see PermissionsPlayer
 * @author Connor Monahan
 */
@SuppressWarnings("WeakerAccess")
public class Group {

    private final String name;
    private final boolean Default; // default is a reserved word
    private final String prefix;
    private final String suffix;
    private final boolean allowedToBuild;
    private final Set<Group> inheritedGroups;
    private final String fallbackGroup;
    private final Configuration config;

    /**
     * Create a new group from the group in the configuration.
     *
     * @param config Plugin configuration, containing group information.
     * @param name Group name.
     */
    Group(Configuration config, String name) {
        ConfigurationSection groupRootSection = config.getConfigurationSection("groups." + name);
        this.name = groupRootSection.getName();
        this.Default = groupRootSection.getBoolean("default");
        this.prefix = ChatColor.translateAlternateColorCodes('&', groupRootSection.getString("info.prefix"));
        this.suffix = ChatColor.translateAlternateColorCodes('&', groupRootSection.getString("info.suffix"));
        this.allowedToBuild = groupRootSection.getBoolean("info.build");
        this.inheritedGroups = new LinkedHashSet<>();
        for (String inheritedGroupName : groupRootSection.getStringList("inheritance")) {
            ConfigurationSection section = groupRootSection.getParent().getConfigurationSection(inheritedGroupName);
            if (section == null) {
                throw new RuntimeException("Failed to load information for group '" + name
                    + "': Unknown group in inheritance '" + inheritedGroupName + "'.");
            }
            this.inheritedGroups.add(new Group(config, inheritedGroupName));
        }
        this.fallbackGroup = groupRootSection.getString("info.fallback", null);
        this.config = config;
    }

    /**
     * Get the server's default group.
     *
     * @param config Permissions configuration file
     * @return group or null if not found. The group should never be null.
     */
    private Group getDefaultGroup(Configuration config) {
        for (String key : config.getConfigurationSection("groups").getKeys(false)) {
            if (config.getBoolean(String.format("groups.%s.default", key))) {
                return new Group(config, key);
            }
        }
        return null;
    }

    private Group getGroup(Configuration config, String name) {
        if (config.contains("groups." + name)) {
            return new Group(config, name);
        }
        return null;
    }

    /**
     * Get a group's name.
     *
     * @return group name
     */
    public String getName() {
        return name;
    }

    /**
     * Check if a group is the default group.
     *
     * @return true if group is default
     */
    public boolean isDefault() {
        return Default;
    }

    /**
     * Get text to prefix in a player's display name. This text has been parsed
     * for color codes.
     *
     * @return display name prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Get text to suffix in a player's display name. This text has been parsed
     * for color codes.
     *
     * @return display name suffix
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Check if a group is allowed to build.
     *
     * @return true if group can build
     */
    public boolean isAllowedToBuild() {
        return allowedToBuild;
    }

    /**
     * Get groups that this group inherits from.
     *
     * @return ordered set of inherited groups
     */
    public Set<Group> getInheritedGroups() {
        return inheritedGroups;
    }

    /**
     * Get the group this is specified to fall back on when ranks expire.
     * This will return the server's default group if there is no group
     * specified in the configuration.
     *
     * @return fallback group.
     */
    public Group getFallbackGroup() {
        Group fallback = getGroup(config, fallbackGroup);
        return fallback == null ? getDefaultGroup(config) : fallback;
    }

    /**
     * Get permissions assigned to a group. This includes permissions inherited
     * from other groups and permissions based on group name.
     *
     * @param world World for world-specific permissions section
     * @return permissions assigned to this group in the specified world
     */
    public Map<String, Boolean> getPermissions(World world) {
        Map<String, Boolean> permissions = new HashMap<>();
        // Position 1, inherited groups
        try {
            for (Group group : this.getInheritedGroups()) {
                for (Map.Entry<String, Boolean> entry : group.getPermissions(world).entrySet()) {
                    // Warning: this could cause a infinite do-loop if used by stupid admins
                    permissions.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (StackOverflowError e) {
            permissions.clear();
            throw new RuntimeException("WARNING! Potential inheritance loop!");
            // No exception passing to prevent 1000000 line stack traces
        }
        // Position 2, permissions assigned based on group name
        for (String namePermission : config.getStringList("autoperms")) {
            SimpleEntry<String, Boolean> permission = this.parsePermission(String.format(namePermission, name));
            permissions.put(permission.getKey(), permission.getValue());
        }
        permissions.put("cpermissions.build", this.isAllowedToBuild());
        // Position 3, general group-specific permissions
        for (String perm : config.getStringList(String.format("groups.%s.permissions", name))) {
            SimpleEntry<String, Boolean> permission = this.parsePermission(perm);
            permissions.put(permission.getKey(), permission.getValue());
        }
        // Position 4, world & group specific permissions
        if (world != null) {
            for (String perm : config.getStringList(String.format("groups.%s.worlds.%s", name, world.getName()))) {
                SimpleEntry<String, Boolean> permission = this.parsePermission(perm);
                permissions.put(permission.getKey(), permission.getValue());
            }
        }
        return permissions;
    }

    private AbstractMap.SimpleEntry<String, Boolean> parsePermission(String permission) {
        boolean enabled = true;
        if (permission.startsWith("-")) {
            // Negated permission
            enabled = false;
            permission = permission.substring(1);
        }
        return new AbstractMap.SimpleEntry<>(permission, enabled);
    }
}
