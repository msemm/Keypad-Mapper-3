/**
 * created: 18.06.2010 09:29:11
 */
package de.enaikoon.android.inviu.opencellidlibrary;


/**
 * A CellID+Location -pair.
 * 
 * @author Marcus Wolschon (Marcus@Wolschon.biz)
 */
public class Meassurement
{

	private boolean uploaded;

	/**
	 * @param aTimestamp
	 * @param aCellid
	 * @param aMcc
	 * @param aMnc
	 * @param aLac
	 * @param aGsmSignalStrength
	 * @param aLat
	 * @param aLon
	 * @param aSpeed
	 * @param aBearing
	 * @param aNetwork
	 *            may be null
	 */
	public Meassurement(long aTimestamp, int aCellid, int aMcc, int aMnc, int aLac, int aGsmSignalStrength, double aLat, double aLon, float aSpeed,
			float aBearing, final String aNetwork, final boolean isUploaded)
	{
		super();
		this.timestamp = aTimestamp;
		this.cellid = aCellid;
		this.mcc = aMcc;
		this.mnc = aMnc;
		this.lac = aLac;
		this.gsmSignalStrength = aGsmSignalStrength;
		this.lat = aLat;
		this.lon = aLon;
		this.speed = aSpeed;
		this.bearing = aBearing;
		this.net = aNetwork;
		this.uploaded = isUploaded;
	}

	/**
	 * @return the uploaded
	 */
	public boolean isUploaded()
	{
		return uploaded;
	}

	/**
	 * @param uploaded
	 *            the uploaded to set
	 */
	public void setUploaded(boolean uploaded)
	{
		this.uploaded = uploaded;
	}

	/**
	 * @return the timestamp
	 */
	public long getTimestamp()
	{
		return this.timestamp;
	}

	/**
	 * @param aTimestamp
	 *            the timestamp to set
	 */
	public void setTimestamp(long aTimestamp)
	{
		this.timestamp = aTimestamp;
	}

	/**
	 * @return the cellid
	 */
	public int getCellid()
	{
		return this.cellid;
	}

	/**
	 * @param aCellid
	 *            the cellid to set
	 */
	public void setCellid(int aCellid)
	{
		this.cellid = aCellid;
	}

	/**
	 * @return the mcc
	 */
	public int getMcc()
	{
		return this.mcc;
	}

	/**
	 * @param aMcc
	 *            the mcc to set
	 */
	public void setMcc(int aMcc)
	{
		this.mcc = aMcc;
	}

	/**
	 * @return the mnc
	 */
	public int getMnc()
	{
		return this.mnc;
	}

	/**
	 * @param aMnc
	 *            the mnc to set
	 */
	public void setMnc(int aMnc)
	{
		this.mnc = aMnc;
	}

	/**
	 * @return the lac
	 */
	public int getLac()
	{
		return this.lac;
	}

	/**
	 * @param aLac
	 *            the lac to set
	 */
	public void setLac(int aLac)
	{
		this.lac = aLac;
	}

	/**
	 * Get the GSM Signal Strength, valid values are (0-31, 99) as defined in TS
	 * 27.007 8.5 (http://www.xs4all.nl/~m10/mac/downloads/3GPP-27007-630.pdf) 0
	 * -113 dBm or less 1 -111 dBm 2...30 -109... -53 dBm 31 -51 dBm or greater
	 * 99 not known or not detectable (Windows Mobile would have
	 * LINEDEVSTATUS.dwSignalLevel in the range 0x00000000 (weakest signal) to
	 * 0x0000FFFF (strongest signal).)
	 * 
	 * @return the gsmSignalStrength
	 */
	public int getGsmSignalStrength()
	{
		return this.gsmSignalStrength;
	}

	public static int gsmSignalStrengthTodBm(final int signal)
	{
		FileLog.writeToLog(Meassurement.class.getName() + ":gsmSignalStrengthTodBm(): gsmSignalStrengthTodBm(" + signal + ")");
		if (signal < 0)
		{
			FileLog.writeToLog(Meassurement.class.getName() + ":gsmSignalStrengthTodBm(): gsmSignalStrengthTodBm(" + signal + ") invalid input-range (too low)");
			return -128;
			// return 0;
		}
		if (signal > 31 && signal != 99)
		{
			FileLog.writeToLog(Meassurement.class.getName() + ":gsmSignalStrengthTodBm(): gsmSignalStrengthTodBm(" + signal + ") invalid input-range (too high)");
			// return 0;
		}
		if (signal == 0)
		{
			return -113;
		}
		if (signal == 1)
		{
			return -111;
		}
		if (signal == 99)
		{
			return 0;
		}
		if (signal >= 32)
		{
			return -51;
		}
		return -109 - (signal - 2) * ((53 - 109) / (30 - 2));

	}

	public int getGsmSignalStrengthIndBm()
	{
		return gsmSignalStrengthTodBm(getGsmSignalStrength());
	}

	/**
	 * @param aGsmSignalStrength
	 *            the gsmSignalStrength to set
	 */
	public void setGsmSignalStrength(int aGsmSignalStrength)
	{
		this.gsmSignalStrength = aGsmSignalStrength;
	}

	/**
	 * @return the lat
	 */
	public double getLat()
	{
		return this.lat;
	}

	/**
	 * @param aLat
	 *            the lat to set
	 */
	public void setLat(double aLat)
	{
		this.lat = aLat;
	}

	/**
	 * @return the lon
	 */
	public double getLon()
	{
		return this.lon;
	}

	/**
	 * @param aLon
	 *            the lon to set
	 */
	public void setLon(double aLon)
	{
		this.lon = aLon;
	}

	/**
	 * @return the speed
	 */
	public float getSpeed()
	{
		return this.speed;
	}

	/**
	 * @param aSpeed
	 *            the speed to set
	 */
	public void setSpeed(float aSpeed)
	{
		this.speed = aSpeed;
	}

	/**
	 * @return the bearing
	 */
	public float getBearing()
	{
		return this.bearing;
	}

	/**
	 * @param aBearing
	 *            the bearing to set
	 */
	public void setBearing(float aBearing)
	{
		this.bearing = aBearing;
	}

	private long timestamp;
	private int cellid;
	private int mcc;
	private int mnc;
	private int lac;
	private int gsmSignalStrength;
	private double lat;
	private double lon;
	private float speed;
	private float bearing;
	/**
	 * may be null.
	 */
	private String net;

	/**
	 * @return may be null
	 */
	public String getNet()
	{
		return net;
	}

	/**
	 * @param net
	 *            may be null
	 */
	public void setNet(String net)
	{
		this.net = net;
	}
}
