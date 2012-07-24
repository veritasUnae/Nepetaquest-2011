package quest;

/*
 * Stores the current state of the keyboard.
 * 
 * A single InputMap is always kept up-to-date by Game, and
 * should be accessed using Game.getGame().getInputMap().
 */

public class InputMap {

	public static final int PRESSED = 0, DOWN = 1;
	public static final int RELEASED = 2, UP = 3;
	
	/*
	 * keyArray indexes key states using the keycode constants
	 * in java.awt.event.KeyEvent (such as KeyEvent.VK_A).
	 */
	
	public int[] keyArray;
	
	/*
	 * Keeps track of the last time a key was released
	 */
	
	public long[] lastRelease;
	public boolean anyKey;
	
	public InputMap()
	{
		keyArray = new int[256];
		for( int i=0; i<256; i++) keyArray[i] = UP;
		lastRelease = new long[256];
		for(int i=0;i<256;i++) lastRelease[i] = 0;
		anyKey = false;
	}
	
	public boolean isKeyDown( int code) {
		if ( keyArray[code] == PRESSED || keyArray[code] == DOWN ) return true;
		else return false;
	}
	public boolean isKeyUp( int code) {
		if ( keyArray[code] == RELEASED || keyArray[code] == UP ) return true;
		else return false;
	}
	public boolean isKeyPressed( int code) {
		if ( keyArray[code] == PRESSED ) return true;
		else return false;
	}
	public boolean isKeyReleased( int code) {
		if ( keyArray[code] == RELEASED ) return true;
		else return false;
	}
}
