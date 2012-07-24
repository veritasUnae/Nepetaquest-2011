package quest.field;

import quest.*;

/*
 * A three-dimensional layout for room tiles.
 * Each grid rectangle contains a stack of up 
 * to four tiles, each with its own depth offset.
 */

public class Tilemap {
	
	private Stack[][] data;
	private int w, h;

	/*
	 * A stack of four tiles.
	 * Negative values for tx or ty imply an empty tile.
	 */
	
	class Stack { 
		
		public int[] tx, ty, d;
		
		Stack()
		{
			tx = new int[4]; //tile's location in tileset
			ty = new int[4];
			d = new int[4];  //tile's depth offset
			
			for( int i=0; i<4; i++)
			{ tx[i] = -1; ty[i] = -1; d[i] = 0; }
		}
		
		public Stack getCopy()
		{
			Stack s = new Stack();
			for( int i=0; i<4; i++)
			{ s.tx[i] = tx[i]; s.ty[i] = ty[i]; s.d[i] = d[i]; }
			
			return s;
		}
	}
	
	/*
	 * Constructors, empty and from room record data.
	 */
	
	Tilemap( int width, int height)
	{
		w = width;
		h = height;
		data = new Stack[w][h];
		
		for( int i=0; i<w; i++)
		for( int j=0; j<h; j++) 
		data[i][j] = new Stack();
	}
	
	Tilemap( int width, int height, byte[] src)
	{
		w = width;
		h = height;
		data = new Stack[w][h];
		
		//src holds the tilemap as encoded in room record files
		int q = 0;
		for( int y=0; y<h; y++)
		for( int x=0; x<w; x++)
		{
			data[x][y] = new Stack();
			
			for( int i=0; i<4; i++)
			{
				data[x][y].tx[i] = src[q];
				data[x][y].ty[i] = src[q+1];
				data[x][y].d[i]  = src[q+2]-127;
				
				if ( data[x][y].tx[i] == 0xff ) data[x][y].tx[i] = -1;
				if ( data[x][y].ty[i] == 0xff ) data[x][y].ty[i] = -1;
				
				q += 3;
			}
		}
	}
	
	/*
	 * Render the tilemap to a given DrawOp
	 */
	
	public void drawTo( DrawOp op, Sprite tileset, int vx, int vy)
	{
		if ( tileset == null ) return;
		
		//establish which tiles need to be drawn
		int x1 = (vx-(vx%Room.TILEW))/Room.TILEW - 1;
		int y1 = (vy-(vy%Room.TILEH))/Room.TILEH - 1;
		int x2 = x1 + 2 + (Game.SCREENW-(Game.SCREENW%Room.TILEW))/Room.TILEW;
		int y2 = y1 + 2 + (Game.SCREENH-(Game.SCREENH%Room.TILEH))/Room.TILEH;
		
		//crop the drawing region to the drawable region
		if ( x1 < 0 ) x1 = 0;
		if ( y1 < 0 ) y1 = 0;
		if ( x2 > w-1 ) x2 = w-1;
		if ( y2 > h-1 ) y2 = h-1;
		
		//render to the DrawOp
		for( int y=y1; y<=y2; y++)
		for( int x=x1; x<=x2; x++)
		{
			Stack s = data[x][y];
			
			for( int i=0; i<4; i++)
			{
				if ( s.tx[i] >= 0 && s.ty[i] >= 0 )
				{
					op.drawTile( tileset, x*Room.TILEW, y*Room.TILEH, (s.d[i]-y)*Room.TILEH, s.tx[i], s.ty[i]);
				}
			}
		}
	}
	
	/*
	 * Getters and setters.
	 */
	
	public Stack getStackReference( int x, int y)
	{
		if ( x < 0 || x >= w || y < 0 || y >= h ) return null;
		return data[x][y];
	}
	
	public int getWidth() { return w; }
	public int getHeight() { return h; }
	
}
