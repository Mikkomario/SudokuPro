package sp_logic;

import genesis_util.Vector3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

//http://www.sudokudragon.com/tutorialhard1.htm

/**
 * This algorithm tries to solve the sudoku puzzle
 * @author Mikko Hilpinen
 * @since 8.8.2015
 */
public class SudokuSolver
{
	// ATTRIBUTES	-------------------
	
	private SudokuGrid originalSudoku, currentSudoku;
	private Slot lastSlot;
	private Stack<StepData> riskSteps;
	private int operations;
	
	
	// CONSTRUCTOR	-------------------
	
	/**
	 * Creates a new sudoku solver
	 * @param sudoku The sudoku that needs solving
	 */
	public SudokuSolver(SudokuGrid sudoku)
	{
		this.originalSudoku = sudoku;
		this.currentSudoku = sudoku;
		this.lastSlot = null;
		this.riskSteps = new Stack<>();
		this.operations = 0;
	}

	
	// OTHER METHODS	---------------
	
	/**
	 * Solves the puzzle all at once
	 * @throws UnsolvablePuzzleException If the puzzle can't be solved
	 */
	public void solveMax() throws UnsolvablePuzzleException
	{
		int guesses = 0;
		Result r;
		do
		{
			this.operations ++;
			r = bruteSolveNext();
			System.out.println(this.operations + ": " + r.getMessage());
			if (r.isGuess)
				guesses ++;
		}
		while (r.isClear());
		
		System.out.println("Guesses: " + guesses);
	}
	
	/**
	 * Solves the next piece in the puzzle using only safe methods
	 * @throws UnsolvablePuzzleException If the sudoku puzzle is unsolvable
	 */
	public void solveNext() throws UnsolvablePuzzleException
	{
		this.operations ++;
		if (this.lastSlot != null)
			this.lastSlot.removeMark();
		
		Result r = bruteSolveNext();
		System.out.println(this.operations + ": " + r.getMessage());
		if (r.getTarget() != null)
		{
			this.lastSlot = r.getTarget();
			this.lastSlot.mark();
		}
	}
	
	private Result bruteSolveNext() throws UnsolvablePuzzleException
	{
		// Goes as far with safe methods as possible
		try
		{
			Result r = safeSolveNext(this.currentSudoku);
			if (r.isClear())
				return r;
		}
		catch (UnsolvablePuzzleException e)
		{
			boolean unsolvable = false;
			do
			{
				unsolvable = false;
				
				// If the puzzle became unsolvable, at least the last risky step was a mistake
				// If there were no risky steps in history, the puzzle is unsolvable
				if (this.riskSteps.isEmpty())
					throw new UnsolvablePuzzleException(e);
				
				// Goes back to the last step and marks the last solution as impossible
				StepData lastStep = this.riskSteps.pop();
				this.currentSudoku.kill();
				
				this.currentSudoku = lastStep.targetGrid;
				this.currentSudoku.setActive(true);
				
				// Removes a possible number from the target since it lead to a dead end
				Slot target = lastStep.targetSlot;
				target.removePossibleNumber(lastStep.newNumber);
				
				if (target.getPossibleNumbers().size() == 0)
					unsolvable = true;
				else if (target.getPossibleNumbers().size() == 1)
				{
					target.setNumber(target.getPossibleNumbers().get(0));
					return new Result(true, "Retraced (solve)", target);
				}
				else
					return new Result(true, "Retraced", target);
			}
			while (unsolvable);
		}
		
		// Has to take chances and guess
		StepData risk = null;
		boolean hookWasUsed = false;
		
		// Tries to find a hook in the sudoku and use it as the target square for the guess
		List<CompleteHook> hooks = findHooks(this.currentSudoku);
		if (!hooks.isEmpty())
		{
			risk = hooks.get(0).getBestGuess();
			hookWasUsed = true;
		}
		else
		{
			// If a hook couldn't be found, finds the slot with least possible choices
			Slot bestGuessSlot = getBestRiskSlot(this.currentSudoku);
			if (bestGuessSlot != null)
			{
				risk = new StepData(this.currentSudoku, bestGuessSlot, 
						bestGuessSlot.getPossibleNumbers().get(0), 
						bestGuessSlot.getPossibleNumbers().size());
			}
			// If a suitable slot couldn't be found, the puzzle is complete
			else
			{
				// Copies the answer to the original sudoku
				this.originalSudoku.copyNumbersFrom(this.currentSudoku);
				this.originalSudoku.setActive(true);
				
				// All temporary sudoku grids will be killed
				if (!this.riskSteps.isEmpty())
					this.riskSteps.remove(0); // The original is at the bottom
				for (StepData step : this.riskSteps)
				{
					step.targetGrid.kill();
				}
				this.riskSteps.clear();
				
				return new Result(false, "Puzzle complete", null);
			}
		}
		
		// Makes the guess and returns
		Result r = makeGuess(risk);
		if (hookWasUsed)
			r.message += " (hook method)";
		return r;
	}
	
