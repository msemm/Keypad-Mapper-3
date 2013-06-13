/**
 * created: 16.06.2010 09:27:03
 */
package de.enaikoon.android.inviu.opencellidlibrary;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Build;

/**
 * Database to store counted raffic in.
 * 
 * @author Marcus Wolschon (Marcus@Wolschon.biz)
 */
public class Database extends SQLiteOpenHelper
{
	public interface DBIterator<T> extends Iterator<Meassurement>
	{

		public int getCount();

		public void close();
	}

	public static interface CellDBListener
	{
		public void newMeassurement(final Meassurement aMeassurement);
	}
	
	public static final String DATABASE_PATH = Configurator.getSDCARD_DIRECTORY_NAME() + Configurator.getDATABASE_NAME();
	
	private static final String COLUMN_TIMESTAMP = "timestamp";
	private static final String COLUMN_CELLID = "cellid";
	private static final String COLUMN_MNC = "mnc";
	private static final String COLUMN_MCC = "mcc";
	private static final String COLUMN_LAC = "lac";
	private static final String COLUMN_LAT = "lat";
	private static final String COLUMN_LON = "lon";
	private static final String COLUMN_SPEED = "speed";
	private static final String COLUMN_HEADING = "heading";
	private static final String COLUMN_RECEPTION = "reception";
	private static final String COLUMN_UPLOADED = "uploaded";

	private static final int DATABASE_VERSION = 5;

	private static final String TABLENAME = "cells";

	private Set<CellDBListener> myListeners = new HashSet<CellDBListener>();

	private Meassurement myLastMeassurement = null;

	private static int myTotalMeassurementCountCache = -1;
	private static int myTodayMeassurementCountCache = -1;
	private static long myTodayMeassurementCountCache_ts = -1;
	private static int myTotalMeassurementUploadedCountCache = -1;
	private static Set<String> myCellIDCountCache = null;
	private static Set<String> myCellIDCountCacheToday = null;
	private static long myCellIDCountCacheToday_ts = -1;
	private static Set<String> myNonUploadedCellIDCountCache = null;

