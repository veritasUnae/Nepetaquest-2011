package quest.field;

import java.awt.Rectangle;
import java.io.*;

import quest.*;

/*
 *  REnemy extends RNPC because it's essentially identical to an NPC
 *  that doesn't roam, can't be inspected, and performs a very specific
 *  action on contact with the player.
 *  
 *  REnemy is a state machine that can be:
 *  -Idle. Moves around like a normal RNPC.
 *  -Noticing. Displays an exclamation mark, stands still for a few frames.
 *  -Chasing. Moves towards the player, performing basic pathfinding.
 *  -Returning. Stops chasing the player, returns to starting location.
 *  
 *  An important thing to note is that, since REnemy is derived from RNPC,
 *  Enemy records within NQR files are polymorphic with NPC records. See the
 *  NQR documentation for details.
 *
 *  In order to function, it requires knowledge of the player's current
 *  location. This is achieved by calling FieldModule.get().getPlayer().
 */

public class REnemy extends RNPC {
	
	enum State { IDLE, NOTICING,
		 		 CHASING, RETURNING }
	State state = State.IDLE;
	
	//variables used internally
	Player player;
	int lastDirChange = -1;
	int timeSpentInCurrentState = 1000;
	
	//configuration variables loaded from .NQR file
	private int alertDistance;
	private int chaseDistance;
	private int chaseSpeed;
	private boolean aggressive = true;
	private boolean sleeping = true;
	
	//shared graphics resources
	private static Sprite shadowSprite = null;
	private static Sprite alertSprite = null;
	
	/*
	 * Important RNPC variables include:
	 * 	int startx, starty;
	 * 	Map<String,Sprite> animsprites; (auto-loaded animations)
	 */
	
	REnemy( InputStream istream, Room room)
	{
		super( istream, room);
		
		//translate certain RNPC variables into REnemy variables
		walkAnimSpeed = 3;
		chaseSpeed = roamSpeed;
		roamSpeed = 1;
		chaseDistance = roamDistance;
		roamDistance = 120;
		aggressive = roaming;
		roaming = true;
		script = "";
		
		//load variables from NQR file
		try {
			
			DataInputStream stream = new DataInputStream(istream);
			
			sleeping = (stream.read() != 0);
			alertDistance = stream.readShort();
			
		} catch ( IOException ioe ) { return; }
		
		
		player = FieldModule.get().getPlayer();
		if ( alertSprite == null ) alertSprite = new Sprite( Game.SPRITEPATH+"enemy_alert.png");
		if ( shadowSprite == null ) shadowSprite = new Sprite( Game.SPRITEPATH+"enemy_shadow.png");
	}
	
	/*
	 * Step event.
	 */
	
	public void onStep()
	{
		//route to an appropriate state step event
		switch( state )
		{
			case IDLE:			stepIdle(); break;
			case NOTICING:		stepNoticing(); break;
			case CHASING:		stepChasing(); break;
			case RETURNING:		stepReturning(); break;
		}
		
		timeSpentInCurrentState++;
	}
	
	private void stepIdle()
	{
		if ( sleeping == false ) super.onStep();
		
		if ( distanceTo(player.x,player.y) < alertDistance && 
			 aggressive == true && timeSpentInCurrentState > 60 ) 
		{
			timeSpentInCurrentState = 0;
			state = State.NOTICING;
		}
	}
	
	private void stepNoticing()
	{
		dir = directionTo(player.x,player.y);
		
		if ( timeSpentInCurrentState > 15 ) 
		{
			timeSpentInCurrentState = 0;
			state = State.CHASING;
		}
	}
	
	private void stepChasing()
	{
		moveTowards( player.x, player.y, chaseSpeed);
		
		if ( distanceTo(startx,starty) > chaseDistance 
			&& distanceTo(player.x,player.y) > 72 )
		{
			timeSpentInCurrentState = 0;
			state = State.RETURNING;
		}
	}
	