	private Result makeGuess(StepData risk)
	{
		Vector3D targetPosition = risk.targetSlot.getPosition();
		
		// Remembers the step so that it can be retraced
		this.riskSteps.push(risk);
		
		// The last sudoku is disabled when the new one is created
		risk.targetGrid.setActive(false);
		this.currentSudoku = new SudokuGrid(risk.targetGrid);
		Slot guessSlot = this.currentSudoku.getSlot(targetPosition.getFirstInt(), 
				targetPosition.getSecondInt());
		guessSlot.guess(risk.newNumber);
		
		return Result.guess(risk, guessSlot);
	}
	
	private static Result hookSolveNext(SudokuGrid sudoku) throws UnsolvablePuzzleException
	{
		// Goes through each hook in the sudoku
		for (CompleteHook hook : findHooks(sudoku))
		{
			// Squares that are affected by both the outside stem and the hook slot can't have 
			// their shared number
			
			// Finds the affected slots
			Slot outsideStem = hook.getOutsideStem();
			List<Slot> connectedToHook = hook.hook.getConnectedSlots();
			List<Slot> connectedToStem = outsideStem.getConnectedSlots();
			List<Slot> affectedSlots = new ArrayList<>();
			for (Slot slot : connectedToHook)
			{
				if (connectedToStem.contains(slot))
					affectedSlots.add(slot);
			}
			
			// Finds the shared number and excludes it from the affected slots
			int sharedNumber = Slot.getSharedNumbers(hook.hook, outsideStem).get(0);
			for (Slot slot : affectedSlots)
			{
				// TODO: Make a separate method for this process
				if (!slot.hasNumber())
				{
					slot.removePossibleNumber(sharedNumber);
					
					if (slot.getPossibleNumbers().size() == 0)
						throw new UnsolvablePuzzleException(slot);
					else if (slot.getPossibleNumbers().size() == 1)
					{
						slot.setNumber(slot.getPossibleNumbers().get(0));
						return new Result(true, "Hook method", slot);
					}
				}
			}
		}
		
		return Result.failure();
	}
	
	// http://www.sudokudragon.com/tutorialhook.htm
	private static List<CompleteHook> findHooks(SudokuGrid sudoku)
	{
		// A hook is a set of three squares where the share a chain of possible numbers like 
		// [1,2] [2,3] and [3,1]. Two of these must be on a single row / column and the third 
		// must be on a same grid with one of them. Each must only have two possible numbers
		
		// Finds possible stems (pairs that share a possible number)
		List<List<Slot>> possibleStems = new ArrayList<>();
		
		for (int x = 0; x < 9; x++)
		{
			for (int y = 0; y < 9; y++)
			{
				Slot stem1 = sudoku.getSlot(x, y);
				
				if (stem1.getPossibleNumbers().size() != 2)
					continue;
				
				// Searches for all possible stems
				List<Slot> possiblePairs = findStemPairs(sudoku, stem1, new Vector3D(x, y), 
						true);
				possiblePairs.addAll(findStemPairs(sudoku, stem1, new Vector3D(x, y), false));
				
				// Makes up the stems from the pairs and moves on
				for (Slot stem2 : possiblePairs)
				{
					List<Slot> stem = new ArrayList<>();
					stem.add(stem1);
					stem.add(stem2);
					possibleStems.add(stem);
				}
			}
		}
		
		// Next finds all complete hooks that can be formed
		List<CompleteHook> completeHooks = new ArrayList<>();
		for (List<Slot> stems : possibleStems)
		{
			for (Slot hook : findHookSlots(stems))
			{
				CompleteHook newHook = new CompleteHook(stems, hook);
				//newHook.mark();
				completeHooks.add(newHook);
			}
		}
		
		return completeHooks;
	}
	
