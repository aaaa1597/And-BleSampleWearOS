package asia.groovelab.blesample.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import asia.groovelab.blesample.TLog;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.callback.DataSentCallback;
import no.nordicsemi.android.ble.callback.FailCallback;
import no.nordicsemi.android.ble.callback.RssiCallback;
import no.nordicsemi.android.ble.callback.SuccessCallback;
import no.nordicsemi.android.ble.data.Data;

import asia.groovelab.blesample.Functions.Func0;
import asia.groovelab.blesample.Functions.Func1;

public class SampleBleManager extends BleManager {
	private static final int connectionTimeout		= 10;/*秒*/
	private static final int connectionRetryTime	= 3;
	private static final int connectionRetryDelay	= 100;/*m秒*/

	private Func1<List<BluetoothGattService>, Object>	discoveredServicesHandler;
	public void setDiscoveredServicesHandler(Func1<List<BluetoothGattService>, Object> handler) { discoveredServicesHandler = handler;}
	private boolean mIsConnecting = false;
	private boolean mWasConnected = false;
	public boolean isConnecting() { return mIsConnecting; }
	public boolean wasConnected() { return mWasConnected; }
	private BleManagerGattCallback gattCallback;

	public SampleBleManager(Context context) {
		super(context);
	}

	private class SampleBleManagerGattCallback extends BleManagerGattCallback {
		@Override
		protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
			TLog.d("");
			mIsConnecting = false;
			mWasConnected = true;
			if(discoveredServicesHandler != null)
				discoveredServicesHandler.invoke(gatt.getServices());
			return true;
		}

		@Override
		protected void onDeviceDisconnected() {
			TLog.d("");
		}
	}

	@NonNull
	@Override
	protected BleManagerGattCallback getGattCallback() {
		if(gattCallback == null)
			gattCallback = new SampleBleManagerGattCallback();
		return gattCallback;
	}

	@Override
	protected boolean shouldClearCacheWhenDisconnected() {
		return true;
	}

	public final void enqueueConnect(@NonNull BluetoothDevice bluetoothDevice) {
		TLog.d("");
		this.mIsConnecting = true;
		connect(bluetoothDevice).timeout(connectionTimeout*1000).retry(3, (int)connectionRetryDelay).enqueue();
	}

	public final void enqueueDisconnect(@Nullable final Func0<Object> doneHandler) {
		TLog.d("");
		this.mIsConnecting = false;
		this.mWasConnected = false;
		this.disconnect().done(new SuccessCallback() {
			public final void onRequestCompleted(@NonNull BluetoothDevice it) {
				TLog.d("");
				if (doneHandler != null)
					doneHandler.invoke();
			}
		}).fail(new FailCallback() {
			public final void onRequestFailed(@NonNull BluetoothDevice $noName_0, int $noName_1) {
				TLog.d("");
				if (doneHandler != null)
					doneHandler.invoke();

			}
		}).enqueue();
	}

	public void readRssi(@NonNull final Func1<Integer, Object> callback) {
		TLog.d("");
		this.readRssi().with(new RssiCallback() {
			public final void onRssiRead(@NonNull BluetoothDevice device, int rssi) {
				callback.invoke(rssi);
			}
		}).enqueue();
	}

	public void readCharacteristic(@NonNull BluetoothGattCharacteristic characteristic, @NonNull final Func1 callback) {
		TLog.d("");
		this.readCharacteristic(characteristic).with(new DataReceivedCallback() {
			public final void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
				callback.invoke(data);
			}
		}).enqueue();
	}

	public final void writeCharacteristic(@NonNull BluetoothGattCharacteristic characteristic, @NonNull Data writeData, @NonNull final Func1 callback) {
		TLog.d("");
		this.writeCharacteristic(characteristic, writeData).with(new DataSentCallback() {
			public final void onDataSent(@NonNull BluetoothDevice device, @NonNull Data data) {
				callback.invoke(data);
			}
		}).enqueue();
	}

	public final void enableNotificationCallBack(@NonNull BluetoothGattCharacteristic characteristic, @NonNull final Func1<Data, Object> callback) {
		TLog.d("");
		this.setNotificationCallback(characteristic).with(new DataReceivedCallback() {
			public final void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
				callback.invoke(data);
			}
		});
		this.enableNotifications(characteristic).enqueue();
	}

}
