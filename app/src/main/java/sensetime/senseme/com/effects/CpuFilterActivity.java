package sensetime.senseme.com.effects;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sensetime.stmobile.STCommon;
import com.sensetime.stmobile.STFilterParamsType;
import com.sensetime.stmobile.STMobileFilterNative;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sensetime.senseme.com.effects.adapter.FilterAdapter;
import sensetime.senseme.com.effects.glutils.STUtils;
import sensetime.senseme.com.effects.utils.FileUtils;
import sensetime.senseme.com.effects.utils.LogUtils;
import sensetime.senseme.com.effects.utils.STLicenseUtils;
import sensetime.senseme.com.effects.view.FilterItem;

public class CpuFilterActivity extends Activity implements View.OnClickListener{

    private final static String TAG = "CpuFilterActivity";
    private ImageView mImageView;
    private TextView mSavingTv;
    private int mImageWidth;
    private int mImageHeight;
    private STMobileFilterNative mSTMobileFilterNative = new STMobileFilterNative();

    private RecyclerView mFilterRecycleView;
    private FilterAdapter mFilterAdapter;
    private ArrayList<FilterItem> mFilterItem = null;
    private SeekBar mFilterSeekBar;

    private final int REQUEST_PICK_IMAGE = 1;
    private Context mContext;
    private Button mShowOriginBtn;
    private Bitmap mImageBitmap;
    private Bitmap mOriginImage;
    private String mCurrentFilterStyle;
    private boolean mNeedFilter = false;
    private float mCurrentFilterStrength = 0.5f;
    private byte[] mImageBuff = null;
    private boolean mPermissionDialogShowing = false;

    public static final int MSG_SAVING_IMG = 1;
    public static final int MSG_SAVED_IMG = 2;

