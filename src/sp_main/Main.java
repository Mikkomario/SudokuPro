package sp_main;

import java.awt.Color;
import java.awt.Font;
import java.io.FileNotFoundException;

import sp_logic.Slot;
import sp_logic.SudokuGrid;
import sp_logic.SudokuSolver;
import sp_logic.UnsolvablePuzzleException;
import flow_io.ListFileReader;
import gateway_event.ButtonEvent;
import gateway_event.ButtonEvent.ButtonEventType;
import gateway_event.ButtonEventListener;
import gateway_ui.AbstractButton;
import gateway_ui.InputBar;
import gateway_ui.MessageBox;
import gateway_ui.RectangleUIComponentBackground;
import genesis_event.DrawableHandler;
import genesis_event.EventSelector;
import genesis_event.HandlerRelay;
import genesis_event.KeyEvent;
import genesis_event.KeyListener;
import genesis_event.KeyListenerHandler;
import genesis_event.MouseListenerHandler;
import genesis_event.KeyEvent.KeyEventType;
import genesis_util.SimpleHandled;
import genesis_util.Vector3D;
import genesis_video.GamePanel;
import genesis_video.GameWindow;

/**
 * The main class of the sudoku game
 * @author Mikko Hilpinen
 * @since 8.8.2015
 */
public class Main
{
	// CONSTRUCTOR	---------------------
	
	private Main()
	{
		// The interface is static
	}

	
	// MAIN METHOD	---------------------
	
	/**
	 * Starts the game
	 * @param args Not used
	 */
	public static void main(String[] args)
	{
		Vector3D resolution = new Vector3D(400, 400);
		Vector3D margins = new Vector3D(64, 64);
		
		GameWindow window = new GameWindow(resolution, "SudokuPro", true, 120, 10);
		GamePanel panel = window.getMainPanel().addGamePanel();
		panel.setBackground(Color.WHITE);
		
		HandlerRelay handlers = new HandlerRelay();
		handlers.addHandler(new DrawableHandler(false, panel.getDrawer()));
		handlers.addHandler(new KeyListenerHandler(false, window.getHandlerRelay()));
		handlers.addHandler(new MouseListenerHandler(false, window.getHandlerRelay()));
		
		SudokuGrid sudoku = new SudokuGrid(handlers, margins, 
				resolution.minus(margins.times(2)));
		new KeySolver(handlers, sudoku);
		
		new MessageBoxInterface(handlers, resolution, margins, sudoku);
	}
	
	private static void loadSudoku(String fileName, SudokuGrid sudoku) throws FileNotFoundException
	{
		ListFileReader reader = new ListFileReader();
		reader.readFile(fileName, "*");
		
		int y = 0;
		for (String line : reader.getLines())
		{
			int x = 0;
			for (int i = 0; i < line.length(); i++)
			{
				char c = line.charAt(i);
				// Whitespaces are skipped
				if (c != ' ')
				{
					// Zeros are not recorded
					int number = Character.getNumericValue(c);
					if (number > 0)
						sudoku.getSlot(x, y).setNumber(number);
					x ++;
				}
			}
			y ++;
		}
		
		sudoku.lockCurrentNumbers();
	}
	
	
	// SUBCLASSES	-------------------
	
	private static class MessageBoxInterface extends SimpleHandled implements ButtonEventListener
	{
		// ATTRIBUTES	---------------
		
		private EventSelector<ButtonEvent> selector;
		private SudokuGrid sudoku;
		
		private MessageBox messageBox;
		private InputBar input;
		
		
		// CONSTRUCTOR	---------------
		
