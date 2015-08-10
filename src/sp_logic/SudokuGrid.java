package sp_logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import genesis_event.HandlerRelay;
import genesis_util.Vector3D;

/**
 * SudokuGrid contains nine 3x3 slot grids
 * @author Mikko Hilpinen
 * @since 8.8.2015
 */
public class SudokuGrid
{
	// ATTRIBUTES	---------------------
	
	private NineSlotGrid[][] grids;
	
	
	// CONSTRUCTOR	---------------------
	
	/**
	 * Creates a new sudoku grid
	 * @param handlers The handlers that will handle the slots
	 * @param position The grid's position
	 * @param dimensions The grid's size
	 */
	public SudokuGrid(HandlerRelay handlers, Vector3D position, Vector3D dimensions)
	{
		this.grids = new NineSlotGrid[3][3];
		Vector3D margins = dimensions.dividedBy(20);
		Vector3D gridDimensions = dimensions.minus(margins.times(3)).dividedBy(3);
		
		for (int x = 0; x < this.grids.length; x++)
		{
			for (int y = 0; y < this.grids[x].length; y++)
			{
				Vector3D gridPosition = position.plus(gridDimensions.plus(margins).times(
						new Vector3D(x, y)));
				this.grids[x][y] = new NineSlotGrid(handlers, gridPosition, gridDimensions, 
						this);
			}
		}
	}
	
	/**
	 * Creates a copy of a sudoku grid
	 * @param other The grid this one is copied from
	 */
	public SudokuGrid(SudokuGrid other)
	{
		this.grids = new NineSlotGrid[3][3];
		
		for (int x = 0; x < this.grids.length; x++)
		{
			for (int y = 0; y < this.grids[x].length; y++)
			{
				this.grids[x][y] = new NineSlotGrid(other.getGrid(x, y), this);
			}
		}
	}
	
	
	// OTHER METHODS	------------------
	
	/**
	 * Copies all numbers from the other grid
	 * @param other The grid then numbers are copied from
	 */
	public void copyNumbersFrom(SudokuGrid other)
	{
		for (int gridX = 0; gridX < this.grids.length; gridX ++)
		{
			for (int gridY = 0; gridY < this.grids[gridX].length; gridY ++)
			{
				NineSlotGrid grid = getGrid(gridX, gridY);
				NineSlotGrid sourceGrid = other.getGrid(gridX, gridY);
				
				for (int x = 0; x < 3; x ++)
				{
					for (int y = 0; y < 3; y ++)
					{
						Slot slot = grid.getSlot(x, y);
						Slot sourceSlot = sourceGrid.getSlot(x, y);
						
						if (slot.getNumber() != sourceSlot.getNumber())
							slot.setNumber(sourceSlot.getNumber());
					}
				}
			}
		}
	}
	
	/**
	 * Activates or disables each slot in the grid
	 * @param newState Should the slots be active or not
	 */
	public void setActive(boolean newState)
	{
		for (Slot slot : getSlots())
		{
			slot.setActive(newState);
		}
	}
	
	/**
	 * Kills all the slots in the grid
	 */
	public void kill()
	{
		for (Slot slot : getSlots())
		{
			slot.getIsDeadStateOperator().setState(true);
		}
	}
	
	/**
	 * Finds the grid at the given position
	 * @param x The grid's x index [0, 3]
	 * @param y The grid's y index [0, 3]
	 * @return The grid at the given index
	 */
	public NineSlotGrid getGrid(int x, int y)
	{
		return this.grids[x][y];
	}
	
	/**
	 * @param x The slot's x index [0, 9]
	 * @param y The slot's y index [0, 9]
	 * @return A slot at the given position
	 */
	public Slot getSlot(int x, int y)
	{
		return getGrid(x / 3, y / 3).getSlot(x % 3, y % 3);
	}
	
