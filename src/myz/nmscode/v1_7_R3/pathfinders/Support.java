/**
 * 
 */
package myz.nmscode.v1_7_R3.pathfinders;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import myz.MyZ;
import myz.nmscode.v1_7_R3.mobs.CustomEntityNPC;
import myz.nmscode.v1_7_R3.mobs.CustomEntityPigZombie;
import myz.nmscode.v1_7_R3.mobs.CustomEntityZombie;
import myz.support.SQLManager;
import myz.support.interfacing.Configuration;
import net.minecraft.server.v1_7_R3.Entity;
import net.minecraft.server.v1_7_R3.EntityCreature;
import net.minecraft.server.v1_7_R3.EntityHorse;
import net.minecraft.server.v1_7_R3.EntityHuman;
import net.minecraft.server.v1_7_R3.EntityInsentient;
import net.minecraft.server.v1_7_R3.PathfinderGoalSelector;
import net.minecraft.server.v1_7_R3.World;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @author Jordan
 * 
 */
public class Support {

	private static final Random random = new Random();
	private static Map<UUID, Double> visibility_override = new HashMap<UUID, Double>();
	private static Field field, field2;

	/**
	 * A modified version of NMS findNearbyVulnerablePlayer(double d1, double
	 * d2, double d3, double d0) Returns the nearest player to the location,
	 * taking movement factors into account.
	 * 
	 * @param entity
	 *            The Entity.
	 * @param x
	 *            The x location.
	 * @param y
	 *            The y location.
	 * @param z
	 *            The z location.
	 * @return The nearest EntityHuman or null if none are nearby.
	 */
	private static EntityHuman findNearbyVulnerablePlayer(Entity entity, double x, double y, double z) {
		if (entity instanceof EntityCreature && ((EntityCreature) entity).getGoalTarget() != null) {
			if (((EntityCreature) entity).getGoalTarget() instanceof EntityHuman)
				return (EntityHuman) ((EntityCreature) entity).getGoalTarget();
			return null;
		}
		World world = entity.world;
		double shortest_distance = -1.0D;
		EntityHuman entityhuman = null;

		boolean disguise = MyZ.instance.getServer().getPluginManager().getPlugin("DisguiseCraft") != null
				&& MyZ.instance.getServer().getPluginManager().getPlugin("DisguiseCraft").isEnabled();
		boolean disguise2 = MyZ.instance.getServer().getPluginManager().getPlugin("LibsDisguises") != null
				&& MyZ.instance.getServer().getPluginManager().getPlugin("LibsDisguises").isEnabled();

		for (int i = 0; i < world.players.size(); ++i) {
			EntityHuman player = (EntityHuman) world.players.get(i);

			if (disguise && myz.utilities.DisguiseUtils.isZombie((Player) player.getBukkitEntity()))
				continue;
			if (disguise2 && myz.utilities.LibsDisguiseUtils.isZombie((Player) player.getBukkitEntity()))
				continue;

			if (!player.isInvulnerable() && player.isAlive()) {
				// Make sure we don't target our owner if we're a horse.
				if (entity instanceof EntityHorse
						&& ((EntityHorse) entity).getOwnerUUID() != null
						&& (((EntityHorse) entity).getOwnerUUID().equals(SQLManager.UUIDtoString(player.getUniqueID())) || MyZ.instance
								.isFriend(SQLManager.fromString(((EntityHorse) entity).getOwnerUUID(), true),
										MyZ.instance.getUID(player.getName()))))
					continue;
				// Get the players distance from the x, y, z.
				double distance_to_player_squared = player.e(x, y, z);
				double refined_radius = experienceBarVisibility((Player) player.getBukkitEntity(), true)
						* (Double) Configuration.getConfig("mobs.aggroMultiplier");

				if (distance_to_player_squared < refined_radius * refined_radius
						&& (shortest_distance == -1.0D || distance_to_player_squared < shortest_distance)) {
					shortest_distance = distance_to_player_squared;
					entityhuman = player;
				}
			}
		}

		if (entity instanceof EntityInsentient)
			((EntityInsentient) entity).setGoalTarget(entityhuman);
		return entityhuman;
	}

	/**
	 * Override the standard visibility value with a set value.
	 * 
	 * @param player
	 *            The player to override.
	 * @param visibility
	 *            The visibility in blocks to override with.
	 */
	public static void elevatePlayer(Player player, double visibility) {
		visibility_override.put(player.getUniqueId(), visibility);
	}

