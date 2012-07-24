package quest.field;

import java.awt.*;

/*
 * A two-dimensional collision map, describing a room's 
 * stairs and walkability. Also stores an array of collision
 * rectangles, for use by NPCs, doodads, etc.
 * Handles collision detection and response.
 */

public class Walkmap {

	private byte[][] data;
	private byte[][] data_slope;
	private int w, h;
	
	private Rectangle[] rects;
	private boolean[] rectFree;
	private static final int CAPACITY = 256;
	
	Walkmap( int width, int height)
	{
		w = width;
		h = height;
		data = new byte[w][h];
		data_slope = new byte[w][h];
		
		rects = new Rectangle[CAPACITY];
		rectFree = new boolean[CAPACITY];
		for( int i=0; i<rects.length; i++) 
		{
			rects[i] = new Rectangle();
			rectFree[i] = true;
		}
	}
	
	Walkmap( byte[][] src)
	{
		w = src.length;
		h = src[0].length;
		
		data = new byte[w][h];
		data_slope = new byte[w][h];
		
		for( int x=0; x<w; x++)
		for( int y=0; y<h; y++)
		{
			data[x][y] = (byte)(src[x][y]&0x0f);
			data_slope[x][y] = (byte)(src[x][y]>>4);
		}
		
		rects = new Rectangle[CAPACITY];
		rectFree = new boolean[CAPACITY];
		for( int i=0; i<rects.length; i++) 
		{
			rects[i] = new Rectangle();
			rectFree[i] = true;
		}
	}
	
	/*
	 * Collision rectangle functions.
	 */
	
	public int registerRect( Rectangle rect)
	{
		int handle = -1;
		for( int i=0; i<CAPACITY; i++) if ( rectFree[i] ) handle = i;
		
		if ( handle == -1 ) return -1;
		
		rects[handle].x = rect.x;
		rects[handle].y = rect.y;
		rects[handle].width = rect.width;
		rects[handle].height = rect.height;
		rectFree[handle] = false;
		return handle;
	}
	
	public void updateRect( int handle, Rectangle rect)
	{
		if ( handle < CAPACITY && handle >= 0 )
		{
			if ( rectFree[handle] == false )
			{
				rects[handle].x = rect.x;
				rects[handle].y = rect.y;
				rects[handle].width = rect.width;
				rects[handle].height = rect.height;
			}
		}
	}
	
	public void unregisterRect( int handle)
	{
		if ( handle != -1 )
		rectFree[handle] = true;
	}
	
	/*
	 * Collision handling.
	 */
	
	public Point moveAgainst( int x, int y, int tw, int th, int xvel, int yvel, int rectToIgnore)
	{
		//returns the resulting location if a rectangular entity of
		//size (tw,th) and centre (x,y) were to attempt to move 
		//across the tilemap with the given velocity.
		
		Point p = new Point(x,y);
		
		int xdiff = Integer.signum(xvel);
		int ydiff = Integer.signum(yvel);
		
		int hlimit = (int) Math.floor( (Room.TILEW/(double)Room.TILEH)+0.1) + 1;
		int vlimit = (int) Math.floor( (Room.TILEH/(double)Room.TILEW)+0.1) + 1;
		
		//horizontal movement
		if ( xdiff != 0 )
		for( int c=0; Math.abs(c)<Math.abs(xvel); c+=xdiff )
		{
			if ( doesIntersect(p.x+xdiff, p.y, tw, th, rectToIgnore) == false ) p.x += xdiff;
			else
			{
				//allow for "sliding" across slopes
				boolean moveDone = false;
				for( int i=1; i<=vlimit && !moveDone; i++)
				{
					if ( doesIntersect(p.x+xdiff, p.y+i, tw, th, rectToIgnore) == false ) 
					{ p.x += xdiff; p.y += i; moveDone = true; }
					else if ( doesIntersect(p.x+xdiff, p.y-i, tw, th, rectToIgnore) == false ) 
					{ p.x += xdiff; p.y -= i; moveDone = true; }
				}
				
				if ( moveDone ) c+=xdiff;
			}
		}
		
		//response to sloped floors
		float aspect = Room.TILEW/(float)Room.TILEH;
		if ( slopeAt(x,y) == 1 ) { yvel -= (int)(((float)xvel)/aspect); ydiff = Integer.signum(yvel); }
		if ( slopeAt(x,y) == 2 ) { yvel += (int)(((float)xvel)/aspect); ydiff = Integer.signum(yvel); }
		
		//vertical movement
		if ( ydiff != 0 )
		for( int c=0; Math.abs(c)<Math.abs(yvel); c+=ydiff )
		{
			if ( doesIntersect(p.x, p.y+ydiff, tw, th, rectToIgnore) == false ) p.y += ydiff;
			else if ( slopeAt(x,y) == 0 )
			{
				//allow for "sliding" across slopes
				boolean moveDone = false;
				for( int i=1; i<=hlimit && !moveDone; i++)
				{
					if ( doesIntersect(p.x+i, p.y+ydiff, tw, th, rectToIgnore) == false ) 
					{ p.x += i; p.y += ydiff; moveDone = true;}
					else if ( doesIntersect(p.x-i, p.y+ydiff, tw, th, rectToIgnore) == false ) 
					{ p.x -= i; p.y += ydiff; moveDone = true; }
				}
				
				if ( moveDone ) c+=ydiff;
			}
		}
		
		return p;
	}
	
