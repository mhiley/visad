//
// McIDASUtil.java
//

/*
The code in this file is Copyright(C) 1999 by Don
Murray.  It is designed to be used with the VisAD system for 
interactive analysis and visualization of numerical data.  
 
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

package edu.wisc.ssec.mcidas;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Class for static McIDAS utility methods.  In many cases, these
 * methods are the Java equivalents of  McIDAS library functions.
 * @see <A HREF="http://www.ssec.wisc.edu/mug/prog_man/prog_man.html">
 *      McIDAS Programmer's Manual</A>
 *
 * @author Don Murray, Unidata
 * @author Tom Whittaker, SSEC
 */
public final class McIDASUtil 
{

    /** McIDAS missing value for 4-byte integers */
    public static final int MCMISSING = 0x80808080;

    /**
     * Converts a packed integer (SIGN DDD MM SS) latitude/longitude to double.
     * Java version of McIDAS <code>flalo</code> function except returns a 
     * double instead of a float.
     *
     * @param value  integer containing the packed data
     * @return  double representation of value
     */
    public static double integerLatLonToDouble(int value)
    {
        return mcPackedIntegerToDouble(value);
    }
   
    /**
     * Converts a double latitude/longitude to a packed integer (SIGN DDD MM SS)
     * Java version of McIDAS <code>ilalo</code> function.
     *
     * @param value  double value of lat/lon
     * @return  packed integer representation of value
     */
    public static int doubleLatLonToInteger(double dvalue)
    {
        return mcDoubleToPackedInteger(dvalue);
    }
   
    /**
     * Converts a packed integer (SIGN DDD/HH MM SS) latitude/longitude 
     * or time (hours) to double. 
     * Java replacements of McIDAS <code>flalo</code> and <code>ftime</code> 
     * functions except returns a double instead of a float.
     *
     * @param value  integer containing the packed data
     * @return  double representation of value
     */
    public static double mcPackedIntegerToDouble(int value)
    {
        int val = value < 0 ? -value : value;
        double dvalue  = ((double) (val/10000) + 
                          ((double) ((val/100)%100))/60.0 +
                          (double) (val%100)/3600.0);
        return (value < 0) ? -dvalue : dvalue;
    }
   
    /**
     * Converts a double latitude/longitude or time (hours) to a 
     * packed integer (SIGN DDD/HH MM SS). Java replacements of McIDAS 
     * <code>ilalo</code> and <code>m0itime</code> functions.
     *
     * @param value  double value of lat/lon or time
     * @return  packed integer representation of value
     */
    public static int mcDoubleToPackedInteger(double dvalue)
    {
        double dval = dvalue < 0 ? -dvalue : dvalue;
        int j = (int) (3600.0*dval + 0.5);
        int value  = 10000*(j/3600) + 100*((j/60)%60) + j%60;
        return (dvalue < 0.0) ? -value : value;
    }

    /**
     * Calculate difference in minutes between two dates/times.  Java
     * version of timdif.for
     *
     * @param     yrday1   Year/day of first time (yyddd or yyyyddd)
     * @param     hms1     Hours/minutes/seconds of first time (hhmmss).
     * @param     yrday2   Year/day of second time (yyddd).
     * @param     hms2     Hours/minutes/seconds of second time (hhmmss).
     *
     * @return  The difference between the two times (time2 - time1), 
     *          in minutes. If the first time is greater than the second, 
     *          the result will be negative.  
     */
    public static double timdif(int yrday1, int hms1, int yrday2, int hms2)
    {
        long secs1 = mcDayTimeToSecs(yrday1, hms1);
        long secs2 = mcDayTimeToSecs(yrday2, hms2);
        return (double) (secs2 - secs1)/60.;
    }

    /**
     * Convert day (yyddd or yyyyddd) and time (hhmmss) to seconds since
     * the epoch (January 1, 1970, 00:00GMT).  Java version of 'mcdaytimetosecs'
     * except it returns a long instead of an int.
     *
     * @param    yearday    year/day in either yyddd or yyyyddd format.  
     *                      Only works for years > 1900.
     * @param    time       time in packed integer format (hhmmss)
     *
     * @return  seconds since the epoch
     *
     */
    public static long mcDayTimeToSecs(int yearday, int time)
    {
        int year = ((yearday/1000)%1900) + 1900;  // convert to yyyyddd first
        int day =  yearday%1000;
        double seconds = mcPackedIntegerToDouble(time)*3600.;

        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.ERA, GregorianCalendar.AD);
        cal.set(Calendar.YEAR, year);
        /* 
           allow us to specify # of days since the year began without having
           worry about leap years and seconds since the day began, instead
           of in the minute.  Saves on some calculations.
        */
        cal.setLenient(true);         

