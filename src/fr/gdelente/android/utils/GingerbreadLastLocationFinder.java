/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gdelente.android.utils;

import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;

/**
 * Optimized implementation of Last Location Finder for devices running
 * Gingerbread and above.
 * 
 * This class let's you find the "best" (most accurate and timely) previously
 * detected location using whatever providers are available.
 * 
 * Where a timely / accurate previous location is not detected it will return
 * the newest location (where one exists) and setup a oneshot location update to
 * find the current location.
 */
public class GingerbreadLastLocationFinder implements ILastLocationFinder {

	protected static String TAG = "LastLocationFinder";
	protected static String SINGLE_LOCATION_UPDATE_ACTION = "com.radioactiveyak.places.SINGLE_LOCATION_UPDATE_ACTION";

	protected LocationListener mLocationListener;
	protected LocationManager mLocationManager;
	protected Context mContext;
	protected Criteria mCriteria;

	/**
	 * Construct a new Gingerbread Last Location Finder.
	 * 
	 * @param context
	 *            Context
	 */
	public GingerbreadLastLocationFinder(Context context,
			LocationListener locationListener) {
		mContext = context;
		mLocationListener = locationListener;
		mLocationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		// Coarse accuracy is specified here to get the fastest possible result.
		// The calling Activity will likely (or have already) request ongoing
		// updates using the Fine location provider.
		mCriteria = new Criteria();
		// FIXME Change to LOW
		mCriteria.setAccuracy(Criteria.ACCURACY_LOW);
	}

	/**
	 * Returns the most accurate and timely previously detected location. Where
	 * the last result is beyond the specified maximum distance or latency a
	 * one-off location update is returned via the {@link LocationListener}
	 * specified in {@link setChangedLocationListener}.
	 * 
	 * @param minDistance
	 *            Minimum distance before we require a location update.
	 * @param minTime
	 *            Minimum time required between location updates.
	 * @return The most accurate and / or timely previously detected location.
	 */
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public Location getLastBestLocation(int minDistance, long minTime) {

		Location bestResult = null;
		float bestAccuracy = Float.MAX_VALUE;
		long bestTime = Long.MIN_VALUE;

		// Iterate through all the providers on the system, keeping
		// note of the most accurate result within the acceptable time limit.
		// If no result is found within maxTime, return the newest Location.
		List<String> matchingProviders = mLocationManager.getAllProviders();
		for (String provider : matchingProviders) {
			Location location = mLocationManager.getLastKnownLocation(provider);
			if (location != null) {
				float accuracy = location.getAccuracy();
				long time = location.getTime();

				if ((time > minTime && accuracy < bestAccuracy)) {
					bestResult = location;
					bestAccuracy = accuracy;
					bestTime = time;
				} else if (time < minTime && bestAccuracy == Float.MAX_VALUE
						&& time > bestTime) {
					bestResult = location;
					bestTime = time;
				}
			}
		}

		// If the best result is beyond the allowed time limit, or the accuracy
		// of the
		// best result is wider than the acceptable maximum distance, request a
		// single update.
		// This check simply implements the same conditions we set when
		// requesting regular
		// location updates every [minTime] and [minDistance].
		if (mLocationListener != null
				&& (bestTime < minTime || bestAccuracy > minDistance)) {

			mLocationManager.requestSingleUpdate(mCriteria, mLocationListener,
					null);
		}
		return bestResult;
	}

	private void enableTestProvider() {
		mLocationManager.removeTestProvider("test");
		mLocationManager.addTestProvider("test", "requiresNetwork" == "",
				"requiresSatellite" == "", "requiresCell" == "",
				"hasMonetaryCost" == "", "supportsAltitude" == "",
				"supportsSpeed" == "", "supportsBearing" == "",

				android.location.Criteria.POWER_LOW,
				android.location.Criteria.ACCURACY_COARSE);

		mLocationManager.setTestProviderEnabled("test", true);

		mLocationManager.setTestProviderStatus("test",
				LocationProvider.AVAILABLE, null, System.currentTimeMillis());

	}

	/**
	 * {@inheritDoc}
	 */
	public void setChangedLocationListener(LocationListener l) {
		mLocationListener = l;
	}

	/**
	 * {@inheritDoc}
	 */
	public void cancel() {
		mLocationManager.removeUpdates(mLocationListener);
	}
}
