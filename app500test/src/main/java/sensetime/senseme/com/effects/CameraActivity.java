package sensetime.senseme.com.effects;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
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
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.sensetime.stmobile.STMobileHumanActionNative;
import com.sensetime.stmobile.model.STPoint;
import com.tatata.hearst.R;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import sensetime.senseme.com.effects.adapter.BeautyOptionsAdapter;
import sensetime.senseme.com.effects.adapter.FilterAdapter;
import sensetime.senseme.com.effects.adapter.ObjectAdapter;
import sensetime.senseme.com.effects.adapter.StickerAdapter;
import sensetime.senseme.com.effects.adapter.StickerOptionsAdapter;
import sensetime.senseme.com.effects.display.CameraDisplayDoubleInput;
import sensetime.senseme.com.effects.display.CameraDisplaySingleInput;
import sensetime.senseme.com.effects.encoder.MediaAudioEncoder;
import sensetime.senseme.com.effects.encoder.MediaEncoder;
import sensetime.senseme.com.effects.encoder.MediaMuxerWrapper;
import sensetime.senseme.com.effects.encoder.MediaVideoEncoder;
import sensetime.senseme.com.effects.glutils.STUtils;
import sensetime.senseme.com.effects.utils.Accelerometer;
import sensetime.senseme.com.effects.utils.CheckAudioPermission;
import sensetime.senseme.com.effects.utils.FileUtils;
import sensetime.senseme.com.effects.utils.LogUtils;
import sensetime.senseme.com.effects.utils.STLicenseUtils;
import sensetime.senseme.com.effects.view.BeautyOptionsItem;
import sensetime.senseme.com.effects.view.FilterItem;
import sensetime.senseme.com.effects.view.ObjectItem;
import sensetime.senseme.com.effects.view.StickerItem;
import sensetime.senseme.com.effects.view.StickerOptionsItem;

public class CameraActivity extends Activity implements View.OnClickListener{
    private final static String TAG = "CameraActivity";
    private Accelerometer mAccelerometer = null;

    //双输入使用
//    private CameraDisplayDoubleInput mCameraDisplay;

    //单输入使用
    private CameraDisplaySingleInput mCameraDisplay;

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
    private ObjectAdapter mObjectAdapter;
    private List<ObjectItem> mObjectList;
    private boolean mNeedObject = false;

    private TextView mSavingTv;
    private TextView mAttributeText;

    private TextView mShowOriginBtn1, mShowOriginBtn2;
    private TextView mShowShortVideoTime;
    private TextView mSmallPreviewSize, mLargePreviewSize;

    private float[] mBeautifyParams = {0.36f, 0.74f, 0.30f, 0.13f, 0.11f, 0.1f};

    private RelativeLayout mTipsLayout;
    private TextView mTipsTextView;
    private ImageView mTipsImageView;
    private Context mContext;
    private Handler mTipsHandler = new Handler();
    private Runnable mTipsRunnable;

    public static final int MSG_SAVING_IMG = 1;
    public static final int MSG_SAVED_IMG = 2;
    public static final int MSG_DRAW_OBJECT_IMAGE_AND_RECT = 3;
    public static final int MSG_DRAW_OBJECT_IMAGE = 4;
    public static final int MSG_CLEAR_OBJECT = 5;
    public static final int MSG_MISSED_OBJECT_TRACK = 6;
    public static final int MSG_DRAW_FACE_EXTRA_POINTS = 7;
    private static final int MSG_NEED_UPDATE_TIMER = 8;
    private static final int MSG_NEED_START_CAPTURE = 9;
    private static final int MSG_NEED_START_RECORDING = 10;
    private static final int MSG_STOP_RECORDING = 11;

    private static final int PERMISSION_REQUEST_WRITE_PERMISSION = 101;
    private boolean mPermissionDialogShowing = false;
    private Thread mCpuInofThread;
    private float mCurrentCpuRate = 0.0f;

    private SurfaceView mSurfaceViewOverlap;
    private Bitmap mGuideBitmap;
    private Paint mPaint = new Paint();

    private int mIndexX = 0, mIndexY = 0;
    private boolean mCanMove = false;


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

    private ImageView mSettingOptionsSwitch;
    private RelativeLayout mSettingOptions;
    private boolean mIsSettingOptionsOpen = false;
    private LinearLayout mFpsInfo;

    private LinearLayout mModeVideo, mSelectionMode, mSelectionVideo;
    private TextView mSelectionPicture;

    private Button mCaptureButton;

    private ImageView mBeautyOptionsSwitchIcon, mStickerOptionsSwitchIcon;
    private TextView mBeautyOptionsSwitchText, mStickerOptionsSwitchText;

    private int mTimeSeconds = 0;
    private int mTimeMinutes = 0;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private boolean mIsRecording = false;
    private String mVideoFilePath = null;
    private long mTouchDownTime = 0;
    private long mTouchCurrentTime = 0;
    private boolean mOnBtnTouch = false;

    private boolean mIsHasAudioPermission = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_SAVING_IMG:
                    ByteBuffer data = (ByteBuffer) msg.obj;
                    Bundle bundle = msg.getData();
                    int imageWidth = bundle.getInt("imageWidth");
                    int imageHeight = bundle.getInt("imageHeight");
                    onPictureTaken(data, FileUtils.getOutputMediaFile(), imageWidth, imageHeight);

