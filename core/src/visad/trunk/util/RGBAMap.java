/*

@(#) $Id: RGBAMap.java,v 1.1 1998-02-23 13:29:56 billh Exp $

VisAD Utility Library: Widgets for use in building applications with
the VisAD interactive analysis and visualization library
Copyright (C) 1998 Nick Rasmussen
VisAD is Copyright (C) 1996 - 1998 Bill Hibbard, Curtis Rueden and Tom
Rink.
 
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 1, or (at your option)
any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License in file NOTICE for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package visad.util;

import java.awt.event.*;
import java.awt.*;

/** 
 * A simple RGB colormap with no interpolation between the internally
 * stored values.  Click and drag with the left mouse button to draw
 * the color curves. Click with the right mouse button to alternate
 * between the red, green and blue curves.
 *
 * @author Nick Rasmussen nick@cae.wisc.edu
 * @version $Revision: 1.1 $, $Date: 1998-02-23 13:29:56 $
 * @since Visad Utility Library, 0.5
 */

public class RGBAMap extends ColorMap 
	implements MouseListener, MouseMotionListener {

	/** The array of RGB tuples */
	private float[][] val;

	/** The left modified value */
	private int valLeft;
	/** The right modified value */
	private int valRight;

	/** A lock to synchronize against when modifing the modified area */
	private Object mutex = new Object();

	/** The index of the color red */
	private static final int RED = 0;
	/** The index of the color green */
	private static final int GREEN = 1;
	/** The index of the color blue */
	private static final int BLUE = 2;
	/** The index of the alpha channel */
	private static final int ALPHA = 3;
	/** The current color for the mouse to draw on */
	private int state = RED;
	
	/** The resolution of the map */
	private int resolution;

	/** Construct an RGBMap with the default resolution of 256 */
	public RGBAMap() {
		this(256);
	}

	/** The RGBAMap map is represented internally by an array of
	 * floats
	 * @param resolution the length of the array
	 */
	public RGBAMap(int resolution) {
		this.resolution = resolution;
		val = new float[resolution][4];
		this.initColormap();
		addMouseListener(this);
		addMouseMotionListener(this);
		//this.addColorChangeListener(this);
	}
	
	/** Returns the resolution of the map */
	public int getMapResolution() {
		return resolution;
	}
	
	/** Returns the dimension of the map */
	public int getMapDimension() {
		return 3;
	}
	
	/** Returns a copy of the color map */
	public float[][] getColorMap() {
	
		float[][] ret = new float[resolution][4];
		
		for (int i = 0; i < resolution; i++) {
			ret[i][0] = val[i][0];
			ret[i][1] = val[i][1];
			ret[i][2] = val[i][2];
			ret[i][3] = val[i][3];
		}
		
		return ret;
	}
	
	/** Returns the tuple at a floating point value val */
	public float[] getTuple(float value) {
	
		float arrayIndex = value * (resolution - 1);
		int index = (int) Math.floor(arrayIndex);
		float partial = arrayIndex - index;
		
		if (index >= resolution || index < 0 || (index == (resolution - 1) && partial != 0)) {
			float[] f = {0,0,0,0};
			return f;
		}
		
		float red, green, blue, alpha;
		if (partial != 0) {
			red = val[index][RED] * (1 - partial) + val[index+1][RED] * partial;
			green = val[index][GREEN] * (1 - partial) + val[index+1][GREEN] * partial;
			blue = val[index][BLUE] * (1 - partial) + val[index+1][BLUE] * partial;
			alpha = val[index][ALPHA] * (1 - partial) + val[index+1][ALPHA] * partial;
		}
		else {
			red = val[index][RED];
			green = val[index][GREEN];
			blue = val[index][BLUE];
			alpha = val[index][ALPHA];
		}
		float[] f = {red, green, blue, alpha};
		return f;
		
	}
	
	protected void sendUpdate(int left, int right) {
	

		synchronized (mutex) {
			if (left < valLeft)
				valLeft = left;
			if (right > valRight)
				valRight = right;
		}
		
// won't update on repaint, so hit it with a big hammer
update(getGraphics());
		repaint();
	}	
	
	
	/** Used internally to post areas to update to the objects listening
	 * to the map 
	 */
	protected void notifyListeners(int left, int right) {
		
		// !!!fix this to reflect a more acurate region of affectation		
		if (left != 0) {
			left--;
		}
		if (right != resolution - 1) {
			right++;
		}
		
		float start = (float) left / (float) (resolution - 1);
		float end = (float) right + 1 / (float) (resolution - 1);
		sendUpdate(left, right);
		super.notifyListeners(new ColorChangeEvent(this, start, end));

	}
	
	/** Implementation of the abstract function in ColorMap
	 * @param value a floating point number between 0 and 1
	 * @return an RGB tuple of floating point numbers in the
	 * range 0 to 1
	 */
	public float[] getRGBTuple(float value) {
		float arrayIndex = value * (resolution - 1);
		int index = (int) Math.floor(arrayIndex);
		float partial = arrayIndex - index;
		if (index >= resolution || index < 0 || (index == (resolution - 1) && partial != 0)) {
			float[] f = {0,0,0};
			return f;
		}
		
		float red, green, blue;
		if (partial != 0) {
			red = val[index][RED] * (1 - partial) + val[index+1][RED] * partial;
			green = val[index][GREEN] * (1 - partial) + val[index+1][GREEN] * partial;
			blue = val[index][BLUE] * (1 - partial) + val[index+1][BLUE] * partial;
		}
		else {
			red = val[index][RED];
			green = val[index][GREEN];
			blue = val[index][BLUE];
		}
		float[] f = {red, green, blue};
		return f;
	}
		
	/** Present to implement MouseListener, currently ignored */
	public void mouseClicked(MouseEvent e) {
		//System.out.println(e.paramString());
	}

	/** Present to implement MouseListener, currently ignored */
	public void mouseEntered(MouseEvent e) {
		//System.out.println(e.paramString());
	}

	/** Present to implement MouseListener, currently ignored */
	public void mouseExited(MouseEvent e) {
		//System.out.println(e.paramString());
	}
	
	/** The last mouse event's x value */
	private int oldX;
	/** The last mouse event's y value */
	private int oldY;
	
	/** A synchronization primitive for the mouse movements */
	private Object mouseMutex = new Object();
	
	/** Updates the internal array and sends notification to the
	 * ColorChangeListeners that are listening to this map
	 */
	public void mousePressed(MouseEvent e) {
		//System.out.println(e.paramString());
		if ((e.getModifiers() & e.BUTTON1_MASK) == 0 && e.getModifiers() != 0) {
			return;
		}
		
		int index = 0;
		int width = getBounds().width;
		int height = getBounds().height;
		int x = e.getX();
		int y = e.getY();

		if (x < 0)
			x = 0;

		if (x >= width)
			x = width - 1;

		if (y < 0)
			y = 0;

		if (y >= height)
			y = height - 1;

		float dist = (float) x / (float) width;
		index = (int) Math.floor(dist * (resolution - 1) + 0.5);
		val[index][state] = 1 - (float) y / (float) height;

		oldX = x;
		oldY = y;
		
		notifyListeners(index, index);

		
	}
	
	/** Listens for releases of the right mouse button, and changes the active color */
	public void mouseReleased(MouseEvent e) {
		//System.out.println(e.paramString());
		if ((e.getModifiers() & e.BUTTON3_MASK) == 0) {
			return;
		}
		state = (state + 1) % 4;
	}

	/** Updates the internal array and sends notification to the
	 * ColorChangeListeners that are listening to this map
	 */
	public void mouseDragged(MouseEvent e) {
		//System.out.println(e.paramString());
		if ((e.getModifiers() & e.BUTTON1_MASK) == 0 && e.getModifiers() != 0) {
			return;
		}
		
		drag(e.getX(), e.getY(), oldX, oldY);
			
		oldX = e.getX();
		oldY = e.getY();
	}
	
	/** Internal mouse dragging function */
	private void drag(int x, int y, int oldx, int oldy) {
	
		if (x < 0)
			x = 0;
		
		if (x >= getBounds().width)
			x = getBounds().width - 1;
		
		if (y < 0)
			y = 0;
		
		if (y >= getBounds().height)
			y = getBounds().height - 1;
			
		if (oldx < 0)
			oldx = 0;
		
		if (oldx >= getBounds().width)
			oldx = getBounds().width - 1;
		
		if (oldy < 0)
			oldy = 0;
		
		if (oldy >= getBounds().height)
			oldy = getBounds().height - 1;
			
		
		float dist = (float) x / (float) (getBounds().width - 1);
		int index = (int) Math.floor(dist * (resolution - 1) + 0.5);
		
		float oldDist = (float) oldx / (float) (getBounds().width - 1);
		int oldPos = (int) Math.floor(oldDist * (resolution - 1) + 0.5);
		
		float oldVal = val[oldPos][state];
		float target = 1 - (float) y / (float) (getBounds().height - 1);
		
		if (index > oldPos) {
			for (int i = oldPos + 1; i <= index; i++) {
				val[i][state] = oldVal * ((float) (index - i)) / ((float) (index - oldPos))
						+ target * ((float) (i - oldPos)) / ((float) (index - oldPos));
			}
			notifyListeners(oldPos + 1, index);
			return;
		}
		if (index < oldPos) {
			for (int i = oldPos - 1; i >= index; i--) {
				val[i][state] = oldVal * ((float) (i - index)) / ((float) (oldPos - index))
						+ target * ((float) (oldPos - i)) / ((float) (oldPos - index));
			}
			notifyListeners(index, oldPos - 1);
			return;
		}
		if (index == oldPos) {
			val[index][state] = target;
			notifyListeners(index, index);
			return;
		}
	}
	
	/** Present to implement MouseMovementListener, currently ignored */
	public void mouseMoved(MouseEvent e) {
		//System.out.println(e.paramString());
	}

	/** Repaints the entire Panel */
	public void paint(Graphics g) {
	
		synchronized (mutex) {
		
			valLeft = 0;
			valRight = resolution - 1;
		}
		
		update(g);
	}
	
	/** The left bound for updating the Panel */
	private float updateLeft = 0;
	
	/** The right bound for updating the Panel */
	private float updateRight = 1;
	
	/** Repaints the modified areas of the Panel */
	public void update(Graphics g) {
	
		int left = 0;
		int right = resolution - 1;
	
		synchronized (mutex) {
			if (valLeft > valRight) return;
		
			left = valLeft;
			right = valRight;
			
			valLeft = resolution - 1;
			valRight = 0;
		}
		
		Rectangle bounds = getBounds();
		
		if (left != 0) {
			left--;
		}
		
		if (right != resolution - 1) {
			right++;
		}
		
		int leftPixel = (left * (bounds.width - 1)) / (resolution - 1);
		int rightPixel = (right * (bounds.width - 1)) / (resolution - 1);
		
		g.setColor(Color.black);
		g.fillRect(leftPixel,0,rightPixel - leftPixel + 1, bounds.height);
		
		
		if (left != 0) {
			left--;
		}
		
		if (right != resolution - 1) {
			right++;
		}
		
		leftPixel = (left * (bounds.width - 1)) / (resolution - 1);
		rightPixel = (right * (bounds.width - 1)) / (resolution - 1);
		
		int prevEnd = leftPixel;
		
		int prevRed = (int) Math.floor((1 - val[left][RED]) * (bounds.height - 1));
		int prevGreen = (int) Math.floor((1 - val[left][GREEN]) * (bounds.height - 1));
		int prevBlue = (int) Math.floor((1 - val[left][BLUE]) * (bounds.height - 1));
		int prevAlpha = (int) Math.floor((1 - val[left][ALPHA]) * (bounds.height - 1));
		
		for (int i = left + 1; i <= right; i++) {
			int lineEnd = (i * (getBounds().width - 1)) / (resolution - 1);
			
			int red = (int) Math.floor((1 - val[i][RED]) * (bounds.height - 1));
			int green = (int) Math.floor((1 - val[i][GREEN]) * (bounds.height - 1));
			int blue = (int) Math.floor((1 - val[i][BLUE]) * (bounds.height - 1));
			int alpha = (int) Math.floor((1 - val[i][ALPHA]) * (bounds.height - 1));
	
			g.setColor(Color.red);
			g.drawLine(prevEnd, prevRed, lineEnd, red);

			g.setColor(Color.green);
			g.drawLine(prevEnd, prevGreen, lineEnd, green);

			g.setColor(Color.blue);
			g.drawLine(prevEnd, prevBlue, lineEnd, blue);
			
			g.setColor(Color.gray);
			g.drawLine(prevEnd, prevAlpha, lineEnd, alpha);
			
			prevEnd = lineEnd;
			
			prevRed = red;
			prevGreen = green;
			prevBlue = blue;
			prevAlpha = alpha;
		}
	}
	
	/** Return the preferred size of this map, taking into account the resolution */ 
	public Dimension getPreferredSize() {
		return new Dimension(resolution, resolution / 2);
	}
	
	/** Initializes the colormap to default values */
	private void initColormap() {
		initColormapVis5D();
	}
	
	private void initColormapVis5D() {
		
		float curve = 1.4f;
		float bias = 1.0f;
		float rfact = 0.5f * bias;
		
		for (int i = 0; i < resolution; i++) {
		
			/* compute s in [0,1] */
			float s = (float) i / (float) (resolution-1);
		
			float t = curve * (s - rfact);   /* t in [curve*-0.5,curve*0.5) */
			val[i][RED] = (float) (0.5 + 0.5 * Math.atan( 7.0*t ) / 1.57);
			val[i][GREEN] = (float) (0.5 + 0.5 * (2 * Math.exp(-7*t*t) - 1));
			val[i][BLUE] = (float) (0.5 + 0.5 * Math.atan( -7.0*t ) / 1.57);
			val[i][ALPHA] = 1.0f;
		}
	}
	
	
	/** Initializes the colormap to be linear in hue */
	private void initColormapHSV() {
		float s = 1;
		float v = 1;
		
		for (int i = 0; i < resolution; i++) {
			
			float h = i * 6 / (float) (resolution - 1);
			
			int hFloor = (int) Math.floor(h);
			float hPart = h - hFloor;
			
			// if hFloor is even
			if ((hFloor & 1) == 0) {
				hPart = 1 - hPart;
			}
			
			float m = v * (1 - s);
			float n = v * (1 - s*hPart);
			
			float r = 0;
			float g = 0;
			float b = 0;
			switch (hFloor) {
				case 6:
				case 0:
					r = v;
					g = n;
					b = m;
					break;
				case 1:
					r = n;
					g = v;
					b = m;
					break;
				case 2:
					r = m;
					g = v;
					b = n;
					break;
				case 3:
					r = m;
					g = n;
					b = v;
					break;
				case 4:
					r = n;
					g = m;
					b = v;
					break;
				case 5:
					r = v;
					g = m;
					b = n;
					break;
			}
			
			val[i][RED] = r;
			val[i][GREEN] = g;
			val[i][BLUE] = b;
			val[i][ALPHA] = 1.0f;
		}
	}
}