	public Database(final Context aContext)
	{
		super(aContext, (Build.VERSION.SDK_INT < 8) ? "EGT.db3" : DATABASE_PATH, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(final SQLiteDatabase db)
	{
		FileLog.writeToLog(getClass().getName() + "onCreate(): onCreate");
		db.execSQL("CREATE TABLE " + TABLENAME + " (" + COLUMN_TIMESTAMP + " LONG," + COLUMN_CELLID + " INTEGER," + COLUMN_MNC + " INTEGER," + COLUMN_MCC
				+ " INTEGER," + COLUMN_LAC + " INTEGER," + COLUMN_RECEPTION + " INTEGER," + COLUMN_LAT + " DOUBLE," + COLUMN_LON + " DOUBLE," + COLUMN_SPEED
				+ " DOUBLE," + COLUMN_HEADING + " DOUBLE," + COLUMN_UPLOADED + " INTEGER, " + " PRIMARY KEY(" + COLUMN_TIMESTAMP + " , " + COLUMN_CELLID
				+ "));");
		db.execSQL("CREATE INDEX IF NOT EXISTS " + TABLENAME + "Idx on " + TABLENAME + " (" + COLUMN_CELLID + " , " + COLUMN_LAC + " , " + COLUMN_MNC + " , "
				+ COLUMN_MCC + " ) ");
		db.execSQL("CREATE INDEX IF NOT EXISTS " + TABLENAME + "Idx2 on " + TABLENAME + " (" + COLUMN_UPLOADED + " ) ");
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion)
	{
		FileLog.writeToLog(getClass().getName() + ":onUpgrade(): Upgrading db from version " + oldVersion + " to " + newVersion + ", deleting data");

		if (oldVersion == 3)
		{
			db.execSQL("CREATE INDEX IF NOT EXISTS " + TABLENAME + "Idx on " + TABLENAME + " (" + COLUMN_CELLID + " , " + COLUMN_LAC + " , " + COLUMN_MNC
					+ " , " + COLUMN_MCC + " ) ");
		} else
		{
			db.execSQL("DROP TABLE IF EXISTS " + TABLENAME);
			db.execSQL("DROP INDEX IF EXISTS " + TABLENAME + "Idx");
			onCreate(db);
		}
	}

	public void clearAllMeassurements()
	{
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("delete from " + TABLENAME);
		myTotalMeassurementCountCache = 0;
		myTotalMeassurementUploadedCountCache = 0;
		myCellIDCountCache = new HashSet<String>();
		myCellIDCountCacheToday = new HashSet<String>();
		myTodayMeassurementCountCache = -1;
		myTodayMeassurementCountCache_ts = -1;
		myNonUploadedCellIDCountCache = new HashSet<String>();
	}

	public int getAllMeassurementsCount()
	{
		if (myTotalMeassurementCountCache >= 0)
		{
			FileLog.writeToLog(getClass().getName() + "getAllMeassurementsCount(): getAllMeassurementsCount - using cache =" + myTotalMeassurementCountCache);

			// redundant sanity check
			if (myTodayMeassurementCountCache > myTotalMeassurementCountCache)
			{
				myTotalMeassurementCountCache = myTodayMeassurementCountCache;
			}
			return myTotalMeassurementCountCache;
		}
		FileLog.writeToLog(getClass().getName() + "getAllMeassurementsCount(): getAllMeassurementsCount - NOT USING CACHE");
		SQLiteDatabase db = getReadableDatabase();
		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(TABLENAME);
		Cursor c = query.query(db, new String[] { "COUNT(*) AS COUNT" }, null, null, null, null, null);
		int countcol = c.getColumnIndex("COUNT");
		while (c.moveToNext())
		{
			int count = c.getInt(countcol);
			c.close();
			myTotalMeassurementCountCache = count;
			return count;
		}
		c.close();
		return 0;
	}

	public int getAllMeassurementsAfterCount(final long aTimestamp)
	{
		if (myTodayMeassurementCountCache_ts == aTimestamp)
		{
			FileLog.writeToLog(getClass().getName() + "getAllMeassurementsAfterCount(): getAllMeassurementsAfterCount - using cache =" + myTodayMeassurementCountCache);
			return myTodayMeassurementCountCache;
		}
		FileLog.writeToLog(getClass().getName() + "getAllMeassurementsAfterCount(): getAllMeassurementsAfterCount - NOT USING CACHE");
		SQLiteDatabase db = getReadableDatabase();
		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(TABLENAME);
		query.appendWhere(COLUMN_TIMESTAMP + " > ?");
		Cursor c = query.query(db, new String[] { "COUNT(*) AS COUNT" }, null, new String[] { "" + aTimestamp }, null, null, null);
		int countcol = c.getColumnIndex("COUNT");
		while (c.moveToNext())
		{
			int count = c.getInt(countcol);
			myTodayMeassurementCountCache = count;
			myTodayMeassurementCountCache_ts = aTimestamp;
			c.close();
			return count;
		}
		c.close();
		return 0;
	}

	public int getAllMeassurementsUploadedCount()
	{
		if (myTotalMeassurementUploadedCountCache > -1)
		{
			FileLog.writeToLog(getClass().getName() + "getAllMeassurementsUploadedCount(): getAllMeassurementsUploadedCount - using cache ="
					+ myTotalMeassurementUploadedCountCache);
			return myTotalMeassurementUploadedCountCache;
		}
		FileLog.writeToLog(getClass().getName() + "getAllMeassurementsUploadedCount(): getAllMeassurementsUploadedCount - NOT USING CACHE");
		SQLiteDatabase db = getReadableDatabase();
		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(TABLENAME);
		query.appendWhere(COLUMN_UPLOADED + " > 0");
		Cursor c = query.query(db, new String[] { "COUNT(*) AS COUNT" }, null, null, null, null, null);
		int countcol = c.getColumnIndex("COUNT");
		while (c.moveToNext())
		{
			int count = c.getInt(countcol);
			c.close();
			myTotalMeassurementUploadedCountCache = count;
			return count;
		}
		c.close();
		return 0;
	}

	public int getAllCellsNonUploadedCount()
	{
		FileLog.writeToLog(getClass().getName() + "getAllCellsUploadedCount(): getAllCellsUploadedCount");
		if (myNonUploadedCellIDCountCache != null)
		{
			return myNonUploadedCellIDCountCache.size();
		}
		getAllCellsCount();
		return myNonUploadedCellIDCountCache.size();
	}

	public int getAllCellsCount()
	{
		if (myCellIDCountCache != null)
		{
			FileLog.writeToLog(getClass().getName() + "getAllCellsCount(): getAllCellsCount - using cache");

			// redundant sanity check
			if (myCellIDCountCacheToday != null && myCellIDCountCacheToday.size() > myCellIDCountCache.size())
			{
				myCellIDCountCache.addAll(myCellIDCountCacheToday);
			}
			return myCellIDCountCache.size();
		}
		FileLog.writeToLog(getClass().getName() + "getAllCellsCount(): getAllCellsCount - NOT USING CACHE");

		SQLiteDatabase db = getReadableDatabase();
		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(TABLENAME);
		Cursor c = query.query(db, new String[] { COLUMN_CELLID, COLUMN_LAC, COLUMN_MCC, COLUMN_MNC, "MIN(" + COLUMN_UPLOADED + ") AS " + COLUMN_UPLOADED },
				null, null, COLUMN_CELLID + ", " + COLUMN_LAC + ", " + COLUMN_MCC + ", " + COLUMN_MNC, null, null);
		try
		{
			int cidCol = c.getColumnIndex(COLUMN_CELLID);
			int lacCol = c.getColumnIndex(COLUMN_LAC);
			int mncCol = c.getColumnIndex(COLUMN_MCC);
			int mccCol = c.getColumnIndex(COLUMN_MCC);
			int uploadedCol = c.getColumnIndex(COLUMN_UPLOADED);
			myCellIDCountCache = new HashSet<String>();
			myNonUploadedCellIDCountCache = new HashSet<String>();
			while (c.moveToNext())
			{
				String fingerprint = c.getInt(cidCol) + "-" + c.getInt(mncCol) + "-" + c.getInt(mccCol) + "-" + c.getInt(lacCol);
				myCellIDCountCache.add(fingerprint);
				if (c.getInt(uploadedCol) < 1)
				{
					myNonUploadedCellIDCountCache.add(fingerprint);
				}
			}
		} finally
		{
			c.close();
		}
		return myCellIDCountCache.size();
	}

	public int getAllCellsAfterCount(final long aTimestamp)
	{
		if (myCellIDCountCacheToday_ts == aTimestamp)
		{
			FileLog.writeToLog(getClass().getName() + "getAllCellsAfterCount(): getAllCellsAfterCount - using cache");
			return myCellIDCountCacheToday.size();
		}
		FileLog.writeToLog(getClass().getName() + "getAllCellsAfterCount(): getAllCellsAfterCount - NOT USING CACHE");
		SQLiteDatabase db = getReadableDatabase();
		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(TABLENAME);
		query.appendWhere(COLUMN_TIMESTAMP + " > ?");
		Cursor c = query.query(db, new String[] { COLUMN_CELLID, COLUMN_LAC, COLUMN_MCC, COLUMN_MNC }, null, new String[] { "" + aTimestamp }, COLUMN_CELLID
				+ ", " + COLUMN_LAC + ", " + COLUMN_MCC + ", " + COLUMN_MNC, null, null);
		try
		{
			int cidCol = c.getColumnIndex(COLUMN_CELLID);
			int lacCol = c.getColumnIndex(COLUMN_LAC);
			int mncCol = c.getColumnIndex(COLUMN_MCC);
			int mccCol = c.getColumnIndex(COLUMN_MCC);
			myCellIDCountCacheToday = new HashSet<String>();
			while (c.moveToNext())
			{
				String fingerprint = c.getInt(cidCol) + "-" + c.getInt(mncCol) + "-" + c.getInt(mccCol) + "-" + c.getInt(lacCol);
				myCellIDCountCacheToday.add(fingerprint);
				myCellIDCountCacheToday_ts = aTimestamp;
			}
		} finally
		{
			c.close();
		}
		return myCellIDCountCacheToday.size();
	}

	/**
	 * @param aTrafficDialogActivity
	 */
	public void addListener(final CellDBListener aListener)
	{
		this.myListeners.add(aListener);
	}

	/**
	 * @param aTrafficDialogActivity
	 */
	public void removeListener(final CellDBListener aListener)
	{
		this.myListeners.remove(aListener);
	}
	
	/**
	 * @param aTrafficDialogActivity
	 */
	public void clearListeners()
	{
		this.myListeners.clear();
	}	

	/**
	 * @param aMeassurement
	 *            the meassurement to add
	 * @param storeMeassurement
	 *            if false, only the listeners are oinformed
	 */
	public void addMeassurement(final Meassurement aMeassurement, boolean storeMeassurement)
	{
		FileLog.writeToLog(getClass().getName() + "addMeassurement(): addMeassurement() " + myListeners.size() + " listeners");

		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLUMN_TIMESTAMP, aMeassurement.getTimestamp());
		values.put(COLUMN_CELLID, aMeassurement.getCellid());
		values.put(COLUMN_MNC, aMeassurement.getMnc());
		values.put(COLUMN_MCC, aMeassurement.getMcc());
		values.put(COLUMN_LAC, aMeassurement.getLac());
		values.put(COLUMN_LAT, aMeassurement.getLat());
		values.put(COLUMN_LON, aMeassurement.getLon());
		values.put(COLUMN_SPEED, aMeassurement.getSpeed());
		values.put(COLUMN_HEADING, aMeassurement.getBearing());
		values.put(COLUMN_RECEPTION, aMeassurement.getGsmSignalStrength());
		values.put(COLUMN_UPLOADED, aMeassurement.isUploaded() ? 1 : 0);

		if (!storeMeassurement)
		{
			FileLog.writeToLog(getClass().getName() + "addMeassurement(): collection disabled at the moment");
		} else
		{
			if (db.insert/* WithOnConflict */(TABLENAME, COLUMN_CELLID, values) == -1)
			{
				FileLog.writeToLog(getClass().getName() + ":addMeassurement: Error inserting cell");
			} else
			{
				if (myTotalMeassurementCountCache > -1)
				{
					myTotalMeassurementCountCache++;
				}
				if (myTodayMeassurementCountCache > -1)
				{
					myTodayMeassurementCountCache++;
				}
				String fingerprint = aMeassurement.getCellid() + "-" + aMeassurement.getMnc() + "-" + aMeassurement.getMcc() + "-" + aMeassurement.getLac();
				if (myCellIDCountCache != null)
				{
					myCellIDCountCache.add(fingerprint);
				}
				if (myCellIDCountCacheToday != null)
				{
					myCellIDCountCacheToday.add(fingerprint);
				}
				if (myNonUploadedCellIDCountCache != null)
				{
					myNonUploadedCellIDCountCache.add(fingerprint);
				}
			}
		}

		Set<CellDBListener> listeners = myListeners;
		this.myLastMeassurement = aMeassurement;
		for (CellDBListener trafficDBListener : listeners)
		{
			try
			{
				trafficDBListener.newMeassurement(aMeassurement);
			} catch (Exception e)
			{
				FileLog.writeExceptionToLog(e);
			}
		}
	}

	/**
	 * @return may be null
	 */
	public Meassurement getLastMeassurement()
	{
		FileLog.writeToLog(getClass().getName() + "getLastMeassurement(): getLastMeassurement()");
		if (myLastMeassurement == null)
		{
			try
			{
				SQLiteDatabase db = getReadableDatabase();
				SQLiteQueryBuilder query = new SQLiteQueryBuilder();
				query.setTables(TABLENAME);

				Cursor c = query.query(db, null, null, null, null, null, " " + COLUMN_TIMESTAMP + " DESC ", "1");
				int timecol = c.getColumnIndex(COLUMN_TIMESTAMP);
				int cellcol = c.getColumnIndex(COLUMN_CELLID);
				int lacCol = c.getColumnIndex(COLUMN_LAC);
				int mncCol = c.getColumnIndex(COLUMN_MNC);
				int mccCol = c.getColumnIndex(COLUMN_MCC);
				int signalCol = c.getColumnIndex(COLUMN_RECEPTION);
				int speedCol = c.getColumnIndex(COLUMN_SPEED);
				int headingCol = c.getColumnIndex(COLUMN_HEADING);
				int latCol = c.getColumnIndex(COLUMN_LAT);
				int lonCol = c.getColumnIndex(COLUMN_LON);
				int uplCol = c.getColumnIndex(COLUMN_UPLOADED);
				while (c.moveToNext())
				{
					Meassurement m = new Meassurement(c.getLong(timecol), c.getInt(cellcol), c.getInt(mccCol), c.getInt(mncCol), c.getInt(lacCol),
							c.getInt(signalCol), c.getDouble(latCol), c.getDouble(lonCol), c.getInt(speedCol), c.getInt(headingCol), "", c.getInt(uplCol) > 0);
					myLastMeassurement = m;
					break;
				}
				c.close();
			} catch (Exception e)
			{
				FileLog.writeExceptionToLog(e);
			}
		}
		return myLastMeassurement;
	}

	public void setAllUploaded()
	{
		FileLog.writeToLog(getClass().getName() + "setAllUploaded(): setAllUploaded()");
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("UPDATE " + TABLENAME + " SET " + COLUMN_UPLOADED + " = 1");
		myTotalMeassurementUploadedCountCache = myTotalMeassurementCountCache;
		myNonUploadedCellIDCountCache = new HashSet<String>();

		// just inform the listeners
		Set<CellDBListener> listeners = myListeners;
		for (CellDBListener trafficDBListener : listeners)
		{
			try
			{
				trafficDBListener.newMeassurement(myLastMeassurement);
			} catch (Exception e)
			{
				FileLog.writeExceptionToLog(e);
			}
		}
		for (CellDBListener trafficDBListener : listeners)
		{
			try
			{
				trafficDBListener.newMeassurement(myLastMeassurement);
			} catch (Exception e)
			{
				FileLog.writeExceptionToLog(e);
			}
		}
	}

	public boolean eraseUploadedMeasurements() {
	    SQLiteDatabase db = getWritableDatabase();
        int ret = db.delete(TABLENAME, COLUMN_UPLOADED + "=?", new String[] {"1"});
        FileLog.writeToLog(getClass().getName() + "eraseUploadedMeasurements(): ERASED " + ret + " uploaded measurements.");
        return (ret > 0) ? true : false;
	}
	
	public DBIterator<Meassurement> getNonUploadedMeassurements()
	{
		FileLog.writeToLog(getClass().getName() + "getAllMeassurements(): getAllMeassurements");
		LinkedList<Meassurement> retval = new LinkedList<Meassurement>();
		SQLiteDatabase db = getReadableDatabase();
		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(TABLENAME);

		query.appendWhere(COLUMN_UPLOADED + " < 1");

		final Cursor c = query.query(db, null, null, null, null, null, null);
		//
		// if (!c.moveToFirst()) {
		// return retval;
		// }
		final int timecol = c.getColumnIndex(COLUMN_TIMESTAMP);
		final int cellcol = c.getColumnIndex(COLUMN_CELLID);
		final int lacCol = c.getColumnIndex(COLUMN_LAC);
		final int mncCol = c.getColumnIndex(COLUMN_MNC);
		final int mccCol = c.getColumnIndex(COLUMN_MCC);
		final int signalCol = c.getColumnIndex(COLUMN_RECEPTION);
		final int speedCol = c.getColumnIndex(COLUMN_SPEED);
		final int headingCol = c.getColumnIndex(COLUMN_HEADING);
		final int latCol = c.getColumnIndex(COLUMN_LAT);
		final int lonCol = c.getColumnIndex(COLUMN_LON);
		final int uplCol = c.getColumnIndex(COLUMN_UPLOADED);

		return new DBIterator<Meassurement>()
		{

			public void close()
			{
				if (!c.isClosed())
				{
					c.close();
				}
			}

			@Override
			public boolean hasNext()
			{
				boolean last = c.isLast();
				if (last)
				{
					c.close();
				}
				return !last;
			}

			@Override
			public Meassurement next()
			{
				c.moveToNext();
				Meassurement m = new Meassurement(c.getLong(timecol), c.getInt(cellcol), c.getInt(mccCol), c.getInt(mncCol), c.getInt(lacCol),
						c.getInt(signalCol), c.getDouble(latCol), c.getDouble(lonCol), c.getInt(speedCol), c.getInt(headingCol), "", c.getInt(uplCol) > 0);
				return m;
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();

			}

			@Override
			public int getCount()
			{
				return c.getCount();
			}
		};
	}

	private int getFirstMeassurement_last_cellid = -1;
	private int getFirstMeassurement_last_lac = -1;
	private int getFirstMeassurement_last_mcc = -1;
	private int getFirstMeassurement_last_mnc = -1;
	private long getFirstMeassurement_last_result = -1;

	/**
	 * 
	 * @param cellid
	 * @param lac
	 * @param mcc
	 * @param mnc
	 * @param timestamp
	 * @return the first timestamp this cell was seen by this client
	 */
	public long getFirstMeassurement(int cellid, int lac, int mcc, int mnc, long timestamp)
	{
		if (getFirstMeassurement_last_cellid == cellid)
			if (getFirstMeassurement_last_lac == lac)
				if (getFirstMeassurement_last_mnc == mnc)
					if (getFirstMeassurement_last_mcc == mcc)
					{
						getFirstMeassurement_last_result = Math.min(getFirstMeassurement_last_result, timestamp);
						return getFirstMeassurement_last_result;
					}
		// FileLog.writeToLog.d(getClass().getName() + "getFirstMeassurement()",
		// "getFirstMeassurement(timestamp=" + timestamp + ")");
		SQLiteDatabase db = getReadableDatabase();
		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(TABLENAME);

		query.appendWhere(COLUMN_CELLID + " = " + cellid + " AND ");
		query.appendWhere(COLUMN_LAC + " = " + lac + " AND ");
		query.appendWhere(COLUMN_MNC + " = " + mnc + " AND ");
		query.appendWhere(COLUMN_MCC + " = " + mcc + " ");

		Cursor c = query.query(db, new String[] { COLUMN_TIMESTAMP }, null, null, null, null, null);

		int timecol = c.getColumnIndex(COLUMN_TIMESTAMP);
		long retval = timestamp;
		while (c.moveToNext())
		{
			retval = Math.min(retval, c.getLong(timecol));
			// FileLog.writeToLog.d(getClass().getName() + "getFirstMeassurement()",
			// "getFirstMeassurement(timestamp=" + timestamp + ") retval <- " +
			// retval);
		}
		c.close();
		// FileLog.writeToLog.d(getClass().getName() + "getFirstMeassurement()",
		// "getFirstMeassurement(timestamp=" + timestamp + ") retval = " +
		// retval);

		// cache in case the same cell is asked twice
		getFirstMeassurement_last_cellid = cellid;
		getFirstMeassurement_last_lac = lac;
		getFirstMeassurement_last_mnc = mnc;
		getFirstMeassurement_last_mcc = mcc;
		getFirstMeassurement_last_result = retval;
		return retval;
	}

}