                    break;
                case MSG_SAVED_IMG:
                    mSavingTv.setVisibility(View.VISIBLE);
                    mSavingTv.setText("图片保存成功");
                    new Handler().postDelayed(new Runnable(){
                        public void run() {
                            mSavingTv.setVisibility(View.GONE);
                        }
                    }, 1000);

                    break;
                case MSG_DRAW_OBJECT_IMAGE_AND_RECT:
                    Rect indexRect = (Rect)msg.obj;
                    drawObjectImage(indexRect, true);

                    break;
                case MSG_DRAW_OBJECT_IMAGE:
                    Rect rect = (Rect)msg.obj;
                    drawObjectImage(rect, false);

                    break;
                case MSG_CLEAR_OBJECT:
                    clearObjectImage();

                    break;
                case MSG_MISSED_OBJECT_TRACK:
                    mObjectAdapter.setSelectedPosition(1);
                    mObjectAdapter.notifyDataSetChanged();
                    break;

                case MSG_DRAW_FACE_EXTRA_POINTS:
                    STPoint[] points = (STPoint[])msg.obj;
                    drawFaceExtraPoints(points);
                    break;

                case MSG_NEED_UPDATE_TIMER:
                    updateTimer();
                    break;

                case MSG_NEED_START_RECORDING:
                    //开始录制
                    startRecording();
                    closeTableView();
                    disableShowLayouts();
                    mShowShortVideoTime.setVisibility(View.VISIBLE);

                    mTimer = new Timer();
                    mTimerTask= new TimerTask() {
                        @Override
                        public void run() {
                            Message msg = mHandler.obtainMessage(MSG_NEED_UPDATE_TIMER);
                            mHandler.sendMessage(msg);
                        }
                    };

                    mTimer.schedule(mTimerTask, 1000, 1000);
                    break;

                case MSG_STOP_RECORDING:
                    new Handler().postDelayed(new Runnable(){
                        public void run() {
                            //结束录制
                            if(mIsRecording){
                                return;
                            }
                            stopRecording();
                            enableShowLayouts();
                            mShowShortVideoTime.setVisibility(View.INVISIBLE);

                            if(mTimeMinutes == 0 && mTimeSeconds < 2){
                                if(mVideoFilePath != null){
                                    File file = new File(mVideoFilePath);
                                    if(file != null){
                                        file.delete();
                                    }
                                }
                                mSavingTv.setText("视频不能少于2秒");
                            }else{
                                mSavingTv.setText("视频保存成功");
                            }

                            resetTimer();
                        }
                    }, 100);

                    mSavingTv.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(new Runnable(){
                        public void run() {
                            mSavingTv.setVisibility(View.GONE);
                        }
                    }, 1000);

                    break;
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
        //LogUtils.setIsLoggable(true);

