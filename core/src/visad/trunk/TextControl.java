//
// TextControl.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2001 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

package visad;

import java.awt.Font;
import java.text.*;
import java.rmi.*;

import visad.util.Util;

/**
   TextControl is the VisAD class for controlling Text display scalars.<P>
*/
public class TextControl extends Control {

  private Font font = null;

  // abcd 5 February 2001
  //private boolean center = false;

  private double size = 1.0;

  // WLH 31 May 2000
  // draw on sphere surface
  private boolean sphere = false;

  private NumberFormat format = null;

  // abcd 1 February 2001
  // Rotation, in degrees, clockwise along positive x axis
  private double rotation = 0.0;

  /**
   * Class to represent the different types of justification
   * Use a class so the user can't just pass in an arbitrary integer
   *
   * abcd 5 February 2001
   */
  public static class Justification {
    String name;

    /** Predefined value for left justification */
    public static final Justification LEFT = new Justification("Left");

    /** Predefined value for center justification */
    public static final Justification CENTER = new Justification("Center");

    /** Predefined value for right justification */
    public static final Justification RIGHT = new Justification("Right");

    /**
     * Constructor - simply store the name
     */
    public Justification(String newName)
    {
      name = newName;
    }
  }
  private Justification justification = Justification.LEFT;

  public TextControl(DisplayImpl d) {
    super(d);
  }

  /** set the Font; in the initial release this has no effect */
  public void setFont(Font f)
         throws VisADException, RemoteException {
    font = f;
    changeControl(true);
  }

  /** return the Font */
  public Font getFont() {
    return font;
  }

  /** set the centering flag; if true, text will be centered at
      mapped locations; if false, text will be to the right
      of mapped locations */
  public void setCenter(boolean c)
         throws VisADException, RemoteException {
    // abcd 5 February 2001
    justification = Justification.CENTER;
    //center = c;
    changeControl(true);
  }

// TODO: Deprecate this?
  /** return the centering flag */
  public boolean getCenter() {
    // abcd 5 February 2001
    //return center;
    if (justification == Justification.CENTER) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Set the justifcation flag
   * Possible values are TextControl.Justification.LEFT,
   * TextControl.Justification.CENTER and TextControl.Justification.RIGHT
   *
   * abcd 5 February 2001
   */
  public void setJustification(Justification newJustification)
         throws VisADException, RemoteException
  {
    // Store the new value
    justification = newJustification;

    // Tell the control it's changed
    changeControl(true);
  }

  /**
   * Return the justification value
   *
   * abcd 5 February 2001
   */
  public Justification getJustification()
  {
    return justification;
  }

  /** set the size of characters; the default is 1.0 */
  public void setSize(double s)
         throws VisADException, RemoteException {
    size = s;
    changeControl(true);
  }

  /** return the size */
  public double getSize() {
    return size;
  }

  // WLH 31 May 2000
  public void setSphere(boolean s)
         throws VisADException, RemoteException {
    sphere = s;
    changeControl(true);
  }

  // WLH 31 May 2000
  public boolean getSphere() {
    return sphere;
  }

  // WLH 16 June 2000
  public void setNumberFormat(NumberFormat f)
         throws VisADException, RemoteException {
    format = f;
    changeControl(true);
  }

  // WLH 16 June 2000
  public NumberFormat getNumberFormat() {
    return format;
  }

  private boolean fontEquals(Font newFont)
  {
    if (font == null) {
      if (newFont != null) {
        return false;
      }
    } else if (newFont == null) {
      return false;
    } else if (!font.equals(newFont)) {
      return false;
    }

    return true;
  }

  // WLH 16 June 2000
  private boolean formatEquals(NumberFormat newFormat)
  {
    if (format == null) {
      if (newFormat != null) {
        return false;
      }
    } else if (newFormat == null) {
      return false;
    } else if (!format.equals(newFormat)) {
      return false;
    }

    return true;
  }

  /**
   * Set the rotation
   *
   * abcd 1 February 2001
   */
  public void setRotation(double newRotation)
         throws VisADException, RemoteException
  {
    // Store the new rotation
    rotation = newRotation;
    // Tell the control it's changed
    changeControl(true);
  }

  /**
   * Get the rotation
   *
   * abcd 1 February 2001
   */
  public double getRotation()
  {
    return rotation;
  }

  /** get a string that can be used to reconstruct this control later */
  public String getSaveString() {
    return null;
  }

  /** reconstruct this control using the specified save string */
  public void setSaveString(String save)
    throws VisADException, RemoteException
  {
    throw new UnimplementedException(
      "Cannot setSaveString on this type of control");
  }

  /** copy the state of a remote control to this control */
  public void syncControl(Control rmt)
    throws VisADException
  {
    if (rmt == null) {
      throw new VisADException("Cannot synchronize " + getClass().getName() +
                               " with null Control object");
    }

    if (!(rmt instanceof TextControl)) {
      throw new VisADException("Cannot synchronize " + getClass().getName() +
                               " with " + rmt.getClass().getName());
    }

    TextControl tc = (TextControl )rmt;

    boolean changed = false;

    if (!fontEquals(tc.font)) {
      changed = true;
      font = tc.font;
    }

    // abcd 5 February 2001
    //if (center != tc.center) {
    //  changed = true;
    //  center = tc.center;
    //}
    if (justification != tc.justification) {
      changed = true;
      justification = tc.justification;
    }

    if (!Util.isApproximatelyEqual(size, tc.size)) {
      changed = true;
      size = tc.size;
    }

    // WLH 31 May 2000
    if (sphere != tc.sphere) {
      changed = true;
      sphere = tc.sphere;
    }

    // WLH 16 June 2000
    if (!formatEquals(tc.format)) {
      changed = true;
      format = tc.format;
    }

    // abcd 1 February 2001
    if (!Util.isApproximatelyEqual(rotation, tc.rotation)) {
      changed = true;
      rotation = tc.rotation;
    }

    if (changed) {
      try {
        changeControl(true);
      } catch (RemoteException re) {
        throw new VisADException("Could not indicate that control" +
                                 " changed: " + re.getMessage());
      }
    }
  }

  public boolean equals(Object o)
  {
    if (!super.equals(o)) {
      return false;
    }

    TextControl tc = (TextControl )o;

    if (!fontEquals(font)) {
      return false;
    }

    // abcd 5 February 2001
    //if (center != tc.center) {
    if (justification != tc.justification) {
      return false;
    }

    // WLH 31 May 2000
    if (sphere != tc.sphere) {
      return false;
    }

    // WLH 16 June 2000
    if (!formatEquals(tc.format)) {
      return false;
    }

    // abcd 1 February 2001
    if (!Util.isApproximatelyEqual(rotation, tc.rotation)) {
      return false;
    }

    if (!Util.isApproximatelyEqual(size, tc.size)) {
      return false;
    }

    return true;
  }
}

