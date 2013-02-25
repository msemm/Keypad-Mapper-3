/**
 * created: 11.06.2010 07:29:17
 */
package de.enaikoon.android.inviu.opencellidlibrary;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

/**
 * This service collects cellids of the Android-device in the background.
 * 
 * @author Marcus Wolschon (Marcus@Wolschon.biz)
 */
public class CellIDCollectionService extends Service
{
	private static boolean isonLocationChangedThreadRunning = false;
	private static boolean isonGPSStatusChangedThreadRunning = false;
	public static boolean isServiceStarted = false;

	public interface GPSListener
	{
		public void gpsChanged(final GpsStatus status, final Location location);
		public void checkGPSState();
	}

	/**
	 * A GPS-signal with less then this many satellites is considered invalid
	 * and handled like not having a gps-fix.
	 */
	public static final int MINSATCOUNT = 3;

	/**
	 * Name of our power-management lock.
	 */
	public static final String POWERLOCKNAME = "WakeLock.Local";

	/**
	 * power-lock to prevent suspension of the device.
	 */
	private WakeLock myPowerLock;

	private HandlerThread myLooper;
	/**
	 * Handler for {@link #myLooper}.
	 */
	private Handler myHandler;

	private static SignalStrength myLastSignalStrength = null;

	NotificationManager mNM;

	/**
	 * @return the myLastSignalStrength
	 */
	public static SignalStrength getMyLastSignalStrength()
	{
		return myLastSignalStrength;
	}

	/**
	 * The database we store counted traffic in.
	 */
	private static Database myDatabase;

	private TelephonyManager myTelephonyManager = null;

	public static LocationManager myLocationManager;
	
	public static RefreshHandlerTurnGPSOff rhTurnGPSOff = new RefreshHandlerTurnGPSOff();

	/**
	 * @param application
	 * @return The database we store counted traffic in.
	 */
	public static Database getDatabase(final Context context)
	{
		if (myDatabase == null)
		{
			myDatabase = new Database(context);
		}
		return myDatabase;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate()
	{
		FileLog.writeToLog(getClass().getName() + "onCreate(): onCreate");
		super.onCreate();

		// preferences
		// SharedPreferences sharedPreferences =
		// getSharedPreferences(SHAREDPREFRENCESKEY, MODE_PRIVATE);
		// boolean autocollect =
		// sharedPreferences.getBoolean(getString(R.string.prefkey_autocollect),
		// true);
		// Editor edit = sharedPreferences.edit();
		// edit.putBoolean(getString(R.string.prefkey_collecting), autocollect);
		// edit.commit();

		// initialize fields
		myDatabase = getDatabase(this);
		if (myLastMeassurement == null)
		{
			myLastMeassurement = myDatabase.getLastMeassurement();
		}
		myTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// // obtain power-management-lock
		PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
		try
		{
			myPowerLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, POWERLOCKNAME);
			myPowerLock.setReferenceCounted(true);
		} catch (Exception e)
		{
			FileLog.writeExceptionToLog(e);
		}
		
		isServiceStarted=true;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (myLocationListener==null)
		{
			myLocationListener = new LocationListener()
			{

				@Override
				public void onStatusChanged(final String provider, final int status, final Bundle extras)
				{
					// ignored
				}

				@Override
				public void onProviderEnabled(final String provider)
				{
					for (final GPSListener listener : myListeners)
					{
						listener.checkGPSState();
					}
				}

				@Override
				public void onProviderDisabled(final String provider)
				{
					for (final GPSListener listener : myListeners)
					{
						listener.checkGPSState();
					}
				}

				@Override
				public void onLocationChanged(final Location location)
				{
					FileLog.writeToLog(getClass().getName() + "onLocationChanged(): isonLocationChangedThreadRunning=" + isonLocationChangedThreadRunning);
					
					if (!isonLocationChangedThreadRunning)
					{
						Thread t1 = new Thread(new Runnable()
						{

							@Override
							public void run()
							{
								isonLocationChangedThreadRunning = true;
								
								FileLog.writeToLog(getClass().getName() + "onLocationChanged(): onLocationChanged");
								mylastLocation = location;
								try
								{
									getHandler().post(new MyUpdater(location));
								} catch (Exception e)
								{
									FileLog.writeExceptionToLog(e);
								}
								try
								{

									for (final GPSListener listener : myListeners)
									{
										// inform our listeners asynchronously to
										// not delay the gps-thread
										Runnable r = new Runnable()
										{
											public void run()
											{

												listener.gpsChanged(myLastGPSStatus, mylastLocation);
											}
										};
										getHandler().post(r);
									}
								} catch (Exception e)
								{
									FileLog.writeExceptionToLog(e);
								}
								isonLocationChangedThreadRunning = false;
							}
						});

						t1.setPriority(Thread.MIN_PRIORITY);
						t1.start();
					}
				}
			};
			
			FileLog.writeToLog(getClass().getName() + ":GPSManager: registering with location-provider \"" + "gps" + "\"");
			//myLocationManager.requestLocationUpdates("gps", 0, 0, myLocationListener);			
		}

		Location lastKnownLocation = myLocationManager.getLastKnownLocation("gps");
		if (lastKnownLocation != null)
		{
			myLocationListener.onLocationChanged(lastKnownLocation);
		}
		
		if (myGPSStatusListener==null)
		{
			myGPSStatusListener = new Listener()
			{

				@Override
				public void onGpsStatusChanged(int event)
				{
					if (!isonGPSStatusChangedThreadRunning)
					{
						Thread t1 = new Thread(new Runnable()
						{

							@Override
							public void run()
							{
								isonGPSStatusChangedThreadRunning = true;
								try
								{
									if (myLocationManager==null)
									{
										myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
									}
									myLastGPSStatus = myLocationManager.getGpsStatus(myLastGPSStatus);

									for (final GPSListener listener : myListeners)
									{
										// inform our listeners asynchronously to
										// not delay the gps-thread
										Runnable r = new Runnable()
										{
											public void run()
											{
												listener.gpsChanged(myLastGPSStatus, mylastLocation);
											}
										};
										getHandler().post(r);
									}
								} catch (Exception e)
								{
									FileLog.writeExceptionToLog(e);
								}
								isonGPSStatusChangedThreadRunning = false;
							}
						});

						t1.setPriority(Thread.MIN_PRIORITY);
						t1.start();
					}
				}
			};
			myLocationManager.addGpsStatusListener(myGPSStatusListener);	
		}

		if (myPhoneStateListener != null)
		{
			myTelephonyManager.listen(myPhoneStateListener, 0);
		}
		myTelephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

		FileLog.writeToLog(getClass().getName() + "onStart(): onStart DONE");
		
		return START_STICKY;
	}	

