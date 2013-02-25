package de.enaikoon.android.inviu.opencellidlibrary;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;

/**
 * 
 * @author Dinko Ivkovic
 * 
 */
public class FileLog
{
	/*
	 * holds a name of directory on sd card
	 */
	private static File f;
	private static FileWriter fw;
	
	private static boolean logFileNamefirstTime = true;
	private static int logFileNameIterator = 1;
	public static String logFileNamePrefix = "opencellidlibrary"; 
	
	private static HandlerCheckLogSize rhCheckLogSize = new HandlerCheckLogSize();

	/**
	 * Writes a message to the Log file
	 * 
	 * @param message
	 *            message which will be written to the Log file
	 */
	public static void writeToLog(final String message)
	{
		if (!Configurator.isPRODUCTION_VERSION() && Configurator.getMaxLogSize() > 0)
		{
			new Thread(new Runnable()
			{

				@Override
				public void run()
				{
					try
					{
						if (!logFileExists())
						{
							checkLogFile();
						}

						Log.d("writeToLog", message);

						getLogFW().write(new Date().toGMTString() + "   " + message + "\n");
						getLogFW().flush();
					} catch (Exception ex)
					{
						checkLogFile();
						Log.e(Configurator.class.getName(), "Error writing to the Log file", ex);
					}
				}
			}).start();
		}
	}

	/**
	 * Writes an exception to the Log file
	 * 
	 * @param ex
	 */
	public static void writeExceptionToLog(final Throwable ex)
	{
		if (!Configurator.isPRODUCTION_VERSION() && Configurator.getMaxLogSize() > 0)
		{
			new Thread(new Runnable()
			{

				@Override
				public void run()
				{
					try
					{
						if (ex != null && ex.getMessage() != null)
						{
							if (!logFileExists())
							{
								checkLogFile();
							}

							Log.e("writeToLog", ex.getMessage(), ex);
							
							final Writer result = new StringWriter();
							final PrintWriter printWriter = new PrintWriter(result);
							ex.printStackTrace(printWriter);
							final String stacktrace = result.toString();
							printWriter.close();
							
							getLogFW().write(new Date().toGMTString() + "   EXCEPTION! \n" + ex.getMessage() + "\n" + stacktrace + "\n");
							getLogFW().flush();
						}
					} catch (Exception e)
					{
						Log.e(Configurator.class.getName(), "Error writing to the Log file", e);
					}
				}
			}).start();
		}
	}

	/**
	 * checks if the Log file exists
	 */
	private static void checkLogFile()
	{
		try
		{
			f = new File(Configurator.getSDCARD_DIRECTORY_NAME());
			// create directory structure if not exists
			f.mkdirs();

			// create a file if not exists
			f = new File(Configurator.getSDCARD_DIRECTORY_NAME() + logFileNamePrefix + logFileNameIterator + ".log");
			fw = new FileWriter(f, true);
		} catch (Exception ex)
		{
			Log.e(FileLog.class.getName(), "Error checking the Log file", ex);
		}
	}

	/**
	 * Returns a FileWriter of the Log file
	 * 
	 * @return FileWriter
	 */
	private static FileWriter getLogFW()
	{
		if (fw == null)
		{
			checkLogFile();
		}

		return fw;
	}

	/**
	 * Deletes the Log file
	 * 
	 * @return true if it is deleted, false in case of an error
	 */
	public static Boolean deleteLogFile()
	{
		try
		{
			f = new File(Configurator.getSDCARD_DIRECTORY_NAME() + logFileNamePrefix + logFileNameIterator + ".log");
			if (f.exists())
			{
				f.delete();
			}
			f = null;

			fw = null;

			return true;

		} catch (Exception ex)
		{
			Log.e(FileLog.class.getName(), "Error deleting a Log file", ex);
			return false;
		}
	}
	
