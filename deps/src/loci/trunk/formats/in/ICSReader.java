//
// ICSReader.java
//

/*
LOCI Bio-Formats package for reading and converting biological file formats.
Copyright (C) 2005-2007 Melissa Linkert, Curtis Rueden, Chris Allan,
Eric Kjellman and Brian Loranger.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Library General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Library General Public License for more details.

You should have received a copy of the GNU Library General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.formats.in;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.StringTokenizer;
import java.util.zip.*;
import loci.formats.*;
import loci.formats.codec.ByteVector;

/**
 * ICSReader is the file format reader for ICS (Image Cytometry Standard)
 * files. More information on ICS can be found at http://libics.sourceforge.net
 *
 * @author Melissa Linkert linkert at wisc.edu
 */

public class ICSReader extends FormatReader {

  // -- Constants --

  /** Metadata field categories. */
  private static final String[] CATEGORIES = new String[] {
    "ics_version", "filename", "source", "layout", "representation",
    "parameter", "sensor", "history", "end"
  };

  /** Metadata field subcategories. */
  private static final String[] SUB_CATEGORIES = new String[] {
    "file", "offset", "parameters", "order", "sizes", "coordinates",
    "significant_bits", "format", "sign", "compression", "byte_order",
    "origin", "scale", "units", "labels", "SCIL_TYPE", "type", "model",
    "s_params"
  };

  /** Metadata field sub-subcategories. */
  private static final String[] SUB_SUB_CATEGORIES = new String[] {
    "Channels", "PinholeRadius", "LambdaEx", "LambdaEm", "ExPhotonCnt",
    "RefInxMedium", "NumAperture", "RefInxLensMedium", "PinholeSpacing"
  };

  // -- Fields --

  /** Current filename. */
  protected String currentIcsId;
  protected String currentIdsId;

  /** Current file. */
  protected RandomAccessStream idsIn; // IDS file
  protected Location icsIn; // ICS file

  /** Flag indicating whether current file is little endian. */
  protected boolean littleEndian;

  /** Number of images. */
  protected int numImages;

  /** Number of bits per pixel. */
  protected int bitsPerPixel;

  /** Flag indicating whether current file is v2.0. */
  protected boolean versionTwo;

  /** Image data. */
  protected byte[] data;

  /** Flag indicating that the images are RGB. */
  private boolean rgb;

  // -- Constructor --

  /** Constructs a new ICSReader. */
  public ICSReader() {
    super("Image Cytometry Standard", new String[] {"ics", "ids"});
  }

  // -- FormatReader API methods --

  /* @see loci.formats.IFormatReader#isThisType(byte[]) */ 
  public boolean isThisType(byte[] block) {
    return false;
  }

  /* @see loci.formats.IFormatReader#getImageCount(String) */ 
  public int getImageCount(String id) throws FormatException, IOException {
    if (!id.equals(currentIdsId) && !id.equals(currentIcsId)) initFile(id);
    if (numImages == 1) return 1;
    return numImages / (rgb ? core.sizeC[0] : 1);
  }

  /* @see loci.formats.IFormatReader#isRGB(String) */ 
  public boolean isRGB(String id) throws FormatException, IOException {
    if (!id.equals(currentIdsId) && !id.equals(currentIcsId)) initFile(id);
    return rgb && core.sizeC[0] > 1;
  }

  /* @see loci.formats.IFormatReader#isLittleEndian(String) */ 
  public boolean isLittleEndian(String id) throws FormatException, IOException {
    if (!id.equals(currentIdsId) && !id.equals(currentIcsId)) initFile(id);
    return littleEndian;
  }

  /* @see loci.formats.IFormatReader#isInterleaved(String, int) */
  public boolean isInterleaved(String id, int subC)
    throws FormatException, IOException
  {
    if (!id.equals(currentIdsId) && !id.equals(currentIcsId)) initFile(id);
    return !rgb;
  }

  /* @see loci.formats.IFormatReader#openBytes(String, int) */
  public byte[] openBytes(String id, int no)
    throws FormatException, IOException
  {
    if (!id.equals(currentId)) initFile(id);
    byte[] buf = new byte[core.sizeX[0] * core.sizeY[0] * (bitsPerPixel / 8) *
      getRGBChannelCount(id)];
    return openBytes(id, no, buf);
  }

