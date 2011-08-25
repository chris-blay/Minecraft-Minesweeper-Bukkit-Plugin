/*
 * Minesweeper Plugin v0.3 by covertbagel for CraftBukkit 1000
 * 16 August 2011
 * Licensed Under GPLv3
 */

package com.covertbagel.minesweeper;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

public class GameBlockListener extends BlockListener {
	public final Minesweeper plugin;
	
	public GameBlockListener(Minesweeper instance) {
		super();
		plugin = instance;
	}
	
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		
		// get list of map areas
		final List<MapArea> mapAreas = plugin.getMapAreas();
		if (mapAreas != null) {
			for (MapArea mapArea : mapAreas) {
				if (mapArea.inArea(block.getX(), block.getY(), block.getZ())) {
					if (block.getType() == Material.REDSTONE_TORCH_ON) {
						// ok so a redstone torch has been placed
						// by the player in one of his/her map areas
						// now let's make sure the block underneath is sand
						Location location = block.getLocation();
						location.setY(location.getY() - 1);
						block = location.getBlock();
						if (block.getType() == Material.SAND) {
							// now just need remove the sand
							block.setType(Material.AIR);
							
							// and check for what's underneath
							location.setY(location.getY() - 1);
							block = location.getBlock();
							if (block.getType() == Material.TNT) {
								// ok i guess we need to blow this thing up
								// i'll start by getting rid of this TNT
								block.setType(Material.AIR);
								
								// and then i'll spawn a primed tnt entity
								TNTPrimed tnt = block.getWorld().spawn(location, TNTPrimed.class);
								tnt.setFuseTicks(0);
								
								// let's remove this map area from the list since
								// it's not going to be around anymore (if it isn't an arena)
								if (mapArea.getLocation() == null) {
									mapAreas.remove(mapArea);
								}
							} else if (block.getType() == Material.WOOL && block.getData() == Minesweeper.PALETTE[0].getData()) {
								// let's uncover all blocks adjacent to the wool
								clearAdjacentBlocks(location);
							}
						}
					}
					break;
				}
			}
		}
	}
	
	private void clearAdjacentBlocks(Location location) {
		int i;
		final Location loc = location.clone();
		
		// go to 'top left'
		loc.setX(loc.getX() + 1);
		loc.setZ(loc.getZ() + 1);
		clearAbove(loc);
		
		// go to 'top center' and 'top right'
		for (i = 0; i < 2; i++) {
			loc.setZ(loc.getZ() - 1);
			clearAbove(loc);
		}
		
		// go to 'middle right' and 'bottom right'
		for (i = 0; i < 2; i++) {
			loc.setX(loc.getX() - 1);
			clearAbove(loc);
		}
		
		// go to 'bottom center' and 'bottom left'
		for (i = 0; i < 2; i++) {
			loc.setZ(loc.getZ() + 1);
			clearAbove(loc);
		}
		
		// go to 'middle left'
		loc.setX(loc.getX() + 1);
		clearAbove(loc);
	}
	
	private void clearAbove(Location location) {
		// make sure bottom block is wool
		final Block bottom = location.getBlock();
		if (bottom.getType() != Material.WOOL) {
			return;
		}
		
		// make sure top block is sand
		final Location loc = location.clone();
		loc.setY(loc.getY() + 1);
		final Block top = loc.getBlock();
		if (top.getType() != Material.SAND) {
			return;
		}
		
		// clear the sand
		top.setType(Material.AIR);
		
		// also check if this location should have adjacent blocks cleared
		if (bottom.getData() == Minesweeper.PALETTE[0].getData()) {
			clearAdjacentBlocks(location);
		}
	}
}
