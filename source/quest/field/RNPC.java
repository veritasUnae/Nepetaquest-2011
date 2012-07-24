package quest.field;

import java.awt.*;
import java.util.*;
import java.io.*;
import quest.*;

/*
 * NPCs are the most complicated room objects at the time of writing.
 * They can be thought of in three ways:
 * 
 * Firstly, they're room objects. They therefore need to load themselves
 * from an NQR file, handle specific events (particularly onInteract),
 * and draw themselves.
 * 
 * Secondly, they implement Actor, such that they can be controlled by
 * FieldScripts. Animations are the most complicated part of this; they're loaded
 * by traversing the sprites/walksprites/animations directory, and then
 * indexed using a name derived from their filename (eg "nepeta_dance.png" -> "dance").
 * This allows FieldScripts to invoke animations by name, rather than using 
 * some kind of restrictive indexing system.
 * 
 * Thirdly, they can move around autonomously. Objects derived from
 * RNPC (such as REnemy) can abandon this functionality by overriding onStep().
 * IMPORTANT NOTE: If you add any functionality to onStep() other than this
 * roaming behaviour, it must also be added to derived classes.
 */

public class RNPC extends RObject implements Actor {

	/*
	 * Variables.
	 */
	
	String spritepath;
	String script;
	
	//positioning, movement and roaming
	int startx, starty;
	int dir, roamSpeed;
	boolean roaming, isMoving = false;
	int roamDistance, timeout = 0;
	
	//base sprite data
	Sprite walksprite;
	int spritew, spriteh;
	int originx, originy;
	int frame = 0;
	boolean movedThisStep = false;
	
	//animations
	HashMap<String,Sprite> animsprites = new HashMap<String,Sprite>(10);
	String currentAnim = null;
	int animFramerate = 1, animRepeats = 0;
	int walkAnimSpeed = 5;
	
	//collisions
	Room room;
	Walkmap walkmap;
	int collHandle = -1;
	
	/*
	 * Constructor.
	 */
	
	RNPC( InputStream istream, Room room)
	{
		type = 'N';
		
		//load data from stream
		try {
			
			DataInputStream stream = new DataInputStream(istream);
			
			//header
			readHeader( stream);
			
			//sprite filename
			spritepath = readPascalString( stream);
			
			//data body
			w = stream.read();
			h = stream.read();
			originx = stream.read();
			originy = stream.read();
			dir = stream.read()-1; if ( dir < 0 ) dir = 0;
			roaming = (stream.read() != 0);
			roamDistance = stream.readShort();
			roamSpeed = stream.read();
			
			//field script
			script = readPascalString( stream);
			
		} catch ( IOException exIO ) { return; }
		
		//load and slice the walksprite
		SpriteBank bank = room.getSpriteBank();
		walksprite = bank.loadSprite( Game.SPRITEPATH + "walksprites/" + spritepath);
		spritew = walksprite.getRawWidth()/4;
		spriteh = walksprite.getRawHeight();
		walksprite.sliceFrames( spritew, spriteh, 0, 0, 4, 20);
		
		//open the animation sprites directory
		Directory sprdir = new Directory( Game.SPRITEPATH + "walksprites/animations");
		java.util.List<String> files = sprdir.getFileList();
		
		//traverse it, loading any animations named after this NPC
		ListIterator<String> it = files.listIterator();
		while( it.hasNext() )
		{
			String file = it.next();
			String npcname = spritepath.substring( 0, spritepath.length()-4);
			if ( file.startsWith(npcname) )
			{
				String sub = file.substring( npcname.length()+1, file.length()-4);
				Sprite sprite = bank.loadSprite(Game.SPRITEPATH+"walksprites/animations/"+file);
				sprite.sliceFrames( spritew, spriteh, 0, 0, 100, 100);
				animsprites.put( sub, sprite);
			}
		}
		
		//register collision rectangle
		this.room = room;
		walkmap = room.getWalkmap();
		if ( enabled )
		{
			Rectangle rect = new Rectangle( x-w/2, y-h/2, w, h);
			collHandle = walkmap.registerRect( rect);
		}
		
		startx = x;
		starty = y;
	}
	
	/*
	 * RObject methods.
	 */

