package visad.data.visad.object;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import visad.BaseUnit;
import visad.CommonUnit;
import visad.Unit;
import visad.VisADException;

import visad.data.visad.BinaryObjectCache;
import visad.data.visad.BinaryReader;
import visad.data.visad.BinaryWriter;

public class BinaryUnit
  implements BinaryObject
{
  public static final int computeBytes(Unit u)
  {
    return 4 +
      BinaryString.computeBytes(u.getIdentifier()) +
      BinaryString.computeBytes(u.getDefinition().trim()) +
      1;
  }

  public static final int computeBytes(Unit[] array)
  {
    return BinaryIntegerArray.computeBytes(array);
  }

  public static final int[] lookupList(BinaryObjectCache cache, Unit[] units)
  {
    // make sure there's something to write
    boolean empty = true;
    for (int i = 0; i < units.length; i++) {
      if (units[i] != null) {
        empty = false;
        break;
      }
    }
    if (empty) {
      return null;
    }

    int[] indices = new int[units.length];

    for (int i = 0; i < units.length; i++) {
      if (units[i] == null) {
        indices[i] = -1;
      } else {
        indices[i] = cache.getIndex(units[i]);
      }
    }

    return indices;
  }

  public static final Unit read(BinaryReader reader)
    throws IOException, VisADException
  {
    BinaryObjectCache cache = reader.getUnitCache();
    DataInput file = reader.getInput();

    final int objLen = file.readInt();
if(DEBUG_RD_UNIT)System.err.println("cchU: objLen (" + objLen + ")");

    // read the index number for this Unit
    final int index = file.readInt();
if(DEBUG_RD_UNIT)System.err.println("cchU: index (" + index + ")");

    // read the Unit identifier
    String idStr = BinaryString.read(reader);
if(DEBUG_RD_UNIT&&!DEBUG_RD_STR)System.err.println("cchU: identifier (" + idStr + ")");

    // read the Unit description
    String defStr = BinaryString.read(reader);
if(DEBUG_RD_UNIT&&!DEBUG_RD_STR)System.err.println("cchU: definition (" + defStr + ")");

    final byte endByte = file.readByte();
if(DEBUG_RD_UNIT)System.err.println("cchU: read " + (endByte == FLD_END ? "FLD_END" : Integer.toString(endByte) + " (wanted FLD_END)"));
    if (endByte != FLD_END) {
      throw new IOException("Corrupted file (no Unit end-marker)");
    }

    Unit u;
    if (defStr.equals("promiscuous")) {
      u = CommonUnit.promiscuous;
    } else {
      try {
        u = visad.data.units.Parser.parse(defStr);
      } catch (Exception e) {
        throw new VisADException("Couldn't parse Unit specification \"" +
                                 defStr + "\"");
      }

      if (!(u instanceof BaseUnit)) {
        try {
          Unit namedUnit = u.clone(idStr);
          u = namedUnit;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    cache.add(index, u);

    return u;
  }

  public static final Unit[] readList(BinaryReader reader)
    throws IOException
  {
    BinaryObjectCache cache = reader.getUnitCache();
    DataInput file = reader.getInput();

    final int len = file.readInt();
if(DEBUG_RD_UNIT)System.err.println("rdUnits: len (" + len + ")");
    if (len < 1) {
      throw new IOException("Corrupted file" +
                            " (bad Unit array length " + len + ")");
    }

    Unit[] units = new Unit[len];
    for (int i = 0; i < len; i++) {
      final int index = file.readInt();
if(DEBUG_RD_UNIT)System.err.println("rdUnits:    #"+i+" index ("+index+")");

      if (index < 0) {
        units[i] = null;
      } else {
        units[i] = (Unit )cache.get(index);
      }
if(DEBUG_RD_UNIT)System.err.println("rdUnits:    === #"+i+" Unit ("+units[i]+")");
    }

    return units;
  }

  public static final int write(BinaryWriter writer, Unit u, Object token)
    throws IOException
  {
    BinaryObjectCache cache = writer.getUnitCache();

    int index = cache.getIndex(u);
    if (index >= 0) {
      return index;
    }

    String uDef = u.getDefinition().trim();

    String uId = u.getIdentifier();

    // cache the Unit so we can find its index number
    index = cache.add(u);
    if (index < 0) {
      throw new IOException("Couldn't cache Unit " + u);
    }

    final int objLen = computeBytes(u);

    DataOutputStream file = writer.getOutputStream();

if(DEBUG_WR_UNIT)System.err.println("wrU: OBJ_UNIT (" + OBJ_UNIT + ")");
    file.writeByte(OBJ_UNIT);
if(DEBUG_WR_UNIT)System.err.println("wrU: objLen (" + objLen + ")");
    file.writeInt(objLen);
if(DEBUG_WR_UNIT)System.err.println("wrU: index (" + index + ")");
    file.writeInt(index);

if(DEBUG_WR_UNIT)System.err.println("wrU: identifier (" + uId + ")");
    BinaryString.write(writer, uId, token);
if(DEBUG_WR_UNIT)System.err.println("wrU: definition (" + uDef + ")");
    BinaryString.write(writer, uDef, token);

if(DEBUG_WR_UNIT)System.err.println("wrU: FLD_END (" + FLD_END + ")");
    file.writeByte(FLD_END);

    return index;
  }

  public static final int[] writeList(BinaryWriter writer, Unit[] units,
                                      Object token)
    throws IOException
  {
    // make sure there's something to write
    boolean empty = true;
    for (int i = 0; i < units.length; i++) {
      if (units[i] != null) {
        empty = false;
        break;
      }
    }
    if (empty) {
      return null;
    }

    int[] indices = new int[units.length];

    for (int i = 0; i < units.length; i++) {
      if (units[i] == null) {
        indices[i] = -1;
      } else {
        indices[i] = write(writer, units[i], token);
      }
    }

    return indices;
  }
}