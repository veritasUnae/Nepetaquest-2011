package quest.field;

import java.io.*;
import java.awt.*;

/*
 * The parent class for all room objects.
 * Room objects are those components of a room other than the
 * walkmap and tilemap; NPCs, treasures, random debris, etc.
 * 
 * They are always initialised from data stored in an NQR file,
 * they always implement event methods which are invoked by
 * by FieldModule, and they can always be enabled or disabled.
 */

public abstract class RObject {

	public char type;
	public String id;
	public int x, y, w, h;
	
	protected boolean enabled;
	public boolean isEnabled() { return enabled; }
	public void enable() { enabled = true; }
	public void disable() { enabled = false; }
	
	public abstract void onStep();
	public abstract void onContact();
	public abstract void onInteract();
	public abstract void onDraw( DrawOp op);
	public abstract void onDestroy();
	
	/*
	 * Static method to parse object records.
	 * Reads in three bytes of the header, then passes
	 * control onto the relevant child constructor.
	 */
	
	public static RObject createFrom( DataInputStream stream, Room room)
	{
		RObject obj = null;
		
		try {
			
			//read in length
			int len = stream.read();
			len = (len<<8) | stream.read();
			
			//read in type
			char temptype = (char) stream.read();
			
			//construct an object of the correct type
			switch(temptype)
			{
				case 'D': 
					obj = new RDoodad( stream, room); 
				break;
				
				case 'G':
					obj = new RGate( stream, room);
				break;
				
				case 'N':
					obj = new RNPC( stream, room);
				break;
				
				case 'E':
					obj = new REnemy( stream, room);
				break;
				
				case 'T':
					obj = new RTrigger( stream, room);
				break;
				
				default: 
					stream.skip(len-1);
			}
			
		} catch ( IOException exIO ) { return null; }
		
		return obj;
	}
	
	/*
	 * Parses data which is shared by all objects.
	 * This should be called at the start of each child constructor.
	 */
	
	protected void readHeader( DataInputStream stream)
	{
		try {
			
			id = readPascalString( stream);
			x = stream.readShort();
			y = stream.readShort();
			enabled = stream.readBoolean();
			
		} catch ( IOException exIO ) { return; }
	}
	
	/*
	 * Utility function. Reads a string with the format "two
	 * bytes indicating length, followed by the string data".
	 */
	
	public static String readPascalString( DataInputStream stream)
	{
		try {
			
			int len = stream.readShort();
			byte[] buf = new byte[len];
			stream.readFully( buf);
			return new String( buf);
			
		} catch ( IOException exIO ) { return null; }
	}
	
	/*
	 * Utility functions which interpret the RObject's x, y, w and h.
	 */
	
	public Rectangle getBounds()
	{
		return new Rectangle( x-w/2, y-h/2, w, h);
	}
	
	public boolean isInteractable( int px, int py, int pw, int ph, int pdir)
	{
		Rectangle player = new Rectangle( px-pw/2, py-ph/2, pw, ph);
		Rectangle entity = getBounds();
		
		entity.x--; entity.y--;
		entity.width += 2;
		entity.height += 2;
		
		if ( entity.intersects(player) )
		{
			Rectangle union = entity.intersection(player);
			
			if ( union.width == 1 )
			{
				if ( union.x <= x - w/4 && pdir == 1 ) return true;
				if ( union.x >= x + w/4 && pdir == 3 ) return true;
			}
			else if ( union.height == 1 )
			{
				if ( union.y <= y - h/4 && pdir == 2 ) return true;
				if ( union.y >= y + h/4 && pdir == 0 ) return true;
			}
			else return true;
		}
			
		return false;
	}
}
