package quest.field;

import quest.*;
import java.awt.*;
import java.io.*;
import java.util.*;

/*
 * The "text message box" that pops up when FieldModule.textMessage() is
 * invoked. An important thing to note is that FieldModule doesn't
 * create and destroy text boxes; it just stores one text box, which
 * is made visible or invisible as necessary.
 * 
 * [Similarly to FieldScript, this is pretty badly-written, but it also
 * happens to be completely self-contained. I'd recommend scrapping 
 * and rewriting it when you get around to implementing talksprites. -Frost]
 */

public class TextBox {

	public static final int CHARH = 14, CHARW = 6;
	
	private String text = null;
	private int textw = 48;
	private Color textcol;
	private Font font;
	
	private int mode = 0;		//0 invisible, 1 opening, 2 open, 3 closing
	private int anim = 0;		//ticks down to zero as animation progresses
	private int count = 0;		//number of characters to display
	
	public TextBox()
	{
		try {
			
			InputStream stream = getClass().getResourceAsStream( Game.DATAPATH+"fonts/LucidaTypewriterBold.ttf");
			font = Font.createFont( Font.TRUETYPE_FONT, stream);
			font = font.deriveFont( Font.BOLD, 10.0f);
			
		} catch ( Exception e ) { e.printStackTrace(); }
	}
	
	public void setText( String speaker, String message)
	{
		if ( mode == 0 ) { anim = 3; mode = 1; }
		else mode = 2;
		
		textcol = new Color( 50, 50, 50);
		if ( speaker.equals("AA") ) textcol = new Color( 0xa1, 0x00, 0x00);
		if ( speaker.equals("AC") ) textcol = new Color( 0x41, 0x66, 0x00);
		if ( speaker.equals("AG") ) textcol = new Color( 0x00, 0x56, 0x82);
		if ( speaker.equals("AT") ) textcol = new Color( 0xa1, 0x50, 0x00);
		if ( speaker.equals("CA") ) textcol = new Color( 0x6a, 0x00, 0x6a);
		if ( speaker.equals("CC") ) textcol = new Color( 0x77, 0x00, 0x3c);
		if ( speaker.equals("CT") ) textcol = new Color( 0x00, 0x00, 0x56);
		if ( speaker.equals("CG") ) textcol = new Color( 0x62, 0x62, 0x62);
		if ( speaker.equals("GA") ) textcol = new Color( 0x00, 0x81, 0x41);
		if ( speaker.equals("GC") ) textcol = new Color( 0x00, 0x82, 0x82);		
		if ( speaker.equals("TA") ) textcol = new Color( 0xa1, 0xa1, 0x00);
		if ( speaker.equals("TC") ) textcol = new Color( 0x2b, 0x00, 0x57);
		
		text = sliceString(message);
		count = 0;
	}
	
	//returns false if this command dismisses the textbox
	public boolean buttonPressed()
	{
		if ( text == null ) return true;
		
		if ( mode == 2 )
		{
			if ( count < text.length() ) 
			{
				count = text.length();
				return true;
			}
			else
			{
				mode = 3;
				anim = 4;
				return false;
			}
		}
		return true;
	}
	
	public boolean isVisible()
	{
		return (mode != 0 && mode != 3);
	}
	
	public void step()
	{
		//iterating through animation
		if ( anim > 0 ) anim--;
		
		//finishing opening or closing animations
		if ( anim == 0 )
		{
			if ( mode == 1 ) mode = 2;
			if ( mode == 3 ) mode = 0;
		}
		
		//gradually displaying text
		if ( text != null )
		{
			count += 2;
			if ( count > text.length() ) count = text.length();
		}
	}
	
	public void drawTo( Graphics g)
	{
		switch ( mode )
		{
			case 1:
				
				g.setColor( new Color( 255, 255, 255, 200-anim*40));
				drawRectangle( g, 3+anim*5, 3, Game.SCREENW-(6+anim*10), 74);
				
			break;
			case 2:
				
				g.setColor( new Color( 255, 255, 255, 200));
				drawRectangle( g, 3, 3, Game.SCREENW-6, 74);
				
				g.setColor( textcol);
				g.setFont( font);
				if ( text != null ) drawString( g, text.substring(0,count), 15, 22);
				
			break;
			case 3:
				
				g.setColor( new Color( 255, 255, 255, (anim*200)/4));
				drawRectangle( g, 3, 3, Game.SCREENW-6, 74);
				
			break;
		}
	}
	
	private void drawRectangle( Graphics g, int x, int y, int w, int h)
	{
		final int b = 4;
		
		Polygon shape = new Polygon();
		shape.addPoint( x, y+b);
		shape.addPoint( x+b, y);
		shape.addPoint( x+w-b, y);
		shape.addPoint( x+w, y+b);
		shape.addPoint( x+w, y+h-b);
		shape.addPoint( x+w-b, y+h);
		shape.addPoint( x+b, y+h);
		shape.addPoint( x, y+h-b);
		
		g.fillPolygon( shape);
		
		Color buf = g.getColor();
		int alpha = buf.getAlpha() + 100;
		if ( alpha > 255 ) alpha = 255;
		
		g.setColor( new Color( 0, 0, 0, alpha) );
		
		g.drawPolygon( shape);
		
		g.setColor( buf);
	}
	
	private void drawString( Graphics g, String string, int x, int y)
	{
		//draws a string, taking into account newlines
		int orig_x = x;
		
		for( int pos = 0; pos < string.length(); pos++)
		{
			char todraw = string.charAt(pos);
			
			if ( todraw == '\n' )
			{
				x = orig_x;
				y += CHARH;
			}
			else
			{
				g.drawString( string.substring(pos,pos+1), x, y);
				x += CHARW;
			}
		}
	}
	
	private String sliceString( String in)
	{
		//returns a string that's been split into
		//suitably-sized lines using the \n character
		
		String out = new String(), buf;
		Scanner scanner = new Scanner(in);
		
		if ( !scanner.hasNext() ) return null;
		buf = scanner.next();
		
		while ( scanner.hasNext() )
		{
			String token = scanner.next();
			
			if ( token.equals( "#N") )
			{
				out = out.concat( buf + "\n");
				buf = new String();
				continue;
			}
			
			if ( buf.length()+token.length() < textw )
			{
				if ( !buf.isEmpty() ) buf = buf.concat( " ");
				buf = buf.concat( token);
			}
			else
			{
				out = out.concat( buf + "\n");
				buf = token;
			}
		}
		
		out = out.concat( buf);
		
		return out;
	}
}
