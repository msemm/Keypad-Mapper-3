package de.enaikoon.android.keypadmapper3.utils;

import java.util.Date;

import android.location.Location;
import android.util.Log;

public class GPSDataValidator {
	/**
	 * used for speed and distance checks between two points modified from external method,
	 * usually from place where the location point is recorded into the database
	 */
	private static Location lastRecodedLocation;

	// represents a difference between gps time and current phone's time
	private static long timeDifference = 0;

	// used for counting a number of valid location points which will be marked
	// as invalid
	private static int omitValidGPSData = 3;

	/**
	 * checks the GPS accuracy
	 * 
	 * @return true if the accuracy is smaller than 40 meters, false otherwise
	 */
	private static boolean checkAccuracy(Location location)	{
	    if (location.getAccuracy() < 40) {
	        return true;
	    } else {
	        return false;
	    }
	}

	/**
	 * checks a valid altitude
	 * 
	 * @return true if the altitude is valid, false otherwise
	 */
	private static Boolean checkAltitude(Location location)	{
		if (location.getAltitude() > 15000 || location.getAltitude() < -500) {
			Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAltitude() = false | " + location.getAltitude());
			return false;
		} else if (location.getAltitude() == 0)	{
			Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAltitude() = false | " + location.getAltitude());
			return false;
		}

		if (lastRecodedLocation == null) {
			Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAltitude() = true | lastRecodedLocation==null");
			return true;
		} else if (location.getTime() == lastRecodedLocation.getTime())	{
			return true;
		} else if (location.getLatitude() == 0 && location.getLongitude() == 0)	{
			Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAltitude() = false | lat=0 lng=0");
			return false;
		} else {
			float distance = (float) (location.getAltitude() - lastRecodedLocation.getAltitude());

			float delta_time = (location.getTime() - lastRecodedLocation.getTime()) / 1000;
			
			if (delta_time == 0) {
				Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAltitude() = true | delta_time: " + delta_time + " ... distance: " + distance);
				return true;
			}		
			
			float speedMS = distance / delta_time;

			if (Math.abs(speedMS) > 100)
			{
				Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAltitude() = false | " + speedMS + " | " + distance);

				return false;
			} else
			{
				Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAltitude() = true | " + speedMS + " | " + distance);

				return true;
			}			
		}
	}

	/**
	 * checks the values retrieved from NMEA parser
	 */
	private static boolean checkDOPValues(double HDOP, double VDOP)
	{
		return (HDOP < 5 && VDOP < 5 && (HDOP + VDOP) < 8);
	}

	/**
	 * checks valid distance between two points
	 * 
	 * @param Location
	 *            object which should be compared with lastRecodedLocation
	 *            object
	 * @return true if the distance is greater than 5m, false otherwise
	 */
	public static Boolean checkValidDistance(Location location)
	{
		try
		{
			if (lastRecodedLocation == null)
			{
				Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkValidDistance() = true | lastRecodedLocation==null");

				return true;
			} else if (location.distanceTo(lastRecodedLocation) < 5)
			{
				Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkValidDistance() = false | " + location.distanceTo(lastRecodedLocation));
				return false;
			} else
			{
				Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkValidDistance() = true | " + location.distanceTo(lastRecodedLocation));

				return true;
			}
		} catch (Exception ex)
		{
			Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkValidDistance() = false | error");
			//AppMain.writeExceptionToLog(ex);
			return false;
		}
	}

