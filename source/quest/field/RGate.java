package quest.field;

import java.io.*;

/*
 * Gates are room objects which trigger FieldModule.switchToRoom()
 * when the player enters a particular rectangular area.
 */

public class RGate extends RObject {
	
	private int destX, destY;
	private String destroom;
	private int movemode;

	/*
	 * Construct an RGate from the given object
	 * record, with a three-byte inset.
	 */
	
	RGate( InputStream istream, Room room)
	{
		type= 'G';
		
		try {
			
			DataInputStream stream = new DataInputStream(istream);
			
			//header
			readHeader( stream);
			
			//destination room and location
			destroom = readPascalString( stream);
			destX = stream.readShort();
			destY = stream.readShort();
			
			//size
			w = stream.readShort();
			h = stream.readShort();
			x += w/2; y += h/2;
			
			//direction of movement
			movemode = stream.read();
		
		} catch ( IOException exIO ) { return; }
	}
	
	/*
	 * Collision event. Switch to the destination room.
	 */
	
	public void onContact()
	{
		FieldModule module = FieldModule.get();
		
		if ( module.getPlayer().dir == movemode-1 )
		module.switchToRoom( destroom, movemode+1, destX, destY);
	}
	
	/*
	 * Null events.
	 */
	
	public void onStep() {}
	public void onInteract() {}
	public void onDraw(DrawOp op) {}
	public void onDestroy() {}

}