    private static final int PERMISSION_REQUEST_WRITE_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpu_filter);
        mContext = this;

        initView();
        initEvent();
        initFilter();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_SAVING_IMG:
                    saveToSDCard(FileUtils.getOutputMediaFile(), mImageBitmap);
                    break;
                case MSG_SAVED_IMG:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mSavingTv.setVisibility(View.GONE);
                        }
                    });
            }
        }
    };

    private void initView(){
        STUtils.copyFilesToLocalIfNeed(this);
        FileUtils.copyModelFiles(this);

        mImageView = (ImageView) findViewById(R.id.iv_image);
        mSavingTv = (TextView) findViewById(R.id.tv_saving_filter_image);
        findViewById(R.id.tv_filter_capture).setOnClickListener(this);
        findViewById(R.id.tv_filter_cancel).setOnClickListener(this);

        mFilterRecycleView = (RecyclerView) findViewById(R.id.rv_cpu_filter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mFilterRecycleView.setLayoutManager(layoutManager);
        mFilterRecycleView.getBackground().setAlpha(100);

        mFilterItem = FileUtils.getFilterFiles(this);
        mFilterAdapter = new FilterAdapter(mFilterItem, this);

        mFilterRecycleView.setAdapter(mFilterAdapter);

        mFilterAdapter.setClickFilterListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = Integer.parseInt(v.getTag().toString());
                mFilterAdapter.setSelectedPosition(position);

                if(position == 0){
                    mNeedFilter = false;
                    mCurrentFilterStyle = null;
                    mFilterSeekBar.setEnabled(false);
                }else if(position == 1){
                    mNeedFilter = true;
                    mCurrentFilterStyle = null;
                    mFilterSeekBar.setEnabled(false);
                }else {
                    mNeedFilter = true;
                    mFilterSeekBar.setEnabled(true);
                    mCurrentFilterStyle = mFilterItem.get(position).model;
                }
                mFilterAdapter.notifyDataSetChanged();
                processImageAndRefresh();
            }
        });

        mFilterSeekBar = (SeekBar) findViewById(R.id.sb_cpu_filter_strength);
        mFilterSeekBar.setProgress(50);
        if(!mNeedFilter){
            mFilterSeekBar.setEnabled(false);
        }
        mFilterSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float param = 0.0f;
                param = (float)progress/100;
                mCurrentFilterStrength = param;
                processImageAndRefresh();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mShowOriginBtn = (Button)findViewById(R.id.tv_cpu_filter_show_origin);
        mShowOriginBtn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mNeedFilter = false;
                    processImageAndRefresh();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mNeedFilter = true;
                    processImageAndRefresh();
                }
                return true;
            }
        });
        mShowOriginBtn.setVisibility(View.VISIBLE);

    }

    private void initEvent(){
        if (!STLicenseUtils.checkLicense(mContext)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "You should be authorized first!", Toast.LENGTH_SHORT).show();
                }
            });
            finish();
            return;
        }

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_PICK_IMAGE);
    }

    private void processImageAndRefresh(){
        byte[] outputImageBuff = new byte[mImageHeight*mImageWidth*3];
        long startTime = System.currentTimeMillis();

        //滤镜
        if(mNeedFilter && mImageBuff != null && mImageBuff.length >0){
            mSTMobileFilterNative.setStyle(mCurrentFilterStyle);
            mSTMobileFilterNative.setParam(STFilterParamsType.ST_FILTER_STRENGTH, mCurrentFilterStrength);
            int ret = mSTMobileFilterNative.process(mImageBuff, STCommon.ST_PIX_FMT_BGR888, mImageWidth, mImageHeight,
                    outputImageBuff, STCommon.ST_PIX_FMT_BGR888);
            LogUtils.i(TAG, "filter process image cost: "+ (System.currentTimeMillis()-startTime));
            if(ret == 0){
                mImageBitmap = STUtils.getBitmapFromBGR(outputImageBuff, mImageWidth, mImageHeight);
            }

            mImageView.setImageBitmap(mImageBitmap);
        }else{
            mImageView.setImageBitmap(mOriginImage);
        }
    }

    private void initFilter(){
        mSTMobileFilterNative.createInstance();

        mCurrentFilterStyle = null;
        mSTMobileFilterNative.setStyle(mCurrentFilterStyle);

        mSTMobileFilterNative.setParam(STFilterParamsType.ST_FILTER_STRENGTH, mCurrentFilterStrength);
    }

    private void releaseFilter(){
        mSTMobileFilterNative.destroyInstance();
    }

    private boolean isWritePermissionAllowed() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    private void requestWritePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            mPermissionDialogShowing = true;
            this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_WRITE_PERMISSION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_WRITE_PERMISSION) {
            mPermissionDialogShowing = false;
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.onClick(findViewById(R.id.tv_capture));
            }
        }
    }

    private void saveToSDCard(File file, Bitmap bmp) {

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bos != null)
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        if (mHandler != null) {
            String path = file.getAbsolutePath();
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);

            if (Build.VERSION.SDK_INT >= 19) {

                MediaScannerConnection.scanFile(this, new String[]{path}, null, null);
            }

            mHandler.sendEmptyMessage(this.MSG_SAVED_IMG);
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.tv_filter_cancel:
                finish();
                break;

            case R.id.tv_filter_capture:
                if (this.isWritePermissionAllowed()) {
                    mSavingTv.setVisibility(View.VISIBLE);
                    mHandler.sendEmptyMessage(this.MSG_SAVING_IMG);
                } else {
                    requestWritePermission();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        releaseFilter();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri uri = data.getData();
                        Bitmap bitmap = null;
                        if("file".equals(uri.getScheme())){
                            bitmap = STUtils.getBitmapFromFile(uri);
                        } else {
                            bitmap  = STUtils.getBitmapAfterRotate(uri, mContext);
                        }
                        if(bitmap != null) {
                            mOriginImage = bitmap;
                            mImageBitmap = bitmap;
                            mImageHeight = bitmap.getHeight();
                            mImageWidth = bitmap.getWidth();
                            mImageBuff = STUtils.getBGRFromBitmap(mOriginImage);
                            mImageView.setImageBitmap(mImageBitmap);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

}
