package com.ismytraindelayed.android;

import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class IsMyTrainDelayedActivity extends Activity {

	Button button;
	TableLayout results;
	JSONArray trains;
	LocationManager locationManager;
	LocationListener locationListener;
	Location geolocation;
	AutoCompleteTextView to;
	AutoCompleteTextView from;
	Boolean departing = true;
	TextView departures;
	TextView arrivals;
	SharedPreferences settings;

	public void restoreSavedJourneys() {
		LinearLayout main = (LinearLayout) findViewById(R.id.main);
		main.setFocusableInTouchMode(true);
		results.removeAllViews();
		if ((settings.contains("saved_journeys"))
				&& (settings.getString("saved_journeys", null).length() > 0)) {
			TableRow journey_title = new TableRow(this);
			TextView journey_title_text = new TextView(this);
			journey_title_text.setText("Your Saved Journeys");
			journey_title_text.setTextSize(24);
			journey_title_text.setPadding(0, 10, 0, 0);
			journey_title.addView(journey_title_text);
			results.addView(journey_title, new TableLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

			String saved_journeys = settings.getString("saved_journeys", null);
			String[] journeys = saved_journeys.split(";");
			for (int a = 0; a < journeys.length; a++) {
				final String[] string = journeys[a].split(",");
				TableRow journey = new TableRow(this);
				TextView journey_text = new TextView(this);
				journey_text.setPadding(0, 5, 0, 5);
				journey_text.setTextSize(18);
				journey_text.setText(string[0] + " and " + string[1]);
				journey.addView(journey_text);
				results.addView(journey, new TableLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				journey.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						from.setText(string[0]);
						to.setText(string[1]);
						button.performClick();
					}
				});
			}
		} else {
			TableRow journey_title = new TableRow(this);
			TextView journey_title_text = new TextView(this);
			journey_title_text.setText("No Saved Journeys");
			journey_title_text.setTextSize(24);
			journey_title_text.setPadding(0, 10, 0, 0);
			journey_title.addView(journey_title_text);
			results.addView(journey_title, new TableLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			TableRow journey = new TableRow(this);
			TextView journey_text = new TextView(this);
			journey_text.setText("Save frequent journeys to save time");
			journey_text.setTextSize(14);
			journey.addView(journey_text);
			results.addView(journey, new TableLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		}
	}

	// SET UP MENU HANDLERS
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.nearest_from:
			find_nearest(0);
			return true;

		case R.id.nearest_to:
			find_nearest(1);
			return true;

		case R.id.saved_journeys:
			restoreSavedJourneys();
			return true;

		case R.id.save_journey:
			SharedPreferences.Editor editor = settings.edit();
			String saved_journeys;
			if (settings.contains("saved_journeys"))
				saved_journeys = settings.getString("saved_journeys", null);
			else
				saved_journeys = "";
			editor.putString(
					"saved_journeys",
					saved_journeys.replace(from.getText() + "," + to.getText()
							+ ";", ""));
			editor.putString("saved_journeys", saved_journeys + from.getText()
					+ "," + to.getText() + ";");
			editor.commit();
			Toast.makeText(getBaseContext(), "Journey Saved", Toast.LENGTH_LONG)
					.show();
			return true;

		case R.id.delete_journey:
			SharedPreferences.Editor editor2 = settings.edit();
			String saved_journeys2 = settings.getString("saved_journeys", null);
			editor2.putString(
					"saved_journeys",
					saved_journeys2.replace(from.getText() + "," + to.getText()
							+ ";", ""));
			editor2.commit();
			Toast.makeText(getBaseContext(), "Removed Saved Journey",
					Toast.LENGTH_LONG).show();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (departing) {
			menu.getItem(3).setTitle("From Nearest Station");
			menu.getItem(4).setTitle("To Nearest Station");
		} else {
			menu.getItem(3).setTitle("Into Nearest Station");
			menu.getItem(4).setTitle("From Nearest Station");
		}
		if ((from.getText().length() > 0) && (to.getText().length() > 0)) {
			String saved_journeys;
			if (settings.contains("saved_journeys"))
				saved_journeys = settings.getString("saved_journeys", null);
			else
				saved_journeys = "";
			if (saved_journeys.contains(from.getText() + "," + to.getText()
					+ ";")) {
				menu.getItem(1).setVisible(false);
				menu.getItem(2).setVisible(true);
			} else {
				menu.getItem(1).setVisible(true);
				menu.getItem(2).setVisible(false);
			}
		} else {
			menu.getItem(1).setVisible(false);
			menu.getItem(2).setVisible(false);
		}
		return true;
	}

	public boolean find_nearest(int field) {
		if (geolocation == null) {
			Toast.makeText(getBaseContext(),
					"Still acquiring your location, please wait",
					Toast.LENGTH_LONG).show();
			return true;
		}
		try {
			String url = "http://ismytraindelayed.com/stations?lat="
					+ geolocation.getLatitude() + "&lng="
					+ geolocation.getLongitude();
			JSONObject response = Utils.getJson(url);
			final AutoCompleteTextView from = (AutoCompleteTextView) findViewById(R.id.from);
			final AutoCompleteTextView to = (AutoCompleteTextView) findViewById(R.id.to);
			if (response.getString("name").length() > 0) {
				if (field == 0)
					to.setText(response.getString("name"));
				else
					from.setText(response.getString("name"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Called when the activity is first created.
	 * 
	 * @return
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// SET UP GLOBALS
		button = (Button) findViewById(R.id.button);
		results = (TableLayout) findViewById(R.id.results);
		from = (AutoCompleteTextView) findViewById(R.id.from);
		to = (AutoCompleteTextView) findViewById(R.id.to);
		settings = getSharedPreferences("SavedJourneys", 0);

		// SET UP AUTOCOMPLETE ADAPTERS
		String[] stations = getResources().getStringArray(
				R.array.stations_array);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.list_item, stations);
		from.setAdapter(adapter);
		to.setAdapter(adapter);

		// SET UP BUTTON LISTENER
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				results.removeAllViews();
				LinearLayout main = (LinearLayout) findViewById(R.id.main);
				main.clearFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(from.getWindowToken(), 0);
				try {
					String to_query = URLEncoder
							.encode(to.getText().toString());
					String from_query = URLEncoder.encode(from.getText()
							.toString());
					String url = "http://ojp.nationalrail.co.uk/service/ldb/liveTrainsJson?departing="
							+ departing
							+ "&liveTrainsFrom="
							+ from_query
							+ "&liveTrainsTo="
							+ to_query
							+ "&serviceId=abcdefghijk&from="
							+ from_query
							+ "&to=" + to_query;
					JSONObject response = Utils.getJson(url);
					if (response.getString("error") == "Failed") {
						TableRow error = new TableRow(getBaseContext());
						TextView error_msg = new TextView(getBaseContext());
						error_msg
								.setText("Sorry, unable to retrieve train times. Check network connection");
						error.addView(error_msg);
						results.addView(error, new TableLayout.LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.WRAP_CONTENT));
					} else {
						trains = response.getJSONArray("trains");
					}
					if (trains.length() == 0) {
						TableRow error = new TableRow(getBaseContext());
						TextView error_msg = new TextView(getBaseContext());
						error_msg.setTextSize(14);
						error_msg
								.setText("Sorry, no train information for that journey");
						error.addView(error_msg);
						results.addView(error, new TableLayout.LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.WRAP_CONTENT));
					}
					for (int i = 0; i < trains.length(); i++) {
						final JSONArray train = trains.getJSONArray(i);
						TableRow row = new TableRow(getBaseContext());
						// TRAIN TIME
						TextView time = new TextView(getBaseContext());
						time.setTextSize(18);
						time.setTextColor(Color.parseColor("#0F2E4C"));
						time.setText(Html.fromHtml(train.getString(1)));
						row.addView(time);
						// DESTINATION
						TextView destination = new TextView(getBaseContext());
						destination.setTextColor(Color.parseColor("#0F2E4C"));
						destination.setTextSize(18);
						destination.setText(Html.fromHtml(train.getString(2)));
						destination.setPadding(5, 5, 5, 0);
						row.addView(destination);
						results.addView(row, new TableLayout.LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.WRAP_CONTENT));
						// STATUS
						TableRow status = new TableRow(getBaseContext());
						TextView status_text = new TextView(getBaseContext());
						TextView empty_cell = new TextView(getBaseContext());
						status.addView(empty_cell);
						status_text.setTextSize(14);
						String status_string = Html
								.fromHtml(train.getString(3)).toString()
								.replace("<br/>", "-").replace("*", "");
						String platform = "";
						if (train.getString(4).length() != 0) {
							platform = " (Platform "
									+ train.getString(4).toString() + ")";
						}
						status_text.setText(status_string + platform);
						if ((!(status_string.contains("On time")))
								&& (!(status_string.contains("Starts here"))))
							status_text.setTextColor(Color.RED);
						status_text.setPadding(5, 0, 5, 5);
						status.addView(status_text);
						results.addView(status, new TableLayout.LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.WRAP_CONTENT));
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		// RESTORE SAVED JOURNEYS
		restoreSavedJourneys();

		// SET UP RADIO LISTENER AND TYPE OF QUERY
		departures = (TextView) findViewById(R.id.radio_departures);
		departures.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				arrivals.setTextColor(Color.parseColor("#c3c3c3"));
				departures.setTextColor(Color.parseColor("#000000"));
				from.setHint("From (Required)");
				to.setHint("Destination (Optional)");
				departing = true;
				if ((to.length() > 0) && (from.length() > 0))
					button.performClick();
			}
		});
		arrivals = (TextView) findViewById(R.id.radio_arrivals);
		arrivals.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				arrivals.setTextColor(Color.parseColor("#000000"));
				departures.setTextColor(Color.parseColor("#c3c3c3"));
				from.setHint("Into (Required)");
				to.setHint("Origin (Optional)");
				departing = false;
				if ((to.length() > 0) && (from.length() > 0))
					button.performClick();
			}
		});

		// SET UP LOCATION MANAGER
		locationManager = (LocationManager) getBaseContext().getSystemService(
				Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				geolocation = location;
				if (geolocation.getAccuracy() < 3000) {
					locationManager.removeUpdates(locationListener);
				}
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

	}
}