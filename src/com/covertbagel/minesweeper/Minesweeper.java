/*
 * Minesweeper Plugin v0.3 by covertbagel for CraftBukkit 1000
 * 16 August 2011
 * Licensed Under GPLv3
 */

package com.covertbagel.minesweeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Minesweeper extends JavaPlugin {
	// map configs - mines and size
	private static final int[] SMALL = {10, 8};
	private static final int[] MEDIUM = {40, 16};
	private static final int[] LARGE = {90, 24};
	
	// used by map generator and placer
	private static final int CELL_NOT_SET = -2;
	private static final int CELL_IS_MINE = -1;
	
	// indexes for the map configs
	public static final int MINES = 0;
	public static final int SIZE = 1;
	
	public static final DyeColor[] PALETTE = {
		DyeColor.WHITE,
		DyeColor.LIGHT_BLUE,
		DyeColor.LIME,
		DyeColor.RED,
		DyeColor.BLUE,
		DyeColor.GREEN,
		DyeColor.PINK,
		DyeColor.CYAN,
		DyeColor.BLACK
	};
	
	private static final String NAME = Minesweeper.class.getSimpleName();
	private static final Logger LOG = Logger.getLogger(NAME);
	
	private static final String PERMISSION_DENIED = "Permission denied.";
	private static final String ARENA_NOT_FOUND = "Arena does not exist.";
	private static final String ARENA_FOUND = "Arena already exists.";
	private static final String ARENA_CREATED = "[%s] Arena %s created by %s.";
	private static final String ARENA_RESET = "[%s] Arena %s reset by %s.";
	private static final String ARENA_VITRIFIED = "[%s] Arena %s vitrified by %s.";
	private static final String COMMAND_LIST =
		"/minesweeper - shows this help; " +
		"/ms-arena-create name (small|medium|large) - create arena; " +
		"/ms-arena-reset name - reset arena; " +
		"/ms-arena-vitrify name - vitrify arena; " +
		"/ms-arena-tp name - teleport to arena; " +
		"/ms-game-create (small|medium|large) - create normal game; " +
		"/ms-items - give yourself items to play Minesweeper";
	
	private GameBlockListener blockListener;
	private List<MapArea> mapAreas = new ArrayList<MapArea>();
	private HashMap<String, MapArea> arenas = new HashMap<String, MapArea>();
	
	@Override
	public void onEnable() {
		// setup block listener
		blockListener = new GameBlockListener(this);
		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACE, blockListener, Priority.Normal, this);
		LOG.info(NAME + " Plugin enabled");
	}
	
	@Override
	public void onDisable() {
		LOG.info(NAME + " Plugin disabled");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
		final String commandName = command.getName();
		if (commandName.equalsIgnoreCase(NAME)) {
			sender.sendMessage(COMMAND_LIST);
			return true;
		} else if (commandName.equalsIgnoreCase("ms-game-create")) {
			if (sender.hasPermission("minesweeper.game.create")) {
				return gameCreate(sender, arguments);
			}
			return permissionDenied(sender);
		} else if (commandName.equalsIgnoreCase("ms-arena-create")) {
			if (sender.hasPermission("minesweeper.arena.create")) {
				return arenaCreate(sender, arguments);
			}
			return permissionDenied(sender);
		} else if (commandName.equalsIgnoreCase("ms-arena-reset")) {
			if (sender.hasPermission("minesweeper.arena.reset")) {
				return arenaReset(sender, arguments);
			}
			return permissionDenied(sender);
		} else if (commandName.equalsIgnoreCase("ms-arena-vitrify")) {
			if (sender.hasPermission("minesweeper.arena.vitrify")) {
				return arenaVitrify(sender, arguments);
			}
			return permissionDenied(sender);
		} else if (commandName.equalsIgnoreCase("ms-arena-tp")) {
			if (sender.hasPermission("minesweeper.arena.teleport")) {
				return arenaTeleport(sender, arguments);
			}
			return permissionDenied(sender);
		} else if (commandName.equalsIgnoreCase("ms-items")) {
			if (givePlayerStuff((Player) sender)) {
				return true;
			}
			return permissionDenied(sender);
		}
		return false;
	}
	
	public List<MapArea> getMapAreas() {
		return mapAreas;
	}
	
	private boolean gameCreate(CommandSender sender, String[] arguments) {
		// parse arguments
		if (arguments.length != 1) {
			return false;
		}
		final int[] info = parseSize(arguments[0]);
		if (info == null) {
			return false;
		}
		
		// get player location
		final Player player = (Player) sender;
		final Location location = player.getLocation();
		
		// save map area for the block listener to use later
		mapAreas.add(new MapArea(location, info, false));
		
		// setup area
		setupArea(Material.STONE, location, info);
		
		// give player some normal and redstone torches
		givePlayerStuff(player);
		
		return true;
	}
	
	private boolean arenaCreate(CommandSender sender, String[] arguments) {
		// parse arguments
		if (arguments.length != 2) {
			return false;
		}
		final String name = arguments[0];
		final int[] info = parseSize(arguments[1]);
		if (name == null || name.length() == 0 || info == null) {
			return false;
		}
		
		// try to look for arena with this name
		final MapArea arena = arenas.get(name);
		if (arena != null) {
			sender.sendMessage(ARENA_FOUND);
			return true;
		}
		
		// get player location
		final Player player = (Player) sender;
		final Location location = player.getLocation();
		
		// save map area for the block listener to use later
		final MapArea mapArea = new MapArea(location.clone(), info, true);
		arenas.put(name, mapArea);
		mapAreas.add(mapArea);
		
		// make big block of obsidian to enclose the arena in
		for (int i = -PALETTE.length + 1; i < 7; i++) {
			placeLayer(location.clone(), i, info[Minesweeper.SIZE] + 4, Material.OBSIDIAN.getId(), (byte) 0);
		}
		
		// setup area
		setupArea(Material.OBSIDIAN, location, info);
		
		// give player some normal and redstone torches
		givePlayerStuff(player);
		
		sender.getServer().broadcastMessage(String.format(ARENA_CREATED, NAME, name, player.getName()));
		return true;
	}
	
	private boolean arenaReset(CommandSender sender, String[] arguments) {
		// parse arguments
		if (arguments.length != 1) {
			return false;
		}
		final String name = arguments[0];
		if (name == null || name.length() == 0) {
			return false;
		}
		
		// look for arena with this name
		final MapArea arena = arenas.get(name);
		if (arena == null) {
			sender.sendMessage(ARENA_NOT_FOUND);
			return true;
		}
		
		// setup area
		setupArea(Material.OBSIDIAN, arena.getLocation().clone(), arena.getInfo());
		
		// give player some normal and redstone torches
		final Player player = (Player) sender;
		givePlayerStuff(player);
		
		sender.getServer().broadcastMessage(String.format(ARENA_RESET, NAME, name, player.getName()));
		return true;
	}
	
	private boolean arenaVitrify(CommandSender sender, String[] arguments) {
		// parse arguments
		if (arguments.length != 1) {
			return false;
		}
		final String name = arguments[0];
		if (name == null || name.length() == 0) {
			return false;
		}
		
		// look for arena with this name
		final MapArea arena = arenas.get(name);
		if (arena == null) {
			sender.sendMessage(ARENA_NOT_FOUND);
			return true;
		}
		
		// remove this arena from the arenas list
		arenas.remove(name);
		
		// modify it so it will be removed from mapAreas list after explosion
		arena.clearLocation();
		
		sender.getServer().broadcastMessage(String.format(ARENA_VITRIFIED, NAME, name, ((Player) sender).getName()));
		return true;
	}
	
	private boolean arenaTeleport(CommandSender sender, String[] arguments) {
		// parse arguments
		if (arguments.length != 1) {
			return false;
		}
		final String name = arguments[0];
		if (name == null || name.length() == 0) {
			return false;
		}
		
		// look for arena with this name
		final MapArea arena = arenas.get(name);
		if (arena == null) {
			sender.sendMessage(ARENA_NOT_FOUND);
			return true;
		}
		
		// move player to location
		final Player player = (Player) sender;
		player.teleport(arena.getLocation());
		
		// give player some normal and redstone torches
		givePlayerStuff(player);
		
		return true;
	}
	
	private static boolean permissionDenied(CommandSender sender) {
		sender.sendMessage(PERMISSION_DENIED);
		return true;
	}
	
	public static void setupArea(Material material, Location location, int[] info) {
		// place lots of the specified material below
		for (int i = 0; i < 6; i++) {
			placeLayer(location.clone(), i, info[Minesweeper.SIZE] + 4, material.getId(), (byte) 0);
		}
		
		// place lots of air above
		for (int i = 0; i < Minesweeper.PALETTE.length; i++) {
			placeLayer(location.clone(), -i, info[Minesweeper.SIZE] + 2, Material.AIR.getId(), (byte) 0);
		}
		
		// place torches around the outside of the area
		placeLayer(location.clone(), 0, info[SIZE] + 2, Material.TORCH.getId(), (byte) 5);
		placeLayer(location.clone(), 0, info[SIZE], Material.AIR.getId(), (byte) 0);
		
		// place bottom layer of TNT
		placeLayer(location.clone(), 3, info[Minesweeper.SIZE], Material.TNT.getId(), (byte) 0);
		
		// generate and place random map
		final int[] map = generateMap(info[Minesweeper.MINES], info[Minesweeper.SIZE]);
		placeMap(location.clone(), 2, info[Minesweeper.SIZE], map);
		
		// place top layer of sand
		placeLayer(location.clone(), 1, info[Minesweeper.SIZE], Material.SAND.getId(), (byte) 0);
		
		// place wool blocks and signs at corners to identify colors
		placePalettes(location, info[Minesweeper.SIZE]);
	}
	
	public static void placePalettes(Location location, int size) {
		size += 2;
		location.setX(location.getX() - size / 2);
		location.setZ(location.getZ() - size / 2);
		placePalette(location.clone());
		location.setX(location.getX() + size - 1);
		placePalette(location.clone());
		location.setZ(location.getZ() + size - 1);
		placePalette(location.clone());
		location.setX(location.getX() - size + 1);
		placePalette(location);
	}
	
	public static void placePalette(Location location) {
		Block block;
		for (int i = 0; i < Minesweeper.PALETTE.length; i++) {
			block = location.getBlock();
			block.setType(Material.WOOL);
			block.setData(Minesweeper.PALETTE[i].getData());
			location.setY(location.getY() + 1);
		}
	}
	
	public static void placeMap(Location location, int depth, int size, int[] map) {
		int i, j, k;
		Block block;
		location.setY(location.getY() - depth);
		location.setX(location.getX() - size / 2);
		location.setZ(location.getZ() - size / 2);
		for (i = 0; i < size; i++) {
			for (j = 0; j < size; j++) {
				k = map[i * size + j];
				if (k == CELL_IS_MINE) {
					location.getBlock().setType(Material.TNT);
				} else if (k >= 0 && k <= 8) {
					block = location.getBlock();
					block.setType(Material.WOOL);
					block.setData(Minesweeper.PALETTE[k].getData());
				}
				location.setX(location.getX() + 1);
			}
			location.setX(location.getX() - size);
			location.setZ(location.getZ() + 1);
		}
	}
	
	public static void placeLayer(Location location, int depth, int size, int typeid, byte data) {
		location.setY(location.getY() - depth);
		location.setX(location.getX() - size / 2);
		location.setZ(location.getZ() - size / 2);
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				location.getBlock().setTypeIdAndData(typeid, data, false);
				location.setX(location.getX() + 1);
			}
			location.setX(location.getX() - size);
			location.setZ(location.getZ() + 1);
		}
	}
	
	public static int[] parseSize(String size) {
		if (size.equalsIgnoreCase("small")) {
			return SMALL;
		} else if (size.equalsIgnoreCase("medium")) {
			return MEDIUM;
		} else if (size.equalsIgnoreCase("large")) {
			return LARGE;
		}
		return null;
	}
	
	public static int[] generateMap(int mines, int size) {
		// create and initialize array
		int i, j, k;
		int[] map = new int[size * size];
		for (i = 0; i < map.length; i++) {
			map[i] = CELL_NOT_SET;
		}
		
		// add random mines
		for (i = 0; i < mines; i++) {
			do {
				j = (int) (Math.random() * map.length);
			} while (map[j] != CELL_NOT_SET);
			map[j] = CELL_IS_MINE;
		}
		
		// set other values
		for (i = 0; i < size; i++) {
			for (j = 0; j < size; j++) {
				k = i * size + j;
				if (map[k] == CELL_NOT_SET) {
					map[k] = 0;
					if (i > 0 && map[(i - 1) * size + j] == CELL_IS_MINE) {
						map[k]++;
					}
					if (i < size - 1 && map[(i + 1) * size + j] == CELL_IS_MINE) {
						map[k]++;
					}
					if (j > 0 && map[i * size + j - 1] == CELL_IS_MINE) {
						map[k]++;
					}
					if (j < size - 1 && map[i * size + j + 1] == CELL_IS_MINE) {
						map[k]++;
					}
					if (i > 0 && j > 0 && map[(i - 1) * size + j - 1] == CELL_IS_MINE) {
						map[k]++;
					}
					if (i > 0 && j < size - 1 && map[(i - 1) * size + j + 1] == CELL_IS_MINE) {
						map[k]++;
					}
					if (i < size - 1 && j > 0 && map[(i + 1) * size + j - 1] == CELL_IS_MINE) {
						map[k]++;
					}
					if (i < size - 1 && j < size - 1 && map[(i + 1) * size + j + 1] == CELL_IS_MINE) {
						map[k]++;
					}
				}
			}
		}
		
		return map;
	}
	
	private static boolean givePlayerStuff(Player player) {
		if (player.hasPermission("minesweeper.items")) {
			Inventory inventory = player.getInventory();
			inventory.addItem(new ItemStack(Material.REDSTONE_TORCH_ON, 64));
			inventory.addItem(new ItemStack(Material.TORCH, 64));
			return true;
		}
		return false;
	}
}