	// False checks from row
	private static List<Slot> findStemPairs(SudokuGrid sudoku, Slot stem1, 
			Vector3D stem1Position, boolean checkFromColumn)
	{
		// Finds the slots to the right / down from the stem1 from other grids
		int startPosition;
		if (checkFromColumn)
			startPosition = stem1Position.getSecondInt() + 1;
		else
			startPosition = stem1Position.getFirstInt() + 1;
		
		List<Slot> possiblePairs = new ArrayList<>();
		for (int position = startPosition; position < 9; position ++)
		{
			int x, y;
			if (checkFromColumn)
			{
				x = stem1Position.getFirstInt();
				y = position;
			}
			else
			{
				x = position;
				y = stem1Position.getSecondInt();
			}
			
			Slot possiblePair = sudoku.getSlot(x, y);
			
			// The pair can't be in the same region, it has to have two possible numbers and 
			// one of them needs to be shared with the other stem
			if (possiblePair.getGrid().equals(stem1.getGrid()))
				continue;
			if (possiblePair.getPossibleNumbers().size() != 2)
				continue;
			if (Slot.getSharedNumbers(stem1, possiblePair).size() != 1)
				continue;
			
			possiblePairs.add(possiblePair);
		}
		
		return possiblePairs;
		/*
		List<Slot> stems = new ArrayList<>();
		
		int startPosition;
		if (forColumns)
			startPosition = stem1Position.getSecondInt() + 1;
		else
			startPosition = stem1Position.getFirstInt() + 1;
		
		for (int position = startPosition; position < 9; position ++)
		{
			Slot possibleStem;
			if (forColumns)
				possibleStem = sudoku.getSlot(stem1Position.getFirstInt(), position);
			else
				possibleStem = sudoku.getSlot(position, stem1Position.getSecondInt());
			
			// The other stem must have two possible numbers and not share a region with the 
			// other one
			if (possibleStem.getPossibleNumbers().size() != 2 || 
					possibleStem.getGrid().equals(stem1.getGrid()))
				continue;
			
			for (int possibleNumber : stem1.getPossibleNumbers())
			{
				if (possibleStem.getPossibleNumbers().contains(possibleNumber))
				{
					stems.add(possibleStem);
					break;
				}
			}
		}
		
		return stems;
		*/
	}
	
	private static List<Slot> findHookSlots(List<Slot> stems)
	{
		// A hook must share a different possible number with each stem and be on a same grid 
		// with either one. It also needs to have only two possible choices
		List<Slot> hookSearchSlots = SudokuGrid.getCombinedSlots(
				stems.get(0).getGrid().getSlots(), stems.get(1).getGrid().getSlots());
		hookSearchSlots.removeAll(stems);
		
		// Finds all the suitable hook slots
		List<Slot> hooks = new ArrayList<>();
		for (Slot slot : hookSearchSlots)
		{
			if (slot.getPossibleNumbers().size() != 2)
				continue;
			
			List<Integer> firstSharedNumbers = Slot.getSharedNumbers(slot, stems.get(0));
			if (firstSharedNumbers.size() != 1)
				continue;
			
			List<Integer> secondSharedNumbers = Slot.getSharedNumbers(slot, stems.get(1));
			if (secondSharedNumbers.size() != 1)
				continue;
			
			// The shared numbers can't be the same for both stems
			if (firstSharedNumbers.get(0) == secondSharedNumbers.get(0))
				continue;
			
			hooks.add(slot);
		}
		
		return hooks;
	}
	
	private static Slot getBestRiskSlot(SudokuGrid sudoku)
	{	
		// Starts from the center grid, then checks the other ones
		Slot best = getBestRiskSlot(sudoku.getGrid(1, 1));
		
		int minNumbers = -1;
		if (best != null)
			minNumbers = best.getPossibleNumbers().size();
		
		// The best possible is a slot where there are only 2 choices
		if (minNumbers == 2)
			return best;
		
		for (int x = 0; x < 3; x++)
		{
			for (int y = 0; y < 3; y++)
			{
				// Skips the center this time
				if (x == 1 && y == 1)
					continue;
				
				Slot gridBest = getBestRiskSlot(sudoku.getGrid(x, y));
				if (gridBest == null)
					continue;
				
				int numbers = gridBest.getPossibleNumbers().size();
				if (numbers == 2)
					return gridBest;
				else if (best == null || numbers < minNumbers)
				{
					best = gridBest;
					minNumbers = numbers;
				}
			}
		}
		
		return best;
	}
	
