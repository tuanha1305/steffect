package sensetime.senseme.com.effects.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

import sensetime.senseme.com.effects.R;
import sensetime.senseme.com.effects.view.FilterItem;

public class FilterAdapter extends RecyclerView.Adapter {

    List<FilterItem> mFilterList;
    private View.OnClickListener mOnClickFilterListener;
    private int mSelectedPosition = 0;
    Context mContext;

    public FilterAdapter(List<FilterItem> list, Context context) {
        mFilterList = list;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.filter_item, null);
        return new FilterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final FilterViewHolder viewHolder = (FilterViewHolder) holder;
        viewHolder.imageView.setImageBitmap(mFilterList.get(position).icon);
        viewHolder.textView.setText(mFilterList.get(position).name);
        holder.itemView.setSelected(mSelectedPosition == position);
        if(mOnClickFilterListener != null) {
            holder.itemView.setTag(position);
            holder.itemView.setOnClickListener(mOnClickFilterListener);

            holder.itemView.setSelected(mSelectedPosition == position);
        }
    }

    public void setClickFilterListener(View.OnClickListener listener) {
        mOnClickFilterListener = listener;
    }

    @Override
    public int getItemCount() {
        return mFilterList.size();
    }

    static class FilterViewHolder extends RecyclerView.ViewHolder {

        View view;
        ImageView imageView;
        TextView textView;

        public FilterViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            imageView = (ImageView) itemView.findViewById(R.id.filter_image);
            textView = (TextView) itemView.findViewById(R.id.filter_text);
        }
    }

    public void setSelectedPosition(int position){
        mSelectedPosition = position;
    }
}
