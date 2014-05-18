/**
 * 
 */
package myz.nmscode.v1_7_R3.mobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import myz.nmscode.compat.CustomMob;
import myz.nmscode.v1_7_R3.utilities.NullEntityNetworkManager;
import myz.nmscode.v1_7_R3.utilities.NullNetServerHandler;
import myz.utilities.Utils;
import net.minecraft.server.v1_7_R3.DamageSource;
import net.minecraft.server.v1_7_R3.Entity;
import net.minecraft.server.v1_7_R3.EntityArrow;
import net.minecraft.server.v1_7_R3.EntityDamageSource;
import net.minecraft.server.v1_7_R3.EntityHorse;
import net.minecraft.server.v1_7_R3.EntityHuman;
import net.minecraft.server.v1_7_R3.EntityLiving;
import net.minecraft.server.v1_7_R3.EntityPlayer;
import net.minecraft.server.v1_7_R3.EntityWolf;
import net.minecraft.server.v1_7_R3.EnumDifficulty;
import net.minecraft.server.v1_7_R3.EnumGamemode;
import net.minecraft.server.v1_7_R3.MinecraftServer;
import net.minecraft.server.v1_7_R3.MobEffectList;
import net.minecraft.server.v1_7_R3.NetworkManager;
import net.minecraft.server.v1_7_R3.PlayerInteractManager;
import net.minecraft.server.v1_7_R3.StatisticList;
import net.minecraft.server.v1_7_R3.WorldServer;
import net.minecraft.util.com.mojang.authlib.GameProfile;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R3.event.CraftEventFactory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author kumpelblase2
 * 
 */
public class CustomEntityPlayer extends EntityPlayer implements CustomMob {

	protected List<org.bukkit.inventory.ItemStack> inventoryItems = new ArrayList<org.bukkit.inventory.ItemStack>();

	public CustomEntityPlayer(MinecraftServer server, WorldServer worldserver, GameProfile gameprofile,
			PlayerInteractManager iteminworldmanager) {
		super(server, worldserver, gameprofile, iteminworldmanager);
		try {
			NetworkManager manager = new NullEntityNetworkManager(false);
			playerConnection = new NullNetServerHandler(server, manager, this);
			manager.a(playerConnection);
		} catch (Exception e) {
		}

		iteminworldmanager.setGameMode(EnumGamemode.SURVIVAL);
		noDamageTicks = 1;
	}

	@Override
	public LivingEntity getEntity() {
		return getBukkitEntity();
	}

	@Override
	public UUID getUID() {
		return getUniqueID();
	}

	public static CustomEntityPlayer newInstance(org.bukkit.entity.Player playerDuplicate) {
		WorldServer worldServer = ((CraftWorld) playerDuplicate.getWorld()).getHandle();
		CustomEntityPlayer player = new CustomEntityPlayer(worldServer.getMinecraftServer(), worldServer, ((CraftPlayer) playerDuplicate)
				.getHandle().getProfile(), new PlayerInteractManager(worldServer));

		Location loc = playerDuplicate.getLocation();
		player.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

		((Player) player.getBukkitEntity()).setItemInHand(playerDuplicate.getItemInHand());
		((Player) player.getBukkitEntity()).setCustomName(playerDuplicate.getName());
		((Player) player.getBukkitEntity()).getEquipment().setArmorContents(playerDuplicate.getInventory().getArmorContents());
		player.setInventory(new ArrayList<ItemStack>(Arrays.asList(playerDuplicate.getInventory().getContents())));
		player.getBukkitEntity().setGameMode(GameMode.SURVIVAL);
		player.getBukkitEntity().setCanPickupItems(false);

		((Player) player.getBukkitEntity()).setHealthScale(playerDuplicate.getHealthScale());
		((Player) player.getBukkitEntity()).setMaxHealth(playerDuplicate.getMaxHealth());
		((Player) player.getBukkitEntity()).setHealth(playerDuplicate.getHealth());
		((Player) player.getBukkitEntity()).setRemoveWhenFarAway(false);

		worldServer.addEntity(player, SpawnReason.CUSTOM);

		return player;
	}

