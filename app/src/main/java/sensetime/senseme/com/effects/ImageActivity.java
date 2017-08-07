package sensetime.senseme.com.effects;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import sensetime.senseme.com.effects.adapter.BeautifyOptionsAdapter;
import sensetime.senseme.com.effects.adapter.FilterAdapter;
import sensetime.senseme.com.effects.adapter.RecyclerAdapter;
import sensetime.senseme.com.effects.adapter.StickerAdapter;
import sensetime.senseme.com.effects.display.ImageDisplay;
import sensetime.senseme.com.effects.glutils.STUtils;
import sensetime.senseme.com.effects.utils.Accelerometer;
import sensetime.senseme.com.effects.utils.FileUtils;
import sensetime.senseme.com.effects.utils.LogUtils;
import sensetime.senseme.com.effects.utils.STLicenseUtils;
import sensetime.senseme.com.effects.view.FilterItem;
import sensetime.senseme.com.effects.view.StickerItem;

public class ImageActivity extends Activity implements View.OnClickListener {
    private final static String TAG = "ImageActivity";
    private Accelerometer mAccelerometer = null;
    private ImageDisplay mImageDisplay;
    private FrameLayout mPreviewFrameLayout;

    private RecyclerView mStickersRecycleView;
    private StickerAdapter mStickerAdapter;
    private ArrayList<StickerItem> mStickerList;

    private FilterAdapter mFilterAdapter;
    private ArrayList<FilterItem> mFilterItem = null;

    private ListView mBeautifyListView;
    private BeautifyOptionsAdapter mBeautifyAdapter;

    private TextView mSavingTv;
    private TextView mAttributeText;

    private boolean mIsAttributeOpen = false;
    private boolean mIsBeautifyOpen = true;
    private boolean mIsStickerOpen = false;
    private boolean mIsFilterOpen = false;
    private boolean mShowOrigin = false;
    private LinearLayout mOptions, mAttributeSwicth, mBeautifySwicth, mStickerSwicth;
    private LinearLayout mFaceAttribute, mBeautify, mSticker, mFilter;
    private LinearLayout mFilterStrength;
    private SeekBar mFilterSeekBar;
    private TextView mAttributeLayoutSwicth;
    private TextView mAttributeOpen, mAttributeClose;
    private TextView mBeautifyLayoutSwicth;
    private TextView mBeautifyOpen, mBeautifyClose;
    private TextView mStickerLayoutSwicth;
    private Button mShowOriginBtn;
    private Context mContext;
    private Bitmap mImageBitmap;
    private float[] mBeautifyParams = {0.36f, 0.74f, 0.30f, 0.13f, 0.11f, 0.1f};

    public static final int MSG_SAVING_IMG = 1;
    public static final int MSG_SAVED_IMG = 2;

    private static final int PERMISSION_REQUEST_WRITE_PERMISSION = 101;
    private boolean mPermissionDialogShowing = false;
    private final int REQUEST_PICK_IMAGE = 1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_SAVING_IMG:
                    mImageBitmap = mImageDisplay.getBitmap();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mContext = this;