        cal.set(Calendar.DAY_OF_YEAR, day);
        int secs = ((int) Math.round(seconds * 1000))/1000;
        cal.set(Calendar.SECOND, secs);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime().getTime()/1000;
    }

    /**
     * Convert date (yyyymmdd or yyyymmdd) and hms (hhmmss) to seconds since
     * the epoch (January 1, 1970, 00:00GMT).  
     *
     * @param    date       year/day in yyymmdd format.
     *                      Only works for years > 1900.
     * @param    time       time in packed integer format (hhmmss)
     *
     * @return  seconds since the epoch
     *
     */
    public static long mcDateHmsToSecs(int date, int time)
    {
        int year = date/10000;
        int month = (date%10000)/100;
        int day =  date%100;
        System.out.println("year = " + year);
        System.out.println("month = " + month);
        System.out.println("day = " + day);
        double seconds = mcPackedIntegerToDouble(time)*3600.;

        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.ERA, GregorianCalendar.AD);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        int secs = ((int) Math.round(seconds * 1000))/1000;
        cal.set(Calendar.SECOND, secs);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime().getTime()/1000;
    }

    /**
     * Convert seconds since the epoch (January 1, 1970, 00:00GMT) to
     * day (yyyyddd) and time (hhmmss).  Java version of 
     * 'mcsecstodaytime' except it returns an int array instead of pointers 
     *
     * @param    secs    seconds since the epoch
     *
     * @return  int[2] array with day (yyyyddd) as first element  and
     *          time (hhmmss - packed integer) as second element.
     */
    public static int[] mcSecsToDayTime(long secs)
    {
        int[] retvals = new int[2];
        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.setTime(new Date(secs*1000));
        retvals[0] = (cal.get(cal.YEAR)*1000) + cal.get(cal.DAY_OF_YEAR);
        retvals[1] = 
            (cal.get(cal.HOUR_OF_DAY)*10000) + 
            (cal.get(cal.MINUTE)*100) +
            cal.get(cal.SECOND);
        return retvals;
    }

    /**
     * Flip the bytes of an integer array.  Java version of 'm0swbyt4'.
     *
     * @param array[] array of integers to be flipped
     * @param first starting element of the array
     * @param last last element of array to flip
     *
     */
    public static void flip(int array[], int first, int last) 
    {
        int i,k;
        for (i=first; i<=last; i++) 
        {
            k = array[i];
            array[i] = ( (k >>> 24) & 0xff) | ( (k >>> 8) & 0xff00) |
                       ( (k & 0xff) << 24 )  | ( (k & 0xff00) << 8);
        }
    }

    /**
     * convert four consequtive bytes into a (signed) int. This
     * is useful in dealing with McIDAS data files.
     *
     * @param byte[] array of 4 bytes
     * @param off is the offset into the byte array
     *
     */
    public static int bytesToInteger(byte[] b, int off) {

      int k = ( b[off] << 24) +
         ((b[off+1] << 16)&0xff0000) +
         ((b[off+2] << 8)&0xff00) +
         ((b[off+3] << 0)&0xff);

       return k;
    }

    /**
     * convert consequtive bytes into a (signed) int array. This
     * is useful in dealing with McIDAS data files.
     *
     * @param byte[] array of bytes
     * @param off is the offset into the byte array
     * @param num number of integers to create
     *
     */
    public static int[] bytesToIntegerArray(byte[] b, int off, int num) {

      int[] values = new int[num];
      for (int i = 0; i < num; i++) {
        byte[] bytes = new byte[4];
	System.arraycopy(b, i*4, bytes, 0, 4);
        values[i] = bytesToInteger(bytes,0);
      }
      return values;
    }

    /**
     * convert signed int to a String representation.  This is useful
     * in dealing with McIDAS data files. Java version of 'clit'.
     *
     * @param value  - integer representation of a string
     *
     * @return  String representation of the int
     */
    public static String intBitsToString(int value)
    {
        byte[] bval = new byte[4];
        bval[0] = (byte) ((value & 0xff000000) >>> 24);
        bval[1] = (byte) ((value & 0x00ff0000) >>> 16);
        bval[2] = (byte) ((value & 0x0000ff00) >>> 8);
        bval[3] = (byte) ((value & 0x000000ff) >>> 0);
        return new String(bval);
    }

    /**
     * convert signed int array to a String representation.  This is useful
     * in dealing with McIDAS data files.  Java version of 'movwc'. 
     *
     * @param values  - integer array representation of a string
     *
     * @return  String representation of the int array
     */
    public static String intBitsToString(int[] values)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < values.length; i++)
            sb.append(intBitsToString(values[i]));
        return sb.toString();
    }

    /**
     * Check to see if the int value is the representation of a
     * string or not.  Java version of ischar_.c (sort of).
     *
     * @param value  integer representation
     * @return true if the int represents a string
     */
    public static boolean isChar(int value) {
        String valueString = intBitsToString(value);
	char[] chars = valueString.toCharArray();
	for (int i = 0; i < 4; i++) {
	   if (!Character.UnicodeBlock.of(chars[i]).equals(
	       Character.UnicodeBlock.BASIC_LATIN) ||
	       Character.isISOControl(chars[i])) return false;
	}
	return true;
    }
}
