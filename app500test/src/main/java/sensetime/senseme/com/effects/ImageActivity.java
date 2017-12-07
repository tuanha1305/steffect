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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tatata.hearst.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import sensetime.senseme.com.effects.adapter.BeautyOptionsAdapter;
import sensetime.senseme.com.effects.adapter.FilterAdapter;
import sensetime.senseme.com.effects.adapter.StickerAdapter;
import sensetime.senseme.com.effects.adapter.StickerOptionsAdapter;
import sensetime.senseme.com.effects.display.ImageDisplay;
import sensetime.senseme.com.effects.glutils.STUtils;
import sensetime.senseme.com.effects.utils.Accelerometer;
import sensetime.senseme.com.effects.utils.FileUtils;
import sensetime.senseme.com.effects.utils.LogUtils;
import sensetime.senseme.com.effects.utils.STLicenseUtils;
import sensetime.senseme.com.effects.view.BeautyOptionsItem;
import sensetime.senseme.com.effects.view.FilterItem;
import sensetime.senseme.com.effects.view.StickerItem;
import sensetime.senseme.com.effects.view.StickerOptionsItem;

public class ImageActivity extends Activity implements View.OnClickListener {
    private final static String TAG = "ImageActivity";
    private Accelerometer mAccelerometer = null;
    private ImageDisplay mImageDisplay;
    private FrameLayout mPreviewFrameLayout;

    private RecyclerView mStickersRecycleView;
    private RecyclerView mStickerOptionsRecycleView, mFilterOptionsRecycleView;
    private StickerOptionsAdapter mStickerOptionsAdapter;
    private BeautyOptionsAdapter mBeautyOptionsAdapter;
    private ArrayList<StickerOptionsItem> mStickerOptionsList;
    private ArrayList<BeautyOptionsItem> mBeautyOptionsList;

    private HashMap<String, StickerAdapter> mStickerAdapters = new HashMap<>();
    private HashMap<String, ArrayList<StickerItem>> mStickerlists = new HashMap<>();


    private FilterAdapter mFilterAdapter;
    private ArrayList<FilterItem> mFilterItem = null;

    private TextView mSavingTv;
    private TextView mAttributeText;
    private TextView mShowOriginBtn1, mShowOriginBtn2;

    private Context mContext;
    private Bitmap mImageBitmap;
    public static float[] DEFAULT_BEAUTIFY_PARAMS = {0.36f, 0.74f, 0.30f, 0.13f, 0.11f, 0.1f};
    private float[] mBeautifyParams = new float[6];

    public static final int MSG_SAVING_IMG = 1;
    public static final int MSG_SAVED_IMG = 2;

    private static final int PERMISSION_REQUEST_WRITE_PERMISSION = 101;
    private boolean mPermissionDialogShowing = false;
    private final int REQUEST_PICK_IMAGE = 1;

    private LinearLayout mStickerOptionsSwitch, mStickerOptions;
    private RecyclerView mStickerIcons;
    private boolean mIsStickerOptionsOpen = false;
    private int mCurrentStickerOptionsIndex = -1;
    private int mCurrentStickerPosition = -1;

    private LinearLayout mBeautyOptionsSwitch, mBaseBeautyOptions, mProfessionalBeautyOptions;
    private RecyclerView mFilterIcons, mBeautyOptionsRecycleView;
    private boolean mIsBeautyOptionsOpen = false;
    private int mBeautyOptionsPosition = 0;
    private ArrayList<SeekBar> mBeautyParamsSeekBarList = new ArrayList<SeekBar>();

    private ImageView mBeautyOptionsSwitchIcon, mStickerOptionsSwitchIcon;
    private TextView mBeautyOptionsSwitchText, mStickerOptionsSwitchText;

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

        //进程后台时被系统强制kill，需重新checkLicense
        if(savedInstanceState!=null && savedInstanceState.getBoolean("process_killed")) {
            if (!STLicenseUtils.checkLicense(this)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "请检查License授权！", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

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

        //copy filter models to sdcard and get all file paths
        mFilterItem = FileUtils.getFilterFiles(this, "filter");
        mFilterAdapter = new FilterAdapter(mFilterItem, this);


        mStickerOptionsRecycleView = (RecyclerView) findViewById(R.id.rv_sticker_options);
        mStickerOptionsRecycleView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL));
        mStickerOptionsRecycleView.addItemDecoration(new ImageActivity.SpaceItemDecoration(0));
        mStickerOptionsRecycleView.getBackground().setAlpha(240);

