package buu.com.heartrate.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import buu.com.heartrate.R;

/**
 * Created by Administrator on 2015/9/25 0025.
 */
public class BtAdapter extends BaseAdapter{

    private Context context;
    private List<String> listDevices;

    public BtAdapter(Context context, List<String> listDevices){
        this.context = context;
        this.listDevices = listDevices;
    }

    @Override
    public int getCount() {
        return listDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {

        ViewHolder holder;
        if(convertView==null){
            holder = new ViewHolder();
            convertView = View.inflate(context, R.layout.list_item,null);
            holder.deviceName = (TextView) convertView.findViewById(R.id.device_name_tv);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder)convertView.getTag();
        }
        holder.deviceName.setText(listDevices.get(position));


        return convertView;
    }

    class ViewHolder{
        TextView deviceName;


    }
}
