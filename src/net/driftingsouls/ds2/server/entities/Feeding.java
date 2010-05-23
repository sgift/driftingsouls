package net.driftingsouls.ds2.server.entities;

/**
 * A interface which states that the implementing class can feed people.
 * 
 * @author Drifting-Souls Team
 */
public interface Feeding 
{
	/**
	 * @return The id of the feeding object.
	 */
	public int getId();
	
	/**
	 * @return The current amount of food on the object.
	 */
	public long getNahrungCargo();
	
	/**
	 * Updates the amount of food on the object.
	 * 
	 * @param newFood The new amount of food.
	 */
	public void setNahrungCargo(long newFood);
}