	private boolean doesIntersect( int x, int y, int tw, int th, int rectToIgnore)
	{
		//returns whether a (tw,th) entity with center (x,y) intersects
		//with any part of the collision map
		
		int x1 = x - tw/2;
		int y1 = y - th/2;
		int x2 = x + tw/2 - 1;
		int y2 = y + th/2 - 1;
		
		int modx1 = x1%Room.TILEW; if ( modx1 < 0 ) modx1 += 24;
		int mody1 = y1%Room.TILEH; if ( mody1 < 0 ) mody1 += 24;
		
		x1 = (x1-modx1)/Room.TILEW;
		y1 = (y1-mody1)/Room.TILEH;
		x2 = (x2-(x2%Room.TILEW))/Room.TILEW;
		y2 = (y2-(y2%Room.TILEH))/Room.TILEH;
		
		//test each tile
		for( int ix = x1; ix <= x2; ix++)
		for( int iy = y1; iy <= y2; iy++)
		{
			if ( getTile(ix,iy) == 1 ) return true;
			
			if ( getTile(ix,iy) > 1 && getTile(ix,iy) <= 5 )
			if ( doesIntersectTile( ix, iy, x, y, tw, th) ) return true;
		}
		
		Rectangle entity = new Rectangle( x-tw/2, y-th/2, tw, th);
		
		//test each collision rect
		for( int i=0; i<CAPACITY; i++)
		{
			if ( i != rectToIgnore )
			if ( rectFree[i] == false )
			if ( rects[i].intersects(entity) ) return true;
		}
		
		return false;
	}
	
	private boolean doesIntersectTile( int tx, int ty, int x, int y, int tw, int th)
	{
		//returns whether a (tw,th) entity with center (x,y) 
		//intersects with the tile in gridsquare (tx,ty)
		
		Rectangle rectTile = new Rectangle( tx*Room.TILEW, ty*Room.TILEH, Room.TILEW, Room.TILEH);
		Rectangle rectEntity = new Rectangle( x-tw/2, y-th/2, tw, th);
		
		//retrieve the intersection of the two rectangles
		if ( rectTile.intersects(rectEntity) == false ) return false;
		Rectangle rect = rectTile.intersection(rectEntity);
		
		//set that intersection relative to the tile
		rect.x -= rectTile.x; rect.y -= rectTile.y;
		
		int type = getTile(tx,ty);
		float px, py;
		
		switch( type )
		{
			//topright solid
			case 2:
				px = (rect.x+rect.width)/(float)Room.TILEW;
				py = rect.y/(float)Room.TILEH;
				if ( py < px ) return true;
			break;
			
			//bottomright solid
			case 3:
				px = (rect.x+rect.width)/(float)Room.TILEW;
				py = (rect.y+rect.height)/(float)Room.TILEH;
				if ( py > 0.999-px ) return true;
			break;	
			
			//bottomleft solid
			case 4:
				px = rect.x/(float)Room.TILEW;
				py = (rect.y+rect.height)/(float)Room.TILEH;
				if ( py > px ) return true;
			break;
			
			//topleft solid
			case 5:
				px = rect.x/(float)Room.TILEW;
				py = rect.y/(float)Room.TILEH;
				if ( py < 0.999-px ) return true;
			break;
		}
		
		return false;
	}
	
	private int slopeAt( int x, int y)
	{
		int tx = (x-(x%Room.TILEW))/Room.TILEW;
		int ty = (y-(y%Room.TILEH))/Room.TILEH;
		
		if ( tx < 0 || tx >= w || y < 0 || ty >= h ) return 0;
		else return (int)data_slope[tx][ty];
	}
	
	/*
	 * Getters and setters
	 */
	
	public byte getTile( int x, int y)
	{
		if ( x < 0 || x >= w || y < 0 || y >= h ) return 1;
		return data[x][y];
	}
	
	public void setTile( int x, int y, byte val)
	{
		if ( x < 0 || x >= w || y < 0 || y >= h ) return;
		data[x][y] = val;
	}
	
	public byte getSlope( int x, int y)
	{
		if ( x < 0 || x >= w || y < 0 || y >= h ) return 0;
		return data_slope[x][y];
	}
	
	public void setSlope( int x, int y, byte val)
	{
		if ( x < 0 || x >= w || y < 0 || y >= h ) return;
		data_slope[x][y] = val;
	}
	
	public int getWidth() { return w; }
	public int getHeight() { return h; }
	
}
