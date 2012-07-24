package quest;

import java.awt.*;
import java.awt.image.*;

public abstract class Module {
	
	private static final int type = 0;
	public BufferedImage frameBuffer;
	
	public int successor;
	public boolean exitFlag = false;
	public boolean saveFlag = false;
	public abstract void exit();
	public abstract void step();
	public Image getFrameBuffer() {
		return frameBuffer;
	}
	public static int getType() {
		return type;
	}

}