	private static void checkLogFileSize(boolean firstTime)
	{
		try
		{
			File fTMP = new File(Configurator.getSDCARD_DIRECTORY_NAME() + logFileNamePrefix + logFileNameIterator + ".log");
			if (fTMP.exists())
			{
				long size = fTMP.length();
				
				if (size>=(Configurator.getMaxLogSize()*1E6/2))
				{
					if (logFileNameIterator==1)
					{
						if (firstTime)
						{
							logFileNameIterator=2;
							
							f=null;
							if (fw!=null)
							{
								fw.close();
								fw=null;	
							}
							
							checkLogFileSize(firstTime);
						}
						else
						{
							File f2 = new File(Configurator.getSDCARD_DIRECTORY_NAME() + logFileNamePrefix + (logFileNameIterator+1) + ".log");
							f2.delete();
							f2=null;
							
							logFileNameIterator=2;
							
							f=null;
							if (fw!=null)
							{
								fw.close();
								fw=null;	
							}
						}
					}
					else if (logFileNameIterator==2)
					{
						File f2 = new File(Configurator.getSDCARD_DIRECTORY_NAME() + logFileNamePrefix + (logFileNameIterator-1) + ".log");
						f2.delete();
						f2=null;
					
						logFileNameIterator=1;
						
						f=null;
						if (fw!=null)
						{
							fw.close();
							fw=null;	
						}
					}
				}
			}
			else
			{
				f=null;
				if (fw!=null)
				{
					fw.close();
					fw=null;	
				}
			}
			
			fTMP=null;
			
			logFileNamefirstTime=false;
		}catch(Exception ex)
		{
			if (ex.getMessage()!=null)
			{
				Log.e("checkLogFileSize", ex.getMessage());	
			}
			else
			{
				Log.e("checkLogFileSize", "Unknown error!!!");
			}
		}
	}
	
	static class HandlerCheckLogSize extends Handler
	{
		public HandlerCheckLogSize()
		{
			sleep(1000);	
		}
		
		@Override
		public void handleMessage(Message msg)
		{
			new Thread(new Runnable()
			{
				
				@Override
				public void run()
				{
					checkLogFileSize(logFileNamefirstTime);
					sleep(1000);
				}
			}).start();
		}

		public void sleep(long delayMillis)
		{
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	};
	
	protected static float getStorageFreeSpace()
	{
		File path = Environment.getExternalStorageDirectory();
		StatFs stat = new StatFs(path.getPath());
		return (((float) stat.getAvailableBlocks() * stat.getBlockSize()) / 1024 / 1000);
	}
	
	private static Boolean logFileExists()
	{
		try
		{
			if (f == null)
			{
				f = new File(Configurator.getSDCARD_DIRECTORY_NAME() + logFileNamePrefix + logFileNameIterator + ".log");
			}

			return f.exists();
		} catch (Exception ex)
		{
			Log.e(Configurator.class.getName(), "Error checking the Log file", ex);
			return false;
		}
	}
	
	/**
	 * Writes an exception to the Log file
	 * 
	 * @param ex
	 */
	public static void writeUnhandledExceptionToLog(final Throwable ex)
	{
		if (!Configurator.isPRODUCTION_VERSION() && Configurator.getMaxLogSize() > 0)
		{
			try
			{
				if (ex != null)
				{
					if (!logFileExists())
					{
						checkLogFile();
					}
					
					final Writer result = new StringWriter();
					final PrintWriter printWriter = new PrintWriter(result);
					ex.printStackTrace(printWriter);
					final String stacktrace = result.toString();
					printWriter.close();
					
					getLogFW().write(new Date().toGMTString() + "   !!!UNHANDLED EXCEPTION!!!\n" + ((ex.getMessage()!=null)?ex.getMessage():"") + "\n" + stacktrace + "\n");
					getLogFW().flush();
				}
			} catch (Exception e)
			{
				Log.e(Configurator.class.getName(), "writeUnhandledExceptionToLog - Error writing to the Log file", e);
			}
		}
	}		
}