	// /**
	// * Registers a periodic alarm.
	// */
	// private void registerAlarm() {
	// mAlarm.registerAlarm();
	// }

	/**
	 * 
	 */
	private Handler getHandler()
	{
		if (myLooper == null)
		{
			myLooper = new HandlerThread("CellIDCollectorService Handler");
			myLooper.setPriority(Thread.MIN_PRIORITY);
			myLooper.start();
		}
		if (myHandler == null)
		{
			myHandler = new Handler(myLooper.getLooper());
		}
		return myHandler;
	}

	/**
	 * remove #myUpdater from {@link #myHandler}.
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy()
	{
		try
		{
			FileLog.writeToLog("onDestroy started!");
			
			if (myPhoneStateListener != null)
			{
				FileLog.writeToLog(getClass().getName() + "onDestroy(): myPhoneStateListener removed from listener");
				myTelephonyManager.listen(myPhoneStateListener, 0);
			}
			if (myGPSStatusListener != null)
			{
				FileLog.writeToLog(getClass().getName() + "onDestroy(): myGPSStatusListener removed from listener");
				myLocationManager.removeGpsStatusListener(myGPSStatusListener);
				// myGPSStatusListener = null;
			}
			if (myLocationListener != null)
			{
				FileLog.writeToLog(getClass().getName() + "onDestroy(): myLocationListener removed from listener");
				myLocationManager.removeUpdates(myLocationListener);
				FileLog.writeToLog("myLocationListener updates removed!");
				// myLocationListener = null;
			}

			myLocationManager = null;
		} catch (Exception e1)
		{
			FileLog.writeExceptionToLog(e1);
		}
		if (myLooper != null)
		{
			myLooper.quit();
			myLooper = null;
		}

		if (myHandler != null)
		{
			myHandler = null;
		}

		if (myPowerLock != null)
		{
			try
			{
				myPowerLock.release();
			} catch (Exception e)
			{
				FileLog.writeExceptionToLog(e);
			}
		}
		
		isServiceStarted=false;		
		
		super.onDestroy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(final Intent anintent)
	{
		return null;
	}

	/**
	 * Runner only to be used inside {@link #myHandler} and {@link #myLooper}.
	 */
	private class MyUpdater implements Runnable
	{

		private CellLocation cellLocation;
		private String mccmnc;
		private Location myLocation;
		private long myTimestamp;

		public MyUpdater(final CellLocation cellLocation)
		{
			this.cellLocation = cellLocation;
			this.mccmnc = myTelephonyManager.getNetworkOperator();
			this.myLocation = mylastLocation;
			myTimestamp = System.currentTimeMillis();
		}

		public MyUpdater(final Location myNewLocation)
		{
			this.cellLocation = myTelephonyManager.getCellLocation();
			this.mccmnc = myTelephonyManager.getNetworkOperator();
			this.myLocation = myNewLocation;
			myTimestamp = System.currentTimeMillis();
		}

