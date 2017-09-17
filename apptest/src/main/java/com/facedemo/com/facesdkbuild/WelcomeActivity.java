package com.facedemo.com.facesdkbuild;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebeauty.com.beautysdk.utils.STLicenseUtils;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class WelcomeActivity extends Activity {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // Set up the login form.
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission has not been granted and must be requested.
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    // Provide an additional rationale to the user if the permission was not granted
                    // and the user would benefit from additional context for the use of the permission.
                }
                // Request the permission. The result will be received in onRequestPermissionResult()
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CAMERA);
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
        STLicenseUtils.checkLicense(this, new STLicenseUtils.OnCheckLicenseListener() {
            @Override
            public void onSuccess() {
                Log.d("info", "onSuccess: 授权成功");
            }

            @Override
            public void onFail() {
                Log.d("info", "onSuccess: 授权失败");
            }
        });
        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(WelcomeActivity.this,MainActivity.class));
            }
        });

    }

    private void checkPremission() {
        final String permission = Manifest.permission.CAMERA;  //相机权限
        final String permission1 = Manifest.permission.WRITE_EXTERNAL_STORAGE; //写入数据权限
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permission1) != PackageManager.PERMISSION_GRANTED) {  //先判断是否被赋予权限，没有则申请权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {  //给出权限申请说明
                ActivityCompat.requestPermissions(WelcomeActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            } else { //直接申请权限
                ActivityCompat.requestPermissions(WelcomeActivity.this, new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, 100); //申请权限，可同时申请多个权限，并根据用户是否赋予权限进行判断
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {  //申请权限的返回值
            case 100:
                int length = grantResults.length;
                final boolean isGranted = length >= 1 && PackageManager.PERMISSION_GRANTED == grantResults[length - 1];
                if (isGranted) {  //如果用户赋予权限，则调用相机
                }else{ //未赋予权限，则做出对应提示

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}

