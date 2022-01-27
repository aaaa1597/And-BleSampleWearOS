package asia.groovelab.blesample.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
	public enum Action {
		Central,
		Peripheral,
		None
	};

	private MutableLiveData<Action> mAction = new MutableLiveData<>(Action.None);
	public LiveData<Action> action() { return mAction;}

	public void onClickCentralButton() {
		mAction.postValue(Action.Central);
	}
	public void onClickPeripheralButton() {
		mAction.postValue(Action.Peripheral);
	}
}