	private static Slot getBestRiskSlot(NineSlotGrid grid)
	{
		Slot best = null;
		int possibleNumbers = -1;
		
		for (Slot slot : grid.getSlots())
		{
			if (!slot.hasNumber())
			{
				int numbers = slot.getPossibleNumbers().size();
				if (numbers == 2)
					return slot;
				else if (best == null || numbers < possibleNumbers)
				{
					possibleNumbers = numbers;
					best = slot;
				}
			}
		}
		
		return best;
	}
	
	private static Result safeSolveNext(SudokuGrid sudoku) throws UnsolvablePuzzleException
	{
		Result r = primarySolveNext(sudoku);
		
		// 0: primary
		// 1: grid (back to 1, skipping 1)
		// 2: Secondary1 -> back to 1, skipping 2
		// 3: Secondary2 -> back to 1, skipping 3
		// 4: Hidden twin -> back to 1, skipping 4
		// 5: Naked twin -> back to 1, skipping 5
		// 6: Hook method -> back to 1, skipping 6
		// 7: Failure
		int currentPhase = 1;
		int lastMaxPhase = 0;
		
		Map<Integer, String> notations = new HashMap<>();
		notations.put(0, "");
		notations.put(1, "");
		notations.put(2, " (common column)");
		notations.put(3, " (common row)");
		notations.put(4, " (hidden twin)");
		notations.put(5, " (naked twin)");
		notations.put(6, " (hook method)");
		
		while (currentPhase < 7 && !r.isClear())
		{
			while (currentPhase <= lastMaxPhase + 1 && !r.isClear())
			{
				if (currentPhase != lastMaxPhase)
				{
					switch (currentPhase)
					{
						case 1: r = gridSolveNext(sudoku); break;
						case 2: r = secondarySolveNext(sudoku, true); break;
						case 3: r = secondarySolveNext(sudoku, false); break;
						case 4:
							for (NineSlotGrid grid : sudoku.getGrids())
							{
								filterHiddenTwins(grid);
							}
							break;
						case 5:
							for (NineSlotGrid grid : sudoku.getGrids())
							{
								r = NakedTwinSolve(grid);
								if (r.isClear())
									break;
							}
							break;
						case 6:
							//r = Result.failure();
							r = hookSolveNext(sudoku);
							break;
					}
					
					if (currentPhase == 7)
						break;
					
					if (r.isClear() && currentPhase < lastMaxPhase)
						r.message += notations.get(lastMaxPhase);
					
					// Skips back to the beginning once a new step is reached
					if (currentPhase > lastMaxPhase)
					{
						lastMaxPhase = currentPhase;
						currentPhase = 0;
					}
				}
				
				currentPhase ++;
			}
		}
		
		return r;
		
		/*
		if (!r.isClear())
			r = gridSolveNext(sudoku);
		
		if (!r.isClear())
			r = secondarySolveNext(sudoku, false);
		
		if (!r.isClear())
			r = secondarySolveNext(sudoku, true);
		
		if (!r.isClear())
		{
			r = gridSolveNext(sudoku);
			if (r.isClear())
				r.message += " (secodary)";
		}
		
		if (!r.isClear())
		{
			for (NineSlotGrid grid : sudoku.getGrids())
			{
				filterHiddenTwins(grid);
			}
			r = gridSolveNext(sudoku);
			if (r.isClear())
				r.message += " (hidden twin)";
		}
		
		if (!r.isClear())
		{
			for (NineSlotGrid grid : sudoku.getGrids())
			{
				r = NakedTwinSolve(grid);
				if (r.isClear())
					break;
			}
		}
		
		if (!r.isClear())
		{
			r = gridSolveNext(sudoku);
			if (r.isClear())
				r.message += " (naked twin)";
		}
		
		return r;
		*/
	}
	
