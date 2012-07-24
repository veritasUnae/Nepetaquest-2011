package quest.field;

import java.io.*;

/*
 * A very simple room object which executes a specific script
 * on contact with the player.
 */

public class RTrigger extends RObject {
	
	String script;
	
	RTrigger( InputStream istream, Room room)
	{
		type = 'T';
		
		try {
			
			DataInputStream stream = new DataInputStream(istream);
			
			//header
			readHeader(stream);
			
			//size
			w = stream.readShort();
			h = stream.readShort();
			x += w/2; y += h/2;
			
			//script
			script = readPascalString(stream);
			
		} catch ( IOException exIO ) { return; }
	}

	public void onContact() 
	{
		disable();
		FieldModule.get().executeScript( new FieldScript(script));
	}

	public void onDestroy() 
	{
	}

	public void onDraw(DrawOp op) 
	{
	}

	public void onInteract() 
	{
	}

	public void onStep() 
	{
	}

}
