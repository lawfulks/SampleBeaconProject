package com.woongyi.samplebeacon;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ActivityMain extends Activity implements OnClickListener {
	private BluetoothAdapter mBluetoothAdapter = null;
	private ArrayList<BeaconInfo> beaconList = null;
	private TextView resultTxtView = null;
	private	Button startBtn = null;
	private Handler mHandler = null;
	private String resultText = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 레이아웃에 포함된 버튼과 텍스트뷰에 대한 객체 생성입니다.
		startBtn = (Button)findViewById(R.id.startBtn);
		resultTxtView = (TextView)findViewById(R.id.resultTxtView);
		// 버튼의 클릭 이벤트 옵션을 부여합니다.
		startBtn.setOnClickListener(this);

		// BLE 스캔을 실행할 블루투스 어뎁터 생성을 위 블루투스 매니 객체를 생성합니다.
		BluetoothManager bluetoothManager =
				(BluetoothManager)getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
		// 블루투스 매니저에서 블루투스 어뎁터 객체를 가져옵니다.
		mBluetoothAdapter = bluetoothManager.getAdapter();
		// 쓰레드 핸들러 객체와 탐색된 비컨을 담는 리스트 객체를 생성합니다.
		mHandler = new Handler();
		beaconList = new ArrayList<>();
	}

	@Override
	public void onClick(View v) {
		// 버튼을 클릭했을 때의 이벤트입니다.
		switch(v.getId()) {
		case R.id.startBtn : 
			// 블루트스가 활성화 여부를 boolean값으로 함수에 넣어줍니다.
			scanLeDevice(mBluetoothAdapter.isEnabled());
			break;
		}
	}

	private void scanLeDevice(final boolean enable) {
		// 활성화가 되어 있으면 스캔을 시작합니다. 쓰레드 핸들러로 3초뒤에 스캔을 정지합니다.
		if (enable) {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					// 스캔이 중지되면 버튼을 다시 활성화합니다.
					startBtn.setEnabled(true);
				}
			}, 3000);
			mBluetoothAdapter.startLeScan(mLeScanCallback);
			// 스캔 시작 중복을 방지하기 위해서 버튼을 비활성화합니다.
			startBtn.setEnabled(false);
		}else{
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}

	// BLE Scan 결과를 CallBack 해주는 콜백 메소드입니다.
	// 실질적인 스캔 부분입니다.
	private BluetoothAdapter.LeScanCallback mLeScanCallback =
			new BluetoothAdapter.LeScanCallback() {
		// 스캔 결과로 BluetoothDevice 정보, ScanData, rssi가 들어옵니다.
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			// 스캔 주기가 매우 짧기때문에 스캔된 각각 블루투스 디바이스에 대해 쓰레드로 정보를 처리합니다.
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// 스캔된 data를 기반으로 활용 가능한 Beacon 정보로 해석합니다.
					BeaconInfo beacon = BeaconInfo.fromScanData(scanRecord, rssi, device);
					// iBeacon이 맞고 새롭게 탐색된 Beacon일 경우 해당 Beacon 정보를 텍스트뷰에 보여줍니다.
					if(beacon.getiBeaconCheck() && !beaconList.contains(beacon)) {
						resultText = resultText + "\n\nUUID : " + beacon.getProximityUuid() + "\nMajor : " +
								beacon.getMajor() + "\nMinor : " + beacon.getMinor();

						resultTxtView.setText(resultText);
						beaconList.add(beacon);

						// 새롭게 탐색된 Beacon의 해석된 정보를 서버에 전송하기 위해 쓰레드 클래스에 값을 넘겨줍니다.
						String data = beacon.getProximityUuid() + "," + beacon.getMajor() + ","
								+ beacon.getMinor() + "," + beacon.getAccuracy();
						new ThreadExpressServer().execute(data);
					}
				}
			});
		}
	};

	// 서버 연동을 하기 위해 네트워크 요청을 하여야 합니다.
	// Android에서 네트워크 요청 시 필히 쓰레드에서 요청을 진행해야 합니다.
	public class ThreadExpressServer extends AsyncTask<String, Void, String> {

		public ThreadExpressServer() {
		}

		
		// 쓰레드가 실행되기 전에 먼저 진행되는 함수입니다. 현재는 공백입니다.
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		// 실제적으로 쓰레드가 실행되는 함수입니다.
		@Override
		protected String doInBackground(String... params) {

			// 서버 연동이 짜여있는 클래스를 객체 생성합니다.
			HttpExpressServer httpMemberLogin = new HttpExpressServer();
			// Beacon 정보를 파라미터로 넘겨주고 서버의 Response 값을 result에 받습니다.
			String reuslt = httpMemberLogin.communication(params[0]);

			return reuslt;
		}

		// 쓰레드가 종료되고 진행되는 함수입니다.
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			
			// 서버의 결과가 존재한다면 텍스트뷰에서 출력합니다.
			if(result != null) {
				resultText = resultText + "\nBeacon Massage : " + result;
				resultTxtView.setText(resultText);
			}
		}
	}
}