	private static Result primarySolveNext(SudokuGrid sudoku) throws UnsolvablePuzzleException
	{
		// Picks the next grid
		for (int gridX = 0; gridX < 3; gridX++)
		{
			for (int gridY = 0; gridY < 3; gridY++)
			{
				NineSlotGrid grid = sudoku.getGrid(gridX, gridY);
				
				// Picks the next target slot
				for (int x = 0; x < 3; x++)
				{
					for (int y = 0; y < 3; y++)
					{
						Slot slot = grid.getSlot(x, y);
						
						// Updates the possible numbers
						if (slot.updateRequired())
						{
							updateSlotNumbers(slot);
							if (slot.getPossibleNumbers().size() == 0)
								throw new UnsolvablePuzzleException(slot);
							
							// If there's only a single possible number, adds it to the slot
							if (slot.getPossibleNumbers().size() == 1)
							{
								slot.setNumber(slot.getPossibleNumbers().get(0));
								return new Result(true, "Only possible number", slot);
							}
						}
					}
				}
			}
		}
		
		return Result.failure();
	}
	
	// False is for rows
	private static Result secondarySolveNext(SudokuGrid sudoku, boolean forColumns) throws 
			UnsolvablePuzzleException
	{
		// Goes throug each grid and checks if a number must be on a certain column / row
		// Modifies other grids accordingly
		for (int gridX = 0; gridX < 3; gridX ++)
		{
			for (int gridY = 0; gridY < 3; gridY ++)
			{
				NineSlotGrid grid = sudoku.getGrid(gridX, gridY);
				List<Integer> remaining = NineSlotGrid.getRemainingNumbers(
						grid.getUsedNumbers());
				
				// Neighbor grids are either on the same row or on the same column
				List<NineSlotGrid> neighbors;
				if (forColumns)
					neighbors = sudoku.getGridColumn(gridX);
				else
					neighbors = sudoku.getGridRow(gridY);
				neighbors.remove(grid);
				
				for (Integer number : remaining)
				{
					// Grids that already have the number are not affected
					List<NineSlotGrid> affected = new ArrayList<>(); 
					for (NineSlotGrid otherGrid : neighbors)
					{
						if (!otherGrid.getUsedNumbers().contains(number))
							affected.add(otherGrid);
					}
					// If there are no affected grids, doesn't need checking
					if (affected.isEmpty())
						continue;
					
					int common = -1;
					boolean notPossible = false;
					
					for (int x = 0; x < 3; x++)
					{
						for (int y = 0; y < 3; y++)
						{
							Slot slot = grid.getSlot(x, y);
							if (slot.getPossibleNumbers().contains(number))
							{
								int position;
								if (forColumns)
									position = x;
								else
									position = y;
								
								if (common < 0)
								{
									common = position;
								}
								else if (position != common)
								{
									notPossible = true;
									break;
								}
							}
						}
					}
					
					if (!notPossible)
					{
						// If a common column / row was found, affects the other grids
						for (NineSlotGrid affectedGrid : affected)
						{
							List<Slot> affectedSlots;
							if (forColumns)
								affectedSlots = affectedGrid.getColumnSlots(common);
							else
								affectedSlots = affectedGrid.getRowSlots(common);
								
							for (Slot slot : affectedSlots)
							{
								if (!slot.hasNumber())
								{
									slot.removePossibleNumber(number);
									if (slot.getPossibleNumbers().size() == 0)
										throw new UnsolvablePuzzleException(slot);
									else if (slot.getPossibleNumbers().size() == 1)
									{
										slot.setNumber(slot.getPossibleNumbers().get(0));
										String message;
										if (forColumns)
											message = "Common column method";
										else
											message = "Common row method";
										return new Result(true, message, slot);
									}
								}
							}
						}
					}
				}
			}
		}
		
		return Result.failure();
	}
	
	// http://www.sudokudragon.com/sudokustrategy.htm#XL2104
	
	private static void filterHiddenTwins(NineSlotGrid grid)
	{
		// If there are two numbers that can appear in only two shared spots, no other number 
		// can appear in those spots
		// First finds the numbers that appear in exactly two spots
		List<Twin> twins = new ArrayList<>();
		List<Slot> slots = grid.getSlots();
		
		for (int number : NineSlotGrid.getRemainingNumbers(NineSlotGrid.getSlotNumbers(slots)))
		{
			List<Slot> spots = new ArrayList<>();
			
			for (Slot slot : slots)
			{
				if (slot.getPossibleNumbers().contains(number))
				{
					spots.add(slot);
					if (spots.size() > 2)
						break;
				}
			}
			
			if (spots.size() == 2)
				twins.add(new Twin(spots, number));
		}
		
		// Checks if there are multiple numbers for some twins
		for (int a = 0; a < twins.size(); a++)
		{
			Twin first = twins.get(a);
			
			for (int b = a + 1; b < twins.size(); b++)
			{
				Twin second = twins.get(b);
				
				// If so, removes the other possible numbers from those slots
				if (second.slots.containsAll(first.slots))
				{
					for (Slot slot : first.slots)
					{
						if (slot.getPossibleNumbers().size() > 2)
						{
							slot.getPossibleNumbers().clear();
							slot.addPossibleNumber(first.number);
							slot.addPossibleNumber(second.number);
						}
					}
				}
			}
		}
	}
	
