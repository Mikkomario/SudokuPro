package sp_logic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import genesis_event.Drawable;
import genesis_event.EventSelector;
import genesis_event.GenesisHandlerType;
import genesis_event.HandlerRelay;
import genesis_event.KeyEvent;
import genesis_event.KeyEvent.ContentType;
import genesis_event.KeyListener;
import genesis_event.MouseEvent;
import genesis_event.MouseListener;
import genesis_event.KeyEvent.KeyEventType;
import genesis_event.MouseEvent.MouseButton;
import genesis_event.MouseEvent.MouseButtonEventScale;
import genesis_event.MouseEvent.MouseButtonEventType;
import genesis_event.StrictEventSelector;
import genesis_util.DepthConstants;
import genesis_util.HelpMath;
import genesis_util.SimpleHandled;
import genesis_util.Vector3D;

/**
 * A slot stores a number. The slots are stored in grids.
 * @author Mikko Hilpinen
 * @since 8.8.2015
 */
public class Slot extends SimpleHandled implements Drawable, KeyListener,
		MouseListener
{
	// ATTRIBUTES	---------------------
	
	private EventSelector<MouseEvent> mouseSelector;
	private EventSelector<KeyEvent> keySelector;
	private Vector3D position, dimensions;
	private int number;
	private List<Integer> possibleNumbers;
	private boolean updateRequired, focus, locked, marked, guess;
	private NineSlotGrid grid;
	private HandlerRelay handlers;
	
	
	// CONSTRUCTOR	---------------------
	
	/**
	 * Creates a new slot to the given location
	 * @param handlers The handlers that will handle the slot
	 * @param position The slot's position
	 * @param dimensions The slot's dimensions
	 * @param grid The grid that holds this slot
	 */
	public Slot(HandlerRelay handlers, Vector3D position, Vector3D dimensions, NineSlotGrid grid)
	{
		super(handlers);
		
		this.position = position;
		this.dimensions = dimensions;
		this.grid = grid;
		this.handlers = handlers;
		
		this.number = 0;
		this.possibleNumbers = new ArrayList<>();
		this.updateRequired = true;
		this.focus = false;
		this.locked = false;
		this.marked = false;
		this.guess = false;
		
		initializeEventSelectors();
		
		getHandlingOperators().addOperatorForType(GenesisHandlerType.DRAWABLEHANDLER);
	}
	
	/**
	 * Creates an exact copy of another slot
	 * @param other The slot this one is copied from
	 * @param grid The grid that holds the copied slot
	 */
	public Slot(Slot other, NineSlotGrid grid)
	{
		super(other.handlers);
		
		this.position = other.position;
		this.dimensions = other.dimensions;
		this.grid = grid;
		this.handlers = other.handlers;
		this.number = other.getNumber();
		this.updateRequired = other.updateRequired();
		this.focus = other.focus;
		this.marked = other.marked;
		this.locked = other.locked;
		this.guess = other.guess;
		
		this.possibleNumbers = new ArrayList<>();
		this.possibleNumbers.addAll(other.getPossibleNumbers());
		
		initializeEventSelectors();
		getHandlingOperators().addOperatorForType(GenesisHandlerType.DRAWABLEHANDLER);
	}
	
	
	// IMPLEMENTED METHODS	--------------------

	@Override
	public EventSelector<MouseEvent> getMouseEventSelector()
	{
		return this.mouseSelector;
	}

	@Override
	public boolean isInAreaOfInterest(Vector3D position)
	{
		return HelpMath.pointIsInRange(position.minus(this.position), Vector3D.zeroVector(), 
				this.dimensions);
	}

	@Override
	public void onMouseEvent(MouseEvent event)
	{
		// If the press was local, gets focus, otherwise loses it
		if (event.getButtonEventScale() == MouseButtonEventScale.LOCAL)
		{
			if (event.getButton() == MouseButton.LEFT)
				this.focus = true;
			else
			{
				StringBuilder s = new StringBuilder("Possible :");
				for (Integer possible : this.possibleNumbers)
				{
					s.append(possible);
					s.append(", ");
				}
				System.out.println(s.toString());
			}
		}
		else
			this.focus = false;
	}

	@Override
	public EventSelector<KeyEvent> getKeyEventSelector()
	{
		return this.keySelector;
	}

	@Override
	public void onKeyEvent(KeyEvent event)
	{
		// if a number key was pressed, changes the number in the slot
		if (this.focus && Character.isDigit(event.getKey()))
			setNumber(Character.getNumericValue(event.getKeyChar()));
	}

	@Override
	public void drawSelf(Graphics2D g2d)
	{
		if (this.locked || this.guess)
		{
			if (this.locked)
				g2d.setColor(Color.LIGHT_GRAY);
			else
				g2d.setColor(Color.CYAN);
			g2d.fillRect(this.position.getFirstInt(), this.position.getSecondInt(), 
					this.dimensions.getFirstInt(), this.dimensions.getSecondInt());
		}
		
		if (this.focus)
			g2d.setColor(Color.RED);
		else if (this.marked)
			g2d.setColor(Color.PINK);
		else if (updateRequired())
			g2d.setColor(Color.BLUE);
		else
			g2d.setColor(Color.BLACK);
		
		g2d.drawRect(this.position.getFirstInt(), this.position.getSecondInt(), 
				this.dimensions.getFirstInt(), this.dimensions.getSecondInt());
		
		if (hasNumber())
			g2d.drawString(this.number + "", this.position.getFirstInt() + (int) (
					this.dimensions.getFirst() * 0.2), this.position.getSecondInt() + 
					(int) (this.dimensions.getSecond() * 0.8));
	}

	@Override
	public int getDepth()
	{
		return DepthConstants.NORMAL;
	}
	
	
	// ACCESSORS	----------------------
	
	/**
	 * @return The number in this slot
	 */
	public int getNumber()
	{
		return this.number;
	}
	
	/**
	 * Changes the number in the slot
	 * @param number The new number in the slot (<= 0 means empty)
	 */
	public void setNumber(int number)
	{
		if (number == this.number)
			return;
		
		this.number = number;
		
		// After a number changes, updates are required 
		setUpdateRequired(number <= 0);
		if (number > 0)
			getPossibleNumbers().clear();
		// The grid needs to be updated, as well as the row and column
		NineSlotGrid.setUpdateRequisition(getConnectedSlots());
	}
	
	/**
	 * @return Does the slot require more updating or a confirmation
	 */
	public boolean updateRequired()
	{
		return this.updateRequired;
	}
	
	/**
	 * Mark the slot as updated or set it to require further updates
	 * @param newStatus The slot's new update status
	 */
	public void setUpdateRequired(boolean newStatus)
	{
		// The slots that already have a number don't ever need updating
		if (newStatus && hasNumber())
			return;
		
		this.updateRequired = newStatus;
	}
	
	/**
	 * @return The grid that holds this slot
	 */
	public NineSlotGrid getGrid()
	{
		return this.grid;
	}
	
	/**
	 * @return All the numbers that have been marked possible for this slot
	 */
	public List<Integer> getPossibleNumbers()
	{
		return this.possibleNumbers;
	}
	
	/**
	 * Updates the list of possible numbers in the slot
	 * @param numbers The numbers that are possible
	 */
	public void setPossibleNumbers(List<Integer> numbers)
	{
		this.possibleNumbers = numbers;
	}
	
	/**
	 * Marks the slot, used for testing
	 */
	public void mark()
	{
		this.marked = true;
	}
	
	/**
	 * Guesses a number into the slot
	 * @param number The number that is put into the slot
	 */
	public void guess(int number)
	{
		this.guess = true;
		setNumber(number);
	}
	
	/**
	 * @return Is the slot's value locked in place
	 */
	public boolean isLocked()
	{
		return this.locked;
	}
	
	
	// OTHER METHODS	-----------------
	
	/**
	 * Makes the slot react to mouse and draw events, or disables it
	 * @param newState Should the slot react to events
	 */
	public void setActive(boolean newState)
	{
		if (isLocked() && newState)
			getHandlingOperators().getShouldBeHandledOperator(
					GenesisHandlerType.DRAWABLEHANDLER).setState(newState);
		else
			getHandlingOperators().setAllStates(newState);
	}
	
	/**
	 * Removes a mark from the slot
	 */
	public void removeMark()
	{
		this.marked = false;
	}
	
	/**
	 * Locks the slot so that it can't be changed anymore
	 */
	public void lock()
	{
		this.locked = true;
		getHandlingOperators().getShouldBeHandledOperator(GenesisHandlerType.MOUSEHANDLER
				).setState(false);
	}
	
	/**
	 * @return The slots that share numbers with this one (grid, row, column)
	 */
	public List<Slot> getConnectedSlots()
	{
		Vector3D position = getPosition();
		List<Slot> slots = SudokuGrid.getCombinedSlots(getGrid().getSlots(), 
				SudokuGrid.getCombinedSlots(getGrid().getGrid().getRow(
				position.getSecondInt()), getGrid().getGrid().getColumn(
				position.getFirstInt())));
		slots.remove(this);
		return slots;
	}
	
	/**
	 * @return The slot's position on the sudoku grid
	 */
	public Vector3D getPosition()
	{
		return getGrid().getGrid().getSlotPosition(this);
	}
	
	/**
	 * @return If the slot is not empty and has a number stored
	 */
	public boolean hasNumber()
	{
		return this.number > 0;
	}
	
	/**
	 * Adds a number to the possible set of numbers in the slot
	 * @param possible The new possible number
	 */
	public void addPossibleNumber(int possible)
	{
		if (!hasNumber() && !this.possibleNumbers.contains(possible))
			this.possibleNumbers.add(possible);
	}
	
	/**
	 * Removes a number from the possible numbers
	 * @param number The number that is no longer possible in this slot
	 */
	public void removePossibleNumber(int number)
	{
		int index = this.possibleNumbers.indexOf(number);
		if (index >= 0)
			this.possibleNumbers.remove(index);
	}
	
	/**
	 * Finds out all the possible numbers that fit into both of the slots
	 * @param first The first slot
	 * @param second The second slot
	 * @return The numbers that fit into both slots
	 */
	public static List<Integer> getSharedNumbers(Slot first, Slot second)
	{
		List<Integer> sharedNumbers = new ArrayList<>();
		sharedNumbers.addAll(first.getPossibleNumbers());
		sharedNumbers.removeAll(NineSlotGrid.getRemainingNumbers(second.getPossibleNumbers()));
		
		return sharedNumbers;
	}
	
	private void initializeEventSelectors()
	{
		// The object reacts to mouse presses
		this.mouseSelector = MouseEvent.createButtonEventTypeSelector(
				MouseButtonEventType.PRESSED);
		// The object reacts to key presses (characters only)
		StrictEventSelector<KeyEvent, KeyEvent.Feature> selector = 
				KeyEvent.createEventTypeSelector(KeyEventType.PRESSED);
		selector.addRequiredFeature(ContentType.KEY);
		this.keySelector = selector;
	}
}
