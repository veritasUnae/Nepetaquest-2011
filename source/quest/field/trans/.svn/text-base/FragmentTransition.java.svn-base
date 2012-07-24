package quest.field.trans;

import java.awt.*;
import java.awt.image.*;

/*
 * Breaks the screen into a grid of squares, each of which
 * shrinks away over the course of twenty frames.
 */

public class FragmentTransition implements Transition {

	BufferedImage frame;
	Image[][] tiles;
	
	int tx, ty;
	int ticks = 0;
	
	public FragmentTransition( BufferedImage src)
	{
		//construct frame as a copy of src
		frame = new BufferedImage( src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = frame.getGraphics();
		g.drawImage( src, 0, 0, null);
		
		//split frame into a grid of tiles
		tx = src.getWidth()/20;
		ty = src.getHeight()/20;
		
		tiles = new Image[tx][ty];
		for( int x=0; x<tx; x++)
		for( int y=0; y<ty; y++)
		{
			tiles[x][y] = new BufferedImage( 20, 20, BufferedImage.TYPE_INT_ARGB);
			Graphics gt = tiles[x][y].getGraphics();
			gt.drawImage( src, 0, 0, 20, 20, x*20, y*20, (x+1)*20, (y+1)*20, null);
		}
	}
	
	public boolean iterate()
	{
		Graphics g = frame.getGraphics();
		
		//blank the framebuffer
		g.setColor( Color.BLACK);
		g.fillRect( 0, 0, frame.getWidth(), frame.getHeight());
		
		//draw each tile in a scaled fashion
		for( int x=0; x<tx; x++)
		for( int y=0; y<ty; y++)
		{
			int scale = (ticks) - ((x+y)*2)/3;
			if ( scale > 10 ) scale = 10;
			if ( scale < 0 ) scale = 0;
			
			g.drawImage( tiles[x][y], (x*20)+scale, (y*20)+scale, ((x+1)*20)-scale, 
					     ((y+1)*20)-scale, 0, 0, 20, 20, null);
		}
		
		//timing
		ticks++;
		if ( ticks >= 100 + (tx/4) + (ty/4) ) return false;
		return true;
	}
	
	public Image getFrame()
	{
		return frame;
	}
}
