package quest.field;

import quest.*;
import java.io.*;
import java.util.*;

/*
 * The functional unit of the field engine.
 * Each room corresponds to an NQR file. In fact, all of the data
 * wrapped by each Room is loaded directly from that room's NQR.
 */

public class Room {

	public static final int TILEW = 24, TILEH = 24;
	
	private int w, h;
	private Walkmap walkmap;
	private Tilemap tilemap;
	private SpriteBank spriteBank;
	private List<RObject> objects;
	private HashMap<String,Actor> actors;
	private Sprite tileset;
	private String initScript;
	
	Room( String filepath)
	{
		spriteBank = new SpriteBank();
		objects = new ArrayList<RObject>();
		actors = new HashMap<String,Actor>( 128);
		
		try {

			//open the room descriptor file
			InputStream baseStream = getClass().getResourceAsStream(filepath);
			DataInputStream stream = new DataInputStream(baseStream);
			
			//read in the header
			stream.read(); //version
			w = stream.read();
			h = stream.read();
			initScript = RObject.readPascalString( stream);
			stream.skipBytes(64); //reserved
			
			//read in the tileset's filepath, and load the tileset from file
			String tilepath = RObject.readPascalString( stream);
			tileset = new Sprite( Game.SPRITEPATH+"tilesets/"+tilepath);
			tileset.sliceFrames( Room.TILEW, Room.TILEH, 0, 0, 1, 1);
		
			//read in the walkdata, and construct a Walkmap from it
			byte[][] walkbuf = new byte[w][h];
			for( int y=0; y<h; y++)
			for( int x=0; x<w; x++)
			{
				walkbuf[x][y] = (byte) stream.read();
			}
			walkmap = new Walkmap( walkbuf);
			
			//read in the tiledata, likewise constructing a Tilemap
			byte[] tilebuf = new byte[w*h*12];
			stream.read( tilebuf, 0, tilebuf.length);
			tilemap = new Tilemap( w, h, tilebuf);
			
			//read in and construct each object
			int count = stream.read();
			for( int i=0; i<count; i++)
			{
				RObject obj = RObject.createFrom( stream, this);
				objects.add( obj);
				
				if ( obj instanceof Actor ) actors.put( obj.id.toLowerCase(), (Actor)obj);
			}
			
			//add the player to the list of actors
			actors.put( "player", FieldModule.get().getPlayer());
			actors.put( "nepeta", FieldModule.get().getPlayer());
			
			//clean up
			stream.close();
			
		} catch ( Exception ex ) { ex.printStackTrace(); return; }
	}
	
	public int getWidth() { return w; }
	public int getPixelWidth() { return w*TILEW; }
	public int getHeight() { return h; }
	public int getPixelHeight() { return h*TILEH; }
	public Walkmap getWalkmap() { return walkmap; }
	public Tilemap getTilemap() { return tilemap; }
	public List<RObject> getObjectList() { return objects; }
	public Map<String,Actor> getActorMap() { return actors; }
	public SpriteBank getSpriteBank() { return spriteBank; }
	public Sprite getTileset() { return tileset; }
	public String getInitScript() { return initScript; }
}
