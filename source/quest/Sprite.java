package quest;

import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.io.InputStream;
import java.util.Scanner;
import java.io.IOException;

/*
 * Convenience class for images. Handles loading, color-keying and
 * frame slicing.
 */

public class Sprite {

	private BufferedImage img;
	private int fWidth, fHeight, fXSep, fYSep, fXCount, fFrames;
	
	public int getWidth() {
		return fWidth;
	}
	
	public int getRawWidth() {
		return img.getWidth(null);
	}
	
	public int getHeight() {
		return fHeight;
	}
	
	public int getRawHeight() {
		return img.getHeight(null);
	}

	public Sprite( String filepath) 
	{
		//System.out.println(filepath);
		//String imageName = filepath.replaceAll(".*?/","");
		URL imgURL;
		/*if(Arrays.asList(Game.getGame().getFiles()).contains(imageName)) {
			try { 
				imgURL = new File(Game.getGame().getRunningPath() + imageName).toURI().toURL();
				System.out.println("Found alternative sprite for " + imageName + "; using it");
			} catch(Exception e) {e.printStackTrace(); imgURL = getClass().getResource(filepath);}
		} else {*/
		//lift the image data from file
			imgURL = getClass().getResource(filepath);
		//}
		//System.out.println(filepath);
		Image tempImg = Toolkit.getDefaultToolkit().createImage(imgURL);
		
		//force image download to complete
		MediaTracker mt = new MediaTracker( Game.getGame());
		mt.addImage( tempImg, 1);
		try {
			mt.waitForAll();
		} catch ( InterruptedException exI ) { exI.printStackTrace(); return;}
		//copy the temp image to the storage image
		img = new BufferedImage( tempImg.getWidth(null), tempImg.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		g.drawImage( tempImg, 0, 0, null);
		
		//colorkey the storage image for (255,0,255), magenta
		for( int y=0; y<img.getHeight(null); y++)
		for( int x=0; x<img.getWidth(null);  x++)
		{
			int pixel = img.getRGB( x, y);
			if ( ( pixel & 0xffffff ) == 0xff00ff ) img.setRGB( x, y, 0x00ffffff);
		}
		
		//set the default frame-separation settings
		sliceFrames( img.getWidth(null), img.getHeight(null), 0, 0, 1, 1);
	}
	
	public void sliceFrames( int width, int height, int xsep, int ysep, int xcount, int fFrames)
	{
		this.fWidth  = width;	//the size of each output frame
		this.fHeight = height;	
		this.fXSep   = xsep;	//the separation between frames
		this.fYSep   = ysep;
		this.fXCount = xcount;  //the number of frames per row
		this.fFrames = fFrames; //the total number of frames
	}
	
	//for drawing as an animation strip
	public void drawFrame( Graphics g, int x, int y, int frame)
	{
		int offx, offy;
		frame = frame % fFrames;
		
		if ( frame >= fXCount )
		{
			offx = frame % fXCount;
			offy = (frame - offx)/fXCount;
		}
		else { offx = frame; offy = 0; }
		
		offx = offx * (fWidth+fXSep);
		offy = offy * (fHeight+fYSep);
		g.drawImage( img, x, y, x+fWidth, y+fHeight, offx, offy, offx+fWidth, offy+fHeight, null);
	}
	
	//for drawing as a tileset
	public void drawTile( Graphics g, int dx, int dy, int sx, int sy)
	{
		g.drawImage( img, dx, dy, dx+fWidth, dy+fHeight, sx*fWidth, 
				     sy*fHeight, (sx+1)*fWidth, (sy+1)*fHeight, null);
	}


	public int getFrames() {
		return fFrames;
	}
	public static Sprite loadSprite(String spritePrefix, String type) {
		Sprite newSprite = new Sprite(spritePrefix+type+".png");
		try {
			InputStream fis = Game.getGame().getClass().getResource(spritePrefix + type + ".txt").openStream();
			Scanner sis = new Scanner(fis);
			sis.useDelimiter("\\n");
			while(sis.hasNext()) {
				String s = sis.nextLine();
				if(s.startsWith("#")) {
				} else {
					String[] vals = s.split(":");
					if(vals.length == 6) {
						newSprite.sliceFrames(Integer.parseInt(vals[0]),Integer.parseInt(vals[1]),
						Integer.parseInt(vals[2]),Integer.parseInt(vals[3]),
						Integer.parseInt(vals[4]),Integer.parseInt(vals[5]));
					}
				}
			}
		} catch (IOException ioe) {}
		return newSprite;
	}

}
