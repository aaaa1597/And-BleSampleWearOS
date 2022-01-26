package asia.groovelab.blesample.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import asia.groovelab.blesample.R;
import asia.groovelab.blesample.model.Item;
import asia.groovelab.blesample.model.Section;

public class ItemListAdapter extends BaseExpandableListAdapter {
	private LayoutInflater mInflater;
	private List<Section> sections = new ArrayList<>();
	private List<List<Item>> items = new ArrayList<>();

	public ItemListAdapter(Context context) {
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public int getGroupCount() {
		return sections.size();
	}

	@Override
	public Object getGroup(int pos) {
		return sections.get(pos);
	}

	@Override
	public View getGroupView(int grppos, boolean isExpanded, View view, ViewGroup parent) {
		if(view == null)
			view = mInflater.inflate(R.layout.row_group, parent, true);

		TextView textview = view.findViewById(R.id.title_text_view);
		textview.setText(sections.get(grppos).getTitle());
		return view;
	}

	@Override
	public int getChildrenCount(int grpidx) {
		if(grpidx >= items.size()) return 0;
		List<Item> itemss = items.get(grpidx);
		if(itemss == null) return 0;
		return itemss.size();
	}

	@Override
	public Item getChild(int grpidx, int childidx) {
		return items.get(grpidx).get(childidx);
	}

	@Override
	public View getChildView(int grpidx, int childidx, boolean isExpanded, View view, ViewGroup parent) {
		if(view == null)
			view = mInflater.inflate(R.layout.row_child, parent, true);

		Item item = items.get(grpidx).get(childidx);
		((TextView)view.findViewById(R.id.uuid_text_view)).setText(item.getUuid());
		((TextView)view.findViewById(R.id.read_value_text_view)).setText(item.getReadValue());
		((TextView)view.findViewById(R.id.readable_text_view)).setTextColor(item.getReadableColorRes());
		((TextView)view.findViewById(R.id.writable_text_view)).setTextColor(item.getWritableColorRes());
		((TextView)view.findViewById(R.id.notifiable_text_view)).setTextColor(item.getNotifiableColorRes());
		return view;
	}

	@Override
	public boolean isChildSelectable(int grpidx, int childidx) {
		return true;
	}

	@Override public long getGroupId(int i) { return 0; }
	@Override public long getChildId(int i, int i1) { return 0; }
	@Override public boolean hasStableIds() { return false; }
}
