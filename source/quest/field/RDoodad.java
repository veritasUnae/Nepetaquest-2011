package quest.field;

import quest.*;
import java.awt.*;
import java.io.*;

/*
 * Doodads are the simplest room objects.
 * They can be solid, they display a (possibly animated) graphic,
 * and they can execute a field script when interacted with.
 */

public class RDoodad extends RObject {

	private Sprite sprite;
	private String script;
	private int offx, offy;			//drawing offset
	private int framecount, delay; 	//animation variables
	private boolean overlay;
	
	private int frametick = 0, frame = 0;
	private Walkmap walkmap = null;
	private int collHandle = -1;
	
	/*
	 * Construct an RDoodad from the given object
	 * record, with a three-byte inset.
	 */
	
	RDoodad( InputStream istream, Room room)
	{
		type = 'D';
		
		String spritepath = null;
		
		try {
			
			DataInputStream stream = new DataInputStream(istream);
			
			//header
			readHeader( stream);
			
			//sprite filename
			spritepath = readPascalString( stream);
			
			//data body
			w = stream.read();
			h = stream.read();
			offx = stream.read();
			offy = stream.read();
			framecount = stream.read();
			delay = stream.read();
			overlay = (stream.read() != 0);
			
			//field script
			script = readPascalString( stream);
			
		} catch ( IOException exIO ) { return; }
		
		//load and slice the given sprite
		SpriteBank bank = room.getSpriteBank();
		sprite = bank.loadSprite( Game.SPRITEPATH + "doodads/" + spritepath);
		if ( framecount == 0 ) framecount = 1;
		sprite.sliceFrames( sprite.getRawWidth()/framecount, sprite.getHeight(), 0, 0, framecount, framecount);
	
		//register collision rectangle
		walkmap = room.getWalkmap();
		if ( enabled )
		{
			Rectangle rect = new Rectangle( x-w/2, y-h/2, w, h);
			collHandle = walkmap.registerRect( rect);
		}
	}
	
	/*
	 * Inspection event. Run the doodad's field script.
	 */
	
	public void onInteract()
	{
		if ( !script.isEmpty() )
		{
			FieldScript s = new FieldScript(script);
			FieldModule.get().executeScript( s);
		}
	}
	
	/*
	 * Drawing event. Animate the doodad. Draw the doodad's sprite.
	 */
	
	public void onDraw( DrawOp op)
	{
		frametick++;
		if ( frametick % delay == 0 ) frame++;
		frame = frame%framecount;
		
		if ( !overlay )
			op.drawSprite( sprite, x-offx, y-offy, -(y+h/2), frame);
		else
			op.drawSprite( sprite, x-offx, y-offy, -10000, frame);
	}
	
	/*
	 * Step event.
	 */
	
	public void onStep()
	{
	}
	
	/*
	 * Destruction event.
	 */
	
	public void onDestroy()
	{
		walkmap.unregisterRect( collHandle);
	}
	
	/*
	 * Enable and disable events.
	 * Remove and restore the collision rectangle.
	 */
	
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
	
	public void onContact() {}
}
