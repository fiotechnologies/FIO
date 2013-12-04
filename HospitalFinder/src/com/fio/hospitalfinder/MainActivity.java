package com.fio.hospitalfinder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final long MIN_DISTANCE_FOR_UPDATE = 1000;// IN METERS
	private static final long MIN_TIME_FOR_UPDATE = 1000 * 60 * 15;// IN MILLI
																	// SECONDS
	private static final String ZIP_CODE = "zipcode";
	private static final String LOCALITY = "locality";
	private static final String SPECIALIST = "specialist";
	protected LocationManager locationManager;
	private String locationProvider;
	private TextView locationLable;
	private ProgressBar addressProgress;
	private String zipCode;
	private String locality;
	private String specialist;

	private final String LOCATION_MSG = "Showing results for ";
	private final String NORESULT_MSG = "No Results found for ";
	private static final int MAX_NO_OF_ATTEMPTS = 2;
	private AlertDialog alertDialog;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		addressProgress = (ProgressBar) findViewById(R.id.addressProgress);
		locationLable = (TextView) findViewById(R.id.locationLable);
		if (savedInstanceState == null) {
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			locationProvider = getAvailableLocationProvider(locationManager);
			if (locationProvider == null || locationProvider.trim().equals("")) {
				showZipcodeAlert(this, "No Location Provider is found");
			} else {
				showHospitalsForLocation();
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(ZIP_CODE, this.zipCode);
		outState.putString(LOCALITY, this.locality);
		outState.putString(SPECIALIST, this.specialist);

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		setContentView(R.layout.main);
		addressProgress = (ProgressBar) findViewById(R.id.addressProgress);
		locationLable = (TextView) findViewById(R.id.locationLable);
		this.zipCode = savedInstanceState.getString(ZIP_CODE);
		this.locality = savedInstanceState.getString(LOCALITY);
		this.specialist = savedInstanceState.getString(SPECIALIST);

		showHospitalList(this.zipCode, this.locality, this.specialist);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.location_refresh:
			clearOldData();
			showHospitalsForLocation();
			return true;
		case R.id.search:
			clearOldData();
			showZipcodeAlert(MainActivity.this, null);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v instanceof ViewGroup) {
			Hospital hospital = getAddressFromListItem((ViewGroup) v);

			menu.setHeaderTitle("Options");
			menu.add(0, v.getId(), 0, "Call");
			menu.add(0, v.getId(), 1, "Show Map");
			Intent callIntent = createCallIntent(hospital.getPhone2());
			(menu.getItem(0)).setIntent(callIntent);
			Intent mapIntent = createMapIntent(hospital);
			(menu.getItem(1)).setIntent(mapIntent);
		}

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		startActivity(item.getIntent());
		return true;
	}

	public void callHospital(View view) {
		ViewParent subparent = view.getParent();
		ViewParent parent = subparent.getParent();
		Hospital hospital;
		if (parent instanceof ViewGroup) {
			hospital = getAddressFromListItem((ViewGroup) parent);
			startActivity(createCallIntent(hospital.getPhone2()));
		}
	}

	public void showMap(View view) {
		ViewParent subparent = view.getParent();
		ViewParent parent = subparent.getParent();
		Hospital hospital;
		if (parent instanceof ViewGroup) {
			hospital = getAddressFromListItem((ViewGroup) parent);
			startActivity(createMapIntent(hospital));
		}
	}

	protected void showHospitalsForLocation() {
		LocationListener locationListner = new HospitalLocationListner();
		locationManager.requestLocationUpdates(locationProvider,
				MIN_TIME_FOR_UPDATE, MIN_DISTANCE_FOR_UPDATE, locationListner);
		Location location = locationManager
				.getLastKnownLocation(locationProvider);
		if (location == null) {
			location = tryToFindLocationAgain();
		}
		getResultsForUpdatedLocation(location);
	}

	private String getAvailableLocationProvider(LocationManager locationManager) {
		String provider = null;
		/*
		 * Criteria criteria = new Criteria();
		 * criteria.setAccuracy(Criteria.ACCURACY_COARSE); provider =
		 * locationManager.getBestProvider(criteria, true);
		 */

		List<String> providers = locationManager.getProviders(true);
		for (String str : providers) {
			if (str.equals(LocationManager.NETWORK_PROVIDER)) {
				provider = str;
				return provider;
			} else {
				provider = str;
			}
		}
		return provider;
	}

	private void showZipcodeAlert(Context ctx, String message) {
		final AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
		alert.setIcon(R.drawable.ic_launcher);
		alert.setTitle("Enter Your Area Information");
		alert.setMessage(message != null && !message.trim().equals("") ? message
				+ "\nEnter your Zip Code or Area"
				: "Enter your Zip Code or Area");

		LayoutInflater inflater = getLayoutInflater();
		final View zipAlertView = inflater.inflate(R.layout.zip_alert, null);
		alert.setView(zipAlertView);

		final EditText zipCodeText = (EditText) zipAlertView
				.findViewById(R.id.zipCode);
		final AutoCompleteTextView areaText = (AutoCompleteTextView) zipAlertView
				.findViewById(R.id.locality);
		String Areas[] = getResources().getStringArray(R.array.area_array);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, Areas);
		areaText.setAdapter(adapter);
		areaText.setThreshold(3);
		final AutoCompleteTextView specialistText = (AutoCompleteTextView) zipAlertView
				.findViewById(R.id.specialist);
		String special[] = getResources().getStringArray(R.array.special_array);
		ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, special);
		specialistText.setAdapter(adapter1);
		specialistText.setThreshold(1);
		// To clear any error message set on the Zip code field
		areaText.addTextChangedListener(new TextWatcher() {
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				zipCodeText.setError(null);
			}

			public void afterTextChanged(Editable s) {
			}
		});

		final Button okButton = (Button) zipAlertView
				.findViewById(R.id.zipAlertOKButton);
		okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (v == okButton) {

					zipCode = zipCodeText.getText().toString();
					locality = areaText.getText().toString();
					specialist = specialistText.getText().toString();

					zipCode = zipCode != null && zipCode.trim().equals("") ? null
							: zipCode;
					locality = locality != null && locality.trim().equals("") ? null
							: locality;
					specialist = specialist != null
							&& specialist.trim().equals("") ? null : specialist;
					if (zipCode == null && locality == null
							&& specialist == null) {
						zipCodeText.setError("Zipcode or Area is required");
					} else {
						closeZipAlert();
						showHospitalList(zipCode, locality, specialist);
					}
				}

			}
		});
		alertDialog = alert.show();
	}

	public void showHospitalList(String zipCode, String locality,
			String specialist) {

		this.zipCode = zipCode;
		this.locality = locality;
		this.specialist = specialist;

		DbAdapter dbAdapter = new DbAdapter(MainActivity.this);
		dbAdapter.open();

		ListView listView = (ListView) findViewById(R.id.hospital_list);

		Cursor cursor = dbAdapter.fetchListItems(zipCode, locality, specialist);
		if (cursor != null && cursor.getCount() > 0) {
			String[] from = { DbAdapter.HOSPITAL_NAME,
					DbAdapter.HOSPITAL_SPECIALIST, DbAdapter.HOSPITAL_ADDRESS,
					DbAdapter.LOCATION, DbAdapter.ZIPCODE,
					DbAdapter.HOSPITAL_PHONE2 };
			int[] to = { R.id.hospital_name, R.id.hospital_address,
					R.id.hospital_specialist, R.id.location, R.id.zipcode,
					R.id.hospital_phone2 };
			SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(
					MainActivity.this, R.layout.hospital_list, cursor, from,
					to, 1);
			listView.setAdapter(cursorAdapter);

			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					registerForContextMenu(view);
					openContextMenu(view);
					// Object itemAtPosition =
					// parent.getItemAtPosition(position);
				}
			});
			showResultMessage(zipCode, locality, LOCATION_MSG, true);
		} else {
			listView.setAdapter(new ArrayAdapter<String>(MainActivity.this,
					R.layout.empty_list,
					new String[] { "Try again with better search parameters!" }));
			showResultMessage(zipCode, locality, NORESULT_MSG, false);
		}
		closeZipAlert();
	}

	private void clearOldData() {
		locationLable.setText("");
		TextView noResult = (TextView) findViewById(R.id.no_results);
		if (noResult != null && noResult.getVisibility() == View.VISIBLE) {
			noResult.setText("");
			noResult.setVisibility(View.GONE);
		}
	}

	private void showResultMessage(String zipCode, String locality,
			String message, boolean success) {
		
		message = message
				+ ((zipCode != null && !zipCode.trim().equals("")) ? "Zip Code : "
						+ zipCode
						: "")
				+ " "
				+ ((locality != null && !locality.trim().equals("")) ? "Area : "
						+ locality
						: "")
						+ " "
				+ ((specialist != null && !specialist.trim().equals("")) ? "Specialist : "
						+ specialist
						: "");
		if (message.trim().endsWith("for")) {
			message = message.trim().replace("for", "");
		}
		locationLable.setText(message);
		// }
		if (success) {
			locationLable.setTextColor(Color.parseColor("#FFA540"));
		} else {
			locationLable.setTextColor(Color.parseColor("#FA0A1A"));
		}
	}

	protected void getResultsForUpdatedLocation(Location location) {
		if (location != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
					&& Geocoder.isPresent()) {
				(new GetAddressTask(this)).execute(location);
			} else {
				showZipcodeAlert(this, "Your Phone does not support Geocoder");
			}
		} else {
			showZipcodeAlert(this, "Your location could not be determined");
		}
	}

	private Location tryToFindLocationAgain() {
		int noOfAttempts = 0;
		Location location = null;
		while (noOfAttempts <= MAX_NO_OF_ATTEMPTS) {
			try {
				Thread.sleep(1000 * 2);
				location = locationManager
						.getLastKnownLocation(locationProvider);
				if (location != null) {
					break;
				}
				noOfAttempts++;
			} catch (InterruptedException ex) {
				// do nothing
			}
		}
		return location;
	}

	private void closeZipAlert() {
		if (alertDialog != null && alertDialog.isShowing()) {
			alertDialog.hide();
		}
	}

	private Intent createCallIntent(CharSequence phNo) {
		if (phNo != null && !phNo.toString().trim().equals("")) {
			Intent callIntent = new Intent(Intent.ACTION_CALL);
			callIntent.setData(Uri.parse("tel:" + phNo));
			return callIntent;
		}
		return null;
	}

	private Hospital getAddressFromListItem(ViewGroup v) {
		if (v == null) {
			throw new IllegalArgumentException(
					"View object in the list item is null");
		}
		Hospital hospital = new Hospital();
		StringBuilder address = new StringBuilder();
		ViewGroup listItem;

		listItem = (ViewGroup) v;
		View phoneView = listItem.getChildAt(6);
		if (phoneView instanceof TextView) {
			hospital.setPhone2(((TextView) phoneView).getText().toString());
		}

		View specialistView = listItem.getChildAt(1);
		if (specialistView instanceof TextView) {
			address.append(((TextView) specialistView).getText());
		}
		View addressView = listItem.getChildAt(2);
		if (addressView instanceof TextView) {
			address.append(((TextView) addressView).getText());
		}

		View locationView = listItem.getChildAt(4);
		if (locationView instanceof TextView) {
			address.append(",");
			address.append(((TextView) locationView).getText());
		}

		View zipCodeView = listItem.getChildAt(5);
		if (zipCodeView instanceof TextView) {
			address.append(",");
			address.append(((TextView) zipCodeView).getText());
		}

		hospital.setAddress(address.toString());
		return hospital;
	}

	private Intent createMapIntent(Hospital hospital) {
		Intent mapIntent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("geo:0,0?q=" + hospital.getAddress()));
		return mapIntent;
	}

	private class HospitalLocationListner implements LocationListener {

		public void onLocationChanged(Location location) {
			getResultsForUpdatedLocation(location);
			locationManager.removeUpdates(this);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// Toast.makeText(MainActivity.this, "Provider status changed",
			// Toast.LENGTH_LONG).show();
		}

		public void onProviderEnabled(String provider) {
			// Toast.makeText(MainActivity.this,
			// "Provider enabled by the user. " + provider + " turned on",
			// Toast.LENGTH_LONG).show();
		}

		public void onProviderDisabled(String provider) {
			// Toast.makeText(MainActivity.this,
			// "Provider disabled by the user. " + provider + " turned off",
			// Toast.LENGTH_LONG).show();
		}
	}

	private class GetAddressTask extends AsyncTask<Location, Void, Address> {

		Context context;

		public GetAddressTask(Context context) {
			super();
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			addressProgress.setVisibility(View.VISIBLE);
		}

		@Override
		protected Address doInBackground(Location... params) {
			Geocoder geocoder = new Geocoder(context, Locale.getDefault());
			Location loc = params[0];
			List<Address> addresses = null;
			if (Geocoder.isPresent()) {
				try {
					addresses = geocoder.getFromLocation(loc.getLatitude(),
							loc.getLongitude(), 1);
				} catch (IOException ex) {
					// do nothing
				}
			}

			if (addresses != null && addresses.size() > 0) {
				Address address = addresses.get(0);
				return address;
			} else {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Address result) {
			addressProgress.setVisibility(View.GONE);
			if (result != null) {
				showHospitalList(result.getPostalCode(),
						result.getSubLocality(), result.getUrl());
			} else {
				showZipcodeAlert(context,
						"Your Address could not be determined");
			}
		}
	}
}