	/**
	 * @param y The y index of the row [0, 3]
	 * @return A row of grids
	 */
	public List<NineSlotGrid> getGridRow(int y)
	{
		List<NineSlotGrid> grids = new ArrayList<>();
		for (int x = 0; x < this.grids.length; x++)
		{
			grids.add(getGrid(x, y));
		}
		
		return grids;
	}
	
	/**
	 * Finds the position of a grid in this grid
	 * @param grid The grid that is searched for
	 * @return The grid's position
	 */
	public Vector3D getGridPosition(NineSlotGrid grid)
	{
		for (int x = 0; x < this.grids.length; x++)
		{
			for (int y = 0; y < this.grids[x].length; y++)
			{
				NineSlotGrid target = getGrid(x, y);
				if (target.equals(grid))
					return new Vector3D(x, y);
			}
		}
		
		return null;
	}
	
	/**
	 * Finds the slot's position in the large grid
	 * @param slot The slot that is searched for
	 * @return The slot's position in the large grid
	 */
	public Vector3D getSlotPosition(Slot slot)
	{
		Vector3D gridPosition = getGridPosition(slot.getGrid());
		return gridPosition.times(3).plus(slot.getGrid().getSlotPosition(slot));
	}
	
	/**
	 * @param x The x index of the column [0, 3]
	 * @return A column of grids
	 */
	public List<NineSlotGrid> getGridColumn(int x)
	{
		List<NineSlotGrid> grids = new ArrayList<>();
		for (int y = 0; y < this.grids[x].length; y++)
		{
			grids.add(getGrid(x, y));
		}
		
		return grids;
	}
	
	/**
	 * @param y The index of the row [0, 9]
	 * @return The row at the given index
	 */
	public List<Slot> getRow(int y)
	{
		int gridIndex = y / 3;
		int rowIndex = y % 3;
		
		List<Slot> row = new ArrayList<>();
		for (NineSlotGrid grid : getGridRow(gridIndex))
		{
			row.addAll(grid.getRowSlots(rowIndex));
		}
		
		return row;
	}
	
	/**
	 * @param x The index of the column [0, 9]
	 * @return The column at the given index
	 */
	public List<Slot> getColumn(int x)
	{
		int gridIndex = x / 3;
		int columnIndex = x % 3;
		
		List<Slot> column = new ArrayList<>();
		for (NineSlotGrid grid : getGridColumn(gridIndex))
		{
			column.addAll(grid.getColumnSlots(columnIndex));
		}
		
		return column;
	}
	
	/**
	 * @return All of the nineSlotGrids in this grid
	 */
	public List<NineSlotGrid> getGrids()
	{
		List<NineSlotGrid> grids = new ArrayList<>();
		for (int x = 0; x < this.grids.length; x++)
		{
			for (int y = 0; y < this.grids[x].length; y++)
			{
				grids.add(getGrid(x, y));
			}
		}
		
		return grids;
	}
	
	/**
	 * @return All the slots in the grid
	 */
	public List<Slot> getSlots()
	{
		List<Slot> slots = new ArrayList<>();
		for (int x = 0; x < this.grids.length; x++)
		{
			for (int y = 0; y < this.grids[x].length; y++)
			{
				slots.addAll(getGrid(x, y).getSlots());
			}
		}
		
		return slots;
	}
	
	/**
	 * Locks the currently set numbers
	 */
	public void lockCurrentNumbers()
	{
		for (Slot slot : getSlots())
		{
			if (slot.hasNumber())
				slot.lock();
		}
	}
	
	/**
	 * Combines two sets of slots into one
	 * @param first The first set
	 * @param second The second set
	 * @return The combined set
	 */
	public static List<Slot> getCombinedSlots(Collection<Slot> first, Collection<Slot> second)
	{
		List<Slot> slots = new ArrayList<>();
		slots.addAll(first);
		
		for (Slot slot : second)
		{
			if (!slots.contains(slot))
				slots.add(slot);
		}
		
		return slots;
	}
}
