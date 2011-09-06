/*
 * Minesweeper Plugin v0.4 by covertbagel for CraftBukkit 1060
 * 5 September 2011
 * Licensed Under GPLv3
 */

package com.covertbagel.minesweeper;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class MapArea {
	private Location location = null;
	private String name = null;
	private int remainingBlocks;
	final private int y;
	final private double maxX, minX, maxZ, minZ;
	final private int[] info;
	
	public MapArea(final Location location, final String name, final int[] info) {
		final int size = info[Minesweeper.SIZE];
		final int x = (int) Math.floor(location.getX());
		final int z = (int) Math.floor(location.getZ());
		if (name != null) {
			this.location = location;
			this.name = name;
		}
		this.y = (int) Math.floor(location.getY());
		this.maxX = (x + size / 2) + 1;
		this.minX = (x - size / 2) - 2;
		this.maxZ = (z + size / 2) + 1;
		this.minZ = (z - size / 2) - 2;
		this.info = info;
		reset();
	}
	
	public boolean inArea(final Block block) {
		final int x = (int) Math.floor(block.getX());
		final int y = (int) Math.floor(block.getY());
		final int z = (int) Math.floor(block.getZ());
		return (
			this.y + 9 > y && // this is the 'height above' distance
			this.y - 7 < y && // this is the 'height below' distance
			this.maxX >= x &&
			this.minX <= x &&
			this.maxZ >= z &&
			this.minZ <= z
		);
	}
	
	public Location getLocation() {
		return this.location;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int[] getInfo() {
		return this.info;
	}
	
	public void vitrify() {
		this.location = null;
		this.name = null;
	}
	
	public void reset() {
		this.remainingBlocks = info[Minesweeper.SIZE] * info[Minesweeper.SIZE] - info[Minesweeper.MINES];
	}
	
	public int blockCleared() {
		return --remainingBlocks;
	}
	
	public boolean isClear() {
		return remainingBlocks == 0;
	}
	
	public boolean isArena() {
		return this.location != null;
	}
}