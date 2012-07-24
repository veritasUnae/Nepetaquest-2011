package quest.field;

/*
 * An interface which can be used to make an implementing
 * class "move like an NPC". Implemented by RNPC and Player.
 * Used by FieldScript (to execute commands such as MOVE),
 * and by FieldModule (to control the player).
 */

public interface Actor {
	
	public static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;
	public static final int MOVESPEED = 2; //default movement speed

	public abstract void move( int dir, int dist); //one step's worth of movement
	public abstract void teleport( int x, int y);
	public abstract void face( int dir);
	public abstract void animate( String id, int framerate, int repeats);
	
}