	private static Result NakedTwinSolve(NineSlotGrid grid) throws UnsolvablePuzzleException
	{
		// If there are two slot that can only have two numbers, those numbers can't be put 
		// anywhere else
		
		// First searches for slots that fit only two numbers
		List<Slot> slots = grid.getSlots();
		List<Slot> twoNumberSlots = new ArrayList<>();
		for (Slot slot : slots)
		{
			if (slot.getPossibleNumbers().size() == 2)
				twoNumberSlots.add(slot);
		}
		
		// Tries to find slots that have same possible numbers
		for (int a = 0; a < twoNumberSlots.size(); a++)
		{
			Slot first = twoNumberSlots.get(a);
			
			for (int b = a + 1; b < twoNumberSlots.size(); b++)
			{
				Slot second = twoNumberSlots.get(b);
				
				if (first.getPossibleNumbers().containsAll(second.getPossibleNumbers()))
				{
					// If a twin is found, removes the numbers from other slots
					for (Slot slot : slots)
					{
						if (!slot.hasNumber() && !slot.equals(first) && !slot.equals(second))
						{
							slot.getPossibleNumbers().removeAll(first.getPossibleNumbers());
							if (slot.getPossibleNumbers().size() == 0)
								throw new UnsolvablePuzzleException(slot);
							else if (slot.getPossibleNumbers().size() == 1)
							{
								slot.setNumber(slot.getPossibleNumbers().get(0));
								return new Result(true, "Naked twin solve", slot);
							}
						}
					}
				}
			}
		}
		
		return Result.failure();
	}
	
	private static Result gridSolveNext(SudokuGrid sudoku) throws UnsolvablePuzzleException
	{
		// Tries to fill the grids after the changes in previous methods
		for (int x = 0; x < 3; x++)
		{
			for (int y = 0; y < 3; y++)
			{
				Result r = fillGrid(sudoku.getGrid(x, y));
				if (r.isClear())
					return r;
			}
		}
		
		return Result.failure();
	}
	
	private static void updateSlotNumbers(Slot slot)
	{
		// Finds all the numbers that remain
		List<Integer> possible = NineSlotGrid.getRemainingNumbers(
				NineSlotGrid.getSlotNumbers(slot.getConnectedSlots()));
		slot.setPossibleNumbers(possible);
		slot.setUpdateRequired(false);
	}
	
	private static Result fillGrid(NineSlotGrid grid) throws UnsolvablePuzzleException
	{
		// If a number fits only one place, adds it to the grid
		List<Slot> slots = grid.getSlots();
		List<Integer> remainingNumbers = NineSlotGrid.getRemainingNumbers(
				NineSlotGrid.getSlotNumbers(slots));
		
		for (Integer number : remainingNumbers)
		{
			Slot firstFit = null;
			boolean tooManySlots = false;
			
			for (Slot slot : slots)
			{
				if (slot.getPossibleNumbers().contains(number))
				{
					if (firstFit == null)
						firstFit = slot;
					else
					{
						tooManySlots = true;
						break;
					}
				}
			}
			
			if (!tooManySlots)
			{
				if (firstFit == null)
					throw new UnsolvablePuzzleException(grid.getSlot(1, 1));
				firstFit.setNumber(number);
				return new Result(true, "Only place in the grid", firstFit);
			}
		}
		
		return Result.failure();
	}
	
	
	// SUBCLASSES	--------------------
	
	/**
	 * Result contains information about a puzzle solving step
	 * @author Mikko Hilpinen
	 * @since 9.8.2015
	 */
	public static class Result
	{
		// ATTRIBUTES	----------------
		
		private String message;
		private boolean isClear, isGuess;
		private Slot target;
		
		
		// CONSTRUCTOR	----------------
		
