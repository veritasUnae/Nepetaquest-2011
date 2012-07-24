package quest.field;

import java.util.*;
import java.util.List;
import java.awt.*;
import quest.*;

/*
 * A sorted list in which drawing operations can be
 * accumulated. When flush() is called, all of the
 * queued drawing operations are executed immediately.
 * 
 * Drawing operations are sorted in negative depth order
 * (Ops with a lower depth are drawn "on top of" Ops
 * with a higher depth). Operations with equal depth 
 * are performed in the order they were passed to DrawOp.
 */

public class DrawOp {
	
	private List<Op> list;

	/*
	 * The Op class represents a single
	 * drawing operation.
	 */
	
	class Op {
		public Sprite sprite;
		public int x, y, depth;
		public boolean isTile;
		public int frame, tx, ty;
		
		Op( Sprite sprite, int x, int y, int depth, boolean isTile, int frame, int tx, int ty)
		{
			this.sprite = sprite;
			this.x = x; this.y = y; this.depth = depth;
			this.isTile = isTile; this.frame = frame;
			this.tx = tx; this.ty = ty;
		}
	}
	
	/*
	 * Constructor.
	 */
	
	DrawOp()
	{
		list = new LinkedList<Op>();
	}
	
	/*
	 * Sorted insertion into the list.
	 * Depth values decrease while the index increases.
	 */
	
	private void insertOp( Op op)
	{
		if ( list.size() == 0 ) 
		{
			list.add( op);
		}
		else
		{
			ListIterator<Op> it = list.listIterator();
			
			while ( it.hasNext() )
			{
				Op op2 = it.next();
				if ( op2.depth < op.depth ) break;
			}
			
			it.previous();
			if ( it.next().depth >= op.depth ) { it.add( op); }
			else { if ( it.hasPrevious() ) it.previous(); it.add( op); }
		}
	}
	
	/*
	 * Drawing operation requests.
	 * Coordinates provided are relative to the topleft corner of the room.
	 */
	
	public void drawSprite( Sprite sprite, int x, int y, int depth, int frame)
	{
		Op op = new Op( sprite, x, y, depth, false, frame, 0, 0);
		insertOp( op);
	}
	
	public void drawTile( Sprite sprite, int x, int y, int depth, int tx, int ty)
	{
		Op op = new Op( sprite, x, y, depth, true, 0, tx, ty);
		insertOp( op);
	}
	
	/*
	 * Rendering.
	 */
	
	public void flush( Graphics g, int offx, int offy)
	{
		//render the list from highest depth to lowest
		for( int i=0; i<list.size(); i++)
		{
			Op op = (Op) list.get(i);
			
			if ( op.isTile == false )
				op.sprite.drawFrame( g, op.x-offx, op.y-offy, op.frame);
			else
				op.sprite.drawTile( g, op.x-offx, op.y-offy, op.tx, op.ty);
		}
		
		list.clear();
	}
}
