package sensetime.senseme.com.effects.adapter;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import sensetime.senseme.com.effects.R;
import sensetime.senseme.com.effects.view.StickerItem;

public class RecyclerAdapter extends RecyclerView.Adapter{
    private ArrayList<StickerItem> mStickerList;
    private View.OnClickListener mOnClickStickerListener;
    private int mSelectedPosition = 0;

    public RecyclerAdapter(ArrayList<StickerItem> stickerList) {
        mStickerList = stickerList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, null);
        RecyclerViewHolder holder = new RecyclerViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((RecyclerViewHolder)holder).iv_sticker.setImageBitmap(mStickerList.get(position).icon);

        if(mOnClickStickerListener != null) {
            ((RecyclerViewHolder) holder).iv_sticker.setTag(position);
            ((RecyclerViewHolder) holder).iv_sticker.setOnClickListener(mOnClickStickerListener);

            holder.itemView.setSelected(mSelectedPosition == position);
        }
    }

    @Override
    public int getItemCount() {
        return mStickerList.size();
    }

    public void setClickStickerListener(View.OnClickListener listener) {
        mOnClickStickerListener = listener;
    }

    static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private ImageView iv_sticker;

        public RecyclerViewHolder(View itemView) {
            super(itemView);
            iv_sticker = (ImageView) itemView.findViewById(R.id.iv_sticker);
        }
    }

    public void setSelectedPosition(int position){
        mSelectedPosition = position;
    }

}
