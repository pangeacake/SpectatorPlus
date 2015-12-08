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
package com.pgcraft.spectatorplus;

import com.pgcraft.spectatorplus.arenas.Arena;
import com.pgcraft.spectatorplus.arenas.ArenasManager;
import com.pgcraft.spectatorplus.spectators.Spectator;
import com.pgcraft.spectatorplus.spectators.SpectatorsManager;
import fr.zcraft.zlib.components.configuration.Configuration;
import fr.zcraft.zlib.core.ZPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class SpectatorPlus extends ZPlugin
{
	public final static double VERSION = 3.0;

	private static SpectatorPlus instance;

	public final static String BASE_PREFIX = ChatColor.BLUE + "Spectator" + ChatColor.DARK_BLUE + "Plus";
	public final static String PREFIX = ChatColor.GOLD + "[" + BASE_PREFIX + ChatColor.GOLD + "] ";

	private SpectateAPI api;
	private SpectatorsManager spectatorsManager;
	private ArenasManager arenasManager;

	private Map<UUID, Spectator> spectators = new HashMap<>();


	@Override
	public void onLoad()
	{
		instance = this;

		// ZLib requirement
		super.onLoad();

		// Registers the Arena class as a serializable one.
		ConfigurationSerialization.registerClass(Arena.class);
	}

	@Override
	public void onEnable()
	{
		// Loading config
		Configuration.init(Toggles.class);

		// Loading managers
		spectatorsManager = new SpectatorsManager(this);
		arenasManager = new ArenasManager(this);

		api = new SpectateAPI(this);
	}



	/* **  Data methods  ** */

	/**
	 * Returns the object representing a player inside SpectatorPlus.
	 *
	 * @param id The player's UUID.
	 *
	 * @return The object. It is created on-the-fly if not already instanced, so this never returns
	 * {@code null}.
	 */
	public Spectator getPlayerData(UUID id)
	{
		Spectator spectator = spectators.get(id);

		if (spectator == null)
		{
			spectator = new Spectator(id);
			spectators.put(id, spectator);
		}

		return spectator;
	}

	/**
	 * Returns the object representing a player inside SpectatorPlus.
	 *
	 * @param player The player.
	 *
	 * @return The object. It is created on-the-fly if not already instanced, so this never returns
	 * {@code null}.
	 */
	public Spectator getPlayerData(Player player)
	{
		return getPlayerData(player.getUniqueId());
	}



	/* **  Notifications methods  ** */

	/**
	 * Sends a message to the payer if the messages are enabled in the config.
	 *
	 * @param message The message to be sent. It will be prefixed by the Spectator Plus prefix.
	 * @param force {@code true} to send the message even if messages are not enabled.
	 */
	public void sendMessage(CommandSender receiver, String message, boolean force)
	{
		if (receiver != null && (!(receiver instanceof Player) || Toggles.OUTPUT_MESSAGES.get() || force))
		{
			receiver.sendMessage(SpectatorPlus.PREFIX + message);
		}
	}

	/**
	 * Sends a message to the payer if the messages are enabled in the config.
	 *
	 * @param message The message to be sent. It will be prefixed by the Spectator Plus prefix.
	 */
	public void sendMessage(CommandSender receiver, String message)
	{
		sendMessage(receiver, message, false);
	}



	/* **  Accessors  ** */

	public SpectatorsManager getSpectatorsManager()
	{
		return spectatorsManager;
	}

	public ArenasManager getArenasManager()
	{
		return arenasManager;
	}

	public SpectateAPI getAPI()
	{
		return api;
	}

	public static SpectatorPlus get()
	{
		return instance;
	}
}