        initView();
        initEvents();
    }

    private void initView() {

        //copy model file to sdcard
        FileUtils.copyModelFiles(this);

        mAccelerometer = new Accelerometer(getApplicationContext());

        GLSurfaceView glSurfaceView = (GLSurfaceView) findViewById(R.id.id_gl_sv);
        mPreviewFrameLayout = (FrameLayout) findViewById(R.id.id_preview_layout);

        mImageDisplay = new ImageDisplay(getApplicationContext(), glSurfaceView);

        //layout elements
        mStickersRecycleView = (RecyclerView) findViewById(R.id.rv_stickers);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mStickersRecycleView.setLayoutManager(layoutManager);
        mStickersRecycleView.addItemDecoration(new SpaceItemDecoration(0));
        mStickersRecycleView.getBackground().setAlpha(100);

        //copy sticker zips to sdcard and get all file paths
        mStickerList = FileUtils.getStickerFiles(this);
        mStickerAdapter = new StickerAdapter(mStickerList, this);

        //copy filter models to sdcard and get all file paths
        mFilterItem = FileUtils.getFilterFiles(this);
        mFilterAdapter = new FilterAdapter(mFilterItem, this);

        mBeautifyListView = (ListView) findViewById(R.id.list_beautify_options);
        mBeautifyAdapter = new BeautifyOptionsAdapter(this, null, mImageDisplay);

        mSavingTv = (TextView) findViewById(R.id.tv_saving_image);


        mOptions = (LinearLayout) findViewById(R.id.layout_options);
        mAttributeSwicth = (LinearLayout) findViewById(R.id.layout_attribute_swicth);
        mFaceAttribute = (LinearLayout) findViewById(R.id.ll_attribute);
        mFaceAttribute.setOnClickListener(this);
        mBeautify = (LinearLayout) findViewById(R.id.ll_beauty);
        mBeautify.setOnClickListener(this);

        mAttributeLayoutSwicth = (TextView) findViewById(R.id.tv_attribute_layout_swicth);
        mAttributeLayoutSwicth.setOnClickListener(this);
        mAttributeOpen = (TextView) findViewById(R.id.tv_attribute_open);
        mAttributeOpen.setOnClickListener(this);
        mAttributeClose = (TextView) findViewById(R.id.tv_attribute_close);
        mAttributeClose.setOnClickListener(this);

        mBeautifySwicth = (LinearLayout) findViewById(R.id.layout_beauty_swicth);
        mBeautifyLayoutSwicth = (TextView) findViewById(R.id.tv_beautify_layout_swicth);
        mBeautifyLayoutSwicth.setOnClickListener(this);
        mBeautifyOpen = (TextView) findViewById(R.id.tv_beautify_open);
        mBeautifyOpen.setOnClickListener(this);
        mBeautifyClose = (TextView) findViewById(R.id.tv_beautify_close);
        mBeautifyClose.setOnClickListener(this);

        mSticker = (LinearLayout) findViewById(R.id.ll_sticker);
        mSticker.setOnClickListener(this);

        mStickerLayoutSwicth = (TextView) findViewById(R.id.tv_sticker_layout_swicth);
        mStickerLayoutSwicth.setOnClickListener(this);

        mFilter = (LinearLayout) findViewById(R.id.ll_filter);
        mFilter.setOnClickListener(this);

        mFilterStrength = (LinearLayout) findViewById(R.id.ll_filter_strength);
        mFilterSeekBar = (SeekBar) findViewById(R.id.sb_filter_strength);

        mAttributeText = (TextView) findViewById(R.id.tv_face_attribute);
        mAttributeText.setVisibility(View.VISIBLE);

    }

    ///////////////////////////////////////
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
                        if(bitmap != null && mImageDisplay != null) {
                            mImageDisplay.setImageBitmap(bitmap);
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

    private void initEvents() {
        // authority
        if (!STLicenseUtils.checkLicense(ImageActivity.this)) {
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
        // change sticker
        mStickerAdapter.setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int position = Integer.parseInt(v.getTag().toString());
                mStickerAdapter.setSelectedPosition(position);

                if(position == 0){
                    mIsStickerOpen = false;
                    mImageDisplay.enableSticker(false);
                }else if(position == 1){
                    mImageDisplay.enableSticker(true);
                    mImageDisplay.setShowSticker(null);
                    mIsStickerOpen = true;
                }else{
                    mImageDisplay.enableSticker(true);
                    mImageDisplay.setShowSticker(mStickerList.get(position).path);
                    mIsStickerOpen = true;
                }

                mStickerAdapter.notifyDataSetChanged();
            }
        });

        mFilterAdapter.setClickFilterListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = Integer.parseInt(v.getTag().toString());
                mFilterAdapter.setSelectedPosition(position);

                if(position == 0){
                    mIsFilterOpen = false;
                    mImageDisplay.enableFilter(false);
                    mFilterSeekBar.setEnabled(false);
                }else if(position == 1){
                    mImageDisplay.setFilterStyle(null);
                    mIsFilterOpen = true;
                    mImageDisplay.enableFilter(true);
                    mFilterSeekBar.setEnabled(false);
                }else {
                    mImageDisplay.setFilterStyle(mFilterItem.get(position).model);
                    mIsFilterOpen = true;
                    mImageDisplay.enableFilter(true);
                    mFilterSeekBar.setEnabled(true);
                }

                mFilterAdapter.notifyDataSetChanged();
            }
        });

        mFilterSeekBar.setProgress(50);
        if(!mIsFilterOpen){
            mFilterSeekBar.setEnabled(false);
        }
        mFilterSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float param = 0;
                param = (float)progress/100;
                mImageDisplay.setFilterStrength(param);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mShowOriginBtn = (Button)findViewById(R.id.tv_show_origin);
        mShowOriginBtn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mImageDisplay.setShowOriginal(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mImageDisplay.setShowOriginal(false);
                }
                return true;
            }
        });
        mShowOriginBtn.setVisibility(View.VISIBLE);

        mImageDisplay.setCostChangeListener(new ImageDisplay.CostChangeListener() {
            @Override
            public void onCostChanged(final int value) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) findViewById(R.id.tv_frame_radio)).setText(String.valueOf(value));
                    }
                });
            }
        });

        findViewById(R.id.tv_smaller_resolution).setVisibility(View.GONE);
        findViewById(R.id.tv_bigger_resolution).setVisibility(View.GONE);
        findViewById(R.id.ll_cpu_radio).setVisibility(View.GONE);
        findViewById(R.id.tv_layout_tips).setVisibility(View.GONE);
        mBeautify.setOnClickListener(this);
        findViewById(R.id.tv_capture).setOnClickListener(this);
        findViewById(R.id.tv_cancel).setOnClickListener(this);

        mIsBeautifyOpen = true;
        mImageDisplay.enableBeautify(true);
        mBeautifyOpen.setBackgroundColor(Color.parseColor("#fe5553"));
        mBeautifyClose.setBackgroundColor(Color.alpha(0));
        mBeautifyAdapter.OpenBeautify();
        mBeautifyAdapter.notifyDataSetChanged();

        mBeautify.setBackgroundColor(Color.parseColor("#fe5553"));
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_attribute:
                mBeautify.setEnabled(false);
                mSticker.setEnabled(false);
                mFilter.setEnabled(false);

                mOptions.setVisibility(View.INVISIBLE);
                mShowOriginBtn.setVisibility(View.INVISIBLE);
                mAttributeSwicth.setVisibility(View.VISIBLE);
                if(mIsAttributeOpen){
                    mAttributeOpen.setBackgroundColor(Color.parseColor("#fe5553"));
                    mAttributeClose.setBackgroundColor(Color.alpha(0));
                }else{
                    mAttributeOpen.setBackgroundColor(Color.alpha(0));
                    mAttributeClose.setBackgroundColor(Color.parseColor("#fe5553"));
                }
                mAttributeLayoutSwicth.setVisibility(View.VISIBLE);

                break;

            case R.id.tv_attribute_open:
                mIsAttributeOpen = true;
                mImageDisplay.enableFaceAttribute(true);
                mAttributeText.setVisibility(View.VISIBLE);
                mAttributeOpen.setBackgroundColor(Color.parseColor("#fe5553"));
                mAttributeClose.setBackgroundColor(Color.alpha(0));
                String str= mImageDisplay.getFaceAttributeString();
                Log.e(TAG, "onClick: "+str );
                mAttributeText.setText(str);
                mAttributeText.setVisibility(View.VISIBLE);
                break;

            case R.id.tv_attribute_close:
                mIsAttributeOpen = false;
                mImageDisplay.enableFaceAttribute(false);
                mAttributeText.setVisibility(View.INVISIBLE);
                mAttributeOpen.setBackgroundColor(Color.alpha(0));
                mAttributeClose.setBackgroundColor(Color.parseColor("#fe5553"));
                break;

            case R.id.tv_attribute_layout_swicth:
                enableSwicthButton();

                mAttributeSwicth.setVisibility(View.INVISIBLE);
                mAttributeLayoutSwicth.setVisibility(View.INVISIBLE);
                mOptions.setVisibility(View.VISIBLE);
                if(mIsAttributeOpen){
                    mFaceAttribute.setBackgroundColor(Color.parseColor("#fe5553"));
                }else {
                    mFaceAttribute.setBackgroundColor(Color.alpha(0));
                }

                mShowOriginBtn.setVisibility(View.VISIBLE);
                break;

            case R.id.ll_beauty:
                mFaceAttribute.setEnabled(false);
                mSticker.setEnabled(false);
                mFilter.setEnabled(false);

                mOptions.setVisibility(View.INVISIBLE);
                mShowOriginBtn.setVisibility(View.INVISIBLE);
                mBeautifySwicth.setVisibility(View.VISIBLE);
                if(mIsBeautifyOpen){
                    mBeautifyOpen.setBackgroundColor(Color.parseColor("#fe5553"));
                    mBeautifyClose.setBackgroundColor(Color.alpha(0));
                    mBeautifyAdapter.setBeautifyParams(mBeautifyParams);
                }else{
                    mBeautifyOpen.setBackgroundColor(Color.alpha(0));
                    mBeautifyClose.setBackgroundColor(Color.parseColor("#fe5553"));
                }

                mBeautifyAdapter.notifyDataSetChanged();

                mBeautifyListView.setVisibility(View.VISIBLE);
                mBeautifyListView.setAdapter(mBeautifyAdapter);
                mBeautifyLayoutSwicth.setVisibility(View.VISIBLE);

                break;

            case R.id.tv_beautify_open:
                mIsBeautifyOpen = true;
                mImageDisplay.enableBeautify(true);
                mBeautifyAdapter.setBeautifyParams(mBeautifyParams);

                mBeautifyOpen.setBackgroundColor(Color.parseColor("#fe5553"));
                mBeautifyOpen.setEnabled(false);
                mBeautifyClose.setBackgroundColor(Color.alpha(0));
                mBeautifyClose.setEnabled(true);
                mBeautifyAdapter.OpenBeautify();
                mBeautifyAdapter.notifyDataSetChanged();
                break;

            case R.id.tv_beautify_close:
                mIsBeautifyOpen = false;
                mImageDisplay.enableBeautify(false);
                mBeautifyParams = mImageDisplay.getBeautyParams();

                mBeautifyOpen.setBackgroundColor(Color.alpha(0));
                mBeautifyOpen.setEnabled(true);
                mBeautifyClose.setBackgroundColor(Color.parseColor("#fe5553"));
                mBeautifyClose.setEnabled(false);
                mBeautifyAdapter.closeBeautify();
                mBeautifyAdapter.notifyDataSetChanged();
                break;

            case R.id.tv_beautify_layout_swicth:
                enableSwicthButton();

                if(mIsBeautifyOpen){
                    mBeautifyParams = mImageDisplay.getBeautyParams();
                }
                mBeautifySwicth.setVisibility(View.INVISIBLE);
                mBeautifyLayoutSwicth.setVisibility(View.INVISIBLE);
                mBeautifyListView.setVisibility(View.INVISIBLE);
                mOptions.setVisibility(View.VISIBLE);
                mShowOriginBtn.setVisibility(View.VISIBLE);

                if(mIsBeautifyOpen){
                    mBeautify.setBackgroundColor(Color.parseColor("#fe5553"));
                }else {
                    mBeautify.setBackgroundColor(Color.alpha(0));
                }
                break;

            case R.id.ll_sticker:
                mFaceAttribute.setEnabled(false);
                mBeautify.setEnabled(false);
                mFilter.setEnabled(false);

                mOptions.setVisibility(View.INVISIBLE);
                mShowOriginBtn.setVisibility(View.INVISIBLE);
                mStickersRecycleView.setVisibility(View.VISIBLE);
                mStickersRecycleView.setAdapter(mStickerAdapter);
                mFilterStrength.setVisibility(View.GONE);
                mStickerLayoutSwicth.setVisibility(View.VISIBLE);

                break;

            case R.id.ll_filter:
                mFaceAttribute.setEnabled(false);
                mBeautify.setEnabled(false);
                mSticker.setEnabled(false);

                mOptions.setVisibility(View.INVISIBLE);
                mShowOriginBtn.setVisibility(View.INVISIBLE);
                mStickersRecycleView.setVisibility(View.VISIBLE);
                mStickersRecycleView.setAdapter(mFilterAdapter);
                mStickerLayoutSwicth.setVisibility(View.VISIBLE);
                mFilterStrength.setVisibility(View.VISIBLE);
                break;

            case R.id.tv_sticker_layout_swicth:
                enableSwicthButton();
                mStickerLayoutSwicth.setVisibility(View.INVISIBLE);
                mStickersRecycleView.setVisibility(View.INVISIBLE);
                mOptions.setVisibility(View.VISIBLE);
                mShowOriginBtn.setVisibility(View.VISIBLE);
                mFilterStrength.setVisibility(View.INVISIBLE);

                if(mIsStickerOpen){
                    mSticker.setBackgroundColor(Color.parseColor("#fe5553"));
                }else {
                    mSticker.setBackgroundColor(Color.alpha(0));
                }

                if(mIsFilterOpen){
                    mFilter.setBackgroundColor(Color.parseColor("#fe5553"));
                }else {
                    mFilter.setBackgroundColor(Color.alpha(0));
                }

                break;

            case R.id.tv_capture:
                if (this.isWritePermissionAllowed()) {
                    mSavingTv.setVisibility(View.VISIBLE);
                    mImageDisplay.setHandler(mHandler);
                    mImageDisplay.enableSave(true);
                } else {
                    requestWritePermission();
                }
                break;
            case R.id.tv_cancel:
                // back to welcome page
                finish();
                break;

            default:
                break;
        }
    }

    // 分隔间距 继承RecyclerView.ItemDecoration
    class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            if (parent.getChildAdapterPosition(view) != 0) {
                outRect.top = space;
            }
        }
    }

    @Override
    protected void onResume() {
        LogUtils.i(TAG, "onResume");
        super.onResume();
        mAccelerometer.start();
        mImageDisplay.onResume();
    }

    @Override
    protected void onPause() {
        LogUtils.i(TAG, "onPause");
        super.onPause();
        if (!mPermissionDialogShowing) {
            mAccelerometer.stop();
            mImageDisplay.onPause();
            //finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageDisplay.onDestroy();
    }

    private void onPictureTaken(ByteBuffer data, File file, int mImageWidth, int mImageHeight) {
        if (mImageWidth <= 0 || mImageHeight <= 0)
            return;
        Bitmap srcBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        data.position(0);
        srcBitmap.copyPixelsFromBuffer(data);
        saveToSDCard(file, srcBitmap);
        srcBitmap.recycle();
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


    void showFaceAttributeInfo() {
        if (mImageDisplay.getFaceAttributeString() != null) {
            if (mImageDisplay.getFaceAttributeString().equals("noFace")) {
                mAttributeText.setText("");
            } else {
                mAttributeText.setText("第一张人脸： " + mImageDisplay.getFaceAttributeString());
            }
        }
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

    private void enableSwicthButton(){
        mFaceAttribute.setEnabled(true);
        mBeautify.setEnabled(true);
        mSticker.setEnabled(true);
        mFilter.setEnabled(true);
    }
}
