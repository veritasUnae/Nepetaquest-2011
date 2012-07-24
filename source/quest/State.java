package quest;

import java.io.*;
import java.util.*;

/*
 * Stores information about the game's persistent state - player's current 
 * overworld location, enemies defeated and levels beaten, that kind of thing.
 * 
 * Game saving and loading is achieved by serializing and unserializing State.
 */

public class State implements Serializable {
	
	private static final long serialVersionUID = -7958252286683749069L;
	
	//not really sure what this is for, but BattleModule depends on it
	public int currentPath = 0; 
	
	//field variable storage
	public String currentRoom;
	public int playerX, playerY;
	public Map<String,int[]> roomVars; //each room gets an int[16], indexed by room filename
	
	public State() { 
		
		//constructing a blank State for newgame
		roomVars = new HashMap<String,int[]>(64);
		
		//traverse the rooms directory, allocating variables for each room
		Directory dir = new Directory( Game.DATAPATH);
		List<String> roomlist = dir.getFileList();
		
		ListIterator<String> it = roomlist.listIterator();
		while ( it.hasNext() )
		{
			String roomname = it.next();
			if ( roomname.endsWith(".nqr") )
			{
				int[] zeroarray = new int[16];
				for( int i=0; i<16; i++) zeroarray[i] = 0;
				roomVars.put( roomname, zeroarray);
			}
		}
	}
	
	/*
	 * Todo: Provide a State( String) constructor that loads 
	 * directly from a saved game. The static loadState function
	 * seems a bit off, logically speaking.
	 */
	
	/*
	 * Todo: Getters and setters for room and event variables.
	 * Important to provide a universal interface.
	 */
	
	public void saveState(String filename) {
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this);
			oos.close();
		} catch (IOException ioe) {ioe.printStackTrace();}
	}
	
	public static State loadState(String filename) {
		State loadedState;
		try {
			FileInputStream fis = new FileInputStream(filename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			try {
				loadedState = (State)ois.readObject();
			} catch (ClassNotFoundException cnfe) {
				cnfe.printStackTrace();
				loadedState = new State();
			}
			ois.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			loadedState = new State();
		}
		return loadedState;
	}
	
}
