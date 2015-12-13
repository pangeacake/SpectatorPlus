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

import com.pgcraft.spectatorplus.Permissions;
import com.pgcraft.spectatorplus.SpectatorPlus;
import com.pgcraft.spectatorplus.Toggles;
import com.pgcraft.spectatorplus.spectators.Spectator;
import com.pgcraft.spectatorplus.tasks.AfterRespawnTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;


public class ServerActionsListener implements Listener
{
	private SpectatorPlus p;

	public ServerActionsListener()
	{
		p = SpectatorPlus.get();
	}


	/**
	 * - Hides player with the permission when they join;
	 * - hides the spectating players from the joining player;
	 * - re-enables the spectator mode if it was enabled before.
	 */
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(final PlayerJoinEvent ev)
	{
		if (Permissions.AUTO_HIDE_FROM_SPECTATORS.grantedTo(ev.getPlayer()))
		{
			p.getPlayerData(ev.getPlayer()).setHiddenFromTp(true);
		}

		for (Player target : p.getServer().getOnlinePlayers())
		{
			if (p.getPlayerData(target).isSpectating())
			{
				ev.getPlayer().hidePlayer(target);
			}
		}

		if (p.getSpectatorsManager().getSavedSpectatingPlayers().getConfig().contains(ev.getPlayer().getUniqueId().toString()))
		{
			p.getPlayerData(ev.getPlayer()).setSpectating(true, true);
		}
	}

	/**
	 * If the player was spectating:
	 * - the spectator mode is disabled, so the in-game inventory is saved;
	 * - the fact that this player was spectating is stored into a file, to
	 *   re-enable the spectator mode on his next join.
	 */
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(final PlayerQuitEvent ev)
	{
		final Spectator spectator = p.getPlayerData(ev.getPlayer());

		if (spectator.isSpectating())
		{
			spectator.setSpectating(false, true);
			spectator.saveSpectatorModeInFile(true);
		}
	}


	/**
	 * Saves the death message & location, if the “teleportation to the death point”
	 * tool is enabled.
	 */
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerDeath(final PlayerDeathEvent ev)
	{
		if (Toggles.TOOLS_TOOLS_TPTODEATH_ENABLED.get())
		{
			final Player killed = ev.getEntity();
			final Spectator spectator = p.getPlayerData(killed);

			spectator.setDeathLocation(killed.getLocation());

			if (Toggles.TOOLS_TOOLS_TPTODEATH_DISPLAYCAUSE.get())
			{
				String deathMessage = ChatColor.stripColor(ev.getDeathMessage());
				String noColorsDisplayName = ChatColor.stripColor(killed.getDisplayName());

				if (deathMessage == null)
				{
					deathMessage = "";
				}
				else
				{
					deathMessage = deathMessage.replace(killed.getName() + " was", "You were")
							.replace(killed.getName(), "You")
							.replace(noColorsDisplayName + " was", "You were")
							.replace(noColorsDisplayName, "You");
				}

				spectator.setLastDeathMessage(ChatColor.stripColor(deathMessage));
			}
		}
	}

	/**
	 * Used to enable the spectator mode for dead players, if this option is enabled
	 * in the config.
	 */
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerRespawn(final PlayerRespawnEvent ev)
	{
		if (Toggles.SPECTATOR_MODE_ON_DEATH.get())
		{
			// Prevent murdering clients! (force close bug if spec mode is enabled instantly)
			Bukkit.getScheduler().runTask(SpectatorPlus.get(), new AfterRespawnTask(ev.getPlayer()));
		}
	}


	/**
	 * Handles MultiverseInventories & other similar plugins.
	 *
	 * Disables spectate mode to restore proper inventory before world change; then
	 * re-enables spectate mode to restore spectator inventory after world change.
	 */
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldChange(final PlayerChangedWorldEvent ev)
	{
		final Spectator spectator = p.getPlayerData(ev.getPlayer());

		if (spectator.isSpectating())
		{
			spectator.setWasSpectatorBeforeWorldChanged(true);
			spectator.setSpectating(false, true);

			Bukkit.getScheduler().runTaskLater(p, new Runnable()
			{
				@Override
				public void run()
				{
					if (spectator.wasSpectatorBeforeWorldChanged())
					{
						spectator.setSpectating(true, false);
						spectator.setWasSpectatorBeforeWorldChanged(false);
					}
				}
			}, 5l);
		}
	}


	/**
	 * Used to prevent spectators from changing their gamemode whilst spectating.
	 */
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onGamemodeChange(final PlayerGameModeChangeEvent e)
	{
		final Spectator spectator = p.getPlayerData(e.getPlayer());

		if (spectator.isSpectating() && e.getNewGameMode() != GameMode.ADVENTURE && !spectator.isGamemodeChangeAllowed())
		{
			e.setCancelled(true);
			e.getPlayer().setAllowFlight(true);
		}
	}


	/**
	 * Used to prevent the food level to drop if the player is a spectator.
	 */
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onFoodLevelChange(final FoodLevelChangeEvent e)
	{
		if (e.getEntity() instanceof Player && !e.getEntity().hasMetadata("NPC") && p.getPlayerData((Player) e.getEntity()).isSpectating())
		{
			e.setCancelled(true);

			((Player) e.getEntity()).setFoodLevel(20);
			((Player) e.getEntity()).setSaturation(20);
		}
	}
}