        initView();
        initEvents();
    }

    private void initView() {
        //copy model file to sdcard
        FileUtils.copyModelFiles(this);

        mAccelerometer = new Accelerometer(getApplicationContext());

        GLSurfaceView glSurfaceView = (GLSurfaceView) findViewById(R.id.id_gl_sv);
        mSurfaceViewOverlap = (SurfaceView) findViewById(R.id.surfaceViewOverlap);
        mPreviewFrameLayout = (FrameLayout) findViewById(R.id.id_preview_layout);

        //单输入使用
        mCameraDisplay = new CameraDisplaySingleInput(getApplicationContext(), mSingleInputChangePreviewSizeListener, glSurfaceView);

        //双输入使用
//        mCameraDisplay = new CameraDisplayDoubleInput(getApplicationContext(), mDoubleInputChangePreviewSizeListener, glSurfaceView);
        mCameraDisplay.setHandler(mHandler);

        mStickerOptionsRecycleView = (RecyclerView) findViewById(R.id.rv_sticker_options);
        mStickerOptionsRecycleView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL));
        mStickerOptionsRecycleView.addItemDecoration(new SpaceItemDecoration(0));
        mStickerOptionsRecycleView.getBackground().setAlpha(240);

        mStickersRecycleView = (RecyclerView) findViewById(R.id.rv_sticker_icons);
        mStickersRecycleView.setLayoutManager(new GridLayoutManager(this, 6));
        mStickersRecycleView.addItemDecoration(new SpaceItemDecoration(0));
        mStickersRecycleView.getBackground().setAlpha(220);

        mFilterOptionsRecycleView = (RecyclerView) findViewById(R.id.rv_filter_icons);
        mFilterOptionsRecycleView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL));
        mFilterOptionsRecycleView.addItemDecoration(new SpaceItemDecoration(0));
        mFilterOptionsRecycleView.getBackground().setAlpha(220);

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
        mStickerOptionsList.add(5, new StickerOptionsItem("object_track",BitmapFactory.decodeResource(mContext.getResources(), R.drawable.object_track_unselected),BitmapFactory.decodeResource(mContext.getResources(), R.drawable.object_track_selected)));

        mStickerOptionsAdapter = new StickerOptionsAdapter(mStickerOptionsList, this);

        mStickersRecycleView.setAdapter(mStickerAdapters.get("sticker_2d"));
        mStickerAdapters.get("sticker_2d").setSelectedPosition(-1);

        findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker_selected));

        mBeautyOptionsRecycleView = (RecyclerView) findViewById(R.id.rv_beauty_options);
        mBeautyOptionsRecycleView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL));
        mBeautyOptionsRecycleView.addItemDecoration(new SpaceItemDecoration(0));
        mBeautyOptionsRecycleView.getBackground().setAlpha(240);

        mBeautyOptionsList = new ArrayList<>();
        mBeautyOptionsList.add(0, new BeautyOptionsItem("滤镜"));
        mBeautyOptionsList.add(1, new BeautyOptionsItem("基础美颜"));
        mBeautyOptionsList.add(2, new BeautyOptionsItem("美形"));

        mBeautyOptionsAdapter = new BeautyOptionsAdapter(mBeautyOptionsList, this);
        mBeautyOptionsRecycleView.setAdapter(mBeautyOptionsAdapter);


        //copy filter models to sdcard and get file paths
        mFilterItem = FileUtils.getFilterFiles(this, "filter");
        mFilterAdapter = new FilterAdapter(mFilterItem, this);

        mStickerOptionsRecycleView.setAdapter(mStickerOptionsAdapter);

        mObjectList = FileUtils.getObjectList();
        mObjectAdapter = new ObjectAdapter(mObjectList, this);
        mObjectAdapter.setSelectedPosition(-1);

        mStickersRecycleView.setAdapter(mStickerAdapters.get("sticker_2d"));
        mFilterOptionsRecycleView.setAdapter(mFilterAdapter);

        mSavingTv = (TextView) findViewById(R.id.tv_saving_image);
        mTipsLayout = (RelativeLayout) findViewById(R.id.tv_layout_tips);
        mAttributeText = (TextView) findViewById(R.id.tv_face_attribute);
        mAttributeText.setVisibility(View.VISIBLE);
        mTipsTextView = (TextView) findViewById(R.id.tv_text_tips);
        mTipsImageView = (ImageView) findViewById(R.id.iv_image_tips);
        mTipsLayout.setVisibility(View.GONE);


        mBeautyOptionsSwitch = (LinearLayout) findViewById(R.id.ll_beauty_options_switch);
        mBeautyOptionsSwitch.setOnClickListener(this);
        mFilterIcons = (RecyclerView) findViewById(R.id.rv_filter_icons);

        mBaseBeautyOptions = (LinearLayout) findViewById(R.id.ll_base_beauty_options);
        mBaseBeautyOptions.setOnClickListener(null);
        mProfessionalBeautyOptions = (LinearLayout) findViewById(R.id.ll_professional_beauty_options);
        mProfessionalBeautyOptions.setOnClickListener(null);
        mIsBeautyOptionsOpen = false;

        mStickerOptionsSwitch = (LinearLayout) findViewById(R.id.ll_sticker_options_switch);
        mStickerOptionsSwitch.setOnClickListener(this);
        mStickerOptions = (LinearLayout) findViewById(R.id.ll_sticker_options);
        mStickerIcons = (RecyclerView) findViewById(R.id.rv_sticker_icons);
        mIsStickerOptionsOpen = false;

        mSettingOptionsSwitch = (ImageView) findViewById(R.id.iv_setting_options_switch);
        mSettingOptionsSwitch.setOnClickListener(this);
        mSettingOptions = (RelativeLayout) findViewById(R.id.rl_setting_options);
        mIsSettingOptionsOpen = false;

        mModeVideo = (LinearLayout) findViewById(R.id.ll_mode_video);
        mModeVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mModeVideo.setVisibility(View.INVISIBLE);
                mSelectionMode.setVisibility(View.VISIBLE);
            }
        });
        mSelectionMode = (LinearLayout) findViewById(R.id.ll_selection_mode);
        mSelectionMode.setVisibility(View.INVISIBLE);
        mSelectionVideo = (LinearLayout) findViewById(R.id.ll_selection_video);
        mSelectionVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mModeVideo.setVisibility(View.VISIBLE);
                mSelectionMode.setVisibility(View.INVISIBLE);
            }
        });
        mSelectionPicture = (TextView) findViewById(R.id.tv_selection_picture);
        mSelectionPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(getApplicationContext(), LoadImageActivity.class));
            }
        });

        findViewById(R.id.tv_cancel).setVisibility(View.INVISIBLE);
        findViewById(R.id.tv_capture).setVisibility(View.INVISIBLE);

        mSmallPreviewSize = (TextView) findViewById(R.id.tv_small_size_unselected);
        mSmallPreviewSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mCameraDisplay != null && !mCameraDisplay.isChangingPreviewSize()){
                    mCameraDisplay.changePreviewSize(0);
                    findViewById(R.id.tv_small_size_selected).setVisibility(View.VISIBLE);
                    findViewById(R.id.tv_small_size_unselected).setVisibility(View.INVISIBLE);
                    findViewById(R.id.tv_large_size_selected).setVisibility(View.INVISIBLE);
                    findViewById(R.id.tv_large_size_unselected).setVisibility(View.VISIBLE);

                    mSmallPreviewSize.setClickable(false);
                    mLargePreviewSize.setClickable(true);
                }
            }
        });
        mLargePreviewSize = (TextView) findViewById(R.id.tv_large_size_unselected);
        mLargePreviewSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mCameraDisplay != null && !mCameraDisplay.isChangingPreviewSize()){
                    mCameraDisplay.changePreviewSize(1);
                    findViewById(R.id.tv_small_size_selected).setVisibility(View.INVISIBLE);
                    findViewById(R.id.tv_small_size_unselected).setVisibility(View.VISIBLE);
                    findViewById(R.id.tv_large_size_selected).setVisibility(View.VISIBLE);
                    findViewById(R.id.tv_large_size_unselected).setVisibility(View.INVISIBLE);

                    mSmallPreviewSize.setClickable(true);
                    mLargePreviewSize.setClickable(false);
                }
            }
        });

        mFpsInfo = (LinearLayout) findViewById(R.id.ll_fps_info);
        mFpsInfo.setVisibility(View.VISIBLE);

        mCaptureButton = (Button) findViewById(R.id.btn_capture_picture);
    }


    private void initEvents() {

        mSurfaceViewOverlap.setZOrderOnTop(true);
        mSurfaceViewOverlap.setZOrderMediaOverlay(true);
        mSurfaceViewOverlap.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mPaint = new Paint();
        mPaint.setColor(Color.rgb(240, 100, 100));
        int strokeWidth = 10;
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);

        // change sticker
        mStickerAdapters.get("sticker_2d").setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTipsLayout.setVisibility(View.GONE);

                int position = Integer.parseInt(v.getTag().toString());
                mStickerAdapters.get("sticker_2d").setSelectedPosition(position);
                mCurrentStickerOptionsIndex = 0;
                mCurrentStickerPosition = position;

                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker));

                mCameraDisplay.enableSticker(true);
                mCameraDisplay.setShowSticker(mStickerlists.get("sticker_2d").get(position).path);

                long action = mCameraDisplay.getStickerTriggerAction();
                showActiveTips(action);

                mStickerAdapters.get("sticker_2d").notifyDataSetChanged();
            }
        });

        mStickerAdapters.get("sticker_3d").setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTipsLayout.setVisibility(View.GONE);

                int position = Integer.parseInt(v.getTag().toString());
                mStickerAdapters.get("sticker_3d").setSelectedPosition(position);
                mCurrentStickerOptionsIndex = 1;
                mCurrentStickerPosition = position;

                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker));

                mCameraDisplay.enableSticker(true);
                mCameraDisplay.setShowSticker(mStickerlists.get("sticker_3d").get(position).path);

                long action = mCameraDisplay.getStickerTriggerAction();
                showActiveTips(action);

                mStickerAdapters.get("sticker_3d").notifyDataSetChanged();
            }
        });

        mStickerAdapters.get("sticker_hand_action").setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTipsLayout.setVisibility(View.GONE);

                int position = Integer.parseInt(v.getTag().toString());
                mStickerAdapters.get("sticker_hand_action").setSelectedPosition(position);
                mCurrentStickerOptionsIndex = 2;
                mCurrentStickerPosition = position;

                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker));

                mCameraDisplay.enableSticker(true);
                mCameraDisplay.setShowSticker(mStickerlists.get("sticker_hand_action").get(position).path);

                long action = mCameraDisplay.getStickerTriggerAction();
                showActiveTips(action);

                mStickerAdapters.get("sticker_hand_action").notifyDataSetChanged();
            }
        });

        mStickerAdapters.get("sticker_bg_segment").setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTipsLayout.setVisibility(View.GONE);

                int position = Integer.parseInt(v.getTag().toString());
                mStickerAdapters.get("sticker_bg_segment").setSelectedPosition(position);
                mCurrentStickerOptionsIndex = 3;
                mCurrentStickerPosition = position;

                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker));

                mCameraDisplay.enableSticker(true);
                mCameraDisplay.setShowSticker(mStickerlists.get("sticker_bg_segment").get(position).path);

                long action = mCameraDisplay.getStickerTriggerAction();
                showActiveTips(action);

                mStickerAdapters.get("sticker_bg_segment").notifyDataSetChanged();
            }
        });

        mStickerAdapters.get("sticker_deformation").setClickStickerListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTipsLayout.setVisibility(View.GONE);

                int position = Integer.parseInt(v.getTag().toString());
                mCurrentStickerOptionsIndex = 4;
                mCurrentStickerPosition = position;

                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker));

                mStickerAdapters.get("sticker_deformation").setSelectedPosition(position);

                mCameraDisplay.enableSticker(true);
                mCameraDisplay.setShowSticker(mStickerlists.get("sticker_deformation").get(position).path);

                long action = mCameraDisplay.getStickerTriggerAction();
                showActiveTips(action);

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
                }else if(position == 5){
                    mStickersRecycleView.setAdapter(mObjectAdapter);
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
                    mCameraDisplay.enableFilter(false);
                }else if(position == 1){
                    mCameraDisplay.setFilterStyle(null);
                    mCameraDisplay.enableFilter(true);
                }else{
                    mCameraDisplay.setFilterStyle(mFilterItem.get(position).model);
                    mCameraDisplay.enableFilter(true);
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
                    mCameraDisplay.setBeautyParam(index, (float)progress/100);
                    updateBeautyParamsStrength(index, progress);
                    mBeautifyParams[index] = (float)progress/100;

                    if(mBeautifyParams[index] == 0f){
                       if(mBeautifyParams[0] == 0f && mBeautifyParams[1] == 0f && mBeautifyParams[2] == 0f &&
                               mBeautifyParams[3] == 0f && mBeautifyParams[4] == 0f && mBeautifyParams[5] == 0f){
                           mCameraDisplay.enableBeautify(false);
                       }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        mObjectAdapter.setClickObjectListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = Integer.parseInt(v.getTag().toString());
                mObjectAdapter.setSelectedPosition(position);

                if(position == 0) {
                    mNeedObject = true;
                    mCameraDisplay.enableObject(true);
                    mGuideBitmap = BitmapFactory.decodeResource(mContext.getResources(), mObjectList.get(position).drawableID);
                    mCameraDisplay.resetIndexRect();
                }

                mObjectAdapter.notifyDataSetChanged();
            }
        });

        mShowOriginBtn1 = (TextView)findViewById(R.id.tv_show_origin1);
        mShowOriginBtn1.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mCameraDisplay.setShowOriginal(true);
                    mCaptureButton.setEnabled(false);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mCameraDisplay.setShowOriginal(false);
                    mCaptureButton.setEnabled(true);
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
                    mCameraDisplay.setShowOriginal(true);
                    mCaptureButton.setEnabled(false);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mCameraDisplay.setShowOriginal(false);
                    mCaptureButton.setEnabled(true);
                }
                return true;
            }
        });
        mShowOriginBtn2.setVisibility(View.INVISIBLE);

        mCaptureButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mTouchDownTime = System.currentTimeMillis();
                    mOnBtnTouch = true;
                    Thread thread= new Thread() {
                        @Override
                        public void run() {
                            while (mOnBtnTouch) {
                                mTouchCurrentTime = System.currentTimeMillis();
                                if (mTouchCurrentTime - mTouchDownTime >= 500 && !mIsRecording && !mIsPaused) {
                                    //开始录制
                                    Message msg = mHandler.obtainMessage(MSG_NEED_START_RECORDING);
                                    mHandler.sendMessage(msg);
                                    mIsRecording = true;
                                }else if(mTouchCurrentTime - mTouchDownTime  >= 10500 && mIsRecording && !mIsPaused){
                                    //超时结束录制
                                    Message msg = mHandler.obtainMessage(MSG_STOP_RECORDING);
                                    mHandler.sendMessage(msg);
                                    mIsRecording = false;

                                    break;
                                }
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    thread.start();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mOnBtnTouch = false;
                    if (mTouchCurrentTime - mTouchDownTime > 500 && mIsRecording && !mIsPaused) {
                        //结束录制
                        Message msg = mHandler.obtainMessage(MSG_STOP_RECORDING);
                        mHandler.sendMessage(msg);
                        mIsRecording = false;
                    }else if(mTouchCurrentTime - mTouchDownTime <= 500){
                        //保存图片
                        if (isWritePermissionAllowed()) {
                            mCameraDisplay.setHandler(mHandler);
                            mCameraDisplay.setSaveImage();
                        } else {
                            requestWritePermission();
                        }
                    }
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    mOnBtnTouch = false;
                }
                return true;
            }
        });

        mShowShortVideoTime = (TextView) findViewById(R.id.tv_short_video_time);

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
                mCameraDisplay.setShowSticker(null);
                mCameraDisplay.enableSticker(false);

                mObjectAdapter.setSelectedPosition(-1);
                mObjectAdapter.notifyDataSetChanged();
                mCameraDisplay.enableObject(false);

                findViewById(R.id.iv_close_sticker).setBackground(getResources().getDrawable(R.drawable.close_sticker_selected));
            }
        });

        findViewById(R.id.tv_change_camera).setOnClickListener(this);
        findViewById(R.id.tv_change_camera).setVisibility(View.VISIBLE);

        // switch camera
        findViewById(R.id.id_tv_changecamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraDisplay.switchCamera();
            }
        });

        mCameraDisplay.enableBeautify(true);
        mIsHasAudioPermission = CheckAudioPermission.isHasPermission(mContext);
    }

    private void startShowCpuInfo() {
        mCpuInofThread = new Thread() {
            @Override
            public void run() {
                super.run();
                while (true) {
                    if(Build.VERSION.SDK_INT < 25){
                        final float rate = getProcessCpuRate();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.tv_cpu_radio)).setText(String.valueOf(rate));
                                if(mCameraDisplay != null){
                                    ((TextView) findViewById(R.id.tv_frame_radio)).setText(String.valueOf(mCameraDisplay.getFrameCost()));
                                }
                            }
                        });
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.tv_cpu_radio)).setText("null");
                                if(mCameraDisplay != null){
                                    ((TextView) findViewById(R.id.tv_frame_radio)).setText(String.valueOf(mCameraDisplay.getFrameCost()));
                                }
                            }
                        });
                    }

                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        mCpuInofThread.start();
    }

    private void stopShowCpuInfo() {
        if (mCpuInofThread != null) {
            mCpuInofThread.interrupt();
            //mCpuInofThread.stop();
            mCpuInofThread = null;
        }
    }

    private void showActiveTips(long actionNum) {
        if (actionNum != -1 && actionNum != 0) {
            mTipsLayout.setVisibility(View.VISIBLE);
        }

        if((actionNum & STMobileHumanActionNative.ST_MOBILE_EYE_BLINK) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_blink);
            mTipsTextView.setText("请眨眨眼~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_MOUTH_AH) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_mouth);
            mTipsTextView.setText("张嘴有惊喜~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HEAD_YAW) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_shake);
            mTipsTextView.setText("请摇摇头~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HEAD_PITCH) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_nod);
            mTipsTextView.setText("请点点头~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_BROW_JUMP) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_frown);
            mTipsTextView.setText("挑眉有惊喜~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HAND_PALM) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_palm);
            mTipsTextView.setText("请伸出手掌~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HAND_LOVE) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_heart_hand);
            mTipsTextView.setText("双手比个爱心吧~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HAND_HOLDUP) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_palm_up);
            mTipsTextView.setText("请托手~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HAND_CONGRATULATE) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_congratulate);
            mTipsTextView.setText("抱个拳吧~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HAND_FINGER_HEART) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_finger_heart);
            mTipsTextView.setText("单手比个爱心吧~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HAND_TWO_INDEX_FINGER) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_two_index_finger);
            mTipsTextView.setText("如图所示伸出手指~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HAND_GOOD) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_thumb);
            mTipsTextView.setText("请伸出大拇指~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HAND_OK) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_ok);
            mTipsTextView.setText("请亮出OK手势~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HAND_SCISSOR) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_scissor);
            mTipsTextView.setText("请比个剪刀手~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HAND_PISTOL) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_pistol);
            mTipsTextView.setText("请比个手枪~");
        }else if((actionNum & STMobileHumanActionNative.ST_MOBILE_HAND_FINGER_INDEX) > 0){
            mTipsImageView.setImageResource(R.drawable.ic_trigger_one_finger);
            mTipsTextView.setText("请伸出食指~");
        }else{
            mTipsImageView.setImageBitmap(null);
            mTipsTextView.setText("");
            mTipsLayout.setVisibility(View.INVISIBLE);
        }

        mTipsLayout.setVisibility(View.VISIBLE);
        if(mTipsRunnable != null){
            mTipsHandler.removeCallbacks(mTipsRunnable);
        }

        mTipsRunnable = new Runnable() {
            @Override
            public void run() {
                mTipsLayout.setVisibility(View.GONE);
            }
        };

        mTipsHandler.postDelayed(mTipsRunnable, 2000);
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

                mSettingOptions.setVisibility(View.INVISIBLE);
                mIsSettingOptionsOpen = false;
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

                mSettingOptions.setVisibility(View.INVISIBLE);
                mIsSettingOptionsOpen = false;
                break;

            case R.id.iv_setting_options_switch:
                if(mIsSettingOptionsOpen){
                    mSettingOptions.setVisibility(View.INVISIBLE);
                    mIsSettingOptionsOpen = false;

                    mShowOriginBtn1.setVisibility(View.VISIBLE);
                    mShowOriginBtn2.setVisibility(View.INVISIBLE);
                }else{
                    mSettingOptions.setVisibility(View.VISIBLE);
                    mIsSettingOptionsOpen = true;

                    mShowOriginBtn1.setVisibility(View.INVISIBLE);
                    mShowOriginBtn2.setVisibility(View.VISIBLE);

                    mStickerOptions.setVisibility(View.INVISIBLE);
                    mStickerIcons.setVisibility(View.INVISIBLE);
                }

                mStickerOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_sticker_options_switch);
                mBeautyOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_beauty_options_switch);
                mStickerOptionsSwitchText = (TextView) findViewById(R.id.tv_sticker_options_switch);
                mBeautyOptionsSwitchText = (TextView) findViewById(R.id.tv_beauty_options_switch);

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
                break;

            case R.id.id_gl_sv:
                mStickerOptions.setVisibility(View.INVISIBLE);
                mStickerIcons.setVisibility(View.INVISIBLE);

                mStickerOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_sticker_options_switch);
                mBeautyOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_beauty_options_switch);
                mStickerOptionsSwitchText = (TextView) findViewById(R.id.tv_sticker_options_switch);
                mBeautyOptionsSwitchText = (TextView) findViewById(R.id.tv_beauty_options_switch);

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

                mSettingOptions.setVisibility(View.INVISIBLE);
                mIsSettingOptionsOpen = false;

                mShowOriginBtn1.setVisibility(View.VISIBLE);
                mShowOriginBtn2.setVisibility(View.INVISIBLE);

                break;

            case R.id.tv_change_camera:
                if(mCameraDisplay != null){
                    mCameraDisplay.switchCamera();
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

    private CameraDisplaySingleInput.ChangePreviewSizeListener mSingleInputChangePreviewSizeListener = new CameraDisplaySingleInput.ChangePreviewSizeListener() {
        @Override
        public void onChangePreviewSize(final int previewW, final int previewH) {
            CameraActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPreviewFrameLayout.requestLayout();
                }
            });
        }
    };

    private CameraDisplayDoubleInput.ChangePreviewSizeListener mDoubleInputChangePreviewSizeListener = new CameraDisplayDoubleInput.ChangePreviewSizeListener() {
        @Override
        public void onChangePreviewSize(final int previewW, final int previewH) {
            CameraActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPreviewFrameLayout.requestLayout();
                }
            });
        }
    };

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

        mCameraDisplay.onResume();
        mCameraDisplay.setShowOriginal(false);

        resetTimer();
        mIsRecording = false;
        startShowCpuInfo();
        mIsPaused = false;
    }

    private boolean mIsPaused = false;

    @Override
    protected void onPause() {
        super.onPause();
        LogUtils.i(TAG, "onPause");

        //if is recording, stop recording
        mIsPaused = true;
        if(mIsRecording){
            mHandler.removeMessages(MSG_STOP_RECORDING);
            stopRecording();
            enableShowLayouts();

            if(mVideoFilePath != null){
                File file = new File(mVideoFilePath);
                if(file != null){
                    file.delete();
                }
            }

            resetTimer();
            mIsRecording = false;
        }

        if (!mPermissionDialogShowing) {
            mAccelerometer.stop();
            mCameraDisplay.onPause();
        }
        stopShowCpuInfo();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        mCameraDisplay.onDestroy();
        mStickerAdapters.clear();
        mStickerlists.clear();
        mFilterItem.clear();
        mObjectList.clear();
        finish();
    }

    private float getProcessCpuRate() {
        long totalCpuTime1 = getTotalCpuTime();
        long processCpuTime1 = getAppCpuTime();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long totalCupTime2 = getTotalCpuTime();
        long processCpuTime2 = getAppCpuTime();

        if(totalCpuTime1 != totalCupTime2){
            float rate = (float) (100 * (processCpuTime2 - processCpuTime1) / (totalCupTime2 - totalCpuTime1));
            if(rate >= 0.0f || rate <= 100.0f){
                mCurrentCpuRate = rate;
            }
        }

        return mCurrentCpuRate;
    }

    private long getTotalCpuTime() {
        // 获取系统总CPU使用时间
        String[] cpuInfos = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Long.parseLong(cpuInfos[2])
                + Long.parseLong(cpuInfos[3]) + Long.parseLong(cpuInfos[4])
                + Long.parseLong(cpuInfos[6]) + Long.parseLong(cpuInfos[5])
                + Long.parseLong(cpuInfos[7]) + Long.parseLong(cpuInfos[8]);
    }

    private long getAppCpuTime() {
        //获取应用占用的CPU时间
        String[] cpuInfos = null;
        int pid = android.os.Process.myPid();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/" + pid + "/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Long.parseLong(cpuInfos[13])
                + Long.parseLong(cpuInfos[14]) + Long.parseLong(cpuInfos[15])
                + Long.parseLong(cpuInfos[16]);

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

            mHandler.sendEmptyMessage(CameraActivity.MSG_SAVED_IMG);
        }
    }


    void showFaceAttributeInfo() {
        if (mCameraDisplay.getFaceAttributeString() != null) {
            if (mCameraDisplay.getFaceAttributeString().equals("noFace")) {
                mAttributeText.setText("");
            } else {
                mAttributeText.setText("第1张人脸: " + mCameraDisplay.getFaceAttributeString());
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eventAction = event.getAction();
        Rect indexRect = mCameraDisplay.getIndexRect();

        if(mIsStickerOptionsOpen || mIsBeautyOptionsOpen || mIsSettingOptionsOpen){
            closeTableView();
        }

        switch (eventAction) {
            case MotionEvent.ACTION_DOWN:
                if((int) event.getX() >= indexRect.left && (int) event.getX() <= indexRect.right &&
                        (int) event.getY() >= indexRect.top && (int) event.getY() <= indexRect.bottom){
                    mCanMove = true;
                    mCameraDisplay.disableObjectTracking();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if(mCanMove){
                    mIndexX = (int) event.getX();
                    mIndexY = (int) event.getY();
                    mCameraDisplay.setIndexRect(mIndexX - indexRect.width()/2, mIndexY -indexRect.width()/2, true);
                }

                break;

            case MotionEvent.ACTION_UP:

                if(mCanMove){
                    mIndexX = (int) event.getX();
                    mIndexY = (int) event.getY();
                    mCameraDisplay.setIndexRect(mIndexX - indexRect.width()/2, mIndexY - indexRect.width()/2, false);
                    mCameraDisplay.setObjectTrackRect();

                    mCanMove = false;
                }
                break;
        }

        return false;
    }


    private void drawObjectImage(final Rect rect, final boolean needDrawRect){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mSurfaceViewOverlap.getHolder().getSurface().isValid()) {
                    return;
                }
                Canvas canvas = mSurfaceViewOverlap.getHolder().lockCanvas();
                if (canvas == null)
                    return;

                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                if(needDrawRect){
                    canvas.drawRect(rect, mPaint);
                }
                canvas.drawBitmap(mGuideBitmap, new Rect(0, 0, mGuideBitmap.getWidth(), mGuideBitmap.getHeight()), rect, mPaint);

                mSurfaceViewOverlap.getHolder().unlockCanvasAndPost(canvas);
            }
        });
    }

    private void clearObjectImage(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mSurfaceViewOverlap.getHolder().getSurface().isValid()) {
                    return;
                }
                Canvas canvas = mSurfaceViewOverlap.getHolder().lockCanvas();
                if (canvas == null)
                    return;

                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                mSurfaceViewOverlap.getHolder().unlockCanvasAndPost(canvas);
            }
        });
    }

    private void drawFaceExtraPoints(final STPoint[] points){
        if(points == null || points.length == 0){
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mSurfaceViewOverlap.getHolder().getSurface().isValid()) {
                    return;
                }
                Canvas canvas = mSurfaceViewOverlap.getHolder().lockCanvas();
                if (canvas == null)
                    return;

                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                STUtils.drawPoints(canvas, mPaint, points);

                mSurfaceViewOverlap.getHolder().unlockCanvasAndPost(canvas);
            }
        });
    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder && mCameraDisplay != null)
                mCameraDisplay.setVideoEncoder((MediaVideoEncoder)encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder && mCameraDisplay != null)
                mCameraDisplay.setVideoEncoder(null);
        }
    };

    private MediaMuxerWrapper mMuxer;
    private void startRecording() {
        try {
            mMuxer = new MediaMuxerWrapper(".mp4");	// if you record audio only, ".m4a" is also OK.

            // for video capturing
            new MediaVideoEncoder(mMuxer, mMediaEncoderListener, mCameraDisplay.getPreviewWidth(), mCameraDisplay.getPreviewHeight());

            if(mIsHasAudioPermission){
                // for audio capturing
                new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
            }

            mMuxer.prepare();
            mMuxer.startRecording();
        } catch (final IOException e) {
            Log.e(TAG, "startCapture:", e);
        }
    }

    private void stopRecording() {
        if (mMuxer != null) {
            mVideoFilePath = mMuxer.getFilePath();
            mMuxer.stopRecording();
            //mMuxer = null;
        }
        
        System.gc();
    }

    private void updateTimer(){
        String timeInfo;
        mTimeSeconds++;

        if(mTimeSeconds >= 60){
            mTimeMinutes++;
            mTimeSeconds = 0;
        }

        if(mTimeSeconds < 10 && mTimeMinutes < 10){
            timeInfo = "00:0"+ mTimeMinutes + ":" + "0" + mTimeSeconds;
        }else if(mTimeSeconds < 10 && mTimeMinutes >= 10){
            timeInfo = "00:"+ mTimeMinutes + ":" + "0" + mTimeSeconds;
        }else if(mTimeSeconds >= 10 && mTimeMinutes < 10){
            timeInfo = "00:0"+ mTimeMinutes + ":" + mTimeSeconds;
        }else {
            timeInfo = "00:"+ mTimeMinutes + ":" + mTimeSeconds;
        }

        mShowShortVideoTime.setText(timeInfo);
    }

    private void resetTimer(){
        mTimeMinutes = 0;
        mTimeSeconds = 0;
        if(mTimer != null){
            mTimer.cancel();
        }
        if(mTimerTask != null){
            mTimerTask.cancel();
        }

        mShowShortVideoTime.setText("00:00:00");
        mShowShortVideoTime.setVisibility(View.INVISIBLE);
    }

    private void closeTableView(){
        mStickerOptions.setVisibility(View.INVISIBLE);
        mStickerIcons.setVisibility(View.INVISIBLE);

        mStickerOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_sticker_options_switch);
        mBeautyOptionsSwitchIcon = (ImageView) findViewById(R.id.iv_beauty_options_switch);
        mStickerOptionsSwitchText = (TextView) findViewById(R.id.tv_sticker_options_switch);
        mBeautyOptionsSwitchText = (TextView) findViewById(R.id.tv_beauty_options_switch);

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

        mSettingOptions.setVisibility(View.INVISIBLE);
        mIsSettingOptionsOpen = false;

        mShowOriginBtn1.setVisibility(View.VISIBLE);
        mShowOriginBtn2.setVisibility(View.INVISIBLE);
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

    private void disableShowLayouts(){
        mShowOriginBtn1.setVisibility(View.INVISIBLE);

        findViewById(R.id.tv_change_camera).setVisibility(View.INVISIBLE);
        mSettingOptionsSwitch.setVisibility(View.INVISIBLE);

        mSelectionMode.setVisibility(View.INVISIBLE);
        mModeVideo.setVisibility(View.INVISIBLE);

        mBeautyOptionsSwitch.setVisibility(View.INVISIBLE);
        mStickerOptionsSwitch.setVisibility(View.INVISIBLE);
    }

    private void enableShowLayouts(){
        mShowOriginBtn1.setVisibility(View.VISIBLE);

        findViewById(R.id.tv_change_camera).setVisibility(View.VISIBLE);
        mSettingOptionsSwitch.setVisibility(View.VISIBLE);

        mModeVideo.setVisibility(View.VISIBLE);

        mBeautyOptionsSwitch.setVisibility(View.VISIBLE);
        mStickerOptionsSwitch.setVisibility(View.VISIBLE);
    }

}
