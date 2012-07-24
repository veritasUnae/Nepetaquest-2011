package quest.field.trans;

import java.awt.*;

/*
 * The interface to all field-to-battle transitions.
 * 
 * Such transitions are constructed with a fully-rendered instance
 * of the current framebuffer. Each iterate() call generates the
 * next step of the transition's animation, storing it in a constant 
 * image exposed through getFrame(). 
 * If iterate() returns false, the animation is complete.
 */

public interface Transition {

	public abstract boolean iterate();
	public abstract Image getFrame();
}
