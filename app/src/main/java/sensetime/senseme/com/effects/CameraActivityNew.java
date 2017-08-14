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
import android.graphics.drawable.GradientDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sensetime.stmobile.STMobileHumanActionNative;
import com.sensetime.stmobile.model.STPoint;

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
import java.util.List;

import sensetime.senseme.com.effects.adapter.BeautifyOptionsAdapter;
import sensetime.senseme.com.effects.adapter.FilterAdapter;
import sensetime.senseme.com.effects.adapter.ObjectAdapter;
import sensetime.senseme.com.effects.adapter.StickerAdapter;
import sensetime.senseme.com.effects.display.CameraDisplay;
import sensetime.senseme.com.effects.glutils.STUtils;
import sensetime.senseme.com.effects.utils.Accelerometer;
import sensetime.senseme.com.effects.utils.FileUtils;
import sensetime.senseme.com.effects.utils.LogUtils;
import sensetime.senseme.com.effects.utils.STLicenseUtils;
import sensetime.senseme.com.effects.view.FilterItem;
import sensetime.senseme.com.effects.view.ObjectItem;
import sensetime.senseme.com.effects.view.StickerItem;

/**
 * Created by ios-dev on 17/8/10.
 */

public class CameraActivityNew extends Activity implements View.OnClickListener{
    private final static String TAG = "CameraActivity";
//    private Accelerometer mAccelerometer = null;
    private CameraDisplay mCameraDisplay;
    private FrameLayout mPreviewFrameLayout;

    private RecyclerView mStickersRecycleView;
    private StickerAdapter mStickerAdapter;
    private ArrayList<StickerItem> mStickerList;

    private FilterAdapter mFilterAdapter;
    private ArrayList<FilterItem> mFilterItem = null;
    private ObjectAdapter mObjectAdapter;
    private List<ObjectItem> mObjectList;

    private ListView mBeautifyListView;
    private BeautifyOptionsAdapter mBeautifyAdapter;

    private TextView mFullResolutionTv;
    private TextView mSmallerResolutionTv;
    private TextView mSavingTv;
    private TextView mAttributeText;

    private boolean mIsAttributeOpen = false;
    private boolean mIsBeautifyOpen = true;
    private boolean mIsStickerOpen = false;
    private boolean mIsFilterOpen = false;
    private boolean mIsObjectOpen = false;
    private LinearLayout mOptions, mAttributeSwicth, mBeautifySwicth, mStickerSwicth;
    private LinearLayout mFaceAttribute, mBeautify, mSticker, mFilter, mObject;
    private LinearLayout mFilterStrength;
    private SeekBar mFilterSeekBar;
    private TextView mAttributeLayoutSwicth;
    private TextView mAttributeOpen, mAttributeClose;
    private TextView mBeautifyLayoutSwicth;
    private TextView mBeautifyOpen, mBeautifyClose;
    private TextView mStickerLayoutSwicth;
    private Button mShowOriginBtn;

    private float[] mBeautifyParams = {0.36f, 0.74f, 0.30f, 0.13f, 0.11f, 0.1f};

    private RelativeLayout mTipsLayout;
    private TextView mTipsTextView;
    private ImageView mTipsImageView;
    private Context mContext;

    public static final int MSG_SAVING_IMG = 1;
    public static final int MSG_SAVED_IMG = 2;
    public static final int MSG_DRAW_OBJECT_IMAGE_AND_RECT = 3;
    public static final int MSG_DRAW_OBJECT_IMAGE = 4;
    public static final int MSG_CLEAR_OBJECT = 5;
    public static final int MSG_MISSED_OBJECT_TRACK = 6;
    public static final int MSG_DRAW_FACE_EXTRA_POINTS = 7;

    private static final int PERMISSION_REQUEST_WRITE_PERMISSION = 101;
    private boolean mPermissionDialogShowing = false;
    private Thread mCpuInofThread;
    private float mCurrentCpuRate = 0.0f;

    private SurfaceView mSurfaceViewOverlap;
    private Bitmap mGuideBitmap;
    private Paint mPaint = new Paint();

    private int mIndexX = 0, mIndexY = 0;
    private boolean mCanMove = false;


    SensemeView sensemeView;

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
        //LogUtils.setIsLoggable(true);

