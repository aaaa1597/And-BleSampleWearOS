package asia.groovelab.blesample.model;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Parcel;
import android.os.Parcelable;

import asia.groovelab.blesample.R;

public class Item implements Parcelable {
	private final String						uuid;
	private final Boolean						isReadable;
	private final Boolean						isWritable;
	private final Boolean						isNotifiable;
	private final BluetoothGattCharacteristic	bluetoothGattCharacteristic;
	private String								readValue;

	public Item(Parcel in) {
		uuid = in.readString();
		byte tmpIsReadable = in.readByte();
		isReadable = tmpIsReadable == 0 ? null : tmpIsReadable == 1;
		byte tmpIsWritable = in.readByte();
		isWritable = tmpIsWritable == 0 ? null : tmpIsWritable == 1;
		byte tmpIsNotifiable = in.readByte();
		isNotifiable = tmpIsNotifiable == 0 ? null : tmpIsNotifiable == 1;
		bluetoothGattCharacteristic = in.readParcelable(BluetoothGattCharacteristic.class.getClassLoader());
		readValue = in.readString();
	}

	public Item(BluetoothGattCharacteristic src) {
		uuid						= src.getUuid().toString();
		isReadable					= (src.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0;
		isWritable					= ((src.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) || ((src.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0);
		isNotifiable				= (src.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0;
		bluetoothGattCharacteristic	= src;
		readValue					= "";
	}

	public static final Creator<Item> CREATOR = new Creator<Item>() {
		@Override
		public Item createFromParcel(Parcel in) {
			return new Item(in);
		}

		@Override
		public Item[] newArray(int size) {
			return new Item[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeString(uuid);
		parcel.writeByte((byte)(isReadable == null ? 0 : isReadable ? 1 : 2));
		parcel.writeByte((byte)(isWritable == null ? 0 : isWritable ? 1 : 2));
		parcel.writeByte((byte)(isNotifiable == null ? 0 : isNotifiable ? 1 : 2));
		parcel.writeParcelable(bluetoothGattCharacteristic, i);
		parcel.writeString(readValue);
	}

	public String	getUuid() { return uuid; }
	public Boolean	getIsReadable() { return isReadable; }
	public Boolean	getIsWritable() { return isWritable; }
	public BluetoothGattCharacteristic getBluetoothGattCharacteristic() { return bluetoothGattCharacteristic; }
	public String	getReadValue() { return readValue; }
	public void		setReadValue(String str) { readValue = str; }
	public int getReadableColorRes() { return (isReadable)  ? 0xff00ff00 : R.color.colorTextDisabled; }
	public int getWritableColorRes() { return (isWritable)  ? 0xff00ff00 : R.color.colorTextDisabled; }
	public int getNotifiableColorRes(){return (isNotifiable)? 0xff00ff00 : R.color.colorTextDisabled; }
}