  /* @see loci.formats.IFormatReader#openBytes(String, int, byte[]) */
  public byte[] openBytes(String id, int no, byte[] buf)
    throws FormatException, IOException
  {
    if (!id.equals(currentIdsId) && !id.equals(currentIcsId)) initFile(id);
    if (no < 0 || no >= getImageCount(currentId)) {
      throw new FormatException("Invalid image number: " + no);
    }
    if (buf.length < core.sizeX[0] * core.sizeY[0] * (bitsPerPixel / 8) *
      getRGBChannelCount(id))
    {
      throw new FormatException("Buffer too small.");
    }

    int bpp = bitsPerPixel / 8;

    int len = core.sizeX[0] * core.sizeY[0] * bpp * getRGBChannelCount(id);
    int offset = len * no;
    if (!rgb && core.sizeC[0] > 4) {
      int pt = 0;
      for (int i=no*bpp; i<data.length; i+=core.sizeC[0]*bpp) {
        System.arraycopy(data, i, buf, pt, bpp);
        pt += bpp;
      }
    }
    else System.arraycopy(data, offset, buf, 0, len);

    // if it's version two, we need to flip the plane upside down
    if (versionTwo) {
      int scanline = core.sizeX[0] * bpp * core.sizeC[0];
      for (int y=0; y<core.sizeY[0]; y++) {
        for (int x=0; x<scanline; x++) {
          byte bottom = buf[y*scanline + x];
          buf[y*scanline + x] = buf[(core.sizeY[0] - y - 1)*scanline + x];
          buf[(core.sizeY[0] - y - 1)*scanline + x] = bottom;
        }
      }
    }
    return buf;
  }

  /* @see loci.formats.IFormatReader#openImage(String, int) */ 
  public BufferedImage openImage(String id, int no)
    throws FormatException, IOException
  {
    if (!id.equals(currentIdsId) && !id.equals(currentIcsId)) initFile(id);

    byte[] plane = openBytes(id, no);
    int channels = rgb ? core.sizeC[0] : 1;

    int bytes = bitsPerPixel / 8;

    if (bytes == 4) {
      float[] f = new float[core.sizeX[0] * core.sizeY[0] * channels];
      int pt = 0;
      for (int i=0; i<f.length; i++) {
        int p = DataTools.bytesToInt(plane, i*4, 4, littleEndian);
        f[i] = Float.intBitsToFloat(p);
      }

      if (normalizeData) f = DataTools.normalizeFloats(f);

      return ImageTools.makeImage(f, core.sizeX[0], core.sizeY[0], 
        channels, true);
    }

    return ImageTools.makeImage(plane, core.sizeX[0], core.sizeY[0], channels, 
      true, bytes, littleEndian);
  }

  /* @see loci.formats.IFormatReader#getUsedFiles(String) */
  public String[] getUsedFiles(String id) throws FormatException, IOException {
    if (!id.equals(currentIdsId) && !id.equals(currentIcsId)) initFile(id);
    if (versionTwo) {
      return new String[] {currentIdsId == null ? id : currentIdsId};
    }
    return new String[] {currentIdsId, currentIcsId};
  }

  /* @see loci.formats.IFormatReader#close(boolean) */
  public void close(boolean fileOnly) throws FormatException, IOException {
    if (fileOnly && idsIn != null) idsIn.close();
    else if (!fileOnly) close();
  }

  /* @see loci.formats.IFormatReader#close() */ 
  public void close() throws FormatException, IOException {
    if (idsIn != null) idsIn.close();
    idsIn = null;
    icsIn = null;
    currentIcsId = null;
    currentIdsId = null;
    data = null;
  }

