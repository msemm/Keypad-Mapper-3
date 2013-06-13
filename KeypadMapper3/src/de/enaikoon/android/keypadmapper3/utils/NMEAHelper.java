package de.enaikoon.android.keypadmapper3.utils;

import java.util.Calendar;
import java.util.TimeZone;

import android.util.Log;

/**
 * 
 * @author Dinko Ivkovic
 * 
 */
public class NMEAHelper
{
	public static int fix = 0;
	public static double PDOP = 0;
	public static double HDOP = 0;
	public static double VDOP = 0;
	public static Long NMEATime;

	/**
	 * parses the NMEa sentences
	 * 
	 * @param NMEA
	 *            sentence
	 */
	public static void parse(String sentence)
	{
		try
		{
			if (sentence != null)
			{
				//FileLog.writeToLog(NMEAHelper.class.getName(), sentence);

				if (sentence.startsWith("$GPGSA") || sentence.startsWith("$GPRMC"))
				{
					String[] sentenceArray = sentence.split(",");
					
					if (sentenceArray[0].equals("$GPGSA"))
					{
						/*
						 * get needed GPS values
						 */
						try
						{
							fix = Integer.parseInt(sentenceArray[2]);
						} catch (Exception e)
						{
						}

						try
						{
							PDOP = Double.parseDouble(sentenceArray[15]);
						} catch (Exception e)
						{
						}

						try
						{
							HDOP = Double.parseDouble(sentenceArray[16]);
						} catch (Exception e)
						{
						}

						try
						{
							VDOP = Double.parseDouble(sentenceArray[17].substring(0, sentenceArray[17].indexOf("*")));
						} catch (Exception e)
						{
						}

					}
					else if (sentenceArray[0].equals("$GPRMC"))
					{
						ParseGPRMC(sentenceArray);
					}					
				}
			}
		} catch (Exception ex)
		{
			Log.e("KeypadMapper", "", ex);
		}
	}
	
	private static void ParseGPRMC(String[] sArrNMEA)
	{
		try
		{
			if (sArrNMEA.length > 9)
			{
				int Hr = 0;
				int Mins = 0;
				int Secs = 0;

				if (!sArrNMEA[1].equals(""))
				{

					Hr = Integer.parseInt(sArrNMEA[1].substring(0, 2));
					Mins = Integer.parseInt(sArrNMEA[1].substring(2, 4));

					if (sArrNMEA[1].length() > 6)
					{

						Secs = Integer.parseInt(sArrNMEA[1].substring(4, 6));
					} else
					{
						Secs = Integer.parseInt(sArrNMEA[1].substring(4));
					}

				}
				if (!sArrNMEA[9].equals(""))
				{
					int Day = Integer.parseInt(sArrNMEA[9].substring(0, 2));
					int Month = Integer.parseInt(sArrNMEA[9].substring(2, 4));
					if (Month > 0)
					{
						Month = Month - 1;
					}
					int Year = Integer.parseInt(sArrNMEA[9].substring(4));
					Year = 2000 + Year;

					if (!sArrNMEA[1].equals(""))
					{

						Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
						cal.set(Year, Month, Day, Hr, Mins, Secs);

						NMEATime = cal.getTimeInMillis();					
					}
				}
			}
		} catch (Exception e)
		{
		    Log.e("KeypadMapper", "", e);
		}
	}
}
