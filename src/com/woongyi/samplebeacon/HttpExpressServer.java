package com.woongyi.samplebeacon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class HttpExpressServer {
	private String url_host = "http://54.64.128.28:1300/";

	public HttpExpressServer() {
		
	}

	public String communication(String val) {
		try {
			// Beacon 정보를 각각 구분하여 저장합니다.
			String uuid = val.split(",")[0];
			String major = val.split(",")[1];
			String minor = val.split(",")[2];
			String accuracy = val.split(",")[3];
			Log.d("Http", uuid + "/" + major + "/" + minor + "/" + accuracy);

			// 지정된 URL로 POST 기반으로 HTTP 연결합니다.
			URI uri = new URI(url_host);
			URL con_url = uri.toURL();
			HttpURLConnection httpConn = null;
			httpConn = (HttpURLConnection)con_url.openConnection();
			httpConn.setDoOutput(true);
			httpConn.setDoInput(true);
			httpConn.setUseCaches(false);
			httpConn.setRequestMethod("POST");
			httpConn.setRequestProperty("Cache-Control", "no-cache");
			httpConn.setRequestProperty("Content-Type", "application/json");
			httpConn.setRequestProperty("Accept", "application/json");
			httpConn.setConnectTimeout(3000);
			httpConn.setReadTimeout(3000);
			
			// JSON 형태로 Beacon 정보를 변환합니다.
			JSONObject job = new JSONObject();
			job.put("string", "Hi");
			job.put("uuid", uuid);
			job.put("major", major);
			job.put("minor", minor);
			job.put("accuracy", accuracy);

			// 서버에 JSON 객체를 전송합니다.
			OutputStream outStream = httpConn.getOutputStream();
			outStream.write(job.toString().getBytes());
			outStream.flush();
			Log.d("Send", job.toString());

			
			// 서버에서 전달 값이 존재할 경우 byte로 data를 전송받습니다.
			// byte의 data를 String으로 변환합니다.
			String response;
			int responseCode = httpConn.getResponseCode();
			if(responseCode == HttpURLConnection.HTTP_OK) {
				InputStream inStream = httpConn.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] byteBuffer = new byte[1024];
				byte[] byteData = null;
				int nLength = 0;
				while((nLength = inStream.read(byteBuffer, 0, byteBuffer.length)) != -1) {
					baos.write(byteBuffer, 0, nLength);
				}
				byteData = baos.toByteArray();

				response = new String(byteData);
				Log.d("Response", response);
				
				// 결과값을 리턴하여 객체 호출하였을 때 사용한 변수에 담아줍니다.
				// 결과값이 없으면 null이 반환됩니다.
				return response;
			}
		}catch(MalformedURLException e) {
		}catch(IOException e) {
		}catch(URISyntaxException e) {
		}catch(JSONException e) {	
		}
		
		return null;
	}
}