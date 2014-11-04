package com.woongyi.samplebeacon;

import android.bluetooth.BluetoothDevice;

public class BeaconInfo {
	private final static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	private final static String appleID = "4c000215";
	public static final int PROXIMITY_IMMEDIATE = 1;
	public static final int PROXIMITY_NEAR = 2;
	public static final int PROXIMITY_FAR = 3;
	public static final int PROXIMITY_UNKNOWN = 0;
	protected boolean iBeaconCheck;
	protected String beaconName;
	protected String bluetoothAddress;
	protected String proximityUuid;
	protected int major;
	protected int minor;
	protected int rssi = 0;
	protected int txPower;
	protected Integer proximity;
	protected Double accuracy;

	protected BeaconInfo() {	}

	public String getBluetoothAddress() {
		return bluetoothAddress;
	}

	public String getProximityUuid() {
		return proximityUuid;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getRssi() {
		return rssi;
	}

	public int getTxPower() {
		return txPower;
	}

	public int getProximity() {
		if (proximity == null) {
			proximity = calculateProximity(getAccuracy());		
		}
		return proximity;		
	}

	public double getAccuracy() {
		if (accuracy == null) {
			accuracy = calculateAccuracy(txPower, rssi);		
		}
		return accuracy;
	}

	public boolean getiBeaconCheck(){
		return iBeaconCheck;
	}

	// byte로 이루어진 ScanData를 Hex String으로 변환하는 함수입니다.
	public static BeaconInfo fromScanData(byte[] scanData, int rssi, BluetoothDevice device) {
		BeaconInfo iBeacon = new BeaconInfo();

		// 조건문 중 0x02와 같은지를 찾습니다. 이는 iBeacon Type이 맞는지 탐색하기 위함입니다.
		// 또한 Beacon의 Data는 21byte로 이루어져있기때문에 15의 16진수 값으로 들어있습니다.
		// 0x02와 0x15가 항상 존재하는지 판별하여야 합니다.
		int startByte = 2;
		while (startByte <= 5) {
			if ((scanData[startByte+2] & 0xff) == 0x02 && (scanData[startByte+3] & 0xff) == 0x15) {
				// 조건문을 통과하면 iBeacon 형식과 일치함을 의미하기때문에 check 변수에 true를 입력합니다.
				iBeacon.iBeaconCheck = true;
				// BluetoothDevice에서 제조사가 입력한 Beacon의 이름을 얻을 수 있습니다.
				// 대부분 존재하지만 공백인 기기도 있습니다.
				iBeacon.beaconName = device.getName();
				// BluetoothDevice에서 MAC Address를 얻을 수 있습니다.
				iBeacon.bluetoothAddress = device.getAddress();
				//				Log.d(iBeacon.beaconName, iBeacon.bluetoothAddress);

				// byte로 이루어진 scanData를 Hex String으로 변환합니다.
				String hexText = bytesToHex(scanData);
				
				// Data들은 순차적으로 위치하였기때문에 각각 Data 길이만큼 잘라서 구분하면 됩니다.
				// uuid는 16byte로 이루어져있기때문에 String 길이로는 총 32만큼 지정하면 됩니다.
				// Major와 Minor는 2byte씩 갖고있기때문에 각각 4만큼 길이를 갖습니다.
				// txPower는 minor 다음 1byte로 존재하기에 2만큼 지정하면 됩니다.
				int uuidStart = hexText.indexOf(appleID) + appleID.length();
				int uuidEnd = uuidStart + 32;
				int majorStart = uuidEnd;
				int minorStart = majorStart + 4;
				int txPowerStart = minorStart + 4;
				iBeacon.proximityUuid = hexText.substring(uuidStart, uuidEnd);
				iBeacon.major = Integer.parseInt(hexText.substring(majorStart, majorStart+4), 16);
				iBeacon.minor = Integer.parseInt(hexText.substring(minorStart, minorStart+4), 16);
				iBeacon.txPower = Integer.parseInt(hexText.substring(txPowerStart, txPowerStart+2), 16);
				iBeacon.rssi = rssi;
				
				// "e2c56db5-dffb-48d2-b060-d0f5a71096e0"
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(iBeacon.proximityUuid.substring(0,8));
				stringBuilder.append("-");
				stringBuilder.append(iBeacon.proximityUuid.substring(8,12));
				stringBuilder.append("-");
				stringBuilder.append(iBeacon.proximityUuid.substring(12,16));
				stringBuilder.append("-");
				stringBuilder.append(iBeacon.proximityUuid.substring(16,20));
				stringBuilder.append("-");
				stringBuilder.append(iBeacon.proximityUuid.substring(20,32));
				iBeacon.proximityUuid = stringBuilder.toString();

				//								Log.i("", "UUID:" + iBeacon.proximityUuid + " Major:" + iBeacon.major + 
				//										" Minor:" + iBeacon.minor + " txPower:" + iBeacon.txPower +
				//										" rssi:" + iBeacon.rssi);

				return iBeacon;
			}
			startByte++;
		}

		iBeacon.iBeaconCheck = false;
		return iBeacon;
	}

	// txPower와 rssi를 받아 m 단위인 실거리를 계산합니다.
	protected static double calculateAccuracy(int txPower, double rssi) {
		if (rssi == 0) {
			return -1.0;
		}

		//		double ratio = rssi*1.0/txPower;
		//		if (ratio < 1.0) {
		//			return Math.pow(ratio,10);
		//		}else {
		//			double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;	
		//			return accuracy;
		//		}
		double ratio_db = txPower - rssi;
		double ratio_linear = Math.pow(10, ratio_db / 10);
		double accuracy = Math.sqrt(ratio_linear) / 1000000000;
		accuracy = accuracy / 10000;
		accuracy = Math.floor(accuracy*1000d) / 1000d;

		return accuracy;
	}	

	// 계싼된 거리 (accuracy)를 기반으로 멈, 가까움, 아주 가까움으로 구분합니다.
	protected static int calculateProximity(double accuracy) {
		if (accuracy < 0) {
			return PROXIMITY_UNKNOWN;	 
		}else if (accuracy < 0.5 ) {
			return BeaconInfo.PROXIMITY_IMMEDIATE;
		}else if (accuracy <= 4.0) { 
			return BeaconInfo.PROXIMITY_NEAR;
		}

		return BeaconInfo.PROXIMITY_FAR;
	}

	// byte를 Hex로 변환하는 함수입니다.
	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for ( int j = 0; j < bytes.length; j++ ) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}

		return new String(hexChars);
	}

	@Override
	public int hashCode() {
		return minor;
	}

	// 탐색된 Beacon들을 저장한 List에서 중첩이 있는지 확인할 때 UUID, Major, Minor 3가지를 비교합니다.
	@Override
	public boolean equals(Object that) {
		if(that instanceof BeaconInfo) {
			BeaconInfo thatIBeacon = (BeaconInfo) that;	
			
			return (thatIBeacon.getMajor() == this.getMajor() && thatIBeacon.getMinor() 
					== this.getMinor() && thatIBeacon.getProximityUuid().equals(this.getProximityUuid()));
		}
		
		return false;
	}
}