	/**
	 * @see experienceBarVisibility(Player player, boolean isAmplified).
	 */
	public static double experienceBarVisibility(Player player) {
		return experienceBarVisibility(player, false);
	}

	/**
	 * The number of experience bars the player must have full, depending on
	 * environmental factors.
	 * 
	 * @param player
	 *            The player.
	 * @param isAmplified
	 *            Whether or not to amplify the results (not for experience
	 *            bar).
	 * @return The number of full exp segments (full is 18, empty is 0). To set
	 *         bars, you must set this value divided by 18f.
	 **/
	public static double experienceBarVisibility(Player player, boolean isAmplified) {
		double total = 10;
		// Default exp bars full is 10. There are 18 total.

		CraftPlayer p = (CraftPlayer) player;

		if (visibility_override.containsKey(player.getUniqueId())) {
			double vis = visibility_override.get(player.getUniqueId());
			visibility_override.remove(player.getUniqueId());
			return vis;
		}
		// Sneaking players must be nearer to be seen.
		if (player.isSneaking())
			total = isAmplified ? 8 : 6; // 6 bars of exp filled.

		// Sprinting players can be seen more easily.
		if (player.isSprinting())
			total = isAmplified ? 25 : 16; // 16 bars of exp filled.

		// Jumping players can be seen more easily.
		if (!p.isOnGround())
			total += isAmplified ? 5 : 2; // Add two blocks of visibility.

		// Rain reduces zombie sight slightly.
		if (p.getHandle().world.isRainingAt((int) player.getLocation().getX(), (int) player.getLocation().getY(), (int) player
				.getLocation().getZ()))
			total -= isAmplified ? 2.5 : 1.75; // Subtract 1.75 blocks of
												// visibility.

		// Night reduces zombie sight slightly.
		if (p.getHandle().world.getTime() > 12300 && p.getHandle().world.getTime() < 23850)
			total -= isAmplified ? 3 : 1.25; // Subtract 1.25 blocks of
												// visibility.

		// Wearing a zombie head makes you nearly invisible to zombies.
		if (p.getEquipment().getHelmet() != null && p.getEquipment().getHelmet().isSimilar(new ItemStack(Material.SKULL_ITEM, 1, (byte) 2)))
			total -= isAmplified ? 8 : 5.5; // Subtract 5.5 blocks of
											// visibility.

		// Invisible players must be very close to be seen.
		if (p.getHandle().isInvisible()) {
			ItemStack[] items = p.getEquipment().getArmorContents();
			int i = 0;
			for (ItemStack item : items)
				if (item != null)
					i++;
			float f = (float) i / (float) items.length;
			if (f < 0.1F)
				f = 0.1F;

			total *= 0.7F * f;
		}

		if (total < 0.5)
			total = 0.5;
		return total;
	}

	private static void see(EntityInsentient entity, Location location, int priority) {
		if (random.nextInt(priority + 1) >= 1 && entity.getGoalTarget() == null || priority > 1) {
			entity.setGoalTarget(null);
			if (entity.getBukkitEntity().getType() == EntityType.ZOMBIE && entity instanceof CustomEntityZombie)
				((CustomEntityZombie) entity).see(location, priority);
			else if (entity.getBukkitEntity().getType() == EntityType.PIG_ZOMBIE && entity instanceof CustomEntityPigZombie)
				((CustomEntityPigZombie) entity).see(location, priority);
			else if (entity.getBukkitEntity().getType() == EntityType.SKELETON && entity instanceof CustomEntityNPC)
				((CustomEntityNPC) entity).see(location, priority);
		}
	}

	public static void see(LivingEntity nearby, Location location, int priority) {
		see((EntityInsentient) ((CraftLivingEntity) nearby).getHandle(), location, priority);
	}

	/**
	 * @see findNearbyVulnerablePlayer(Entity entity, double x, double y, double
	 *      z)
	 */
	public static EntityHuman findNearbyVulnerablePlayer(Entity entity) {
		return findNearbyVulnerablePlayer(entity, entity.locX, entity.locY, entity.locZ);
	}

	public static Field getField() throws NoSuchFieldException, SecurityException {
		if (field == null) {
			field = PathfinderGoalSelector.class.getDeclaredField("b");
			field.setAccessible(true);
		}
		return field;
	}

	public static Field getSecondField() throws NoSuchFieldException, SecurityException {
		if (field2 == null) {
			field2 = PathfinderGoalSelector.class.getDeclaredField("c");
			field2.setAccessible(true);
		}
		return field2;
	}
}
