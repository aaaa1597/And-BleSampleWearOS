package asia.groovelab.blesample.model;

import android.bluetooth.BluetoothGattService;
import android.os.Parcel;
import android.os.Parcelable;

public class Section implements Parcelable {
	private String title;
	protected Section(Parcel in) {
		title = in.readString();
	}
	public Section(BluetoothGattService src) {
		title = src.getUuid().toString();
	}

	public static final Creator<Section> CREATOR = new Creator<Section>() {
		@Override
		public Section createFromParcel(Parcel in) {
			return new Section(in);
		}

		@Override
		public Section[] newArray(int size) {
			return new Section[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeString(title);
	}

	public String getTitle() { return title;}
}
