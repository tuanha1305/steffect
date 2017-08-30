package com.facebeauty.com.beautysdk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.facebeauty.com.beautysdk.httputil.HttpUtils;
import com.sensetime.stmobile.STMobileAuthentificationNative;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by leiwang on 2016/12/2.
 */
public class STLicenseUtils {
    private final static String TAG = "STLicenseUtils";
    private final static String PREF_ACTIVATE_CODE_FILE = "activate_code_file";
    private final static String PREF_ACTIVATE_CODE = "activate_code";
    private static final String LICENSE_NAME = "SenseME.lic";

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

    private static void checkLicense(final Context context) {
        String path = context.getFilesDir() + "/key.lic";
        File file = new File(path);
        if (file.exists()) {
            try {
                FileInputStream in = new FileInputStream(file);
                if (checkLicense(context, in)) {
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
    }

    public static void checkLicense(final Context context, OnCheckLicenseListener licenseListener) {
        onCheckLicenseListener = licenseListener;
        checkTokenLicense(context);

    }

    private static void getLicense(final Context context) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                String json = "{ \"head\": { \"uid\" : \"\" , \"sid\" : \"\" , \"plat\" : \"\" , \"st\" : \"\", \"ver\" : \"\" , \"imei\" : \"\" , \"oc\" : \"\" }}";
                String path = "http://api.7fineday.com/front/api/face/authkey";
                String licenseJsonStr = HttpUtils.getLicense(path, json, "POST");
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

    /**
     * 检查token合法性
     *
     * @return true, 成功 false,失败
     */
    private static boolean checkTokenLicense(Context context, InputStream stream) {
        String tokenLicenseBuffer = null;
        String path = context.getFilesDir() + "/token.lic";
        File file = new File(path);
        if (file.exists() && file.length() > 0) {
            try {
                ObjectInputStream is = new ObjectInputStream(
                        new FileInputStream(file));
                tokenLicenseBuffer = (String) is.readObject();
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // license文件为空,则直接返回
        if (TextUtils.isEmpty(tokenLicenseBuffer)) {
            LogUtils.e(TAG, "read token license data error");
            return false;
        }

        try {
            JSONObject tokenJsonObject = new JSONObject(tokenLicenseBuffer);
            String tokenStr = tokenJsonObject.getString("token");
            //validDate":"2017-09-04 23:17:29"
            String validDateStr = tokenJsonObject.getString("validDate");
            if (TextUtils.isEmpty(tokenStr)) {
                return false;
            }
            if (TextUtils.isEmpty(validDateStr)) {
                return false;
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date cacheDate = simpleDateFormat.parse(validDateStr);
            Date nowDate = new Date();
            if (nowDate.before(cacheDate)) {
                return true;
            }
            return false;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void checkTokenLicense(Context context) {
        String path = context.getFilesDir() + "/token.lic";
        File file = new File(path);
        if (file.exists()) {
            try {
                FileInputStream in = new FileInputStream(file);
                if (checkTokenLicense(context, in)) {
                    checkLicense(context);
                } else {
                    getTokenLicense(context);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (onCheckLicenseListener != null) {
                    onCheckLicenseListener.onFail();
                }
            }
        } else {
            getTokenLicense(context);
        }
    }

    private static void getTokenLicense(final Context context) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {

                String json = "{" +
                        "\"head\": {" +
                        "\"uid\": \"\"," +
                        "\"sid\": \"\"," +
                        "\"plat\": \"android\"," +
                        "\"st\": \"\"," +
                        "\"ver\": \"\"," +
                        "\"imei\": \"\"," +
                        "\"oc\": \"\"" +
                        "}," +
                        "\"body\": {" +
                        "\"token\": \"ss\"," +
                        "\"package_name\": \"com.facedemo.com.facesdkbuild\"," +
                        "\"type\": \"2\"" +
                        "}" +
                        "}";
                String path = "http://api.7fineday.com/front/api/face/auth";
                String licenseJsonStr = HttpUtils.getLicense(path, json.trim(), "GET");
                if (TextUtils.isEmpty(licenseJsonStr))
                    return false;
                try {
                    JSONObject jsonObject = new JSONObject(licenseJsonStr);
                    if (jsonObject.getString("status").equals("1")) {
//                        {"status":1,"data":{"token":"5MHolRK2CL+gHkEt+SQW5GPu0hCsRqT7TskJaQHkr7w=","validDate":"2017-09-04 23:17:29"},"msg":"返回数据"}
                        String tokenLicenseUrl = jsonObject.getString("data");
                        JSONObject tokenJsonObject = jsonObject.getJSONObject("data");
                        String tokenStr = tokenJsonObject.getString("token");
                        String validDateStr = tokenJsonObject.getString("validDate");

                        if (TextUtils.isEmpty(tokenStr))
                            return false;

                        if (TextUtils.isEmpty(validDateStr)) {
                            return false;
                        }
                        writeTokenLicenseToFile(context, tokenJsonObject.toString());
//                        将获取到的写到文件中
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result) {
                    checkLicense(context);
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

    public static void writeTokenLicenseToFile(Context context, String data) throws IOException {
        String fileDir = context.getFilesDir() + "/token.lic";
        File file = new File(fileDir);
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                if (file.getParentFile().mkdirs()) {
                    file.createNewFile();
                }
            }
        }
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(data);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public interface OnCheckLicenseListener {
        void onSuccess();

        void onFail();
    }

    private static OnCheckLicenseListener onCheckLicenseListener;
}