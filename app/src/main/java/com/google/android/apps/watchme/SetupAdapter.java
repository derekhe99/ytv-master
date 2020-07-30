package com.google.android.apps.watchme;

import android.app.Activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class SetupAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final String[] maintitle;
    private final String[] subtitle;
    private final boolean[] checks;

    public SetupAdapter(Activity context, String[] maintitle, String[] subtitle, boolean[] checks) {
        super(context, R.layout.front_list, maintitle);
        // TODO Auto-generated constructor stub

        this.context=context;
        this.maintitle=maintitle;
        this.subtitle=subtitle;
        this.checks=checks;

    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.setup_list, null,true);

        TextView titleText = (TextView) rowView.findViewById(R.id.title);
        TextView subtitleText = (TextView) rowView.findViewById(R.id.subtitle);
        CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.check);

        titleText.setText(maintitle[position]);
        subtitleText.setText(subtitle[position]);
        checkBox.setChecked(checks[position]);

        return rowView;

    };
}