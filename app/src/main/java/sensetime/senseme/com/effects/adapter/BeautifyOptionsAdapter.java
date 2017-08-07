package sensetime.senseme.com.effects.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SeekBar;
import android.widget.TextView;

import sensetime.senseme.com.effects.R;
import sensetime.senseme.com.effects.display.CameraDisplay;
import sensetime.senseme.com.effects.display.ImageDisplay;

/**
 * @author evilyin(ChenZhixi)
 * @since 2016/12/29
 */
public class BeautifyOptionsAdapter extends BaseAdapter {

    private Context mContext;
    private CameraDisplay mCameraDisplay;
    private ImageDisplay mImageDisplay;
    private float[] mBeautifyParams = new float[6];
    private boolean mIsBeautifyOpen = false;
    private boolean mIsFirstOpen = true;
    String[] optionsName = {
            "红润",
            "磨皮",
            "美白",
            "大眼",
            "瘦脸",
            "小脸"
    };

    public BeautifyOptionsAdapter(Context context, CameraDisplay cameraDisplay, ImageDisplay imageDisplay){
        this.mContext = context;
        this.mCameraDisplay = cameraDisplay;
        this.mImageDisplay = imageDisplay;
    }

    @Override
    public int getCount() {
        return optionsName.length;
    }

    @Override
    public Object getItem(int position) {
        return optionsName[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setBeautifyParams(float[] values){
        for(int i =0; i< values.length; i++){
            mBeautifyParams[i] = values[i];
        }
        mIsBeautifyOpen = true;
        mIsFirstOpen = true;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.beautify_options_item, null);
        }
        SeekBar optionBar = (SeekBar) convertView.findViewById(R.id.seek_bar_option);

        if(mIsFirstOpen){
            optionBar.setProgress((int)(mBeautifyParams[position]*100));

            if(mCameraDisplay != null){
                mCameraDisplay.setBeautyParam(position, mBeautifyParams[position]);
            }
            if(mImageDisplay != null){
                mImageDisplay.setBeautyParam(position, mBeautifyParams[position]);
            }
        }
        optionBar.setEnabled(mIsBeautifyOpen);

        final TextView optionText = (TextView) convertView.findViewById(R.id.text_option);
        optionText.setText(optionsName[position]);
        if((int)(mBeautifyParams[position]*100) <1){
            optionText.setTextColor(Color.GRAY);
        }

        final TextView beautyStrength = (TextView) convertView.findViewById(R.id.tv_strength);
        beautyStrength.setText((int)(mBeautifyParams[position]*100)+"");

        optionBar.setMax(100);
        optionBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = (float) progress / 100;
                if(!mIsFirstOpen){
                    if(mCameraDisplay != null){
                        mCameraDisplay.setBeautyParam(position, value);
                        mBeautifyParams[position] = value;
                    }
                    if(mImageDisplay != null){
                        mImageDisplay.setBeautyParam(position, value);
                        mBeautifyParams[position] = value;
                    }
                }

                if(progress < 1){
                    optionText.setTextColor(Color.GRAY);
                }else{
                    optionText.setTextColor(Color.WHITE);
                }

                beautyStrength.setText((int)(mBeautifyParams[position]*100)+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsFirstOpen = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        return convertView;
    }

    public void closeBeautify(){
        for(int i = 0; i< 6; i++){
            mBeautifyParams[i] = 0.0f;
        }
        mIsBeautifyOpen = false;
        mIsFirstOpen = true;
    }

    public void OpenBeautify(){
        mIsBeautifyOpen = true;
        mIsFirstOpen = true;
    }

}