		public MessageBoxInterface(HandlerRelay handlers, Vector3D resolution, Vector3D margins, 
				SudokuGrid sudoku)
		{
			super(handlers);
			
			this.sudoku = sudoku;
			
			this.selector = ButtonEvent.createButtonEventSelector(ButtonEventType.RELEASED);
			
			Vector3D boxDimensions = new Vector3D(
					resolution.getFirst() - margins.getFirst() * 2, resolution.getSecond() / 2);
			Font font = new Font(Font.SERIF, Font.PLAIN, 14);
			this.messageBox = new MessageBox(resolution.dividedBy(2).minus(
					boxDimensions.dividedBy(2)), boxDimensions, margins.dividedBy(3), 
					"Please input the sudoku file name", "#", font, font, Color.BLACK, 
					handlers);
			new RectangleUIComponentBackground(this.messageBox, handlers, Color.BLACK, 
					Color.WHITE);
			Color[] lineColors = {Color.BLACK};
			Color[] fillColors = {Color.WHITE, Color.LIGHT_GRAY, Color.DARK_GRAY};
			AbstractButton button = this.messageBox.addButton(lineColors, fillColors, 
					new Vector3D(100, 50), new Vector3D(20, 10), "OK", false);
			this.messageBox.addButton(lineColors, fillColors, new Vector3D(100, 50), 
					new Vector3D(20, 10), "Cancel", true);
			this.input = new InputBar(handlers, new Vector3D(5, 5), 
					new Vector3D(10, 30), font, Color.WHITE, 
					this.messageBox.getDepth() - 2);
			new RectangleUIComponentBackground(this.input, handlers, null, Color.BLACK);
			this.messageBox.addInputBar(this.input, true);
			
			button.getListenerHandler().add(this);
		}
		
		
		// IMPLEMENTED METHODS	-------

		@Override
		public EventSelector<ButtonEvent> getButtonEventSelector()
		{
			return this.selector;
		}

		@Override
		public void onButtonEvent(ButtonEvent e)
		{
			// Tries to load the sudoku
			String fileName = this.input.getInputReader().getInput();
			try
			{
				loadSudoku(fileName, this.sudoku);
				this.messageBox.getIsDeadStateOperator().setState(true);
				getIsDeadStateOperator().setState(true);
			}
			catch (FileNotFoundException e1)
			{
				this.messageBox.setMessage("Couldn't find " + fileName + 
						".#Please input another file name");
			}
		}
	}
	
	private static class KeySolver extends SimpleHandled implements KeyListener
	{
		// ATTRIBUTES	---------------
		
		private SudokuSolver solver;
		private EventSelector<KeyEvent> selector;
		
		
		// CONSTRUCTOR	---------------
		
		public KeySolver(HandlerRelay handlers, SudokuGrid sudoku)
		{
			super(handlers);
			
			this.solver = new SudokuSolver(sudoku);
			this.selector = KeyEvent.createEventTypeSelector(KeyEventType.PRESSED);
		}
		
		
		// IMPLEMENTED METHODS	------

		@Override
		public EventSelector<KeyEvent> getKeyEventSelector()
		{
			return this.selector;
		}

		@Override
		public void onKeyEvent(KeyEvent event)
		{
			if (event.getKey() == KeyEvent.RIGHT)
			{
				try
				{
					this.solver.solveNext();
				}
				catch (UnsolvablePuzzleException e)
				{
					for (Slot slot : e.getSource().getConnectedSlots())
					{
						slot.mark();
					}
					System.err.println("Can't solve the puzzle");
					System.err.println("Source position: " + e.getSource().getPosition());
					e.printStackTrace();
					getIsDeadStateOperator().setState(true);
				}
			}
			else if (event.getKey() == KeyEvent.UP)
			{
				try
				{
					this.solver.solveMax();
				}
				catch (UnsolvablePuzzleException e)
				{
					for (Slot slot : e.getSource().getConnectedSlots())
					{
						slot.mark();
					}
					System.err.println("Can't solve the puzzle");
					System.err.println("Source position: " + e.getSource().getPosition());
					e.printStackTrace();
					getIsDeadStateOperator().setState(true);
				}
			}
		}
	}
}
