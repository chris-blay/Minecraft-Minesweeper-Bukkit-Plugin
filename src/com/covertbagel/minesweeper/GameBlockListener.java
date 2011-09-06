/*
 * Minesweeper Plugin v0.4 by covertbagel for CraftBukkit 1060
 * 5 Semptember 2011
 * Licensed Under GPLv3
 */

package com.covertbagel.minesweeper;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

public class GameBlockListener extends BlockListener {
	private final Minesweeper plugin;
	
	public GameBlockListener(Minesweeper instance) {
		super();
		plugin = instance;
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		final MapArea mapArea = findArea(block);
		if (mapArea != null) {
			// check player permission
			final Player player = event.getPlayer();
			if (!player.hasPermission("minesweeper.play")) {
				plugin.permissionDenied(player);
				event.setCancelled(true);
				return;
			}
			
			// switch on type of block broke
			switch (block.getType()) {
			case REDSTONE_TORCH_ON:
			case REDSTONE_TORCH_OFF:
			case TORCH:
				// these items are ok to have removed from an area
				break;
			default:
				// nothing else should be removed from an area
				plugin.permissionDenied(player);
				event.setCancelled(true);
			}
		}
	}
	
	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		final Block block = event.getBlock();
		final MapArea mapArea = findArea(block);
		if (mapArea != null) {
			// check player permission
			final Player player = event.getPlayer();
			if (!player.hasPermission("minesweeper.play")) {
				plugin.permissionDenied(player);
				event.setCancelled(true);
				return;
			}
			
			// switch on type of block placed
			switch (block.getType()) {
			case REDSTONE_TORCH_ON:
			case REDSTONE_TORCH_OFF:
				// ok so a redstone torch has been placed by a player
				// in one of the worlds map areas now let's see if
				// the block underneath is sand or wool
				final Location location = block.getLocation();
				location.setY(location.getY() - 1);
				switch (location.getBlock().getType()) {
				case SAND:
					// go down one more block so we can call clearAbove()
					location.setY(location.getY() - 1);
					clearAbove(location, mapArea);
					
					// check if the map is clear
					if (mapArea.isClear()) {
						plugin.getServer().broadcastMessage(String.format(Minesweeper.ARENA_WIN, plugin.NAME, mapArea.getName()));
					}
					break;
				case WOOL:
					// a redstone torch was placed directly on wool. let's
					// clear any unflagged blocks directly adjacent to this one
					clearAdjacentBlocks(location, mapArea);
				}
				break;
			case TORCH:
				// it's ok if normal torches are placed anywhere
				break;
			default:
				// nothing else should be placed in the area
				plugin.permissionDenied(player);
				event.setCancelled(true);
			}
		}
	}
	
	private void explodinate(final Location location, final MapArea mapArea) {
		// ok we guess we need to blow this thing up
		// we'll start by getting rid of this TNT
		final Block block = location.getBlock();
		block.setType(Material.AIR);
		
		// and then we'll spawn a primed tnt entity
		final TNTPrimed tnt = block.getWorld().spawn(location, TNTPrimed.class);
		tnt.setFuseTicks(0);
		
		// broadcast a message describing the sad fate
		// of this game/arena
		plugin.getServer().broadcastMessage(String.format(Minesweeper.ARENA_LOSE, plugin.NAME, mapArea.getName()));
		
		if (mapArea.isArena()) {
			// let's schedule an auto-reset for this arena
			plugin.delayedArenaReset(mapArea);
		} else {
			// let's remove this map area from the list. since
			// it isn't an arena since it's not going to
			// be around anymore
			plugin.getMapAreas().remove(mapArea);
		}
	}
	
	private MapArea findArea(final Block block) {
		final List<MapArea> mapAreas = plugin.getMapAreas();
		if (mapAreas != null) {
			for (MapArea mapArea : mapAreas) {
				if (mapArea.inArea(block)) {
					return mapArea;
				}
			}
		}
		return null;
	}
	
	private void clearAdjacentBlocks(final Location location, final MapArea mapArea) {
		int i;
		final Location loc = location.clone();
		
		// go to 'top left'
		loc.setX(loc.getX() + 1);
		loc.setZ(loc.getZ() + 1);
		clearAbove(loc, mapArea);
		
		// go to 'top center' and 'top right'
		for (i = 0; i < 2; i++) {
			loc.setZ(loc.getZ() - 1);
			clearAbove(loc, mapArea);
		}
		
		// go to 'middle right' and 'bottom right'
		for (i = 0; i < 2; i++) {
			loc.setX(loc.getX() - 1);
			clearAbove(loc, mapArea);
		}
		
		// go to 'bottom center' and 'bottom left'
		for (i = 0; i < 2; i++) {
			loc.setZ(loc.getZ() + 1);
			clearAbove(loc, mapArea);
		}
		
		// go to 'middle left'
		loc.setX(loc.getX() + 1);
		clearAbove(loc, mapArea);
	}
	
	private void clearAbove(final Location location, final MapArea mapArea) {
		// check type of block
		final Block bottom = location.getBlock();
		final Material bottomType = bottom.getType();
		if (bottomType != Material.WOOL && bottomType != Material.TNT) {
			return;
		}
		
		// make sure top block is sand
		final Location loc = location.clone();
		loc.setY(loc.getY() + 1);
		final Block top = loc.getBlock();
		if (top.getType() != Material.SAND) {
			return;
		}
		
		// only clear if block above sand is a redstone torch
		loc.setY(loc.getY() + 1);
		if (loc.getBlock().getType() == Material.TORCH) {
			return;
		}
		
		// now check if the bottom block is TNT
		if (bottomType == Material.TNT) {
			explodinate(location, mapArea);
		}
		
		// clear the sand
		top.setType(Material.AIR);
		mapArea.blockCleared();
		
		// also check if this location should have adjacent blocks cleared
		if (bottom.getData() == Minesweeper.PALETTE[0].getData()) {
			clearAdjacentBlocks(location, mapArea);
		}
	}
}