		public void run()
		{
			try
			{
				updateCellLocation(cellLocation, myLocation, mccmnc, myTimestamp);
			} catch (Exception e)
			{
				FileLog.writeExceptionToLog(e);
			} finally
			{
				// Releases the local wake lock.
				// try {
				// if (myPowerLock != null && myPowerLock.isHeld()) {
				// myPowerLock.release();
				// }
				// } catch (Exception e) {
				// android.util.Log.e(getClass().getName() + ":run",
				// "cannot release WakeLock myPowerLock", e);
				// }
			}
		}
	};

	/**
	 * @return the mylastLocation
	 */
	public static Location getLastLocation()
	{
		return mylastLocation;
	}

	/**
	 * @return the mylastLocation
	 */
	public static GpsStatus getLastGpsStatus()
	{
		return myLastGPSStatus;
	}

	private static Location mylastLocation;
	private static GpsStatus myLastGPSStatus;

	private final PhoneStateListener myPhoneStateListener = new PhoneStateListener()
	{

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.telephony.PhoneStateListener#onSignalStrengthsChanged(android
		 * .telephony.SignalStrength)
		 */
		@Override
		public void onSignalStrengthsChanged(final SignalStrength aSignalStrength)
		{
			myLastSignalStrength = aSignalStrength;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.telephony.PhoneStateListener#onCellLocationChanged(android
		 * .telephony.CellLocation)
		 */
		@Override
		public void onCellLocationChanged(final CellLocation aLocation)
		{
			FileLog.writeToLog(getClass().getName() + "onCellLocationChanged(): onCellLocationChanged");
			try
			{
				try
				{
					myLocationManager.removeUpdates(myLocationListener);	
				}catch(Exception ex)
				{
					FileLog.writeExceptionToLog(ex);
				}
				
				myLocationManager.requestLocationUpdates("gps", 0, 0, myLocationListener); 
				
				if (Configurator.getGpsTimeout()>0)
				{
					rhTurnGPSOff.sleep(Configurator.getGpsTimeout());	
				}				
				
				//getHandler().post(new MyUpdater(aLocation));
			} catch (Exception e)
			{
				FileLog.writeExceptionToLog(e);
			}
		}

	};

	private static Meassurement myLastMeassurement;
	private static Location myLastMeassurementLocation;

	// /**
	// * Get all interfaces and read their traffic-count.
	// */
	// private void updateCellData() {
	// CellLocation cellLocation = myTelephonyManager.getCellLocation();
	// String mccmnc = myTelephonyManager.getNetworkOperator();
	// updateCellLocation(cellLocation, mylastLocation, mccmnc,
	// System.currentTimeMillis());
	// }

	/**
	 * ONLY to be called from inside {@link MyUpdater} in {@link #myHandler} and
	 * {@link #myLooper}..
	 */
	private void updateCellLocation(final CellLocation cellLocation, final Location lastLocation, final String mccmnc, final long timeStamp)
	{
		FileLog.writeToLog(getClass().getName() + "updateCellLocation(): entering");
		try
		{
			if (Configurator.isDatabaseValid())
			{
				if (myLocationManager == null)
				{
					return;
				}

				GpsStatus lastGpsStatus = myLocationManager.getGpsStatus(null);
				if (lastGpsStatus == null || lastLocation == null)
				{
					FileLog.writeToLog(getClass().getName() + "updateCellLocation(): no last GPS");
					return;
				}
				Iterable<GpsSatellite> satellites = lastGpsStatus.getSatellites();
				int satCount = 0;
				for (GpsSatellite gpsSatellite : satellites)
				{
					if (gpsSatellite.usedInFix())
					{
						satCount++;
					}
				}
				
				FileLog.writeToLog("updateCellLocation(): satCount < MINSATCOUNT = " + ((satCount < MINSATCOUNT)?"true":"false"));
				
				if (satCount < MINSATCOUNT)
				{
					FileLog.writeToLog(getClass().getName() + "updateCellLocation(): <3 GPS satelites used");
					return;
				}

				if (cellLocation instanceof GsmCellLocation && myLastSignalStrength != null && myLastSignalStrength.isGsm()
						&& myTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM)
				{
					FileLog.writeToLog("updateCellLocation() IF1");
					
					GsmCellLocation gsmcell = (GsmCellLocation) cellLocation;
					int cellid = gsmcell.getCid();
					if (mccmnc == null || mccmnc.length() < 4)
					{
						FileLog.writeToLog("updateCellLocation(): (mccmnc == null || mccmnc.length() < 4) = " + (mccmnc == null || mccmnc.length() < 4));
						FileLog.writeToLog(getClass().getName() + ":updateCellLocation(): no current NetworkOperator");
						return;
					}
					int mcc = Integer.parseInt(mccmnc.substring(0, 3));
					int mnc = Integer.parseInt(mccmnc.substring(3));
					int lac = gsmcell.getLac();
					int gsmSignalStrength = myLastSignalStrength.getGsmSignalStrength();
					String network = myTelephonyManager.getNetworkOperatorName();
					if (network == null || network.trim().length() == 0)
					{
						network = myTelephonyManager.getSimOperatorName();
					}

					if (lastLocation.getLatitude()==0 && lastLocation.getLongitude()==0)
					{
						FileLog.writeToLog("updateCellLocation(): lastLocation lat, lng = 0");						
						return;
					}
					
					FileLog.writeToLog("updateCellLocation(): new measurement: " + timeStamp + " | " + cellid + " | " + mcc + " | " + mnc + " | " + lac + " | " + gsmSignalStrength + " | " + lastLocation.getLatitude() + " | " + lastLocation.getLongitude() + " | " + ((!lastLocation.hasSpeed()) ? 0.0f : lastLocation.getSpeed()) + " | " + ((!lastLocation.hasBearing()) ? 0.0f : lastLocation.getBearing()) + " | " + network + " | " + false);
					
					Meassurement saveme = new Meassurement(timeStamp, cellid, mcc, mnc, lac, gsmSignalStrength, lastLocation.getLatitude(),
							lastLocation.getLongitude(), ((!lastLocation.hasSpeed()) ? 0.0f : lastLocation.getSpeed()), // 0.0
																														// if
																														// no
																														// speed
							((!lastLocation.hasBearing()) ? 0.0f : lastLocation.getBearing()), // 0.0
																								// if
																								// no
																								// bearing
							network, false);

					if (myLastMeassurement != null)
					{
						FileLog.writeToLog("updateCellLocation(): (myLastMeassurement != null)");
						
						// if the cell changed, always include the meassurement
						if (myLastMeassurement.getCellid() == cellid && myLastMeassurement.getMnc() == mnc && myLastMeassurement.getMcc() == mcc)
						{
							// if the signal-strength for the same cell is off
							// by at least this much, include the meassrement
							int limit = Configurator.getMinSignalLevelDifference();
							boolean sameSignalLevel = Math.abs(myLastMeassurement.getGsmSignalStrength() - gsmSignalStrength) < limit;

							double dlimit = Configurator.getMinDistance();
							boolean sameLocation = myLastMeassurementLocation != null && lastLocation.distanceTo(myLastMeassurementLocation) < dlimit;

							limit = Configurator.getMinTimestampDifference();
							boolean sameTime = (saveme.getTimestamp() - myLastMeassurement.getTimestamp()) < limit;

							if (sameSignalLevel)
							{
								if (sameLocation)
								{
									FileLog.writeToLog(getClass().getName() + ":updateCellLocation(): ignoring measurement as it is nearly identical to the last one");
									
									return;
								}
							}
							if (sameTime)
							{
								FileLog.writeToLog(getClass().getName() + ":updateCellLocation(): ignoring measurement as it "
										+ (saveme.getTimestamp() - myLastMeassurement.getTimestamp()) + " = less then " + limit + " miliseconds ago");
								
								return;
							}
						}
					}
					getDatabase(getApplication()).addMeassurement(saveme, true);
					myLastMeassurement = saveme;
					myLastMeassurementLocation = lastLocation;

					FileLog.writeToLog(getClass().getName() + ":updateCellLocation(): start uploader!");
					
					try
					{
						if (Configurator.isAutomaticUpload())
						{
							Uploader uploader = new Uploader(getDatabase(getApplication()), this, true);
							(new Thread(uploader, "AutoUploader")).start();	
						}	
					}catch(Exception ex)
					{
						FileLog.writeExceptionToLog(ex);
					}
				} else
				{
					FileLog.writeToLog(getClass().getName() + ":updateCellLocation(): cellLocation is not GSM");
				}
				if (cellLocation instanceof CdmaCellLocation)
				{
					// future expansion
				}
			}
		} catch (Exception e)
		{
			FileLog.writeExceptionToLog(e);
		}
	}

	private static Set<GPSListener> myListeners = new HashSet<GPSListener>();

	public static LocationListener myLocationListener;

	private Listener myGPSStatusListener;

	public static void addLocationListener(GPSListener cellIDDialogActivity)
	{
		myListeners.add(cellIDDialogActivity);
	}

	public static void removeLocationListener(GPSListener cellIDDialogActivity)
	{
		myListeners.remove(cellIDDialogActivity);
	}
	
	public static void clearLocationListeners()
	{
		myListeners.clear();
	}
	
	public static class RefreshHandlerTurnGPSOff extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			if (myLocationListener != null)
			{
				myLocationManager.removeUpdates(myLocationListener);
				// myLocationListener = null;
			}
		}

		public void sleep(long delayMillis)
		{
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	};		
}
