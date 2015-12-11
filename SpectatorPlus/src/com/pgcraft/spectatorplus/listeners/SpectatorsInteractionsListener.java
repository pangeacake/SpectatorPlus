/*
 * Copyright or © or Copr. AmauryCarrade (2015)
 * 
 * http://amaury.carrade.eu
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package com.pgcraft.spectatorplus.listeners;

import com.pgcraft.spectatorplus.SpectatorPlus;
import com.pgcraft.spectatorplus.Toggles;
import com.pgcraft.spectatorplus.guis.InventoryGUI;
import fr.zcraft.zlib.components.gui.Gui;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Gate;
import org.bukkit.material.TrapDoor;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;


public class SpectatorsInteractionsListener implements Listener
{
	SpectatorPlus p;

	public SpectatorsInteractionsListener()
	{
		p = SpectatorPlus.get();
	}



	/* **  Inventory-related  ** */


	/**
	 * Cancels inventories interactions for the spectators.
	 */
	@EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInventoryClick(final InventoryClickEvent ev)
	{
		if (p.getPlayerData(((Player) ev.getWhoClicked())).isSpectating())
		{
			ev.setCancelled(true);
		}
	}

	/**
	 * Cancels inventories interactions for the spectators.
	 */
	@EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInventoryDrag(final InventoryDragEvent ev)
	{
		if (p.getPlayerData(((Player) ev.getWhoClicked())).isSpectating())
		{
			ev.setCancelled(true);
		}
	}



	/* **  Blocks-related  ** */


	/**
	 * Used to prevent spectators from blocking players from placing blocks.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onBlockCanBuild(final BlockCanBuildEvent ev)
	{
		if (!ev.isBuildable())
		{
			// Get location of the block that is going to be placed
			Location blockLocation = ev.getBlock().getLocation(); // event.getBlock() returns the block to be placed -1 y

			boolean allowed = false; // If there are any actual players there, the event should not be over-wrote.

			for (Player target : p.getServer().getOnlinePlayers())
			{
				if (target.getWorld().equals(ev.getBlock().getWorld())) // Player in same world?
				{
					Location playerLocation = target.getLocation();

					// If the player is at this location
					if (playerLocation.getX() > blockLocation.getBlockX() - 1
							&& playerLocation.getX() < blockLocation.getBlockX() + 1
							&& playerLocation.getZ() > blockLocation.getBlockZ() - 1
							&& playerLocation.getZ() < blockLocation.getBlockZ() + 1
							&& playerLocation.getY() > blockLocation.getBlockY() - 2
							&& playerLocation.getY() < blockLocation.getBlockY() + 1)
					{
						if (p.getPlayerData(target).isSpectating())
						{
							allowed = true;
						}
						else
						{
							allowed = false;
							break;
						}
					}

				}
			}

			ev.setBuildable(allowed);
		}
	}

	/**
	 * Used to prevent spectators from placing blocks, and to teleport spectators blocking players
	 * from placing blocks.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onBlockPlace(final BlockPlaceEvent ev)
	{
		if (p.getPlayerData(ev.getPlayer()).isSpectating())
		{
			ev.setCancelled(true);
			return;
		}

		// Get location of the block that is going to be placed
		Location blockL = ev.getBlock().getLocation();

		for (Player target : p.getServer().getOnlinePlayers())
		{
			// Player spectating & in same world?
			if (p.getPlayerData(target).isSpectating() && target.getWorld().equals(ev.getBlock().getWorld()))
			{
				Location playerL = target.getLocation();

				// Is this player at the location of the block being placed?
				if (playerL.getX() > blockL.getBlockX() - 1
						&& playerL.getX() < blockL.getBlockX() + 1
						&& playerL.getZ() > blockL.getBlockZ() - 1
						&& playerL.getZ() < blockL.getBlockZ() + 1
						&& playerL.getY() > blockL.getBlockY() - 2
						&& playerL.getY() < blockL.getBlockY() + 1)
				{
					// The location of the player placing the block is a safe location
					target.teleport(ev.getPlayer(), PlayerTeleportEvent.TeleportCause.PLUGIN);
					SpectatorPlus.get().sendMessage(target, "You were teleported away from a placed block.");
				}
			}
		}
	}

	/**
	 * Used to prevent spectators from breaking blocks.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onBlockBreak(final BlockBreakEvent ev)
	{
		if (p.getPlayerData(ev.getPlayer()).isSpectating())
		{
			ev.setCancelled(true);
		}
	}



	/* **  Entities-related  ** */

	/**
	 * Cancels any damage taken or caused by a spectator.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onEntityDamageEvent(final EntityDamageByEntityEvent e)
	{
		// Manages spectators damaging players
		if (e.getDamager() instanceof Player && e.getEntity() instanceof Player)
		{
			if ((!e.getDamager().hasMetadata("NPC") && p.getPlayerData(((Player) e.getDamager())).isSpectating()) || (!e.getEntity().hasMetadata("NPC") && p.getPlayerData(((Player) e.getEntity())).isSpectating()))
			{
				e.setCancelled(true);
			}
		}

		// Manage spectators damaging mobs
		else if (!(e.getEntity() instanceof Player) && e.getDamager() instanceof Player)
		{
			if (!e.getDamager().hasMetadata("NPC") && p.getPlayerData(((Player) e.getDamager())).isSpectating())
			{
				e.setCancelled(true);
			}
		}

		// Manage mobs damaging spectators
		else if (e.getEntity() instanceof Player)
		{
			if (!e.getEntity().hasMetadata("NPC") && p.getPlayerData(((Player) e.getEntity())).isSpectating())
			{
				e.setCancelled(true);
			}
		}

		// Otherwise both entities are mobs, ignore the event.
	}

	/**
	 * Makes non-potions projectiles fly through the spectators.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onEntityHitByProjectileEvent(final EntityDamageByEntityEvent ev)
	{
		if (ev.getDamager() instanceof Projectile
				&& !(ev.getDamager() instanceof ThrownPotion) // splash potions are cancelled in PotionSplashEvent
				&& ev.getEntity() instanceof Player
				&& !ev.getEntity().hasMetadata("NPC") // Check for NPC's, as they are instances of Players sometimes...
				&& p.getPlayerData(((Player) ev.getEntity())).isSpectating())
		{

			ev.setCancelled(true);

			final Player spectatorInvolved = (Player) ev.getEntity();
			final boolean wasFlying = spectatorInvolved.isFlying();
			final Location initialSpectatorLocation = spectatorInvolved.getLocation();

			final Vector initialProjectileVelocity = ev.getDamager().getVelocity();
			final Location initialProjectileLocation = ev.getDamager().getLocation();

			spectatorInvolved.setFlying(true);
			spectatorInvolved.teleport(initialSpectatorLocation.clone().add(0, 6, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);

			// Prevents the arrow from bouncing on the entity
			Bukkit.getScheduler().runTaskLater(p, new Runnable()
			{
				@Override
				public void run()
				{
					ev.getDamager().teleport(initialProjectileLocation);
					ev.getDamager().setVelocity(initialProjectileVelocity);
				}
			}, 1L);

			// Teleports back the spectator
			Bukkit.getScheduler().runTaskLater(p, new Runnable()
			{
				@Override
				public void run()
				{
					spectatorInvolved.teleport(initialSpectatorLocation.setDirection(spectatorInvolved.getLocation().getDirection()), PlayerTeleportEvent.TeleportCause.PLUGIN);
					spectatorInvolved.setFlying(wasFlying);
				}
			}, 5L);
		}
	}

	/**
	 * Used to make splash potions flew by the spectators.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPotionSplash(final PotionSplashEvent ev)
	{
		final ArrayList<UUID> spectatorsAffected = new ArrayList<>();

		for (LivingEntity player : ev.getAffectedEntities())
		{
			if (player instanceof Player && !player.hasMetadata("NPC") && p.getPlayerData(((Player) player)).isSpectating())
			{
				spectatorsAffected.add(player.getUniqueId());
			}
		}

		// If there isn't any spectator affected, it's a splash on players only and the spectators cannot
		// affect the behavior of the potion.
		// So, in this case, we don't care about the event.
		if (!spectatorsAffected.isEmpty())
		{

			// If there are some spectators involved, we try to find how they are involved.
			// If all the spectators involved are far away from the impact point, there isn't
			// any needed action.
			// Else, if a spectator is the impact point, he perturbed the launch of the potion, and
			// the same thing is done as for the non-potions projectiles (teleport the spectators up, etc.).
			// In all cases, the effect is removed from the spectators.

			Boolean teleportationNeeded = false;

			for (Entity entity : ev.getEntity().getNearbyEntities(2, 2, 2))
			{
				if (entity instanceof Player && !entity.hasMetadata("NPC") && p.getPlayerData(((Player) entity)).isSpectating())
				{
					// The potion hits a spectator
					teleportationNeeded = true;
				}
			}

			final HashMap<UUID, Boolean> oldFlyMode = new HashMap<>();

			for (UUID spectatorUUID : spectatorsAffected)
			{

				Player spectator = p.getServer().getPlayer(spectatorUUID);

				// The effect is removed
				ev.setIntensity(spectator, 0);

				if (teleportationNeeded)
				{
					oldFlyMode.put(spectator.getUniqueId(), spectator.isFlying());
					spectator.setFlying(true);

					// High teleportation because the potions can be thrown up
					spectator.teleport(spectator.getLocation().add(0, 10, 0));
				}
			}

			if (teleportationNeeded)
			{

				final Location initialProjectileLocation = ev.getEntity().getLocation();
				final Vector initialProjectileVelocity = ev.getEntity().getVelocity();

				// Prevents the potion from splashing on the entity
				p.getServer().getScheduler().runTaskLater(p, new Runnable()
				{
					@Override
					public void run()
					{
						// Because the original entity is, one tick later, destroyed, we need to spawn a new one
						// Cancelling the event only cancels the effect.
						ThrownPotion clonedEntity = (ThrownPotion) ev.getEntity().getWorld().spawnEntity(initialProjectileLocation, ev.getEntity().getType());

						// For other plugins (may be used)
						clonedEntity.setShooter(ev.getEntity().getShooter());
						clonedEntity.setTicksLived(ev.getEntity().getTicksLived());
						clonedEntity.setFallDistance(ev.getEntity().getFallDistance());
						clonedEntity.setBounce(ev.getEntity().doesBounce());
						if (ev.getEntity().getPassenger() != null)
						{
							clonedEntity.setPassenger(ev.getEntity().getPassenger()); // hey, why not
						}

						// Clones the effects
						clonedEntity.setItem(ev.getEntity().getItem());

						// Clones the speed/direction
						clonedEntity.setVelocity(initialProjectileVelocity);

						// Just in case
						ev.getEntity().remove();
					}
				}, 1L);

				// Teleports back the spectators
				p.getServer().getScheduler().runTaskLater(p, new Runnable()
				{
					@Override
					public void run()
					{
						for (UUID spectatorUUID : spectatorsAffected)
						{
							Player spectator = p.getServer().getPlayer(spectatorUUID);

							spectator.teleport(spectator.getLocation().add(0, -10, 0));
							spectator.setFlying(oldFlyMode.get(spectatorUUID));
						}
					}
				}, 5L);

				// Cancels the effect for everyone (because the thrown potion is re-spawned,
				// avoids a double effect for some players).
				ev.setCancelled(true);

				// Side note: there is a visual glitch (the players will see a double splash,
				// the real one plus the splash on the spectator), but the behavior is preserved and
				// the effect is applied once, on the players.
			}
		}
	}

	/**
	 * Used to prevent the mobs to be interested by (and aggressive against) spectators.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onEntityTarget(final EntityTargetEvent ev)
	{
		// Check to make sure it isn't an NPC
		if (ev.getTarget() != null && ev.getTarget() instanceof Player && !ev.getTarget().hasMetadata("NPC") && p.getPlayerData(((Player) ev.getTarget())).isSpectating())
		{
			ev.setCancelled(true);
		}
	}

	/**
	 * Used to prevent players & mobs from damaging spectators, and stop the fire display when a
	 * spectator go out of a fire/lava block.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onEntityDamage(final EntityDamageEvent ev)
	{
		// Check to make sure it isn't an NPC
		if (ev.getEntity() instanceof Player && !ev.getEntity().hasMetadata("NPC") && p.getPlayerData((Player) ev.getEntity()).isSpectating())
		{
			ev.setCancelled(true);
			ev.getEntity().setFireTicks(0);
		}
	}

	/**
	 * Used to prevent any interaction on entities
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerInteractEntity(final PlayerInteractEntityEvent ev)
	{
		if (p.getPlayerData(ev.getPlayer()).isSpectating() && ev.getRightClicked() instanceof Player && !ev.getRightClicked().hasMetadata("NPC"))
		{
			ev.setCancelled(true);
		}
	}

	/**
	 * Used to prevent any interaction on blocks (item frames, buttons, levers, pressure plates...).
	 * If the Skript integration is enabled, the event is not cancelled and Skript users will have
	 * to cancel the event.
	 *
	 * See https://github.com/pgmann/SpectatorPlus/pull/38 .
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerInteract(final PlayerInteractEvent ev)
	{
		if (p.getPlayerData(ev.getPlayer()).isSpectating() && !Toggles.SKRIPT_INTEGRATION.get())
		{
			ev.setCancelled(true);
		}
	}



	/* **  Drop- & pickup-related  ** */


	/**
	 * Used to prevent spectators from dropping items on ground.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerDropItem(final PlayerDropItemEvent ev)
	{
		if (p.getPlayerData(ev.getPlayer()).isSpectating())
		{
			ev.setCancelled(true);
		}
	}

	/**
	 * Used to prevent spectators from picking up items.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerPickupItem(final PlayerPickupItemEvent ev)
	{
		if (p.getPlayerData(ev.getPlayer()).isSpectating())
		{
			ev.setCancelled(true);
		}
	}



	/* **  Doors-related  ** */

	/**
	 * Cancel the use of the doors, trapdoors, etc.
	 *
	 * This event is not directly cancelled as the cancellation is part of the {@link
	 * #onPlayerInteract(PlayerInteractEvent)} event handler.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerUseDoor(final PlayerInteractEvent ev)
	{
		if (p.getPlayerData(ev.getPlayer()).isSpectating() && ev.hasBlock())
		{
			final Material clickedType = ev.getClickedBlock().getType();

			// Allows spectators to pass through doors.
			if (clickedType == Material.WOODEN_DOOR || clickedType == Material.IRON_DOOR_BLOCK || clickedType == Material.FENCE_GATE)
			{
				Player spectator = ev.getPlayer();
				Location doorLocation = ev.getClickedBlock()
						.getLocation()
						.setDirection(spectator.getLocation().getDirection());

				int relativeHeight = 0;
				if (clickedType == Material.WOODEN_DOOR || clickedType == Material.IRON_DOOR_BLOCK)
				{
					Material belowBlockType = ev.getClickedBlock()
							.getLocation().add(0, -1, 0)
							.getBlock().getType();

					if (belowBlockType == Material.WOODEN_DOOR || belowBlockType == Material.IRON_DOOR_BLOCK)
					{
						// The spectator clicked the top part of the door.
						relativeHeight = -1;
					}
				}

					/*
					 * North: small Z
					 * South: big Z
					 * East:  big X
					 * West:  small X
					 */
				switch (ev.getBlockFace())
				{
					case EAST:
						spectator.teleport(doorLocation.add(-0.5, relativeHeight, 0.5), PlayerTeleportEvent.TeleportCause.PLUGIN);
						break;

					case NORTH:
						spectator.teleport(doorLocation.add(0.5, relativeHeight, 1.5), PlayerTeleportEvent.TeleportCause.PLUGIN);
						break;

					case SOUTH:
						spectator.teleport(doorLocation.add(0.5, relativeHeight, -0.5), PlayerTeleportEvent.TeleportCause.PLUGIN);
						break;

					case WEST:
						spectator.teleport(doorLocation.add(1.5, relativeHeight, 0.5), PlayerTeleportEvent.TeleportCause.PLUGIN);
						break;

					case UP:
						// If it's a fence gate, we uses the relative position of the player and the
						// gate.
						if (ev.getClickedBlock().getState().getData() instanceof Gate)
						{
							Gate fenceGate = (Gate) ev.getClickedBlock().getState().getData();
							// The BlockFace represents the block in the direction of the "line" of
							// the gate. So we needs to invert the relative teleportation.
							switch (fenceGate.getFacing())
							{
								case NORTH:
								case SOUTH:
									if (spectator.getLocation().getX() > doorLocation.getX())
									{
										spectator.teleport(doorLocation.add(-0.5, relativeHeight, 0.5), PlayerTeleportEvent.TeleportCause.PLUGIN);
									}
									else
									{
										spectator.teleport(doorLocation.add(1.5, relativeHeight, 0.5), PlayerTeleportEvent.TeleportCause.PLUGIN);
									}
									break;

								case EAST:
								case WEST:
									if (spectator.getLocation().getZ() > doorLocation.getZ())
									{
										spectator.teleport(doorLocation.add(0.5, relativeHeight, -0.5), PlayerTeleportEvent.TeleportCause.PLUGIN);
									}
									else
									{
										spectator.teleport(doorLocation.add(0.5, relativeHeight, 1.5), PlayerTeleportEvent.TeleportCause.PLUGIN);
									}
									break;
							}
						}

						break;
				}

			}

			// Allows spectators to pass through trap doors
			else if (clickedType == Material.TRAP_DOOR)
			{
				if (!((TrapDoor) ev.getClickedBlock().getState().getData()).isOpen())
				{
					Player spectator = ev.getPlayer();
					Location doorLocation = ev.getClickedBlock()
							.getLocation()
							.setDirection(spectator.getLocation().getDirection());

					switch (ev.getBlockFace())
					{
						case UP:
							spectator.teleport(doorLocation.add(0.5, -1, 0.5), PlayerTeleportEvent.TeleportCause.PLUGIN);
							break;

						case DOWN:
							spectator.teleport(doorLocation.add(0.5, 1, 0.5), PlayerTeleportEvent.TeleportCause.PLUGIN);
							break;

						default:
							break;
					}
				}
			}
		}
	}

	/**
	 * Cancels chest opening animation, doors, anything when the player right clicks.
	 *
	 * This event is not directly cancelled as the cancellation is part of the {@link
	 * #onPlayerInteract(PlayerInteractEvent)} event handler.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerUseContainer(final PlayerInteractEvent ev)
	{
		if (p.getPlayerData(ev.getPlayer()).isSpectating() && ev.hasBlock())
		{
			if (ev.getClickedBlock().getState() instanceof InventoryHolder)
			{
				Inventory original = ((InventoryHolder) ev.getClickedBlock().getState()).getInventory();

				// We use an InventoryGUI if possible because they are live-updated. We cannot just
				// copy the inventory, because the content will not be updated, but we cannot just open
				// the inventory too, because this will execute the opening animation and sound to other
				// players.
				if (original.getType() == InventoryType.CHEST)
				{
					Gui.open(ev.getPlayer(), new InventoryGUI(original));
				}

				// The zLib's GUI API currently only supports chest inventories, so other ones
				// are opened directly: they will be live-updated by Minecraft directly and
				// don't have an opening animation.
				// The interactions are cancelled by other events, so the container will be read-only.
				else
				{
					ev.getPlayer().openInventory(original);
				}
			}
		}
	}



	/* **  Riding-related  ** */

	/**
	 * Stops spectators riding horses, Minecarts, etc.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onVehicleEnter(final VehicleEnterEvent e)
	{
		if (e.getEntered() instanceof Player && p.getPlayerData((Player) e.getEntered()).isSpectating())
		{
			e.setCancelled(true);
		}
	}
}
