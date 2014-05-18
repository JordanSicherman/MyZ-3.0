/**
 * 
 */
package myz.nmscode.v1_7_R3.mobs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import myz.nmscode.compat.CustomMob;
import myz.nmscode.v1_7_R3.pathfinders.PathfinderGoalLookAtTarget;
import myz.nmscode.v1_7_R3.pathfinders.PathfinderGoalNearestAttackableZombieTarget;
import myz.nmscode.v1_7_R3.pathfinders.PathfinderGoalWalkTo;
import myz.nmscode.v1_7_R3.pathfinders.PathfinderGoalZombieAttack;
import myz.nmscode.v1_7_R3.pathfinders.Support;
import myz.support.interfacing.Configuration;
import net.minecraft.server.v1_7_R3.DamageSource;
import net.minecraft.server.v1_7_R3.Entity;
import net.minecraft.server.v1_7_R3.EntityHuman;
import net.minecraft.server.v1_7_R3.EntityLiving;
import net.minecraft.server.v1_7_R3.EntitySkeleton;
import net.minecraft.server.v1_7_R3.EntityVillager;
import net.minecraft.server.v1_7_R3.EntityZombie;
import net.minecraft.server.v1_7_R3.EnumDifficulty;
import net.minecraft.server.v1_7_R3.GenericAttributes;
import net.minecraft.server.v1_7_R3.ItemStack;
import net.minecraft.server.v1_7_R3.Items;
import net.minecraft.server.v1_7_R3.PathfinderGoal;
import net.minecraft.server.v1_7_R3.PathfinderGoalFloat;
import net.minecraft.server.v1_7_R3.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_7_R3.PathfinderGoalMoveIndoors;
import net.minecraft.server.v1_7_R3.PathfinderGoalMoveThroughVillage;
import net.minecraft.server.v1_7_R3.PathfinderGoalMoveTowardsRestriction;
import net.minecraft.server.v1_7_R3.PathfinderGoalOpenDoor;
import net.minecraft.server.v1_7_R3.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_7_R3.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_7_R3.PathfinderGoalRestrictOpenDoor;
import net.minecraft.server.v1_7_R3.PathfinderGoalSelector;
import net.minecraft.server.v1_7_R3.World;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R3.util.UnsafeList;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

/**
 * @author Jordan
 * 
 */
public class CustomEntityZombie extends EntityZombie implements CustomMob {

	protected List<org.bukkit.inventory.ItemStack> inventory = new ArrayList<org.bukkit.inventory.ItemStack>();
	private int priority = 0;

	public CustomEntityZombie(World world) {
		super(world);

		populateGoals();
	}

	@Override
	public LivingEntity getEntity() {
		return (LivingEntity) getBukkitEntity();
	}

	@Override
	public UUID getUID() {
		return getUniqueID();
	}

	@Override
	public Object getWorld() {
		return world;
	}