        //layout elements
        mStickersRecycleView = (RecyclerView) findViewById(R.id.rv_sticker_icons);

        mStickersRecycleView.setLayoutManager(new GridLayoutManager(this, 6));
        mStickersRecycleView.addItemDecoration(new ImageActivity.SpaceItemDecoration(0));
        mStickersRecycleView.getBackground().setAlpha(220);

        mStickerlists.put("sticker_2d", FileUtils.getStickerFiles(this, "2D"));
        mStickerlists.put("sticker_3d", FileUtils.getStickerFiles(this, "3D"));
        mStickerlists.put("sticker_hand_action", FileUtils.getStickerFiles(this, "hand_action"));
        mStickerlists.put("sticker_bg_segment", FileUtils.getStickerFiles(this, "segment"));
        mStickerlists.put("sticker_deformation", FileUtils.getStickerFiles(this, "deformation"));

        mStickerAdapters.put("sticker_2d", new StickerAdapter(mStickerlists.get("sticker_2d"), this));
        mStickerAdapters.put("sticker_3d", new StickerAdapter(mStickerlists.get("sticker_3d"), this));
        mStickerAdapters.put("sticker_hand_action", new StickerAdapter(mStickerlists.get("sticker_hand_action"), this));
        mStickerAdapters.put("sticker_bg_segment", new StickerAdapter(mStickerlists.get("sticker_bg_segment"), this));
        mStickerAdapters.put("sticker_deformation", new StickerAdapter(mStickerlists.get("sticker_deformation"), this));