  /** Initializes the given ICS file. */
  protected void initFile(String id) throws FormatException, IOException {
    if (debug) debug("ICSReader.initFile(" + id + ")");
    super.initFile(id);

    status("Finding companion file");

    String icsId = id, idsId = id;
    int dot = id.lastIndexOf(".");
    String ext = dot < 0 ? "" : id.substring(dot + 1).toLowerCase();
    if (ext.equals("ics")) {
      // convert C to D regardless of case
      char[] c = idsId.toCharArray();
      c[c.length - 2]++;
      idsId = new String(c);
    }
    else if (ext.equals("ids")) {
      // convert D to C regardless of case
      char[] c = icsId.toCharArray();
      c[c.length - 2]--;
      icsId = new String(c);
    }

    if (icsId == null) throw new FormatException("No ICS file found.");
    Location icsFile = new Location(icsId);
    if (!icsFile.exists()) throw new FormatException("ICS file not found.");

    status("Checking file version");

    // check if we have a v2 ICS file
    RandomAccessStream f = new RandomAccessStream(icsId);
    byte[] b = new byte[17];
    f.read(b);
    f.close();
    if (new String(b).trim().equals("ics_version\t2.0")) {
      idsIn = new RandomAccessStream(icsId);
      versionTwo = true;
    }
    else {
      if (idsId == null) throw new FormatException("No IDS file found.");
      Location idsFile = new Location(idsId);
      if (!idsFile.exists()) throw new FormatException("IDS file not found.");
      currentIdsId = idsId;
      idsIn = new RandomAccessStream(idsId);
    }

    currentIcsId = icsId;

    icsIn = icsFile;

    status("Reading metadata");

    RandomAccessStream reader = new RandomAccessStream(icsIn.getAbsolutePath());
    StringTokenizer t;
    String token;
    b = new byte[(int) reader.length()];
    reader.read(b);
    reader.close();
    String s = new String(b);
    StringTokenizer st = new StringTokenizer(s, "\n");
    String line = st.nextToken();
    line = st.nextToken();
    while (line != null && !line.trim().equals("end")) {
      t = new StringTokenizer(line);
      StringBuffer key = new StringBuffer();
      while (t.hasMoreTokens()) {
        token = t.nextToken();
        boolean foundValue = true;
        for (int i=0; i<CATEGORIES.length; i++) {
          if (token.equals(CATEGORIES[i])) foundValue = false;
        }
        for (int i=0; i<SUB_CATEGORIES.length; i++) {
          if (token.equals(SUB_CATEGORIES[i])) foundValue = false;
        }
        for (int i=0; i<SUB_SUB_CATEGORIES.length; i++) {
          if (token.equals(SUB_SUB_CATEGORIES[i])) foundValue = false;
        }

        if (foundValue) {
          StringBuffer value = new StringBuffer();
          value.append(token);
          while (t.hasMoreTokens()) {
            value.append(" ");
            value.append(t.nextToken());
          }
          addMeta(key.toString().trim(), value.toString().trim());
        }
        else {
          key.append(token);
          key.append(" ");
        }
      }
      if (st.hasMoreTokens()) line = st.nextToken();
      else line = null;
    }

    status("Populating metadata");

    String images = (String) getMeta("layout sizes");
    String ord = (String) getMeta("layout order");
    ord = ord.trim();
    // bpp, width, height, z, channels
    StringTokenizer t1 = new StringTokenizer(images);
    StringTokenizer t2 = new StringTokenizer(ord);

    rgb = ord.indexOf("ch") >= 0 && ord.indexOf("ch") < ord.indexOf("x");

    String imageToken;
    String orderToken;
    while (t1.hasMoreTokens() && t2.hasMoreTokens()) {
      imageToken = t1.nextToken().trim();
      orderToken = t2.nextToken().trim();
      if (orderToken.equals("bits")) {
        bitsPerPixel = Integer.parseInt(imageToken);
      }
      else if(orderToken.equals("x")) {
        core.sizeX[0] = Integer.parseInt(imageToken);
      }
      else if(orderToken.equals("y")) {
        core.sizeY[0] = Integer.parseInt(imageToken);
      }
      else if(orderToken.equals("z")) {
        core.sizeZ[0] = Integer.parseInt(imageToken);
      }
      else if(orderToken.equals("ch")) {
        core.sizeC[0] = Integer.parseInt(imageToken);
        if (core.sizeC[0] > 4) rgb = false;
      }
      else {
        core.sizeT[0] = Integer.parseInt(imageToken);
      }
    }

    numImages = core.sizeZ[0] * core.sizeC[0] * core.sizeT[0];
    if (numImages == 0) numImages++;

    String endian = (String) getMeta("representation byte_order");
    littleEndian = true;

    if (endian != null) {
      StringTokenizer endianness = new StringTokenizer(endian);
      String firstByte = endianness.nextToken();
      int first = Integer.parseInt(firstByte);
      littleEndian =
        ((String) getMeta("representation format")).equals("real") ?
        first == 1 : first != 1;
    }

    String test = (String) getMeta("representation compression");
    boolean gzip = (test == null) ? false : test.equals("gzip");

    if (versionTwo) {
      s = idsIn.readLine();
      while(!s.trim().equals("end")) s = idsIn.readLine();
    }
    data = new byte[(int) (idsIn.length() - idsIn.getFilePointer())];

    // extra check is because some of our datasets are labeled as 'gzip', and
    // have a valid GZIP header, but are actually uncompressed
    if (gzip && ((data.length / (numImages) < 
      (core.sizeX[0] * core.sizeY[0] * bitsPerPixel / 8))))
    {
      status("Decompressing pixel data"); 
      idsIn.read(data);
      byte[] buf = new byte[8192];
      ByteVector v = new ByteVector();
      try {
        GZIPInputStream decompressor =
          new GZIPInputStream(new ByteArrayInputStream(data));
        int r = decompressor.read(buf, 0, buf.length);
        while (r > 0) {
          v.add(buf, 0, r);
          r = decompressor.read(buf, 0, buf.length);
        }
        data = v.toByteArray();
      }
      catch (IOException dfe) {
        throw new FormatException("Error uncompressing gzip'ed data", dfe);
      }
    }
    else idsIn.readFully(data);

    status("Populating metadata");

    // Populate metadata store

    // The metadata store we're working with.
    MetadataStore store = getMetadataStore(id);

    store.setImage((String) getMeta("filename"), null, null, null);

    // populate Pixels element

    String o = (String) getMeta("layout order");
    o = o.trim();
    o = o.substring(o.indexOf("x")).trim();
    char[] tempOrder = new char[(o.length() / 2) + 1];
    int pt = 0;
    for (int i=0; i<o.length(); i+=2) {
      tempOrder[pt] = o.charAt(i);
      pt++;
    }
    o = new String(tempOrder).toUpperCase().trim();
    if (o.indexOf("Z") == -1) o = o + "Z";
    if (o.indexOf("T") == -1) o = o + "T";
    if (o.indexOf("C") == -1) o = o + "C";

    String fmt = (String) getMeta("representation format");
    String sign = (String) getMeta("representation sign");

    if (bitsPerPixel < 32) littleEndian = !littleEndian;

    if (fmt.equals("real")) core.pixelType[0] = FormatTools.FLOAT;
    else if (fmt.equals("integer")) {
      while (bitsPerPixel % 8 != 0) bitsPerPixel++;
      if (bitsPerPixel == 24 || bitsPerPixel == 48) bitsPerPixel /= 3;

      switch (bitsPerPixel) {
        case 8:
          core.pixelType[0] = FormatTools.UINT8;
          break;
        case 16:
          core.pixelType[0] = FormatTools.UINT16;
          break;
        case 32:
          core.pixelType[0] = FormatTools.UINT32;
          break;
      }
    }
    else {
      throw new RuntimeException("Unknown pixel format: " + format);
    }

    core.currentOrder[0] = o.trim();

    store.setPixels(
      new Integer(core.sizeX[0]), // SizeX
      new Integer(core.sizeY[0]), // SizeY
      new Integer(core.sizeZ[0]), // SizeZ
      new Integer(core.sizeC[0]), // SizeC
      new Integer(core.sizeT[0]), // SizeT
      new Integer(core.pixelType[0]), // PixelType
      new Boolean(!littleEndian), // BigEndian
      core.currentOrder[0], // DimensionOrder
      null, // Use image index 0
      null); // Use pixels index 0

    String pixelSizes = (String) getMeta("parameter scale");
    o = (String) getMeta("layout order");
    if (pixelSizes != null) {
      StringTokenizer pixelSizeTokens = new StringTokenizer(pixelSizes);
      StringTokenizer axisTokens = new StringTokenizer(o);

      Float pixX = null, pixY = null, pixZ = null, pixC = null, pixT = null;

      while (pixelSizeTokens.hasMoreTokens()) {
        String axis = axisTokens.nextToken().trim().toLowerCase();
        String size = pixelSizeTokens.nextToken().trim();
        if (axis.equals("x")) pixX = new Float(size);
        else if (axis.equals("y")) pixY = new Float(size);
        else if (axis.equals("ch")) pixC = new Float(size);
        else if (axis.equals("z")) pixZ = new Float(size);
        else if (axis.equals("t")) pixT = new Float(size);
      }
      store.setDimensions(pixX, pixY, pixZ, pixC, pixT, null);
    }

    String em = (String) getMeta("sensor s_params LambdaEm");
    String ex = (String) getMeta("sensor s_params LambdaEx");
    int[] emWave = new int[core.sizeC[0]];
    int[] exWave = new int[core.sizeC[0]];
    if (em != null) {
      StringTokenizer emTokens = new StringTokenizer(em);
      for (int i=0; i<core.sizeC[0]; i++) {
        emWave[i] = (int) Float.parseFloat(emTokens.nextToken().trim());
      }
    }
    if (ex != null) {
      StringTokenizer exTokens = new StringTokenizer(ex);
      for (int i=0; i<core.sizeC[0]; i++) {
        exWave[i] = (int) Float.parseFloat(exTokens.nextToken().trim());
      }
    }

    for (int i=0; i<core.sizeC[0]; i++) {
      store.setLogicalChannel(i, null, null, new Integer(emWave[i]),
        new Integer(exWave[i]), null, null, null);
    }
  }

}
