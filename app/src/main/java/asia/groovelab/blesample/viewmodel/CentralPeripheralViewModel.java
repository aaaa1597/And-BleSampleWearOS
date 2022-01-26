package asia.groovelab.blesample.viewmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.observer.BondingObserver;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

import asia.groovelab.blesample.Tlog;
import asia.groovelab.blesample.ble.SampleBleManager;
import asia.groovelab.blesample.Functions.Func0;
import asia.groovelab.blesample.Functions.Func1;
import asia.groovelab.blesample.Functions.Func2;
import asia.groovelab.blesample.model.Item;
import asia.groovelab.blesample.model.Peripheral;
import asia.groovelab.blesample.model.Section;

public class CentralPeripheralViewModel extends ViewModel {
	private Peripheral			peripheral = null;
	private SampleBleManager	bleManager;

	private MutableLiveData<String>				appTitle = new MutableLiveData<>();
	private MutableLiveData<String>				address = new MutableLiveData<>();
	private MutableLiveData<String>				rssi = new MutableLiveData<>();
	private MutableLiveData<List<Section>>		sections = new MutableLiveData<>();
	private MutableLiveData<List<List<Item>>>	items = new MutableLiveData<>();
	private MutableLiveData<Integer>			progressBarVisibility = new MutableLiveData<>();

	private MutableLiveData<Boolean> reconnected = new MutableLiveData<>();
	public MutableLiveData<Boolean> getReconnected() { return reconnected; }
	private MutableLiveData<Boolean> disconnectedFromDevice = new MutableLiveData<>();
	public MutableLiveData<Boolean> getDisconnectedFromDevice() { return disconnectedFromDevice; }
	private Func2<UUID, byte[], Object> wroteCharacteristicHandler = null;
	public void setWroteCharacteristicHandler(Func2<UUID, byte[], Object> func) { wroteCharacteristicHandler = func; }
	private Func2<UUID, byte[], Integer> notifiedCharacteristicHandler = null;
	public void setNotifiedCharacteristicHandler(Func2<UUID, byte[], Integer> func) { notifiedCharacteristicHandler = func; }

	public void setContext(Context context) {
		bleManager = new SampleBleManager(context);
		bleManager.setConnectionObserver(new ConnectionObserver() {
			@Override
			public void onDeviceReady(@NonNull BluetoothDevice device) {
				bleManager.readRssi(new Func1<Integer, Object>() {
					@Override
					public Object invoke(Integer o) {
						rssi.postValue(o + "dbm");
						return null;
					}
				});
			}

			@Override
			public void onDeviceConnected(@NonNull BluetoothDevice device) {
				Tlog.d("");
				if (bleManager.isConnecting()) {
					if(peripheral == null) return;
					BluetoothDevice dev = peripheral.getBluetoothDevice();
					reconnected.postValue(true);
					bleManager.enqueueConnect(dev);
					return;
				}

				if (bleManager.wasConnected()) {
					disconnect(new Func0() {
						@Override
						public Object invoke() {
							disconnectedFromDevice.postValue(true);
							return null;
						}});
				}
			}

			@Override public void onDeviceConnecting(@NonNull BluetoothDevice device) { Tlog.d(""); }
			@Override public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) { Tlog.d(""); }
			@Override public void onDeviceDisconnecting(@NonNull BluetoothDevice device) { Tlog.d(""); }
			@Override public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) { Tlog.d(""); }
		});
		bleManager.setBondingObserver(new BondingObserver() {
			@Override public void onBondingRequired(@NonNull BluetoothDevice device) { }
			@Override public void onBonded(@NonNull BluetoothDevice device) { }
			@Override public void onBondingFailed(@NonNull BluetoothDevice device) {}
		});
		bleManager.setDiscoveredServicesHandler(new Func1<List<BluetoothGattService>, Object>() {
			@Override
			public Object invoke(List<BluetoothGattService> services) {
				List<Section> sectionList = services.stream().map(it -> new Section(it)).collect(Collectors.toList());
				sections.postValue(sectionList);

				List<List<Item>> itemList = services.stream().map(
					service -> service.getCharacteristics().stream().map(
							characteristic -> {
									bleManager.enableNotificationCallBack(characteristic, new Func1<Data, Object>() {
										@Override
										public Data invoke(Data data) {
											if(notifiedCharacteristicHandler != null)
												notifiedCharacteristicHandler.invoke(characteristic.getUuid(), data.getValue());
											return null;
										}
									});
									return new Item(characteristic);
							}
					).collect(Collectors.toList())
				).collect(Collectors.toList());
				items.postValue(itemList);
				progressBarVisibility.postValue(View.GONE);
				return null;
			}
		});
	}

	public void connect(Peripheral peripheral) {
		appTitle.postValue(peripheral.getLocalName());
		address.postValue(peripheral.getAddress());
		progressBarVisibility.postValue(View.VISIBLE);

		this.peripheral = peripheral;

		if(peripheral.getBluetoothDevice() != null)
			bleManager.enqueueConnect(peripheral.getBluetoothDevice());
	}

	public void disconnect(Func0<Object> doneHandler) {
		progressBarVisibility.postValue(View.VISIBLE);

		bleManager.enqueueDisconnect(new Func0<Object>() {
			@Override
			public Object invoke() {
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						doneHandler.invoke();
					}
				}, 200);
				return null;
			}
		});
	}

	public void readCharacteristic(Item item) {
		if (!item.getIsReadable())
			return;

		bleManager.readCharacteristic(item.getBluetoothGattCharacteristic(), new Func1<Data,Object>() {
			@Override
			public Object invoke(Data data) {
				if(data != null && data.getValue() != null) {
					byte[] readValue = data.getValue();
					Item litem = getItem(item.getBluetoothGattCharacteristic());
					if(litem != null) {
						litem.setReadValue(Arrays.toString(readValue));
						items.postValue(items.getValue());
					}
				}
				return null;
			}
		});
	}

	public void writeCharacteristic(Item item, String writeText) {
		if (!item.getIsWritable())
			return;

		String tmpstr = (writeText.length()%2==0) ? writeText : "0" + writeText;
		List<String> tmplist = new ArrayList<>();
		for(int idx = 0; idx < tmpstr.length(); idx+=2)
			tmplist.add(tmpstr.substring(idx,idx+2));

		List<Byte> tmpBytes = tmplist.stream().map(it -> (Byte)(byte)Integer.parseInt(it, 16)).collect(Collectors.toList());
		byte[] bytes = new byte[tmpBytes.size()];
		for (int idx = 0; idx < tmpBytes.size(); idx++)
			bytes[idx] = tmpBytes.get(idx);

		bleManager.writeCharacteristic(item.getBluetoothGattCharacteristic(), new Data(bytes), new Func1<Data, Object>() {
			@Override
			public Object invoke(Data data) {
				if(wroteCharacteristicHandler!=null)
					wroteCharacteristicHandler.invoke(item.getBluetoothGattCharacteristic().getUuid(), data.getValue());
				return null;
			}
		});
	}

	public Item getItem(Integer sectionPosition, Integer itemPosition) {
		if(items.getValue() == null || items.getValue().get(sectionPosition) == null)
			return null;
		return items.getValue().get(sectionPosition).get(itemPosition);
	}

	private Item getItem(BluetoothGattCharacteristic characteristic) {
		if(items == null || items.getValue() == null)
			return null;
		for(List<Item> item : items.getValue()) {
			Item findit = item.stream().filter(it -> it.getUuid().equals(characteristic.getUuid().toString())).findAny().orElse(null);
			if(findit != null)
				return findit;
		}
		return null;
	}
}
