package io.github.dumbooctopus.schooltracker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by neilprajapati on 6/3/18.
 */

public class ShiftDataArrayAdapter extends ArrayAdapter<ShiftData> {
    private Context mContext;
    private List<ShiftData> shiftDataList = new ArrayList<>();

    public ShiftDataArrayAdapter(@NonNull Context context, ArrayList<ShiftData> list) {
        super(context, 0 , list);
        mContext = context;
        shiftDataList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_shift_data,parent,false);

        ShiftData currentShiftData = shiftDataList.get(position);


        TextView location = (TextView) listItem.findViewById(R.id.location);
        location.setText(currentShiftData.getLocation());

        TextView startTime = (TextView) listItem.findViewById(R.id.start_time);
        startTime.setText(currentShiftData.getStartString());

        TextView duration = (TextView) listItem.findViewById(R.id.duration);
        duration.setText(currentShiftData.getDurationString());

        return listItem;
    }
}
