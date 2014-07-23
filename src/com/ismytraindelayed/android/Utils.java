package com.ismytraindelayed.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {

	/**
	 * Call the web service URL and return a JSON string. If there is any
	 * problem, still return a valid JSON object
	 * 
	 * @param url
	 *            The URL to call
	 * @return The JSON object returned
	 */
	public static JSONObject getJson(String url) {
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("User-Agent", "com.ismytraindelayed.android");
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				return jsonError();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return jsonError();
		} catch (IOException e) {
			e.printStackTrace();
			return jsonError();
		}
		try {
			return new JSONObject(builder.toString());
		} catch (JSONException e) {
			return jsonError();
		}
	}

	public static JSONObject jsonError() {
		JSONObject error = new JSONObject();
		try {
			error.put("error", "failed");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return error;
	}
}
