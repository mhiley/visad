//
// ThingReferenceImpl.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2000 Bill Hibbard, Curtis Rueden, Tom
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

import java.util.*;
import java.rmi.*;

/**
   ThingReferenceImpl is a local implementation of ThingReference.<P>

   ThingReferenceImpl is not Serializable and should not be copied
   between JVMs.<P>
*/
public class ThingReferenceImpl extends Object implements ThingReference {

  /** name of scalar type */
  String Name;

  /** Thing object refered to (mutable);
      ThingReferenceImpl is not Serializable, but mark as transient anyway */
  private transient Thing thing;
  /** ref = this if thing is local
      ref = a RemoteThingReferenceImpl if thing is remote;
      ThingReferenceImpl is not Serializable, but mark as transient anyway */
  private transient ThingReference ref;

  /** Tick increments each time thing changes */
  private long Tick;

  /** Vector of ThingChangedLinks;
      ThingReferenceImpl is not Serializable, but mark as transient anyway */
  transient Vector ListenerVector = new Vector();

  public ThingReferenceImpl(String name) throws VisADException {
    if (name == null) {
      throw new VisADException("ThingReference: name cannot be null");
    }
    Name = name;
    Tick = Long.MIN_VALUE + 1;
  }

  public Thing getThing() {
    return thing;
  }

  /** set this ThingReferenceImpl to refer to t;
      must be local ThingImpl */
  public synchronized void setThing(Thing t)
         throws VisADException, RemoteException {
/* WLH 9 July 98
    if (t == null) {
      throw new ReferenceException("ThingReferenceImpl: thing cannot be null");
    }
*/
    if (t instanceof RemoteThing) {
      throw new RemoteVisADException("ThingReferenceImpl.setThing: cannot use " +
                                     "RemoteThing");
    }
    if (thing != null) thing.removeReference(ref);
    ref = this;
    thing = t;
    if (t != null) t.addReference(ref);
    incTick();
  }

  /** method for use by RemoteThingReferenceImpl that adapts this
      ThingReferenceImpl */
  synchronized void adaptedSetThing(RemoteThing t, RemoteThingReference r)
               throws VisADException, RemoteException {
    if (thing != null) thing.removeReference(ref);
    ref = r;
    thing = t;
    t.addReference(ref);
    incTick();
  }

  public long getTick() {
    return Tick;
  }

  /** synchronized because incTick, setThing, and adaptedSetThing
      share access to thing and ref */
  public synchronized long incTick()
         throws VisADException, RemoteException {
    Tick += 1;
    if (Tick == Long.MAX_VALUE) Tick = Long.MIN_VALUE + 1;
    if (ListenerVector != null) {
      synchronized (ListenerVector) {
        Enumeration listeners = ListenerVector.elements();
        while (listeners.hasMoreElements()) {
          ThingChangedLink listener =
            (ThingChangedLink) listeners.nextElement();
          ThingChangedEvent e =
            new ThingChangedEvent(listener.getId(), Tick);
          listener.queueThingChangedEvent(e);
        }
      }
    }
    return Tick;
  }

  public ThingChangedEvent acknowledgeThingChanged(Action a)
         throws VisADException {
    if (!(a instanceof ActionImpl)) {
      throw new RemoteVisADException("ThingReferenceImpl.acknowledgeThingChanged:" +
                                     " Action must be local");
    }
    if (ListenerVector == null) return null;
    ThingChangedLink listener = findThingChangedLink(a);
    return listener.acknowledgeThingChangedEvent();
  }

  public ThingChangedEvent adaptedAcknowledgeThingChanged(RemoteAction a)
         throws VisADException {
    if (ListenerVector == null) return null;
    ThingChangedLink listener = findThingChangedLink(a);
    return listener.acknowledgeThingChangedEvent();
  }

  /** find ThingChangedLink with action */
  public ThingChangedLink findThingChangedLink(Action a)
         throws VisADException {
    if (a == null) {
      throw new ReferenceException("ThingReferenceImpl.findThingChangedLink: " +
                                   "Action cannot be null");
    }
    if (ListenerVector == null) return null;
    synchronized (ListenerVector) {
      Enumeration listeners = ListenerVector.elements();
      while (listeners.hasMoreElements()) {
        ThingChangedLink listener =
          (ThingChangedLink) listeners.nextElement();
        if (a.equals(listener.getAction())) return listener;
      }
    }
    return null;
  }

  public String getName() {
    return Name;
  }

  /** addThingChangedListener and removeThingChangedListener provide
      ThingChangedEvent source semantics;
      Action must be local ActionImpl */
  public void addThingChangedListener(ThingChangedListener a, long id)
         throws VisADException {
    if (!(a instanceof ActionImpl)) {
      throw new RemoteVisADException("ThingReferenceImpl.addThingChanged" +
                                     "Listener: Action must be local");
    }
    synchronized (this) {
      if (ListenerVector == null) ListenerVector = new Vector();
    }
    synchronized(ListenerVector) {
      if (findThingChangedLink((ActionImpl) a) != null) {
        throw new ReferenceException("ThingReferenceImpl.addThingChangedListener:" +
                                     " link to Action already exists");
      }
      ListenerVector.addElement(new ThingChangedLink((ActionImpl) a, id));
    }
  }

  /** method for use by RemoteThingReferenceImpl that adapts this
      ThingReferenceImpl */
  void adaptedAddThingChangedListener(RemoteAction a, long id)
       throws VisADException {
    synchronized (this) {
      if (ListenerVector == null) ListenerVector = new Vector();
    }
    synchronized(ListenerVector) {
      if (findThingChangedLink(a) != null) {
        throw new ReferenceException("ThingReferenceImpl.addThingChangedListener:" +
                                     " link to Action already exists");
      }
      ListenerVector.addElement(new ThingChangedLink(a, id));
    }
  }

  /** ThingChangedListener must be local ActionImpl */
  public void removeThingChangedListener(ThingChangedListener a)
         throws VisADException {
    if (!(a instanceof ActionImpl)) {
      throw new RemoteVisADException("ThingReferenceImpl.removeThingChanged" +
                                     "Listener: Action must be local");
    }
    if (ListenerVector != null) {
      synchronized(ListenerVector) {
        ThingChangedLink listener = findThingChangedLink((ActionImpl) a);
        if (listener != null) {
          ListenerVector.removeElement(listener);
        }
      }
    }
  }

  /** method for use by RemoteThingReferenceImpl that adapts this
      ThingReferenceImpl */
  void adaptedRemoveThingChangedListener(RemoteAction a)
       throws VisADException {
    if (ListenerVector != null) {
      synchronized(ListenerVector) {
        ThingChangedLink listener = findThingChangedLink(a);
        if (listener != null) {
          ListenerVector.removeElement(listener);
        }
      }
    }
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof ThingReference)) return false;
    return obj == this;
  }

  public String toString() {
    return "ThingReference " + Name;
  }

}