	/**
	 * checks the acceleration between two location objects
	 * 
	 * @param location
	 * @return true if the acceleration is smaller than 3m2/s, false otherwise
	 */
	private static Boolean checkAcceleration(Location location)
	{
		try
		{
			if (lastRecodedLocation == null)
			{
				Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAcceleration() = true | lastRecodedLocation==null");

				return true;
			} else if (location.getTime() == lastRecodedLocation.getTime())
			{
				return true;
			} else if (location.getLatitude() == 0 && location.getLongitude() == 0)
			{
				Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAcceleration() = false | lat=0 lng=0");
				return false;
			} else
			{
				if (location.getTime() < lastRecodedLocation.getTime())
				{
					Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAcceleration() = false | " + location.getTime() + " < "
							+ lastRecodedLocation.getTime());
					return false;
				}

				String logvalue = "\n";
				logvalue += "	OLD location data:\n";
				logvalue += "		Time: " + lastRecodedLocation.getTime() + "\n";
				logvalue += "		Lat: " + lastRecodedLocation.getLatitude() + "\n";
				logvalue += "		Lng: " + lastRecodedLocation.getLongitude() + "\n";
				logvalue += "		Speed: " + (lastRecodedLocation.getSpeed() * 60 * 60 / 1000) + "\n";
				logvalue += "	NEW location data:\n";
				logvalue += "		Time: " + location.getTime() + "\n";
				logvalue += "		Lat: " + location.getLatitude() + "\n";
				logvalue += "		Lng: " + location.getLongitude() + "\n";
				logvalue += "		Speed: " + (location.getSpeed() * 60 * 60 / 1000) + "\n";
				logvalue += "	RESULTS:\n";

				float delta_time = (location.getTime() - lastRecodedLocation.getTime()) / 1000;
				logvalue += "		delta_time: " + delta_time + "\n";
				float distance = (int) (location.distanceTo(lastRecodedLocation));
				logvalue += "		distance: " + distance + "\n";

				if (delta_time == 0)
				{
					Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAcceleration() = true | delta_time: " + delta_time + " ... distance: " + distance + logvalue);

					return true;
				}

				float average_speed = distance / delta_time;
				logvalue += "		average_speed: " + average_speed + "\n";
				float delta_speed = average_speed - lastRecodedLocation.getSpeed();
				logvalue += "		delta_speed: " + delta_speed + "\n";
				float acceleration = delta_speed / delta_time;
				logvalue += "		acceleration: " + acceleration + "\n";

				if (Math.abs(acceleration) > 3)
				{
					Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAcceleration() = false | " + acceleration + logvalue);
					
					return false;
				} else
				{
					Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAcceleration() = true | " + acceleration + logvalue);

					return true;
				}
			}
		} catch (Exception ex)
		{
			Log.d(GPSDataValidator.class.getPackage().getName(), "Method: " + GPSDataValidator.class.getPackage().getName() + ".checkAcceleration() = false | error");
			//AppMain.writeExceptionToLog(ex);
			return false;
		}
	}

	/**
	 * checks the number of used satellites
	 * 
	 * @param numberOfUsedSatellites
	 * @return true if the number of used satellites is greater than 3, false
	 *         otherwise
	 */
	private static boolean gpsFixCheck(int numberOfUsedSatellites)
	{
		return (numberOfUsedSatellites >= 3) ? true : false;
	}

	/**
	 * checks if the time of new location is greater than the time of last valid
	 * location and calculates the time difference between valid GPS time and
	 * current phone's time
	 * 
	 * @param lastValidLocation
	 * @param newLocation
	 * @return true if the time of new location is greater than the time of last
	 *         valid location, false otherwise
	 */
	private static boolean gpsTimeCheck(Location lastValidLocation, Location newLocation)
	{
		if (lastValidLocation == null || newLocation == null)
		{
			return true;
		} else
		{
			boolean result = (newLocation.getTime() > lastValidLocation.getTime());

			if (result)
			{
				timeDifference = (newLocation.getTime() - new Date().getTime()) / 1000;
			}

			return result;
		}
	}

	/**
	 * checks if the position of the Location object is 0,0
	 * 
	 * @param newLocation
	 * @return true if the latitude or logitude value is different than 0, false
	 *         otherwise
	 */
	private static boolean latlngCheck(Location newLocation)
	{
		if (newLocation.getLatitude() != 0 || newLocation.getLongitude() != 0)
		{
			return true;
		} else
		{
			Log.d(GPSDataValidator.class.getPackage().getName(), "LatLng check invalid: LAT=0; LNG=0");
			return false;
		}
	}

	/**
	 * omits the X of valid locations after the retrieval of one invalid
	 * location
	 * 
	 * @return true if the location is omitted, false otherwise
	 */
	private static boolean omitValidGPSData()
	{
		if (omitValidGPSData > 0)
		{
			//GPSDataLog.writeToLog(AppMain.location, false);

			Log.d(GPSDataValidator.class.getPackage().getName(), "GPSDataValidator() LOCATION OMITTED! " + omitValidGPSData);

			omitValidGPSData--;

			return true;
		} else
		{
			return false;
		}
	}

	/**
	 * initiate GPS data checks
	 * 
	 * @param lastValidLocation
	 *            - represents a last retrieved location which is valid
	 * @param newLocation
	 *            - new location which should be checked
	 * @param fix
	 *            - the number of used satellites
	 * @param HDOP
	 *            - HDOP value from NMEA parser
	 * @param VDOP
	 *            - VDOP value from NMEA parser
	 * @return true if all checks are valid, false otherwise
	 */
	public static boolean validateGPSData(Location lastValidLocation, Location newLocation, int fix, double HDOP, double VDOP)
	{
		if (latlngCheck(newLocation))
		{
			if (gpsTimeCheck(lastValidLocation, newLocation))
			{
				if (gpsFixCheck(fix))
				{
					if (checkAcceleration(newLocation))
					{
						if (checkAltitude(newLocation))
						{
							if (checkAccuracy(newLocation))
							{
								Log.d(GPSDataValidator.class.getPackage().getName(), "Accuracy check valid: " + newLocation.getAccuracy());
								
								if (checkDOPValues(HDOP, VDOP))
								{
									Log.d(GPSDataValidator.class.getPackage().getName(), "DOP check valid: HDOP: " + NMEAHelper.HDOP + " | VDOP: " + NMEAHelper.VDOP);

									if (!omitValidGPSData())
									{
										return true;
									}

								} else
								{
									Log.d(GPSDataValidator.class.getPackage().getName(), "DOP check failed: HDOP: " + NMEAHelper.HDOP + " | VDOP: " + NMEAHelper.VDOP);

									//GPSDataLog.writeToLog(newLocation, false);

									Log.d(GPSDataValidator.class.getPackage().getName(), "PREPARE LOCATION OMITTING: 1 | DOP");

									omitValidGPSData = 3;
								}
							} else
							{
								Log.d(GPSDataValidator.class.getPackage().getName(), "Accuracy check failed: " + newLocation.getAccuracy());

								//GPSDataLog.writeToLog(newLocation, false);

								Log.d(GPSDataValidator.class.getPackage().getName(), "PREPARE LOCATION OMITTING: 2 | ACCURACY");

								omitValidGPSData = 3;
							}	
						}
						else
						{
							Log.d(GPSDataValidator.class.getPackage().getName(), "Altitude check failed: " + newLocation.getAltitude());

							//GPSDataLog.writeToLog(newLocation, false);

							Log.d(GPSDataValidator.class.getPackage().getName(), "PREPARE LOCATION OMITTING: 3 | ALTITUDE");

							omitValidGPSData = 3;
						}
					} else
					{
						//GPSDataLog.writeToLog(newLocation, false);

						Log.d(GPSDataValidator.class.getPackage().getName(), "PREPARE LOCATION OMITTING: 4 | ACCELERATION");

						omitValidGPSData = 3;
					}
				} else
				{
					//GPSDataLog.writeToLog(newLocation, false);

					Log.d(GPSDataValidator.class.getPackage().getName(), "PREPARE LOCATION OMITTING: 5 | FIX=" + fix);

					omitValidGPSData = 3;
				}
			} else
			{
				//GPSDataLog.writeToLog(newLocation, false);

				try
				{
					String log = "WRONG TIME: ";

					log += "\nLocation provider = " + newLocation.getProvider();

					if (lastValidLocation == null)
					{
						log += "\nlastValidLocation==null";
					} else if (newLocation == null)
					{
						log += "\nlocation != null";
					} else
					{
						log += "\n        location.getTime() = " + newLocation.getTime();
						log += "\nlastValidLocation.getTime() = " + lastValidLocation.getTime();
						log += "\n           time difference = " + (newLocation.getTime() - lastValidLocation.getTime());
					}

					Log.d(GPSDataValidator.class.getPackage().getName(), log);
				} catch (Exception ex)
				{
				}
			}
		}

		return false;
	}

	public static void setLastRecodedLocation(Location loc)
	{
		lastRecodedLocation = loc;
	}

	public static long getTimeDifference()
	{
		return timeDifference;
	}
}
