package de.enaikoon.android.inviu.opencellidlibrary;

import java.io.File;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Environment;
import android.os.StatFs;

public class Configurator
{
	private static String SDCARD_DIRECTORY_NAME = Environment.getExternalStorageDirectory() + "/" + "ENAiKOON" + "/";
	private static String DATABASE_NAME = "EGT.db3";

	private static int maxDatabaseSize=Integer.MAX_VALUE;
	private static int maxLogSize=Integer.MAX_VALUE;
	private static long gpsTimeout=Long.MAX_VALUE;
	private static boolean PRODUCTION_VERSION=false;
	
	private static boolean automaticUpload=true;
	
	private static int minSignalLevelDifference=4;
	private static int minDistance=15;
	private static int minTimestampDifference=5000;
	
	
	/**
	 * @return database size in MB
	 */
	public static double getDatabaseSize()
	{
		try
		{

			File f = new File(Database.DATABASE_PATH);

			return ((double) f.length()) / 1000 / 1000;
		} catch (Exception ex)
		{
			return 0;
		}
	}

	public static Boolean isDatabaseValid()
	{
		if ((Configurator.getDatabaseSize() > maxDatabaseSize)
				|| (Configurator.getDatabaseSize() > (getStorageFreeSpace() - getStorageFreeSpace() * 10 / 100)))
		{
			return false;
		} else
		{
			return true;
		}
	}
	
	protected static float getStorageFreeSpace()
	{
		File path = Environment.getExternalStorageDirectory();
		StatFs stat = new StatFs(path.getPath());
		return (((float) stat.getAvailableBlocks() * stat.getBlockSize()) / 1024 / 1000);
	}

	public static int getMaxDatabaseSize()
	{
		return maxDatabaseSize;
	}

	/**
	 * Defines maximum database size in which the data will be locally stored
	 * 
	 * default value - Integer.MAX_VALUE MB
	 * 
	 * @param maxDatabaseSize - maximum database size in MB
	 */
	public static void setMaxDatabaseSize(int maxDatabaseSize)
	{
		Configurator.maxDatabaseSize = maxDatabaseSize;
	}

	public static boolean isPRODUCTION_VERSION()
	{
		return PRODUCTION_VERSION;
	}

	/**
	 * If PRODUCTION_VERSION is set to TRUE
	 * - the data will be transferred to the production server
	 * - logging will be active
	 * 
	 * default value - false
	 * 
	 * @param pRODUCTION_VERSION - true if the data should be transferred to the production server, false otherwise
	 */

	public static void setPRODUCTION_VERSION(boolean pRODUCTION_VERSION)
	{
		PRODUCTION_VERSION = pRODUCTION_VERSION;
	}

	public static int getMaxLogSize()
	{
		return maxLogSize;
	}

	/**
	 * Defines maximum log file size
	 * 
	 * default value - Integer.MAX_VALUE MB
	 * 
	 * @param maxLogSize - maximum log file size in MB
	 */

	public static void setMaxLogSize(int maxLogSize)
	{
		Configurator.maxLogSize = maxLogSize;
	}

	public static long getGpsTimeout()
	{
		return gpsTimeout;
	}

	/**
	 * Defines maximum amount of time after the GPS will be turned off, until next cellID change
	 * 
	 * default value - Long.MAX_VALUE ms
	 * 
	 * @param gpsTimeout - time in miliseconds
	 */
	public static void setGpsTimeout(long gpsTimeout)
	{
		Configurator.gpsTimeout = gpsTimeout;
	}

	public static boolean isAutomaticUpload()
	{
		return automaticUpload;
	}

	/**
	 * defines if the data will be automatically transferred to the server
	 * 
	 *   default value - true
	 *   
	 * @param automaticUpload
	 */
	public static void setAutomaticUpload(boolean automaticUpload)
	{
		Configurator.automaticUpload = automaticUpload;
	}

	public static String getSDCARD_DIRECTORY_NAME()
	{
		return SDCARD_DIRECTORY_NAME;
	}

	/**
	 * defines the path where the database and log file will be stored
	 * 
	 * default value - Environment.getExternalStorageDirectory() + "/" + "ENAiKOON" + "/"
	 * 
	 * @param sDCARD_DIRECTORY_NAME
	 */
	public static void setSDCARD_DIRECTORY_NAME(String sDCARD_DIRECTORY_NAME)
	{
		SDCARD_DIRECTORY_NAME = sDCARD_DIRECTORY_NAME;
	}

	public static int getMinSignalLevelDifference()
	{
		return minSignalLevelDifference;
	}

	/**
	 * defines allowed minimum signal level difference between current signal and the last retrieved signal
	 * @param minSignalLevelDifference
	 */
	public static void setMinSignalLevelDifference(int minSignalLevelDifference)
	{
		Configurator.minSignalLevelDifference = minSignalLevelDifference;
	}

	public static int getMinDistance()
	{
		return minDistance;
	}

	/**
	 * defines allowed minimum distance between current position and the last retrieved position
	 * @param minDistance - distance in meters
	 */
	public static void setMinDistance(int minDistance)
	{
		Configurator.minDistance = minDistance;
	}

	public static int getMinTimestampDifference()
	{
		return minTimestampDifference;
	}

	/**
	 * defines allowed minimum time interval between current position and the last retrieved position
	 * @param minTimestampDifference in miliseconds
	 */
	public static void setMinTimestampDifference(int minTimestampDifference)
	{
		Configurator.minTimestampDifference = minTimestampDifference;
	}
	
	public static String getDATABASE_NAME()
	{
		return DATABASE_NAME;
	}

	/**
	 * defines database name which will be stored in getSDCARD_DIRECTORY_NAME()
	 * @param DATABASE_NAME
	 */
	public static void setDATABASE_NAME(String DATABASE_NAME)
	{
		Configurator.DATABASE_NAME = DATABASE_NAME;
	}	
}