	public static CustomEntityZombie newInstance(Player player) {
		World world = ((CraftWorld) player.getWorld()).getHandle();
		CustomEntityZombie zombie = new CustomEntityZombie(world);

		zombie.setPosition(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
		world.addEntity(zombie, SpawnReason.CUSTOM);
		return zombie;
	}

	private void emptyInventory() {
		if (inventory != null)
			for (org.bukkit.inventory.ItemStack item : inventory)
				if (item != null)
					getBukkitEntity().getWorld().dropItemNaturally(getBukkitEntity().getLocation(), item);
		inventory = null;
	}

	private void populateGoals() {
		try {
			Support.getField().set(goalSelector, new UnsafeList<PathfinderGoalSelector>());
			Support.getField().set(targetSelector, new UnsafeList<PathfinderGoalSelector>());
			Support.getSecondField().set(goalSelector, new UnsafeList<PathfinderGoalSelector>());
			Support.getSecondField().set(targetSelector, new UnsafeList<PathfinderGoalSelector>());
		} catch (Exception exc) {
			exc.printStackTrace();
		}

		getNavigation().b(true);
		goalSelector.a(0, new PathfinderGoalFloat(this));
		goalSelector.a(2, new PathfinderGoalZombieAttack(this, EntityHuman.class, (Double) Configuration.getConfig("mobs.zombie.speed")
				* (isBaby() ? 0.4 : 1), false));
		goalSelector.a(3, new PathfinderGoalZombieAttack(this, EntityVillager.class, (Double) Configuration.getConfig("mobs.zombie.speed")
				* (isBaby() ? 0.4 : 1), true));
		goalSelector.a(3, new PathfinderGoalZombieAttack(this, EntitySkeleton.class, (Double) Configuration.getConfig("mobs.zombie.speed")
				* (isBaby() ? 0.4 : 1), true));
		goalSelector.a(4, new PathfinderGoalMoveTowardsRestriction(this, 1.0D));
		goalSelector.a(5, new PathfinderGoalMoveThroughVillage(this, 1.0D, false));
		goalSelector.a(6, new PathfinderGoalRandomStroll(this, 1.0D));
		goalSelector.a(2, new PathfinderGoalMoveIndoors(this));
		goalSelector.a(3, new PathfinderGoalRestrictOpenDoor(this));
		goalSelector.a(4, new PathfinderGoalOpenDoor(this, true));
		goalSelector.a(7, new PathfinderGoalLookAtTarget(this, EntityHuman.class, 8.0F));
		goalSelector.a(7, new PathfinderGoalRandomLookaround(this));
		targetSelector.a(1, new PathfinderGoalHurtByTarget(this, true));
		targetSelector.a(2, new PathfinderGoalNearestAttackableZombieTarget(this, EntityHuman.class, 0, true));
		targetSelector.a(2, new PathfinderGoalNearestAttackableZombieTarget(this, EntityVillager.class, 0, false));
		targetSelector.a(2, new PathfinderGoalNearestAttackableZombieTarget(this, EntitySkeleton.class, 0, false));
	}

	public void addPather(Location to, float speed) {
		goalSelector.a(4, new PathfinderGoalWalkTo(this, to, speed));
	}

	@Override
	public boolean canSpawn() {
		return world.difficulty != EnumDifficulty.PEACEFUL && world.b(boundingBox) && world.getCubes(this, boundingBox).isEmpty()
				&& !world.containsLiquid(boundingBox);
	}

	/*@Override
	public boolean m(Entity entity) {
		return entity.damageEntity(DamageSource.mobAttack(this), (float) Configuration.getZombieDamage() * (isBaby() ? 0.5f : 1f));
	}*/

	public void cleanPather(PathfinderGoal goal) {
		populateGoals();
		priority = 0;
	}

	@Override
	public boolean damageEntity(DamageSource damagesource, float f) {
		if (!super.damageEntity(damagesource, f))
			return false;
		else {
			EntityLiving entityliving = getGoalTarget();

			if (entityliving == null && bT() instanceof EntityLiving)
				entityliving = (EntityLiving) bT();

			if (entityliving == null && damagesource.getEntity() instanceof EntityLiving)
				entityliving = (EntityLiving) damagesource.getEntity();

			return true;
		}
	}

	@Override
	public void die() {
		super.die();
		emptyInventory();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		emptyInventory();
	}

	public void see(Location location, int priority) {
		if (priority < this.priority)
			return;
		if (random.nextInt(priority + 1) >= 1 && getGoalTarget() == null || priority > 1) {
			setGoalTarget(null);
			target = null;
			double dub = (Double) Configuration.getConfig("mobs.zombie.speed");
			addPather(location, (float) dub * (isBaby() ? 0.4f : 1f));
		}
	}

	/**
	 * Set this zombie's inventory contents. Generally contains a player's
	 * inventory plus their helmet and subtract their item in hand.
	 * 
	 * @param inventory
	 *            The list of items to set.
	 */
	@Override
	public void setInventory(List<org.bukkit.inventory.ItemStack> inventory) {
		this.inventory = inventory;
	}

	@Override
	protected void aC() {
		super.aC();
		getAttributeInstance(GenericAttributes.e).setValue((Double) Configuration.getConfig("mobs.zombie.damage") * (isBaby() ? 0.75 : 1));
	}

	@Override
	protected void bC() {
		if (random.nextFloat() < (world.difficulty == EnumDifficulty.NORMAL ? 0.05F : 0.01F)) {
			int i = random.nextInt(3);

			if (i == 0)
				setEquipment(0, new ItemStack(Items.STONE_SWORD));
			else
				setEquipment(0, new ItemStack(Items.STONE_SPADE));
		}
	}

	@Override
	protected Entity findTarget() {
		EntityHuman entityhuman = Support.findNearbyVulnerablePlayer(this);

		return entityhuman != null && this.p(entityhuman) ? entityhuman : null;
	}

	@Override
	protected ItemStack getRareDrop(int i) {
		switch (random.nextInt(3)) {
		case 0:
			return new ItemStack(Items.POTION);

		case 1:
			return new ItemStack(Items.BOWL);

		default:
			return new ItemStack(Items.LEASH);
		}
	}
}