        mStickerOptionsList = new ArrayList<>();
        mStickerOptionsList.add(0, new StickerOptionsItem("sticker_2d",BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sticker_2d_unselected),BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sticker_2d_selected)));
        mStickerOptionsList.add(1, new StickerOptionsItem("sticker_3d",BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sticker_3d_unselected),BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sticker_3d_selected)));
        mStickerOptionsList.add(2, new StickerOptionsItem("sticker_hand_action",BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sticker_hand_action_unselected),BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sticker_hand_action_selected)));
        mStickerOptionsList.add(3, new StickerOptionsItem("sticker_bg_segment",BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sticker_bg_segment_unselected),BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sticker_bg_segment_selected)));
        mStickerOptionsList.add(4, new StickerOptionsItem("sticker_deformation",BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sticker_dedormation_unselected),BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sticker_dedormation_selected)));

        mStickerOptionsAdapter = new StickerOptionsAdapter(mStickerOptionsList, this);

        mStickersRecycleView.setAdapter(mStickerAdapters.get("sticker_2d"));
        mStickerAdapters.get("sticker_2d").setSelectedPosition(-1);

        findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker_selected));

        mStickerOptionsRecycleView.setAdapter(mStickerOptionsAdapter);

        mStickerOptionsSwitch = (LinearLayout) findViewById(R.id.ll_sticker_options_switch);
        mStickerOptionsSwitch.setOnClickListener(this);
        mStickerOptions = (LinearLayout) findViewById(R.id.ll_sticker_options);
        mStickerIcons = (RecyclerView) findViewById(R.id.rv_sticker_icons);
        mIsStickerOptionsOpen = false;


        mBeautyOptionsRecycleView = (RecyclerView) findViewById(R.id.rv_beauty_options);
        mBeautyOptionsRecycleView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL));
        mBeautyOptionsRecycleView.addItemDecoration(new ImageActivity.SpaceItemDecoration(0));
        mBeautyOptionsRecycleView.getBackground().setAlpha(240);

        mBeautyOptionsList = new ArrayList<>();
        mBeautyOptionsList.add(0, new BeautyOptionsItem("滤镜"));
        mBeautyOptionsList.add(1, new BeautyOptionsItem("基础美颜"));
        mBeautyOptionsList.add(2, new BeautyOptionsItem("美形"));

        mBeautyOptionsAdapter = new BeautyOptionsAdapter(mBeautyOptionsList, this);
        mBeautyOptionsRecycleView.setAdapter(mBeautyOptionsAdapter);

        mBeautyOptionsSwitch = (LinearLayout) findViewById(R.id.ll_beauty_options_switch);
        mBeautyOptionsSwitch.setOnClickListener(this);

        mBaseBeautyOptions = (LinearLayout) findViewById(R.id.ll_base_beauty_options);
        mBaseBeautyOptions.setOnClickListener(null);
        mProfessionalBeautyOptions = (LinearLayout) findViewById(R.id.ll_professional_beauty_options);
        mProfessionalBeautyOptions.setOnClickListener(null);
        mIsBeautyOptionsOpen = false;


        mFilterOptionsRecycleView = (RecyclerView) findViewById(R.id.rv_filter_icons);
        mFilterOptionsRecycleView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL));
        mFilterOptionsRecycleView.addItemDecoration(new ImageActivity.SpaceItemDecoration(0));
        mFilterOptionsRecycleView.getBackground().setAlpha(220);

        //copy filter models to sdcard and get file paths
        mFilterItem = FileUtils.getFilterFiles(this, "filter");
        mFilterAdapter = new FilterAdapter(mFilterItem, this);
        mFilterOptionsRecycleView.setAdapter(mFilterAdapter);

        mFilterIcons = (RecyclerView) findViewById(R.id.rv_filter_icons);


        mAttributeText = (TextView) findViewById(R.id.tv_face_attribute);
        mAttributeText.setVisibility(View.VISIBLE);

        mSavingTv = (TextView) findViewById(R.id.tv_saving_image);

        findViewById(R.id.iv_setting_options_switch).setVisibility(View.INVISIBLE);
        findViewById(R.id.ll_selection_mode).setVisibility(View.INVISIBLE);
        findViewById(R.id.ll_mode_video).setVisibility(View.INVISIBLE);
        findViewById(R.id.btn_capture_picture).setVisibility(View.INVISIBLE);
        findViewById(R.id.ll_blank_view).setVisibility(View.GONE);
    }

    private void initEvents() {

        for(int i = 0; i < 6; i++){
            mBeautifyParams[i] = DEFAULT_BEAUTIFY_PARAMS[i];
        }

        int mode = 0;
        mode = this.getIntent().getBundleExtra("bundle").getInt("mode");

        switch (mode){
            case LoadImageActivity.MODE_DERAWABLE_IMAGE:
                int index = 1;
                index = this.getIntent().getBundleExtra("bundle").getInt("drawableIndex");

                if(index == 1){
                    mImageBitmap = STUtils.getBitmapFromDrawable(mContext, R.drawable.dilireba);
                }else if(index == 2){
                    mImageBitmap = STUtils.getBitmapFromDrawable(mContext, R.drawable.dilireba);
                }else if(index == 3){
                    mImageBitmap = STUtils.getBitmapFromDrawable(mContext, R.drawable.dilireba);
                }else {
                    mImageBitmap = STUtils.getBitmapFromDrawable(mContext, R.drawable.dilireba);
                }
                break;
            case LoadImageActivity.MODE_GALLERY_IMAGE:

                Uri imageUri =  this.getIntent().getParcelableExtra("imageUri");
                if("file".equals(imageUri.getScheme())){
                    mImageBitmap = STUtils.getBitmapFromFile(imageUri);
                } else {
                    mImageBitmap  = STUtils.getBitmapAfterRotate(imageUri, mContext);
                }
                break;

            case LoadImageActivity.MODE_TAKE_PHOTO:
                Uri photoUri =  this.getIntent().getParcelableExtra("imageUri");
                mImageBitmap  = STUtils.getBitmapFromFileAfterRotate(photoUri);
                break;
        }

        mImageDisplay.setImageBitmap(mImageBitmap);

        // change sticker
        mStickerAdapters.get("sticker_2d").setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int position = Integer.parseInt(v.getTag().toString());
                mStickerAdapters.get("sticker_2d").setSelectedPosition(position);
                mCurrentStickerOptionsIndex = 0;
                mCurrentStickerPosition = position;
                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker));

                mImageDisplay.enableSticker(true);
                mImageDisplay.setShowSticker(mStickerlists.get("sticker_2d").get(position).path);

                mStickerAdapters.get("sticker_2d").notifyDataSetChanged();
            }
        });

        mStickerAdapters.get("sticker_3d").setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = Integer.parseInt(v.getTag().toString());
                mStickerAdapters.get("sticker_3d").setSelectedPosition(position);
                mCurrentStickerOptionsIndex = 1;
                mCurrentStickerPosition = position;
                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker));

                mImageDisplay.enableSticker(true);
                mImageDisplay.setShowSticker(mStickerlists.get("sticker_3d").get(position).path);

                mStickerAdapters.get("sticker_3d").notifyDataSetChanged();
            }
        });

        mStickerAdapters.get("sticker_hand_action").setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int position = Integer.parseInt(v.getTag().toString());
                mStickerAdapters.get("sticker_hand_action").setSelectedPosition(position);
                mCurrentStickerOptionsIndex = 2;
                mCurrentStickerPosition = position;
                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker));

                mImageDisplay.enableSticker(true);
                mImageDisplay.setShowSticker(mStickerlists.get("sticker_hand_action").get(position).path);

                mStickerAdapters.get("sticker_hand_action").notifyDataSetChanged();
            }
        });

        mStickerAdapters.get("sticker_bg_segment").setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int position = Integer.parseInt(v.getTag().toString());
                mStickerAdapters.get("sticker_bg_segment").setSelectedPosition(position);
                mCurrentStickerOptionsIndex = 3;
                mCurrentStickerPosition = position;
                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker));

                mImageDisplay.enableSticker(true);
                mImageDisplay.setShowSticker(mStickerlists.get("sticker_bg_segment").get(position).path);

                mStickerAdapters.get("sticker_bg_segment").notifyDataSetChanged();
            }
        });

        mStickerAdapters.get("sticker_deformation").setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int position = Integer.parseInt(v.getTag().toString());
                mCurrentStickerOptionsIndex = 4;
                mCurrentStickerPosition = position;
                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker));

                mStickerAdapters.get("sticker_deformation").setSelectedPosition(position);

                mImageDisplay.enableSticker(true);
                mImageDisplay.setShowSticker(mStickerlists.get("sticker_deformation").get(position).path);

                mStickerAdapters.get("sticker_deformation").notifyDataSetChanged();
            }
        });



        mStickerOptionsAdapter.setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = Integer.parseInt(v.getTag().toString());
                mStickerOptionsAdapter.setSelectedPosition(position);
                mStickersRecycleView.setLayoutManager(new GridLayoutManager(mContext, 6));

                mStickerAdapters.get("sticker_2d").setSelectedPosition(-1);
                mStickerAdapters.get("sticker_3d").setSelectedPosition(-1);
                mStickerAdapters.get("sticker_hand_action").setSelectedPosition(-1);
                mStickerAdapters.get("sticker_bg_segment").setSelectedPosition(-1);
                mStickerAdapters.get("sticker_deformation").setSelectedPosition(-1);

                if(mCurrentStickerOptionsIndex == 0){
                    mStickerAdapters.get("sticker_2d").setSelectedPosition(mCurrentStickerPosition);
                }else if(mCurrentStickerOptionsIndex == 1){
                    mStickerAdapters.get("sticker_3d").setSelectedPosition(mCurrentStickerPosition);
                }else if(mCurrentStickerOptionsIndex == 2){
                    mStickerAdapters.get("sticker_hand_action").setSelectedPosition(mCurrentStickerPosition);
                }else if(mCurrentStickerOptionsIndex == 3){
                    mStickerAdapters.get("sticker_bg_segment").setSelectedPosition(mCurrentStickerPosition);
                }else if(mCurrentStickerOptionsIndex == 4){
                    mStickerAdapters.get("sticker_deformation").setSelectedPosition(mCurrentStickerPosition);
                }


                if(position == 0){
                    mStickersRecycleView.setAdapter(mStickerAdapters.get("sticker_2d"));
                }else if(position == 1){
                    mStickersRecycleView.setAdapter(mStickerAdapters.get("sticker_3d"));
                }else if(position == 2){
                    mStickersRecycleView.setAdapter(mStickerAdapters.get("sticker_hand_action"));
                }else if(position == 3){
                    mStickersRecycleView.setAdapter(mStickerAdapters.get("sticker_bg_segment"));
                }else if(position == 4){
                    mStickersRecycleView.setAdapter(mStickerAdapters.get("sticker_deformation"));
                }
                mStickerOptionsAdapter.notifyDataSetChanged();
            }
        });


        mFilterAdapter.setClickFilterListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = Integer.parseInt(v.getTag().toString());
                mFilterAdapter.setSelectedPosition(position);

                if(position == 0){
                    mImageDisplay.enableFilter(false);
                }else if(position == 1){
                    mImageDisplay.setFilterStyle(null);
                    mImageDisplay.enableFilter(true);
                }else{
                    mImageDisplay.setFilterStyle(mFilterItem.get(position).model);
                    mImageDisplay.enableFilter(true);
                }

                mFilterAdapter.notifyDataSetChanged();
            }
        });

        mBeautyOptionsAdapter.setClickBeautyListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = Integer.parseInt(v.getTag().toString());
                mBeautyOptionsAdapter.setSelectedPosition(position);
                mBeautyOptionsPosition = position;

                if(position == 0){
                    mFilterOptionsRecycleView.setVisibility(View.VISIBLE);
                    mBaseBeautyOptions.setVisibility(View.INVISIBLE);
                    mProfessionalBeautyOptions.setVisibility(View.INVISIBLE);
                }else if(position == 1){
                    mFilterOptionsRecycleView.setVisibility(View.INVISIBLE);
                    mBaseBeautyOptions.setVisibility(View.VISIBLE);
                    mProfessionalBeautyOptions.setVisibility(View.INVISIBLE);
                }else if(position == 2){
                    mFilterOptionsRecycleView.setVisibility(View.INVISIBLE);
                    mBaseBeautyOptions.setVisibility(View.INVISIBLE);
                    mProfessionalBeautyOptions.setVisibility(View.VISIBLE);
                }

                mBeautyOptionsAdapter.notifyDataSetChanged();
            }
        });

        mBeautyParamsSeekBarList.add(0, (SeekBar) findViewById(R.id.sb_beauty_redden_strength));
        mBeautyParamsSeekBarList.add(1, (SeekBar) findViewById(R.id.sb_beauty_smooth_strength));
        mBeautyParamsSeekBarList.add(2, (SeekBar) findViewById(R.id.sb_beauty_whiten_strength));
        mBeautyParamsSeekBarList.add(3, (SeekBar) findViewById(R.id.sb_beauty_enlarge_eye_strength));
        mBeautyParamsSeekBarList.add(4, (SeekBar) findViewById(R.id.sb_beauty_shrink_face_strength));
        mBeautyParamsSeekBarList.add(5, (SeekBar) findViewById(R.id.sb_beauty_shrink_jaw_strength));

        for(int i = 0; i < 6; i++){
            final int index = i;

            updateBeautyParamsStrength(i, (int)(mBeautifyParams[i]*100));
            mBeautyParamsSeekBarList.get(i).setProgress((int)(mBeautifyParams[i]*100));
            mBeautyParamsSeekBarList.get(i).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mImageDisplay.setBeautyParam(index, (float)progress/100);
                    updateBeautyParamsStrength(index, progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }


        mShowOriginBtn1 = (TextView)findViewById(R.id.tv_show_origin1);
        mShowOriginBtn1.setOnTouchListener(new View.OnTouchListener() {

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
        mShowOriginBtn1.setVisibility(View.VISIBLE);

        mShowOriginBtn2 = (TextView)findViewById(R.id.tv_show_origin2);
        mShowOriginBtn2.setOnTouchListener(new View.OnTouchListener() {

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
        mShowOriginBtn2.setVisibility(View.INVISIBLE);

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

        findViewById(R.id.ll_cpu_radio).setVisibility(View.GONE);
        findViewById(R.id.tv_layout_tips).setVisibility(View.GONE);
        findViewById(R.id.tv_capture).setOnClickListener(this);
        findViewById(R.id.tv_cancel).setOnClickListener(this);
        findViewById(R.id.id_gl_sv).setOnClickListener(this);

        findViewById(R.id.rv_close_sticker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStickerAdapters.get("sticker_2d").setSelectedPosition(-1);
                mStickerAdapters.get("sticker_2d").notifyDataSetChanged();

                mStickerAdapters.get("sticker_3d").setSelectedPosition(-1);
                mStickerAdapters.get("sticker_3d").notifyDataSetChanged();

                mStickerAdapters.get("sticker_hand_action").setSelectedPosition(-1);
                mStickerAdapters.get("sticker_hand_action").notifyDataSetChanged();

                mStickerAdapters.get("sticker_bg_segment").setSelectedPosition(-1);
                mStickerAdapters.get("sticker_bg_segment").notifyDataSetChanged();

                mStickerAdapters.get("sticker_deformation").setSelectedPosition(-1);
                mStickerAdapters.get("sticker_deformation").notifyDataSetChanged();

                mCurrentStickerPosition = -1;
                mImageDisplay.enableSticker(false);

                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker_selected));
            }
        });

        mImageDisplay.enableBeautify(true);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_sticker_options_switch:
                mStickerOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_sticker_options_switch);
                mBeautyOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_beauty_options_switch);
                mStickerOptionsSwitchText = (TextView) findViewById(R.id.tv_sticker_options_switch);
                mBeautyOptionsSwitchText = (TextView) findViewById(R.id.tv_beauty_options_switch);
                if(mIsStickerOptionsOpen){
                    mStickerOptions.setVisibility(View.INVISIBLE);
                    mStickerIcons.setVisibility(View.INVISIBLE);
                    mStickerOptionsSwitchIcon.setImageDrawable(getResources().getDrawable(R.drawable.sticker));
                    mStickerOptionsSwitchText.setTextColor(Color.parseColor("#666666"));
                    mIsStickerOptionsOpen = false;

                    mShowOriginBtn1.setVisibility(View.VISIBLE);
                    mShowOriginBtn2.setVisibility(View.INVISIBLE);
                }else{
                    mStickerOptions.setVisibility(View.VISIBLE);
                    mStickerIcons.setVisibility(View.VISIBLE);
                    mStickerOptionsSwitchIcon.setImageDrawable(getResources().getDrawable(R.drawable.sticker_chosed));
                    mStickerOptionsSwitchText.setTextColor(Color.parseColor("#c460e1"));
                    mIsStickerOptionsOpen = true;

                    mShowOriginBtn1.setVisibility(View.INVISIBLE);
                    mShowOriginBtn2.setVisibility(View.VISIBLE);
                }

                mFilterOptionsRecycleView.setVisibility(View.INVISIBLE);
                mBeautyOptionsRecycleView.setVisibility(View.INVISIBLE);
                mBaseBeautyOptions.setVisibility(View.INVISIBLE);
                mProfessionalBeautyOptions.setVisibility(View.INVISIBLE);
                mBeautyOptionsSwitchIcon.setImageDrawable(getResources().getDrawable(R.drawable.beauty));
                mBeautyOptionsSwitchText.setTextColor(Color.parseColor("#666666"));
                mIsBeautyOptionsOpen = false;

                break;

            case R.id.ll_beauty_options_switch:
                mStickerOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_sticker_options_switch);
                mBeautyOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_beauty_options_switch);
                mStickerOptionsSwitchText = (TextView) findViewById(R.id.tv_sticker_options_switch);
                mBeautyOptionsSwitchText = (TextView) findViewById(R.id.tv_beauty_options_switch);
                if(mIsBeautyOptionsOpen){
                    mFilterOptionsRecycleView.setVisibility(View.INVISIBLE);
                    mBeautyOptionsRecycleView.setVisibility(View.INVISIBLE);
                    mBaseBeautyOptions.setVisibility(View.INVISIBLE);
                    mProfessionalBeautyOptions.setVisibility(View.INVISIBLE);
                    mBeautyOptionsSwitchIcon.setImageDrawable(getResources().getDrawable(R.drawable.beauty));
                    mBeautyOptionsSwitchText.setTextColor(Color.parseColor("#666666"));
                    mIsBeautyOptionsOpen = false;

                    mShowOriginBtn1.setVisibility(View.VISIBLE);
                    mShowOriginBtn2.setVisibility(View.INVISIBLE);
                }else{
                    if(mBeautyOptionsPosition == 0){
                        mFilterOptionsRecycleView.setVisibility(View.VISIBLE);
                    }else if(mBeautyOptionsPosition == 1){
                        mBaseBeautyOptions.setVisibility(View.VISIBLE);
                    }else if(mBeautyOptionsPosition == 2){
                        mProfessionalBeautyOptions.setVisibility(View.VISIBLE);
                    }
                    mBeautyOptionsRecycleView.setVisibility(View.VISIBLE);
                    mBeautyOptionsSwitchIcon.setImageDrawable(getResources().getDrawable(R.drawable.beauty_chosed));
                    mBeautyOptionsSwitchText.setTextColor(Color.parseColor("#c460e1"));
                    mIsBeautyOptionsOpen = true;

                    mShowOriginBtn1.setVisibility(View.INVISIBLE);
                    mShowOriginBtn2.setVisibility(View.VISIBLE);

                    mStickerOptions.setVisibility(View.INVISIBLE);
                    mStickerIcons.setVisibility(View.INVISIBLE);
                }

                mStickerOptionsSwitchIcon.setImageDrawable(getResources().getDrawable(R.drawable.sticker));
                mStickerOptionsSwitchText.setTextColor(Color.parseColor("#666666"));
                mIsStickerOptionsOpen = false;

                break;

            case R.id.id_gl_sv:

                mStickerOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_sticker_options_switch);
                mBeautyOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_beauty_options_switch);
                mStickerOptionsSwitchText = (TextView) findViewById(R.id.tv_sticker_options_switch);
                mBeautyOptionsSwitchText = (TextView) findViewById(R.id.tv_beauty_options_switch);

                mStickerOptions.setVisibility(View.INVISIBLE);
                mStickerIcons.setVisibility(View.INVISIBLE);
                mStickerOptionsSwitchIcon.setImageDrawable(getResources().getDrawable(R.drawable.sticker));
                mStickerOptionsSwitchText.setTextColor(Color.parseColor("#666666"));
                mIsStickerOptionsOpen = false;

                mFilterOptionsRecycleView.setVisibility(View.INVISIBLE);
                mBeautyOptionsRecycleView.setVisibility(View.INVISIBLE);
                mBaseBeautyOptions.setVisibility(View.INVISIBLE);
                mProfessionalBeautyOptions.setVisibility(View.INVISIBLE);
                mBeautyOptionsSwitchIcon.setImageDrawable(getResources().getDrawable(R.drawable.beauty));
                mBeautyOptionsSwitchText.setTextColor(Color.parseColor("#666666"));
                mIsBeautyOptionsOpen = false;

                mShowOriginBtn1.setVisibility(View.VISIBLE);
                mShowOriginBtn2.setVisibility(View.INVISIBLE);

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
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("process_killed",true);
        super.onSaveInstanceState(savedInstanceState);
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

    private void updateBeautyParamsStrength(int index, int strength){
        switch (index){
            case 0:
                ((TextView)findViewById(R.id.tv_beauty_redden_strength)).setText(""+strength);
                break;

            case 1:
                ((TextView)findViewById(R.id.tv_beauty_smooth_strength)).setText(""+strength);
                break;

            case 2:
                ((TextView)findViewById(R.id.tv_beauty_whiten_strength)).setText(""+strength);
                break;

            case 3:
                ((TextView)findViewById(R.id.tv_beauty_enlarge_eye_strength)).setText(""+strength);
                break;

            case 4:
                ((TextView)findViewById(R.id.tv_beauty_shrink_face_strength)).setText(""+strength);
                break;

            case 5:
                ((TextView)findViewById(R.id.tv_beauty_shrink_jaw_strength)).setText(""+strength);
                break;

            default:
                break;
        }
    }

}
