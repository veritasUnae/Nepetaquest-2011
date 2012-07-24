package quest.field;

import java.awt.Point;
import java.awt.Rectangle;

import quest.*;

/*
 * The actor that represents the player on the field.
 * Controlled by FieldModule in response to input.
 */

public class Player implements Actor {

	public int x, y;
	public int w = 18, h = 12;
	
	public int dir;
	boolean movedThisStep = false;
	
	Sprite walksprite;
	Sprite walksprite_move;
	int spritew, spriteh;
	int frame = 0;
	
	int collHandle = -1;
	
	Player( int x, int y, int dir)
	{
		this.x = x;
		this.y = y;
		this.dir = dir;
		
		walksprite = new Sprite( Game.SPRITEPATH+"walksprites/nepeta.png");
		walksprite_move = new Sprite( Game.SPRITEPATH+"walksprites/animations/nepeta_move.png");
		spritew = walksprite.getRawWidth()/4;
		spriteh = walksprite.getRawHeight();
		walksprite.sliceFrames( spritew, spriteh, 0, 0, 4, 20);
		walksprite_move.sliceFrames( spritew, spriteh, 0, 0, 4, 20);
	}
	
	public void init( Room room)
	{
		/*
		 * This method is necessary to remove a circular dependency.
		 * Certain room objects require player info when constructed (eg REnemy).
		 * So, those parts of player construction which require room info
		 * must be delayed until the room has been fully constructed.
		 */
		
		Rectangle rect = new Rectangle( x-w/2, y-h/2, w, h);
		collHandle = room.getWalkmap().registerRect( rect);
	}
	
	public void face(int dir) 
	{
		this.dir = dir;
	}
	
	public void move(int dir, int dist) 
	{
		Walkmap walkmap = FieldModule.get().getRoom().getWalkmap();
		this.dir = dir;
		
		int xvel = 0;
		int yvel = 0;
		
		if ( dir == NORTH ) yvel -= dist;
		if ( dir == EAST  ) xvel += dist;
		if ( dir == SOUTH ) yvel += dist;
		if ( dir == WEST  ) xvel -= dist;
		
		Point dest = walkmap.moveAgainst( x, y, w, h, xvel, yvel, collHandle);
		x = dest.x;
		y = dest.y;
		
		Rectangle rect = new Rectangle( x-w/2, y-h/2, w, h);
		walkmap.updateRect( collHandle, rect);
		
		movedThisStep = true;
	}
	
	public void teleport( int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public void animate( String id, int framerate, int repeats) 
	{

	}
	
	public void draw( DrawOp op)
	{
		frame = (frame+1)%20;
		
		if ( !movedThisStep ) op.drawSprite( walksprite, x-12, y-30, -(y+3), dir);
		else op.drawTile( walksprite_move, x-12, y-30, -(y+3), (frame/5), dir);
		
		movedThisStep = false;
	}
	
}
