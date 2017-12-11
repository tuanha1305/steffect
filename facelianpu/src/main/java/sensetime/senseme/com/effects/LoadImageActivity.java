package sensetime.senseme.com.effects;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tatata.hearst.R;

import java.io.File;

import sensetime.senseme.com.effects.glutils.STUtils;
import sensetime.senseme.com.effects.utils.STLicenseUtils;

public class LoadImageActivity extends Activity {

    private Context mContext;
    private String mPicturePath;
    private LinearLayout mModePicture, mSelectionMode, mSelectionPicture;
    private TextView mSelectionVideo;

    private LinearLayout mGalleryButton, mTakePictureButton;

    private final int REQUEST_PICK_IMAGE_GALLERY = 100;
    private final int REQUEST_PICK_IMAGE_TAKE_PHOTO = 101;

    public static final int MODE_DERAWABLE_IMAGE = 1;
    public static final int MODE_GALLERY_IMAGE = 2;
    public static final int MODE_TAKE_PHOTO = 3;

    private static final String PICTURE_PATH = Environment.getExternalStorageDirectory() + "/DCIM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_load_image);

        mContext = this;

        //fix sdk version >23 camera crash
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        initView();

        if (!STLicenseUtils.checkLicense(this)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "请检查License授权！", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initView(){
        mModePicture = (LinearLayout) findViewById(R.id.ll_mode_picture);
        mModePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mModePicture.setVisibility(View.INVISIBLE);
                mSelectionMode.setVisibility(View.VISIBLE);
            }
        });
        mSelectionMode = (LinearLayout) findViewById(R.id.ll_selection_mode);
        mSelectionMode.setVisibility(View.INVISIBLE);
        mSelectionVideo = (TextView) findViewById(R.id.tv_selection_video);
        mSelectionVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(getApplicationContext(), CameraActivity.class));
            }
        });
        mSelectionPicture = (LinearLayout) findViewById(R.id.ll_selection_picture);
        mSelectionPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mModePicture.setVisibility(View.VISIBLE);
                mSelectionMode.setVisibility(View.INVISIBLE);
            }
        });

        mGalleryButton = (LinearLayout) findViewById(R.id.ll_from_gallery);
        mGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, REQUEST_PICK_IMAGE_GALLERY);
            }
        });

        mTakePictureButton = (LinearLayout) findViewById(R.id.ll_from_camera);
        mTakePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                Uri imageUri = Uri.fromFile(new File(PICTURE_PATH, "sensetime.jpg"));
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

                startActivityForResult(intent, REQUEST_PICK_IMAGE_TAKE_PHOTO);
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_PICK_IMAGE_GALLERY:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri uri = data.getData();
                        startImageActivity(MODE_GALLERY_IMAGE, -1, uri);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;

            case REQUEST_PICK_IMAGE_TAKE_PHOTO:
            if (resultCode == RESULT_OK) {
                Uri uri = Uri.fromFile(new File(PICTURE_PATH + "/sensetime.jpg"));

                startImageActivity(MODE_TAKE_PHOTO, -1, uri);
            }

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void startImageActivity(int mode, int drawableIndex, Uri uri){
        Intent intent = new Intent(this.getApplicationContext(), ImageActivity.class);

        Bundle data = new Bundle();
        data.putInt("mode", mode);
        data.putInt("drawableIndex", drawableIndex);

        intent.putExtra("bundle", data);
        intent.putExtra("imageUri", uri);
        this.startActivity(intent);
    }
}
