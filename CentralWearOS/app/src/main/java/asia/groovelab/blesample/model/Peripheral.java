package asia.groovelab.blesample.model;

import android.bluetooth.BluetoothDevice;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;

public class Peripheral implements Parcelable {
	private String			localName;
	private String			address;
	private int				rssi;
	private String			serviceUuid;
	private BluetoothDevice	bluetoothDevice;
	private String			rssiString = "dbm";

	public Peripheral(Parcel in) {
		localName		= in.readString();
		address			= in.readString();
		rssi			= in.readInt();
		serviceUuid		= in.readString();
		bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
		rssiString		= rssi + "dbm";
	}

	public Peripheral(ScanResult scanResult) {
		localName		= scanResult.getDevice().getName();
		address			= scanResult.getDevice().getAddress();
		rssi			= scanResult.getRssi();
		if(scanResult.getScanRecord().getServiceUuids()!=null && scanResult.getScanRecord().getServiceUuids().size()!=0)
			serviceUuid	= scanResult.getScanRecord().getServiceUuids().get(0).getUuid().toString();
		else
			serviceUuid	= null;
		bluetoothDevice = scanResult.getDevice();
		rssiString		= rssi + "dbm";
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(localName);
		dest.writeString(address);
		dest.writeInt(rssi);
		dest.writeString(serviceUuid);
		dest.writeParcelable(bluetoothDevice, flags);
	}

	public static final Creator<Peripheral> CREATOR = new Creator<Peripheral>() {
		@Override
		public Peripheral createFromParcel(Parcel in) {
			return new Peripheral(in);
		}

		@Override
		public Peripheral[] newArray(int size) {
			return new Peripheral[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	public String			getLocalName()		{return localName;}
	public String			getAddress()		{return address;}
	public int				getRssi()			{return rssi;}
	public String			getServiceUuid()	{return serviceUuid;}
	public BluetoothDevice	getBluetoothDevice(){return bluetoothDevice;}
	public void	setLocalName(String slocalname)		{localName=slocalname;}
	public void	setAddress(String laddress)			{address = laddress;}
	public void	setRssi(int arssi)					{rssi=arssi;}
	public void	setServiceUuid(String aserviceUuid)	{serviceUuid=aserviceUuid;}
	public void	setBluetoothDevice(BluetoothDevice abluetoothDevice){bluetoothDevice=abluetoothDevice;}
}