	@Override
	public boolean damageEntity(DamageSource damagesource, float f) {
		// EntityPlayer default start.
		boolean flag = server.X() && server.getPvP() && "fall".equals(damagesource.translationIndex);

		if (!flag && invulnerableTicks > 0 && !damagesource.ignoresInvulnerability())
			return false;
		else if (damagesource instanceof EntityDamageSource) {
			Entity entity = damagesource.getEntity();

			if (entity instanceof EntityHuman && !this.a((EntityHuman) entity))
				return false;

			if (entity instanceof EntityArrow) {
				EntityArrow entityarrow = (EntityArrow) entity;

				if (entityarrow.shooter instanceof EntityHuman && !this.a((EntityHuman) entityarrow.shooter))
					return false;
			}
		}

		aU = 0;
		if (getHealth() <= 0.0F)
			return false;
		else if (damagesource.o() && this.hasEffect(MobEffectList.FIRE_RESISTANCE))
			return false;
		else {
			// EntityHuman default start.
			if (isSleeping() && !world.isStatic)
				this.a(true, true, false);

			if (damagesource.r()) {
				if (world.difficulty == EnumDifficulty.PEACEFUL)
					f = 0.0F;

				if (world.difficulty == EnumDifficulty.EASY)
					f = f / 2.0F + 1.0F;

				if (world.difficulty == EnumDifficulty.HARD)
					f = f * 3.0F / 2.0F;
			}

			if (f == 0.0F)
				return false;
			else {
				Entity entity = damagesource.getEntity();

				if (entity instanceof EntityArrow && ((EntityArrow) entity).shooter != null)
					entity = ((EntityArrow) entity).shooter;

				this.a(StatisticList.u, Math.round(f * 10.0F));
			}
			// EntityHuman default end.

			if ((damagesource == DamageSource.ANVIL || damagesource == DamageSource.FALLING_BLOCK) && this.getEquipment(4) != null) {
				this.getEquipment(4).damage((int) (f * 4.0F + random.nextFloat() * f * 2.0F), this);
				f *= 0.75F;
			}

			aF = 1.5F;
			flag = true;

			EntityDamageEvent event = CraftEventFactory.handleEntityDamageEvent(this, damagesource, f);
			if (event != null) {
				if (event.isCancelled())
					return false;
				f = (float) event.getDamage();
			}

			if (noDamageTicks > maxNoDamageTicks / 2.0F) {
				if (f <= lastDamage)
					return false;

				this.d(damagesource, f - lastDamage);
				lastDamage = f;
				flag = false;
			} else {
				lastDamage = f;
				aw = getHealth();
				noDamageTicks = maxNoDamageTicks;
				this.d(damagesource, f);
				hurtTicks = ay = 10;
			}

			az = 0.0F;
			Entity entity = damagesource.getEntity();

			if (entity != null) {
				if (entity instanceof EntityLiving)
					this.b((EntityLiving) entity);

				if (entity instanceof EntityHuman) {
					lastDamageByPlayerTime = 100;
					killer = (EntityHuman) entity;
				} else if (entity instanceof EntityWolf) {
					EntityWolf entitywolf = (EntityWolf) entity;

					if (entitywolf.isTamed()) {
						lastDamageByPlayerTime = 100;
						killer = null;
					}
					// MyZ start
				} else if (entity instanceof EntityHorse) {
					EntityHorse entityhorse = (EntityHorse) entity;

					if (entityhorse.getOwnerUUID() != null && !entityhorse.getOwnerUUID().isEmpty()) {
						lastDamageByPlayerTime = 100;
						killer = null;
					}
				}
				// MyZ end
			}

			if (flag) {
				world.broadcastEntityEffect(this, (byte) 2);
				if (damagesource != DamageSource.DROWN)
					P();

				if (entity != null /* MyZ start */&& onGround /* MyZ end */) {
					double d0 = entity.locX - locX;

					double d1;

					for (d1 = entity.locZ - locZ; d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D)
						d0 = (Math.random() - Math.random()) * 0.01D;

					az = (float) (Math.atan2(d1, d0) * 180.0D / 3.1415927410125732D) - yaw;
					this.a(entity, f, d0, d1);
				} else
					az = (int) (Math.random() * 2.0D) * 180;
			}

			String s;

			if (getHealth() <= 0.0F) {
				s = aT();
				if (flag && s != null)
					makeSound(s, be(), bf());

				this.die(damagesource);
			} else {
				s = aS();
				if (flag && s != null)
					makeSound(s, be(), bf());
			}

			return true;
		}
	}

	@Override
	public void die() {
		Utils.playerNPCDied(this, getBukkitEntity().getWorld());
		Utils.spawnPlayerZombie(getBukkitEntity(), inventoryItems);
		inventoryItems = null;

		dead = true;
		getBukkitEntity().remove();
	}

	@Override
	public void die(DamageSource source) {
		Utils.playerNPCDied(this, getBukkitEntity().getWorld());
		Utils.spawnPlayerZombie(getBukkitEntity(), inventoryItems);
		inventoryItems = null;

		dead = true;
		getBukkitEntity().remove();
	}

	@Override
	public void h() {
		// Taken from RemoteEntities#RemotePlayerEntity.java
		yaw = az;
		super.h();
		this.e();

		if (noDamageTicks > 0)
			noDamageTicks--;

		// Taken from Citizens2#EntityHumanNPC.java#129 - #138 - slightly
		// modified.
		if (Math.abs(motX) < 0.001F && Math.abs(motY) < 0.001F && Math.abs(motZ) < 0.001F)
			motX = motY = motZ = 0;
		// End Citizens
	}

	/**
	 * Set this player's inventory contents.
	 * 
	 * @param inventory
	 *            The list of items to set.
	 */
	@Override
	public void setInventory(List<org.bukkit.inventory.ItemStack> inventory) {
		inventoryItems = inventory;
	}
}
