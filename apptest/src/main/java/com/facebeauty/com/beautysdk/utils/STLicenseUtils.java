package com.facebeauty.com.beautysdk.utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import com.facebeauty.com.beautysdk.httputil.HttpUtils;
import com.sensetime.stmobile.STMobileAuthentificationNative;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
/**
 * Created by leiwang on 2016/12/2.
 */
public class STLicenseUtils {
    private final static String TAG = "STLicenseUtils";
    private final static String PREF_ACTIVATE_CODE_FILE = "activate_code_file";
    private final static String PREF_ACTIVATE_CODE = "activate_code";
    private static final String LICENSE_NAME = "SenseME.lic";
//    public static boolean getCheckLicense(Context context) {
//        return checkLicense(context);
//    }
    /**
     * 检查activeCode合法性
     *
     * @return true, 成功 false,失败
     */
//    private static boolean checkLicense(Context context) {
//        StringBuilder sb = new StringBuilder();
//        InputStreamReader isr = null;
//        BufferedReader br = null;
//        // 读取license文件内容
//        try {
//            isr = new InputStreamReader(context.getResources().getAssets().open(LICENSE_NAME));
//            br = new BufferedReader(isr);
//            String line = null;
//            while ((line = br.readLine()) != null) {
//                sb.append(line).append("\n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (isr != null) {
//                try {
//                    isr.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            if (br != null) {
//                try {
//                    br.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//
//        }
//
//        // license文件为空,则直接返回
//        if (sb.toString().length() == 0) {
//            LogUtils.e(TAG, "read license data error");
//            return false;
//        }
//
//        String licenseBuffer = sb.toString();
//        /**
//         * 以下逻辑为：
//         * 1. 获取本地保存的激活码
//         * 2. 如果没有则生成一个激活码
//         * 3. 如果有, 则直接调用checkActiveCode*检查激活码
//         * 4. 如果检查失败，则重新生成一个activeCode
//         * 5. 如果生成失败，则返回失败，成功则保存新的activeCode，并返回成功
//         */
//        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREF_ACTIVATE_CODE_FILE, Context.MODE_PRIVATE);
//        String activateCode = sp.getString(PREF_ACTIVATE_CODE, null);
//        Integer error = new Integer(-1);
//        if (activateCode == null || (STMobileAuthentificationNative.checkActiveCodeFromBuffer(context, licenseBuffer, licenseBuffer.length(), activateCode, activateCode.length()) != 0)) {
//            LogUtils.e(TAG, "activeCode: " + (activateCode == null));
//            activateCode = STMobileAuthentificationNative.generateActiveCodeFromBuffer(context, licenseBuffer, licenseBuffer.length());
//            if (activateCode != null && activateCode.length() > 0) {
//                SharedPreferences.Editor editor = sp.edit();
//                editor.putString(PREF_ACTIVATE_CODE, activateCode);
//                editor.commit();
//                return true;
//            }
//            LogUtils.e(TAG, "generate license error: " + error);
//            return false;
//        }
//
//        LogUtils.e(TAG, "activeCode: " + activateCode);
//
//        return true;
//    }
    /**
     * 检查activeCode合法性
     *
     * @return true, 成功 false,失败
     */
    private static boolean checkLicense(Context context, InputStream stream) {
        StringBuilder sb = new StringBuilder();
        InputStreamReader isr = null;
        BufferedReader br = null;
        // 读取license文件内容
        try {
            isr = new InputStreamReader(stream);
            br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // license文件为空,则直接返回
        if (sb.toString().length() == 0) {
            LogUtils.e(TAG, "read license data error");
            return false;
        }
        String licenseBuffer = sb.toString();
        /**
         * 以下逻辑为：
         * 1. 获取本地保存的激活码
         * 2. 如果没有则生成一个激活码
         * 3. 如果有, 则直接调用checkActiveCode*检查激活码
         * 4. 如果检查失败，则重新生成一个activeCode
         * 5. 如果生成失败，则返回失败，成功则保存新的activeCode，并返回成功
         */
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREF_ACTIVATE_CODE_FILE, Context.MODE_PRIVATE);
        String activateCode = sp.getString(PREF_ACTIVATE_CODE, null);
        Integer error = new Integer(-1);
        if (activateCode == null || (STMobileAuthentificationNative.checkActiveCodeFromBuffer(context, licenseBuffer, licenseBuffer.length(), activateCode, activateCode.length()) != 0)) {
            LogUtils.e(TAG, "activeCode: " + (activateCode == null));
            activateCode = STMobileAuthentificationNative.generateActiveCodeFromBuffer(context, licenseBuffer, licenseBuffer.length());
            if (activateCode != null && activateCode.length() > 0) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(PREF_ACTIVATE_CODE, activateCode);
                editor.commit();
                return true;
            }
            LogUtils.e(TAG, "generate license error: " + error);
            return false;
        }
        LogUtils.e(TAG, "activeCode: " + activateCode);
        return true;
    }
    public static void checkLicense(final Context context, OnCheckLicenseListener licenseListener) {
        onCheckLicenseListener = licenseListener;
        String path = context.getFilesDir() + "/key.lic";
        File file = new File(path);
        if (file.exists()) {
            try {
                FileInputStream in = new FileInputStream(file);
                if (checkLicense(context, in)) {
//                    return true;
                    if (onCheckLicenseListener != null) {
                        onCheckLicenseListener.onSuccess();
                    }
                } else {
                    getLicense(context);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (onCheckLicenseListener != null) {
                    onCheckLicenseListener.onFail();
                }
            }
        } else {
            getLicense(context);
        }
//        return false;
    }
    public static void getLicense(final Context context) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }
            @Override
            protected Boolean doInBackground(Void... voids) {
                String json = "{ \"head\": { \"uid\" : \"\" , \"sid\" : \"\" , \"plat\" : \"\" , \"st\" : \"\", \"ver\" : \"\" , \"imei\" : \"\" , \"oc\" : \"\" }}";
                String path = "http://api.7fineday.com/front/api/face/authkey";
                String licenseJsonStr = HttpUtils.getLicense(path, json);
                if (TextUtils.isEmpty(licenseJsonStr))
                    return false;
                try {
                    JSONObject jsonObject = new JSONObject(licenseJsonStr);
                    if (jsonObject.getString("status").equals("1")) {
                        String licenseUrl = jsonObject.getString("data");
                        if (TextUtils.isEmpty(licenseUrl))
                            return false;
                        boolean result = downloadLicense(context, licenseUrl);
                        return result;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                //TODO
                if (result) {
                    if (onCheckLicenseListener != null) {
                        onCheckLicenseListener.onSuccess();
                    }
                } else {
                    if (onCheckLicenseListener != null) {
                        onCheckLicenseListener.onFail();
                    }
                }
            }
        }.execute();
    }
    private static boolean downloadLicense(Context context, String licenseUrl) throws Exception {
        URL url = new URL(licenseUrl);
        Log.i("TAG", url.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
        String fileDir = context.getFilesDir() + "/key.lic";
        File file = new File(fileDir);
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                if (file.getParentFile().mkdirs()) {
                    file.createNewFile();
                }
            }
        } else {
            return false;
        }
        FileOutputStream fos = new FileOutputStream(file);
        byte data[] = new byte[4 * 1024];
        int count;
        while ((count = bis.read(data)) != -1) {
            fos.write(data, 0, count);
            fos.flush();
        }
        fos.flush();
        fos.close();
        bis.close();
        FileInputStream in = new FileInputStream(file);
        boolean result = STLicenseUtils.checkLicense(context, in);
        in.close();
        return result;
    }
    public interface OnCheckLicenseListener {
        void onSuccess();
        void onFail();
    }
    private static OnCheckLicenseListener onCheckLicenseListener;
}