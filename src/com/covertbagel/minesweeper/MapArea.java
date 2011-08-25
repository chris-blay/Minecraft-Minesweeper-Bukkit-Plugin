/*
 * Minesweeper Plugin v0.3 by covertbagel for CraftBukkit 1000
 * 16 August 2011
 * Licensed Under GPLv3
 */

package com.covertbagel.minesweeper;

import org.bukkit.Location;

public class MapArea {
	private Location location = null;
	final private int y;
	final private double maxX, minX, maxZ, minZ;
	final private int[] info;
	
	public MapArea(Location location, int[] info, boolean rememberLocation) {
		final int size = info[Minesweeper.SIZE];
		final int x = (int) Math.floor(location.getX());
		final int z = (int) Math.floor(location.getZ());
		if (rememberLocation) {
			this.location = location;
		}
		this.y = (int) Math.floor(location.getY());
		this.maxX = x + size / 2;
		this.minX = x - size / 2;
		this.maxZ = z + size / 2;
		this.minZ = z - size / 2;
		this.info = info;
	}
	
	public boolean inArea(double x, double y, double z) {
		x = Math.floor(x);
		y = Math.floor(y);
		z = Math.floor(z);
		return (
			this.y == y &&
			this.maxX >= x &&
			this.minX <= x &&
			this.maxZ >= z &&
			this.minZ <= z
		);
	}
	
	public Location getLocation() {
		return this.location;
	}
	
	public void clearLocation() {
		this.location = null;
	}
	
	public int[] getInfo() {
		return this.info;
	}
}
