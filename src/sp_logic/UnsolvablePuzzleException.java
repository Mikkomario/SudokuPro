package sp_logic;

/**
 * These exceptions are thrown when the sudoku cannot be solved
 * @author Mikko Hilpinen
 * @since 8.8.2015
 */
public class UnsolvablePuzzleException extends Exception
{
	// ATTRIBUTES	------------------
	
	private static final long serialVersionUID = -4287114058415043644L;
	private Slot source;
	
	
	// CONSTRUCTOR	------------------
	
	/**
	 * Creates a new exception
	 * @param source The slot that is unsolvable
	 */
	public UnsolvablePuzzleException(Slot source)
	{
		this.source = source;
	}
	
	/**
	 * Creates a new exception that is derived from another exception
	 * @param source The exception that caused this one
	 */
	public UnsolvablePuzzleException(UnsolvablePuzzleException source)
	{
		super(source);
		this.source = source.getSource();
	}
	
	
	// ACCESSORS	------------------
	
	/**
	 * @return The slot that was noticed unsolvable
	 */
	public Slot getSource()
	{
		return this.source;
	}
}