		/**
		 * Creates a new result
		 * @param clear Was the solve successful
		 * @param message A message given with the result
		 * @param target The slot that was changed (if any)
		 */
		public Result(boolean clear, String message, Slot target)
		{
			this.message = message;
			this.isClear = clear;
			this.target = target;
			this.isGuess = false;
		}
		
		
		// ACCESSORS	----------------
		
		/**
		 * @return Did the result solve a piece of the puzzle
		 */
		public boolean isClear()
		{
			return this.isClear;
		}
		
		/**
		 * @return The message given with the result (if any)
		 */
		public String getMessage()
		{
			return this.message;
		}
		
		/**
		 * @return The slot that was changed with the result (if applicable)
		 */
		public Slot getTarget()
		{
			return this.target;
		}
		
		
		// OTHER METHODS	------------
		
		/**
		 * @return Creates a result from a failed attempt
		 */
		public static Result failure()
		{
			return new Result(false, "Failure", null);
		}
		
		private static Result guess(StepData risk, Slot newSlot)
		{
			Result r = new Result(true, "Guess (1 / " +risk.possibilities + ")", 
					newSlot);
			r.isGuess = true;
			return r;
		}
	}
	
	private static class StepData
	{
		// ATTRIBUTES	--------------
		
		private SudokuGrid targetGrid;
		private Slot targetSlot;
		private int newNumber;
		private int possibilities;
		
		
		// CONSTRUCTOR	--------------
		
		public StepData(SudokuGrid grid, Slot slot, int newNumber, int possibilities)
		{
			this.targetGrid = grid;
			this.targetSlot = slot;
			this.newNumber = newNumber;
			this.possibilities = possibilities;
		}
	}
	
	private static class Twin
	{
		// ATTRIBUTES	-------------
		
		private List<Slot> slots;
		private int number;
		
		
		// CONSTRUCTOR	-------------
		
		public Twin(List<Slot> slots, int number)
		{
			this.slots = slots;
			this.number = number;
		}
	}
	
	private static class CompleteHook
	{
		// ATTRIBUTES	-------------
		
		private List<Slot> stems;
		private Slot hook;
		
		
		// CONSTRUCTOR	-------------
		
		public CompleteHook(List<Slot> stems, Slot hook)
		{
			this.stems = stems;
			this.hook = hook;
			
			// Validates the hook
			/*
			if (stems.size() != 2)
				System.err.println("Invalid stem size");
			
			Vector3D position1 = stems.get(0).getPosition();
			Vector3D position2 = stems.get(1).getPosition();
			if ((position1.getFirstInt() != position2.getFirstInt()) && 
					(position1.getSecondInt() != position2.getSecondInt()))
			{
				System.err.println("Stems are not on the same row / column");
				System.err.println(position1);
				System.err.println(position2);
			}
			
			if (!hook.getGrid().equals(stems.get(0).getGrid()) && !hook.getGrid().equals(
					stems.get(1).getGrid()))
				System.err.println("Hook doesn't share a grid with a stem");
			
			if (Slot.getSharedNumbers(stems.get(0), stems.get(1)).size() != 1)
				System.err.println("Possible stem numbers don't link together");
			
			if (Slot.getSharedNumbers(hook, stems.get(0)).size() != 1 || 
					Slot.getSharedNumbers(hook, stems.get(1)).size() != 1)
				System.err.println("Possible hook numbers don't link with the stems");
				*/
		}
		
		
		// OTHER METHODS	---------
		
		public Slot getOutsideStem()
		{
			for (Slot stem : this.stems)
			{
				if (!stem.getGrid().equals(this.hook.getGrid()))
				{
					return stem;
				}
			}
			
			return null;
		}
		
		public StepData getBestGuess()
		{
			// The best target slot is the stem outside the hook grid
			Slot best = getOutsideStem();
			
			// The new number is the one shared between the stems
			int newNumber = -1;
			for (int number : this.stems.get(0).getPossibleNumbers())
			{
				if (this.stems.get(1).getPossibleNumbers().contains(number))
				{
					newNumber = number;
					break;
				}
			}
			
			return new StepData(best.getGrid().getGrid(), best, newNumber, 
					best.getPossibleNumbers().size());
		}
		/*
		public void mark()
		{
			this.hook.mark();
			this.stems.get(0).mark();
			this.stems.get(1).mark();
		}
		*/
	}
}
