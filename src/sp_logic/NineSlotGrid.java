package sp_logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import genesis_event.HandlerRelay;
import genesis_util.Vector3D;

/**
 * This grid holds nine slots in it
 * @author Mikko Hilpinen
 * @since 8.8.2015
 */
public class NineSlotGrid
{
	// ATTRIBUTES	-----------------------
	
	private Slot[][] slots; // First x, then y
	private SudokuGrid grid;
	
	
	// CONSTRUCTOR	-----------------------
	
	/**
	 * Creates a new grid
	 * @param handlers The handlers that will handle the slots
	 * @param position The position of the grid
	 * @param dimensions The dimensions of the grid
	 * @param grid The grid that holds this grid
	 */
	public NineSlotGrid(HandlerRelay handlers, Vector3D position, Vector3D dimensions, 
			SudokuGrid grid)
	{
		this.slots = new Slot[3][3];
		this.grid = grid;
		Vector3D margins = dimensions.dividedBy(20);
		Vector3D slotDimensions = dimensions.minus(margins.times(3)).dividedBy(3);
		
		for (int x = 0; x < this.slots.length; x++)
		{
			for (int y = 0; y < this.slots[x].length; y++)
			{
				Vector3D slotPosition = position.plus(slotDimensions.plus(margins).times(
						new Vector3D(x, y)));
				this.slots[x][y] = new Slot(handlers, slotPosition, slotDimensions, this);
			}
		}
	}
	
	/**
	 * Creates a grid that is an exact copy of another grid
	 * @param other The grid this one is copied from
	 * @param grid The grid that holds the copied grid
	 */
	public NineSlotGrid(NineSlotGrid other, SudokuGrid grid)
	{
		this.slots = new Slot[3][3];
		this.grid = grid;
		
		for (int x = 0; x < this.slots.length; x++)
		{
			for (int y = 0; y < this.slots[x].length; y++)
			{
				this.slots[x][y] = new Slot(other.getSlot(x, y), this);
			}
		}
	}
	
	
	// ACCESSORS	--------------------
	
	/**
	 * @return The grid that holds this grid
	 */
	public SudokuGrid getGrid()
	{
		return this.grid;
	}

	
	// OTHER METHODS	-----------------
	
	/**
	 * Provides access to a certain slot
	 * @param x The slot's x index [0, 3]
	 * @param y The slot's y index [0, 3]
	 * @return The slot at the given index
	 */
	public Slot getSlot(int x, int y)
	{
		return this.slots[x][y];
	}
	
	/**
	 * Finds a slot's position in the grid
	 * @param slot The slot that is searched for
	 * @return The slot's position in the grid or null if slot is not in the grid
	 */
	public Vector3D getSlotPosition(Slot slot)
	{
		for (int x = 0; x < this.slots.length; x++)
		{
			for (int y = 0; y < this.slots[x].length; y++)
			{
				Slot target = getSlot(x, y);
				if (slot.equals(target))
					return new Vector3D(x, y);
			}
		}
		
		return null;
	}
	
	/**
	 * @return A list of numbers already in this grid
	 */
	public List<Integer> getUsedNumbers()
	{
		return getSlotNumbers(getSlots());
	}
	
	/**
	 * @return All the slots in the grid
	 */
	public List<Slot> getSlots()
	{
		List<Slot> slots = new ArrayList<>();
		for (int x = 0; x < this.slots.length; x++)
		{
			for (int y = 0; y < this.slots[x].length; y++)
			{
				slots.add(getSlot(x, y));
			}
		}
		return slots;
	}
	
	/**
	 * Collects all the slots on the given row
	 * @param row The row the slots are on
	 * @return The slots in the given row
	 */
	public List<Slot> getRowSlots(int row)
	{
		List<Slot> slots = new ArrayList<>();
		for (int x = 0; x < this.slots.length; x++)
		{
			slots.add(getSlot(x, row));
		}
		
		return slots;
	}
	
	/**
	 * Collects all the slots on the given column
	 * @param column The column the slots are on
	 * @return The slots in the given column
	 */
	public List<Slot> getColumnSlots(int column)
	{
		List<Slot> slots = new ArrayList<>();
		for (int y = 0; y < this.slots[column].length; y++)
		{
			slots.add(getSlot(column, y));
		}
		
		return slots;
	}
	
	/**
	 * Gets all the numbers from the given row
	 * @param row The row the numbers are collected from
	 * @return The numbers stored on the row
	 */
	public List<Integer> getRowNumbers(int row)
	{
		return getSlotNumbers(getRowSlots(row));
	}
	
	/**
	 * Gets all the numbers from the given column
	 * @param column The column the numbers are collected from
	 * @return The numbers stored on the column
	 */
	public List<Integer> getColumnNumbers(int column)
	{
		return getSlotNumbers(getColumnSlots(column));
	}
	
	/**
	 * Collects all the numbers that are missing from the given set of numbers
	 * @param numbers A set of numbers
	 * @return The numbers that are missing from the set
	 */
	public static List<Integer> getRemainingNumbers(Collection<Integer> numbers)
	{
		List<Integer> remaining = new ArrayList<>();
		for (int i = 1; i <= 9; i++)
		{
			if (!hasNumber(numbers, i))
				remaining.add(i);
		}
		
		return remaining;
	}
	
	/**
	 * Checks if the given set of numbers contains the given number
	 * @param numbers A set of numbers
	 * @param number The number the set could contain
	 * @return Does the set contain the given number
	 */
	public static boolean hasNumber(Collection<Integer> numbers, int number)
	{
		return numbers.contains(number);
	}
	
	/**
	 * Finds all the numbers stored in the given set of slots
	 * @param slots A set of slots
	 * @return The numbers stored in the slots
	 */
	public static List<Integer> getSlotNumbers(Collection<Slot> slots)
	{
		List<Integer> numbers = new ArrayList<>();
		for (Slot slot : slots)
		{
			if (slot.hasNumber() && !numbers.contains(slot.getNumber()))
				numbers.add(slot.getNumber());
		}
		
		return numbers;
	}
	
	/**
	 * Marks all the slots to require an update
	 * @param slots The slots that will require an update
	 */
	public static void setUpdateRequisition(Collection<Slot> slots)
	{
		for (Slot slot : slots)
		{
			slot.setUpdateRequired(true);
		}
	}
}