	private void stepReturning()
	{
		moveTowards( startx, starty, chaseSpeed);
		
		if ( distanceTo(startx,starty) < 12 ) 
		{
			timeSpentInCurrentState = 0;
			state = State.IDLE;
		}
		
		if ( timeSpentInCurrentState > 300 )
		{
			startx = x;
			starty = y;
			timeSpentInCurrentState = 0;
			state = State.IDLE;
		}
	}
	
	/*
	 * Utility functions.
	 */
	
	private void moveTowards( int destx, int desty, int speed)
	{
		// Used by CHASING and RETURNING enemies
		
		//find direction to face towards destination
		int ndir = directionTo(destx,desty);
		
		//change to that direction with a slight delay (prevents jittering)
		if ( ndir != -1 ) 
		{
			if ( ndir != dir && lastDirChange < 0 )
			{ 
				dir = ndir; 
				lastDirChange = 10 + (int)(Math.random()*15.0); 
			}
		}
		lastDirChange--;
		
		//if currently moving away from nearby player, face player immediately
		if ( Math.abs(destx - x) + Math.abs(desty - y) < 24 )
		{
			if ( dir == NORTH && desty > y ) dir = ndir;
			if ( dir == SOUTH && desty < y ) dir = ndir;
			if ( dir == EAST && destx < x ) dir = ndir;
			if ( dir == WEST && destx > x ) dir = ndir;
		}
		
		//move in facing direction
		int curx = x, cury = y;
		move( dir, speed);
		
		//basic pathfinding. if unable to move, try the second-most-likely direction.
		if ( curx == x && cury == y && lastDirChange < 0 )
		{
			if ( dir == NORTH || dir == SOUTH )
			{
				if ( destx < x ) dir = WEST;
				else dir = EAST;
			}
			else if ( dir == WEST || dir == EAST )
			{
				if ( desty < y ) dir = NORTH;
				else dir = SOUTH;
			}
			lastDirChange = 10 + (int)(Math.random()*15.0);
			
			move( dir, 2);
		}
	}
	
	private int distanceTo( int x2, int y2)
	{
		return Math.abs(x2-x)+Math.abs(y2-y);
	}
	
	private int directionTo( int x2, int y2)
	{
		if ( Math.abs(x2 - x) > Math.abs(y2 - y) )
		{
			if ( x2 > x ) return EAST;
			else return WEST;
		}
		else
		{
			if ( y2 > y ) return SOUTH;
			else return NORTH;
		}
	}
	
	/*
	 * Remaining RObject functions.
	 */
	
	public Rectangle getBounds()
	{
		return new Rectangle( x-((w/2)+3), y-((h/2)+3), w+6, h+6);
	}
	
	public void onContact()
	{
		//trigger a battle somehow
		//for now, just vanish and blur the screen
		disable();
		FieldModule.get().TEMPORARY_battlefwoosh();
		FieldModule.get().textMessage( "NA", "Sorry, no battles yet.");
	}
	
	public void onDraw( DrawOp op)
	{
		//draw shadow, if flying
		if ( originy > spriteh + 3 )
		{
			op.drawSprite( shadowSprite, x-shadowSprite.getWidth()/2, 
						   y-shadowSprite.getHeight()/2, -(y+shadowSprite.getHeight()/2), 0);
		}
		
		//draw walksprite
		super.onDraw(op);
		
		if ( state == State.NOTICING )
		{
			//if it exists, execute "alert" animation
			//TODO
			
			//draw exclamation mark over player
			int alertOffset = timeSpentInCurrentState;
			if ( alertOffset > 9 ) alertOffset = 9;
			if ( alertOffset >= 6 ) alertOffset = 6-(alertOffset-6);
			
			op.drawSprite( alertSprite, x-alertSprite.getWidth()/2, 
						   y+2-(alertOffset+originy+alertSprite.getHeight()), -10000, 0);
		}
	}
	
	public void onInteract()
	{
	}
}