        initView();
        initEvents();
    }

    private void initView() {
        //copy model file to sdcard
//        FileUtils.copyModelFiles(this);

//        mAccelerometer = new Accelerometer(getApplicationContext());

//        sensemeView = (SensemeView) findViewById(R.id.sensemeView);
        sensemeView.init(this);


//        sensemeView.setLeftMeiMao();
//        sensemeView.setRightMeiMao();
//
//        sensemeView.setYanJieMao();
//        sensemeView.setYanXian();
//        sensemeView.setYanYing();

//        sensemeView.setSaihong(R.mipmap.ear_000);

        sensemeView.setDownMouse(178/255f,18/255f,32/255f,0.6f);
        sensemeView.setUpMouse(178/255f,18/255f,32/255f,0.6f);

//        GLSurfaceView glSurfaceView = (GLSurfaceView) findViewById(R.id.id_gl_sv);
//        mSurfaceViewOverlap = (SurfaceView) findViewById(R.id.surfaceViewOverlap);
//        mPreviewFrameLayout = (FrameLayout) findViewById(R.id.id_preview_layout);
//        mCameraDisplay = new CameraDisplay(getApplicationContext(), mListener, glSurfaceView);
//        mCameraDisplay.setHandler(mHandler);

        //layout elements
//        mStickersRecycleView = (RecyclerView) findViewById(R.id.rv_stickers);
//        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
//        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
//        mStickersRecycleView.setLayoutManager(layoutManager);
//        mStickersRecycleView.addItemDecoration(new SpaceItemDecoration(0));
//        mStickersRecycleView.getBackground().setAlpha(100);
//
//        //copy sticker zips to sdcard and get file paths
//        mStickerList = FileUtils.getStickerFiles(this);
//        mStickerAdapter = new StickerAdapter(mStickerList, this);
//
//        //copy filter models to sdcard and get file paths
//        mFilterItem = FileUtils.getFilterFiles(this);
//        mFilterAdapter = new FilterAdapter(mFilterItem, this);
//
//        mObjectList = FileUtils.getObjectList();
//        mObjectAdapter = new ObjectAdapter(mObjectList, this);
//
//        mBeautifyListView = (ListView) findViewById(R.id.list_beautify_options);
//        mBeautifyAdapter = new BeautifyOptionsAdapter(this,mCameraDisplay, null);
//
//
//        mSmallerResolutionTv = (TextView) findViewById(R.id.tv_smaller_resolution);
//        mFullResolutionTv = (TextView) findViewById(R.id.tv_bigger_resolution);
//        mSavingTv = (TextView) findViewById(R.id.tv_saving_image);
//
//        mOptions = (LinearLayout) findViewById(R.id.layout_options);
//        mAttributeSwicth = (LinearLayout) findViewById(R.id.layout_attribute_swicth);
//        mFaceAttribute = (LinearLayout) findViewById(R.id.ll_attribute);
//        mFaceAttribute.setOnClickListener(this);
//        mBeautify = (LinearLayout) findViewById(R.id.ll_beauty);
//        mBeautify.setOnClickListener(this);
//
//        mAttributeLayoutSwicth = (TextView) findViewById(R.id.tv_attribute_layout_swicth);
//        mAttributeLayoutSwicth.setOnClickListener(this);
//        mAttributeOpen = (TextView) findViewById(R.id.tv_attribute_open);
//        mAttributeOpen.setOnClickListener(this);
//        mAttributeClose = (TextView) findViewById(R.id.tv_attribute_close);
//        mAttributeClose.setOnClickListener(this);
//
//        mBeautifySwicth = (LinearLayout) findViewById(R.id.layout_beauty_swicth);
//        mBeautifyLayoutSwicth = (TextView) findViewById(R.id.tv_beautify_layout_swicth);
//        mBeautifyLayoutSwicth.setOnClickListener(this);
//        mBeautifyOpen = (TextView) findViewById(R.id.tv_beautify_open);
//        mBeautifyOpen.setOnClickListener(this);
//        mBeautifyClose = (TextView) findViewById(R.id.tv_beautify_close);
//        mBeautifyClose.setOnClickListener(this);
//
//        mSticker = (LinearLayout) findViewById(R.id.ll_sticker);
//        mSticker.setOnClickListener(this);
//
//        mStickerLayoutSwicth = (TextView) findViewById(R.id.tv_sticker_layout_swicth);
//        mStickerLayoutSwicth.setOnClickListener(this);
//
//        mFilter = (LinearLayout) findViewById(R.id.ll_filter);
//        mFilter.setOnClickListener(this);
//
//        mFilterStrength = (LinearLayout) findViewById(R.id.ll_filter_strength);
//        mFilterSeekBar = (SeekBar) findViewById(R.id.sb_filter_strength);
//
//        mObject = (LinearLayout) findViewById(R.id.ll_object);
//        mObject.setVisibility(View.VISIBLE);
//        mObject.setOnClickListener(this);
//
//        mTipsLayout = (RelativeLayout) findViewById(R.id.tv_layout_tips);
//        mAttributeText = (TextView) findViewById(R.id.tv_face_attribute);
//        mAttributeText.setVisibility(View.VISIBLE);
//        mTipsTextView = (TextView) findViewById(R.id.tv_text_tips);
//        mTipsImageView = (ImageView) findViewById(R.id.iv_image_tips);
//        mTipsLayout.setVisibility(View.GONE);
    }

    private void initEvents() {
        // authority
//        if (!STLicenseUtils.checkLicense(this)) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(getApplicationContext(), "You should be authorized first!", Toast.LENGTH_SHORT).show();
//                }
//            });
//            finish();
//            return;
//        }
//
//
//        mSurfaceViewOverlap.setZOrderOnTop(true);
//        mSurfaceViewOverlap.setZOrderMediaOverlay(true);
//        mSurfaceViewOverlap.getHolder().setFormat(PixelFormat.TRANSLUCENT);
//
//        mPaint = new Paint();
//        mPaint.setColor(Color.rgb(240, 100, 100));
//        int strokeWidth = 10;
//        mPaint.setStrokeWidth(strokeWidth);
//        mPaint.setStyle(Paint.Style.STROKE);
//
//        // change sticker
//        mStickerAdapter.setClickStickerListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mTipsLayout.setVisibility(View.GONE);
//
//                int position = Integer.parseInt(v.getTag().toString());
//                mStickerAdapter.setSelectedPosition(position);
//
//                if(position == 0){
//                    mIsStickerOpen = false;
//                    mCameraDisplay.enableSticker(false);
//                }else if(position == 1){
//                    mCameraDisplay.enableSticker(true);
//                    mCameraDisplay.setShowSticker(null);
//                    mIsStickerOpen = true;
//                }else{
//                    mCameraDisplay.enableSticker(true);
//                    mCameraDisplay.setShowSticker(mStickerList.get(position).path);
//                    mIsStickerOpen = true;
//
//                    int action = mCameraDisplay.getStickerTriggerAction();
//                    showActiveTips(action);
//                }
//                mStickerAdapter.notifyDataSetChanged();
//            }
//        });
//
//        mFilterAdapter.setClickFilterListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                int position = Integer.parseInt(v.getTag().toString());
//                mFilterAdapter.setSelectedPosition(position);
//
//                if(position == 0){
//                    mIsFilterOpen = false;
//                    mCameraDisplay.enableFilter(false);
//                    mFilterSeekBar.setEnabled(false);
//                }else if(position == 1){
//                    mCameraDisplay.setFilterStyle(null);
//                    mIsFilterOpen = true;
//                    mCameraDisplay.enableFilter(true);
//                    mFilterSeekBar.setEnabled(false);
//                }else{
//                    mCameraDisplay.setFilterStyle(mFilterItem.get(position).model);
//                    mIsFilterOpen = true;
//                    mCameraDisplay.enableFilter(true);
//                    mFilterSeekBar.setEnabled(true);
//                }
//
//                mFilterAdapter.notifyDataSetChanged();
//            }
//        });
//
//
//        mObjectAdapter.setClickObjectListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                int position = Integer.parseInt(v.getTag().toString());
//                mObjectAdapter.setSelectedPosition(position);
//
//                if(position == 0){
//                    mIsObjectOpen = false;
//                    mCameraDisplay.enableObject(false);
//                }else if(position == 1){
//                    mIsObjectOpen = true;
//                    mCameraDisplay.enableObject(false);
//                    mCameraDisplay.resetObjectTrack();
//                }else{
//                    mIsObjectOpen = true;
//                    mCameraDisplay.enableObject(false);
//                    mGuideBitmap = BitmapFactory.decodeResource(mContext.getResources(), mObjectList.get(position).drawableID);
//                    mCameraDisplay.resetIndexRect();
//                }
//
//                mObjectAdapter.notifyDataSetChanged();
//            }
//        });
//
//        mFilterSeekBar.setProgress(50);
//        if(!mIsFilterOpen){mFilterSeekBar.setEnabled(false);}
//        mFilterSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                float param = 0;
//                param = (float)progress/100;
//                mCameraDisplay.setFilterStrength(param);
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {}
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {}
//        });
//
//        mShowOriginBtn = (Button)findViewById(R.id.tv_show_origin);
//        mShowOriginBtn.setOnTouchListener(new View.OnTouchListener() {
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                // TODO Auto-generated method stub
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    mCameraDisplay.setShowOriginal(true);
//                    clearObjectImage();
//                } else if (event.getAction() == MotionEvent.ACTION_UP) {
//                    mCameraDisplay.setShowOriginal(false);
//                }
//                return true;
//            }
//        });
//        mShowOriginBtn.setVisibility(View.VISIBLE);
//
//        mSmallerResolutionTv.setOnClickListener(this);
//        mFullResolutionTv.setOnClickListener(this);
//        GradientDrawable mFullResolutionTvGrad = (GradientDrawable) mFullResolutionTv.getBackground();
//        mFullResolutionTvGrad.setColor(Color.parseColor("#00000000"));
//        GradientDrawable mSmallerResolutionTvGrad = (GradientDrawable) mSmallerResolutionTv.getBackground();
//        mSmallerResolutionTvGrad.setColor(Color.parseColor("#fe5553"));
//        mSmallerResolutionTv.setClickable(false);
//        mBeautify.setOnClickListener(this);
//        findViewById(R.id.tv_capture).setOnClickListener(this);
//        findViewById(R.id.tv_cancel).setOnClickListener(this);
//
//        findViewById(R.id.tv_change_camera).setOnClickListener(this);
//        findViewById(R.id.tv_change_camera).setVisibility(View.VISIBLE);
//
//        // switch camera
//        findViewById(R.id.id_tv_changecamera).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCameraDisplay.switchCamera();
//            }
//        });
//
//        // change preview size
//        findViewById(R.id.id_tv_changepreviewsize).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCameraDisplay.changePreviewSize(0);
//            }
//        });
//
//        // frame utility
//        mCameraDisplay.setFpsChangeListener(new CameraDisplay.FpsChangeListener() {
//            @Override
//            public void onFpsChanged(final int value) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ((TextView) findViewById(R.id.tv_frame_radio)).setText(String.valueOf(value));
//                    }
//                });
//            }
//        });
//
//        // frame utility
//        mCameraDisplay.setFaceAttributeChangeListener(new CameraDisplay.FaceAttributeChangeListener() {
//            @Override
//            public void onFaceAttributeChanged(final String attribute) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mCameraDisplay.getFaceAttribute()) {
//                            showFaceAttributeInfo();
//                        }
//                    }
//                });
//            }
//        });
//
//        mIsBeautifyOpen = true;
//        mCameraDisplay.enableBeautify(true);
//        mBeautifyOpen.setBackgroundColor(Color.parseColor("#fe5553"));
//        mBeautifyClose.setBackgroundColor(Color.alpha(0));
//        mBeautifyAdapter.OpenBeautify();
//        mBeautifyAdapter.notifyDataSetChanged();
//
//        mBeautify.setBackgroundColor(Color.parseColor("#fe5553"));

    }

    private void startShowCpuInfo() {
//        mCpuInofThread = new Thread() {
//            @Override
//            public void run() {
//                super.run();
//                while (true) {
//                    final float rate = getProcessCpuRate();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            ((TextView) findViewById(R.id.tv_cpu_radio)).setText(String.valueOf(rate));
//                        }
//                    });
//                    try {
//                        sleep(500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        };
//        mCpuInofThread.start();
    }

    private void stopShowCpuInfo() {
        if (mCpuInofThread != null) {
            mCpuInofThread.interrupt();
            //mCpuInofThread.stop();
            mCpuInofThread = null;
        }
    }

    private void showActiveTips(int actionNum) {
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
            mTipsImageView.setImageResource(R.drawable.ic_trigger_thumb);
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
        }else{
            mTipsLayout.setVisibility(View.INVISIBLE);
        }

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mTipsLayout.setVisibility(View.GONE);
            }
        };
        handler.postDelayed(runnable, 2000);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_attribute:
                mBeautify.setEnabled(false);
                mSticker.setEnabled(false);
                mFilter.setEnabled(false);
                mObject.setEnabled(false);

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
                mCameraDisplay.enableFaceAttribute(true);
                mAttributeText.setVisibility(View.VISIBLE);
                mAttributeOpen.setBackgroundColor(Color.parseColor("#fe5553"));
                mAttributeClose.setBackgroundColor(Color.alpha(0));
                break;

            case R.id.tv_attribute_close:
                mIsAttributeOpen = false;
                mCameraDisplay.enableFaceAttribute(false);
                mAttributeText.setVisibility(View.INVISIBLE);
                mAttributeOpen.setBackgroundColor(Color.alpha(0));
                mAttributeClose.setBackgroundColor(Color.parseColor("#fe5553"));
                break;

            case R.id.tv_attribute_layout_swicth:
                enableSwicthButton();

                mAttributeSwicth.setVisibility(View.INVISIBLE);
                mAttributeLayoutSwicth.setVisibility(View.INVISIBLE);
                mOptions.setVisibility(View.VISIBLE);
                mShowOriginBtn.setVisibility(View.VISIBLE);
                if(mIsAttributeOpen){
                    mFaceAttribute.setBackgroundColor(Color.parseColor("#fe5553"));
                }else {
                    mFaceAttribute.setBackgroundColor(Color.alpha(0));
                }
                break;

            case R.id.ll_beauty:
                mFaceAttribute.setEnabled(false);
                mSticker.setEnabled(false);
                mFilter.setEnabled(false);
                mObject.setEnabled(false);

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
                mCameraDisplay.enableBeautify(true);
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
                mCameraDisplay.enableBeautify(false);
                mBeautifyParams = mCameraDisplay.getBeautyParams();
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
                    mBeautifyParams = mCameraDisplay.getBeautyParams();
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
                mObject.setEnabled(false);

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
                mObject.setEnabled(false);

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

                if(mIsObjectOpen){
                    mObject.setBackgroundColor(Color.parseColor("#fe5553"));
                }else{
                    mObject.setBackgroundColor(Color.alpha(0));
                }
                break;

            case R.id.ll_object:
                mFaceAttribute.setEnabled(false);
                mBeautify.setEnabled(false);
                mSticker.setEnabled(false);
                mFilter.setEnabled(false);

                mOptions.setVisibility(View.INVISIBLE);
                mShowOriginBtn.setVisibility(View.INVISIBLE);
                mStickersRecycleView.setVisibility(View.VISIBLE);
                mStickersRecycleView.setAdapter(mObjectAdapter);
                mStickerLayoutSwicth.setVisibility(View.VISIBLE);
                mFilterStrength.setVisibility(View.GONE);

                if(mCameraDisplay.getCameraID() == 1 && !mIsObjectOpen){
                    mCameraDisplay.switchCamera();
                }

                break;

            case R.id.tv_smaller_resolution:
                // switch to smaller resolution
                GradientDrawable mFullResolutionTvGrad1 = (GradientDrawable) mFullResolutionTv.getBackground();
                mFullResolutionTvGrad1.setColor(Color.parseColor("#00000000"));
                GradientDrawable mSmallerResolutionTvGrad1 = (GradientDrawable) mSmallerResolutionTv.getBackground();
                mSmallerResolutionTvGrad1.setColor(Color.parseColor("#fe5553"));
                mCameraDisplay.changePreviewSize(1);
                mSmallerResolutionTv.setClickable(false);
                mFullResolutionTv.setClickable(true);
                break;
            case R.id.tv_bigger_resolution:
                // switch to bigger resolution
                GradientDrawable mFullResolutionTvGrad2 = (GradientDrawable) mFullResolutionTv.getBackground();
                mFullResolutionTvGrad2.setColor(Color.parseColor("#fe5553"));
                GradientDrawable mSmallerResolutionTvGrad2 = (GradientDrawable) mSmallerResolutionTv.getBackground();
                mSmallerResolutionTvGrad2.setColor(Color.parseColor("#00000000"));
                mCameraDisplay.changePreviewSize(0);
                mFullResolutionTv.setClickable(false);
                mSmallerResolutionTv.setClickable(true);
                break;

            case R.id.tv_change_camera:
                if(mCameraDisplay != null){
                    mCameraDisplay.switchCamera();
                }

                break;
            case R.id.tv_capture:
                if (this.isWritePermissionAllowed()) {
                    mSavingTv.setVisibility(View.VISIBLE);
                    mCameraDisplay.setHandler(mHandler);
                    mCameraDisplay.setSaveImage();
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

    private CameraDisplay.ChangePreviewSizeListener mListener = new CameraDisplay.ChangePreviewSizeListener() {
        @Override
        public void onChangePreviewSize(final int previewW, final int previewH) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPreviewFrameLayout.requestLayout();
                }
            });
        }
    };

    @Override
    protected void onResume() {
        LogUtils.i(TAG, "onResume");
        super.onResume();
//        mAccelerometer.start();
//        mCameraDisplay.onResume();
        sensemeView.onResume();

        //不支持640*480
//        if (mCameraDisplay != null && !mCameraDisplay.getSupportPreviewsize(0)) {
//            mSmallerResolutionTv.setClickable(false);
//            GradientDrawable mFullResolutionTvGrad1 = (GradientDrawable) mFullResolutionTv.getBackground();
//            mFullResolutionTvGrad1.setColor(Color.parseColor("#fe5553"));
//            GradientDrawable mSmallerResolutionTvGrad1 = (GradientDrawable) mSmallerResolutionTv.getBackground();
//            mSmallerResolutionTvGrad1.setColor(Color.parseColor("#00000000"));
//            //不支持1280*720
//        } else if (mCameraDisplay != null && !mCameraDisplay.getSupportPreviewsize(1)) {
//            mFullResolutionTv.setClickable(false);
//            GradientDrawable mFullResolutionTvGrad2 = (GradientDrawable) mFullResolutionTv.getBackground();
//            mFullResolutionTvGrad2.setColor(Color.parseColor("#00000000"));
//            GradientDrawable mSmallerResolutionTvGrad2 = (GradientDrawable) mSmallerResolutionTv.getBackground();
//            mSmallerResolutionTvGrad2.setColor(Color.parseColor("#fe5553"));
//        }

//        startShowCpuInfo();
    }

    @Override
    protected void onPause() {
        LogUtils.i(TAG, "onPause");
        super.onPause();
        if (!mPermissionDialogShowing) {
//            mAccelerometer.stop();
//            mCameraDisplay.onPause();
            sensemeView.onPause();
            finish();
        }
//        stopShowCpuInfo();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
//        mCameraDisplay.onDestroy();
        sensemeView.onDestory();
//        mStickerList.clear();
//        mFilterItem.clear();
//        mObjectList.clear();
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

            mHandler.sendEmptyMessage(MSG_SAVED_IMG);
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

    private void enableSwicthButton(){
        mFaceAttribute.setEnabled(true);
        mBeautify.setEnabled(true);
        mSticker.setEnabled(true);
        mFilter.setEnabled(true);
        mObject.setEnabled(true);
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        int eventAction = event.getAction();
//        Rect indexRect = mCameraDisplay.getIndexRect();
//
//        switch (eventAction) {
//            case MotionEvent.ACTION_DOWN:
//                if((int) event.getX() >= indexRect.left && (int) event.getX() <= indexRect.right &&
//                        (int) event.getY() >= indexRect.top && (int) event.getY() <= indexRect.bottom){
//                    mCanMove = true;
//                    mCameraDisplay.disableObjectTracking();
//                }
//                break;
//
//            case MotionEvent.ACTION_MOVE:
//                if(mCanMove){
//                    mIndexX = (int) event.getX();
//                    mIndexY = (int) event.getY();
//                    mCameraDisplay.setIndexRect(mIndexX - indexRect.width()/2, mIndexY -indexRect.width()/2, true);
//                }
//
//                break;
//
//            case MotionEvent.ACTION_UP:
//
//                if(mCanMove){
//                    mIndexX = (int) event.getX();
//                    mIndexY = (int) event.getY();
//                    mCameraDisplay.setIndexRect(mIndexX - indexRect.width()/2, mIndexY - indexRect.width()/2, false);
//                    mCameraDisplay.setObjectTrackRect();
//
//                    mCanMove = false;
//                }
//                break;
//        }
//
//        return false;
//    }


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

}
