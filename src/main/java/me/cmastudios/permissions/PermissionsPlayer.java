package me.cmastudios.permissions;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.permissions.PermissionDefault;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * Represents a player managed by the Permissions plugin. Players are defined
 * to have a single {@link Group} which is stored in the plugin's database.
 * The player's group defines everything about a player, from permissions to
 * name color in chat.
 * <p>
 * Please do not keep copies of this class after a player logs out or switches
 * worlds. Permissions are recalculated on these events, which may cause a
 * temporary rank to expire, updating the database.
 *
 * @author Connor Monahan
 */
public class PermissionsPlayer {

    private final Permissions plugin;
    private final OfflinePlayer player;
    private Group group;
    private Timestamp expiration;
    private World world;

    PermissionsPlayer(Permissions plugin, OfflinePlayer player, Group group, Timestamp expiration, World world) {
        this.plugin = plugin;
        this.player = player;
        this.group = group;
        this.expiration = expiration;
        this.world = world;
    }

    /**
     * Get the server player represented by this permissions object.
     *
     * @return Player represented by this class.
     */
    public OfflinePlayer getPlayer() {
        return player;
    } 

    /**
     * Get the group the player is currently in. This function references the
     * player's group when this class was originally created or the state after
     * {@link #setGroup} was called on this object.
     *
     * @return The player's group.
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Update the user's group in the object and in the database. This function
     * does not attempt to recalculate permissions for online users, so it is
     * necessary to call {@link Permissions#updatePermissions} if the player is
     * currently online.
     *
     * @param Group The group to place the user in.
     * @throws SQLException Database error saving the new group.
     */
    public void setGroup(Group group) throws SQLException {
        this.group = group;
        save();
    }

    /**
     * Get the time when the player's current rank expires. This returns null
     * if their rank does not expire.
     *
     * @return rank expiration date or null if permanent.
     */
    public Date getExpirationDate() {
        return expiration;
    }

    /**
     * Set the time when the player's current rank expires. This will update
     * the database.
     *
     * @param expiration Time when the rank will expire.
     * @throws SQLException Database error saving the expiration date.
     */
    public void setExpirationDate(Date expiration) throws SQLException {
        if (expiration instanceof Timestamp) {
            this.expiration = (Timestamp) expiration;
        } else {
            this.expiration = new Timestamp(expiration.getTime());
        }
        save();
    }

    /**
     * Get the world the player is currently in. This may not be the world the
     * player is physically in, only the one specified on lookup.
     *
     * @return World the player is marked as being in.
     */
    public World getWorld() {
        return world;
    }

    /**
     * Set the world the player is currently in. This only effects permissions
     * lookups in {@link #has}.
     *
     * @param world World to set the player in.
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * Check if a player has a certain permission. This will get all the
     * permissions from the group for the player's current world and check
     * if the group has the permission set to true. This will check the default
     * value of the permission. If the player is online, the logic will be
     * short-circuited, using {@link org.bukkit.entity.Player#hasPermission}
     * to allow other plugins a chance to modify the permissions themselves.
     *
     * @param permission Permission node to check.
     * @return true if the player has the permission, false otherwise.
     */
    public boolean has(String permission) {
        if (player.isOnline()) {
            return player.getPlayer().hasPermission(permission);
        }
        Map<String, Boolean> permissions = group.getPermissions(world);
        if (permissions.containsKey(permission)) {
            return permissions.get(permission);
        }
        PermissionDefault def = Bukkit.getPluginManager().getPermission(permission).getDefault();
        if (def == PermissionDefault.TRUE) return true;
        if (def == PermissionDefault.OP && player.isOp()) return true;
        if (def == PermissionDefault.NOT_OP && !player.isOp()) return true;
        return false;
    }

    private void save() throws SQLException {
        PlayerGroupDatabase.setGroup(plugin.getDatabaseConnection(), player, group, expiration);
    }
}