	public void onDraw(DrawOp op) 
	{
		frame++;
		
		int depth = -(y+spriteh-originy);
		if ( originy > spriteh ) depth = -y;
		
		if ( currentAnim == null || currentAnim.isEmpty() )
		{
			if ( !movedThisStep || animsprites.get("move") == null ) 
			{
				if ( animsprites.get("idle") == null )
				{
					frame = 0;
					op.drawSprite( walksprite, x-originx, y-originy, depth, dir);
				}
				else
				{
					int framesPerLine = (animsprites.get("idle").getRawWidth()+(spritew/2))/spritew;
					op.drawTile( animsprites.get("idle"), x-originx, y-originy, depth,
							 (frame/walkAnimSpeed)%framesPerLine, dir);
				}
			}
			else 
			{
				int framesPerLine = (animsprites.get("move").getRawWidth()+(spritew/2))/spritew;
				op.drawTile( animsprites.get("move"), x-originx, y-originy, depth,
							 (frame/walkAnimSpeed)%framesPerLine, dir);
			}
			
			movedThisStep = false;
		}
		else
		{
			Sprite spr = animsprites.get( currentAnim);
			op.drawSprite( spr, x-originx, y-originy, -(y+(walksprite.getHeight()-originy)), frame/animFramerate);
			
			if ( spr.getRawWidth()/spr.getWidth() <= frame/animFramerate && frame%animFramerate == 0 )
			{
				frame = 0;
				
				if ( animRepeats > 0 ) animRepeats--;
				else currentAnim = null;
			}
		}
	}

	public void onInteract() 
	{
		//turn to face the player
		Player player = FieldModule.get().getPlayer();
		int xdiff = player.x-x;
		int ydiff = player.y-y;
		
		if ( Math.abs(xdiff) > Math.abs(ydiff))
		{
			if ( xdiff > 0 ) face( Actor.EAST);
			else face( Actor.WEST);
		}
		else
		{
			if ( ydiff > 0 ) face( Actor.SOUTH);
			else face( Actor.NORTH);
		}
		
		//stop moving
		isMoving = false;
		
		//run interaction script
		if ( !script.isEmpty() )
		{
			FieldScript fscript = new FieldScript( script);
			FieldModule.get().executeScript( fscript);
		}
	}
	
	public void onStep() 
	{
		//roaming behaviour
		if ( roaming )
		{
			timeout--;
			
			if ( isMoving ) 
			{
				move( dir, roamSpeed);
				
				//randomly change direction
				if ( Math.random() < 0.1 && timeout < 0 ) 
				{
					dir = (int) Math.floor( Math.random()*4.0);
					timeout = 15;
				}
				
				//face back to origin if it's far away
				if ( x - startx > roamDistance  ) { face( Actor.WEST); timeout = 15; }
				if ( x - startx < -roamDistance ) { face( Actor.EAST); timeout = 15; }
				if ( y - starty > roamDistance  ) { face( Actor.NORTH); timeout = 15; }
				if ( y - starty < -roamDistance ) { face( Actor.SOUTH); timeout = 15; }
				
				//randomly stop
				if ( Math.random() < 0.01 ) isMoving = false;
			}
			else
			{
				//randomly start moving
				if ( Math.random() < 0.02 ) isMoving = true;
			}
		}
	}
	
	public void onDestroy() 
	{
		walkmap.unregisterRect( collHandle);
	}
	
	public void onContact() {}
	
	public void disable()
	{
		super.disable();
		walkmap.unregisterRect( collHandle);
	}
	
	public void enable()
	{
		super.enable();
		Rectangle rect = new Rectangle( x-w/2, y-h/2, w, h);
		collHandle = walkmap.registerRect( rect);
	}

	/*
	 * Actor methods
	 */

	public void move(int dir, int dist) 
	{
		this.dir = dir;
		
		if ( dist != 0 )
		{
			int xvel = 0;
			int yvel = 0;
			
			if ( dir == NORTH ) yvel -= dist;
			if ( dir == EAST  ) xvel += dist;
			if ( dir == SOUTH ) yvel += dist;
			if ( dir == WEST  ) xvel -= dist;
			
			Point dest = walkmap.moveAgainst( x, y, w, h, xvel, yvel, collHandle);			
			if ( dest.x != x || dest.y != y )
			{
				x = dest.x;
				y = dest.y;
				
				movedThisStep = true;
			}
			
			Rectangle rect = new Rectangle( x-w/2, y-h/2, w, h);
			walkmap.updateRect( collHandle, rect);
		}
	}
	
	public void face(int dir) 
	{
		this.dir = dir;
	}

	public void teleport(int x, int y) 
	{
		this.x = x;
		this.y = y;
	}
	
	public void animate( String id, int framerate, int repeats)  
	{
		if ( !animsprites.containsKey(id) ) return;
		if ( repeats <= 0 ) return;
		if ( framerate <= 0 ) return;
		
		currentAnim = id;
		animFramerate = framerate;
		animRepeats = repeats;
		frame = 0;
	}

}
