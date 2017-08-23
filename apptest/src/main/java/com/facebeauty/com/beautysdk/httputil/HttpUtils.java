package com.facebeauty.com.beautysdk.httputil;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.facebeauty.com.beautysdk.utils.ApiSecurity;
import com.facebeauty.com.beautysdk.utils.FileUtils;
import com.facebeauty.com.beautysdk.utils.STLicenseUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by wangdi on 2017/8/17.
 */

public class HttpUtils {
    private static final String LICENSE_NAME = "SenseME.lic";
    public static String pathAbs = Environment.getExternalStorageDirectory().getAbsolutePath();

    public static String getLicense(String path, String json) {
//        ByteArrayOutputStream byteArrayOutputStream;
        BufferedReader bufferedReader = null;
        byte[] data = new byte[1024];

        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(path);
            urlConnection = (HttpURLConnection) url.openConnection();
        /* optional request header */
//            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        /* optional request header */
            urlConnection.setRequestProperty("Accept", "application/json");
//            dto.setCreator(java.net.URLEncoder.encode(dto.getCreator(), "utf-8"));
            // read response
        /* for Get request */
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(json);
            wr.flush();
            wr.close();
            // try to get response
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == 200) {
                inputStream = urlConnection.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                StringBuilder stringBuilder = new StringBuilder();

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String decryptStr = ApiSecurity.getInstance().decrypt(stringBuilder.toString());
//                int len=0;
//                while((len=inputStream.read())!=-1){
//                    byteArrayOutputStream.write(data,0,len);
//                }
//                String str = new String(byteArrayOutputStream.toByteArray());
//               String decryptStr =  ApiSecurity.getInstance().decrypt(str);
//               boolean result =  STLicenseUtils.checkLicense(decryptStr);
//                write(pathAbs+"/"+LICENSE_NAME,inputStream);
//                boolean flag = STLicenseUtils.checkLicense(context);
                return decryptStr;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }

    public static void getLicenseContent(String licenseFilePathStr) {
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(licenseFilePathStr);
            urlConnection = (HttpURLConnection) url.openConnection();
        /* optional request header */
//            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        /* optional request header */
            urlConnection.setRequestProperty("Accept", "application/json");
//            dto.setCreator(java.net.URLEncoder.encode(dto.getCreator(), "utf-8"));
            // read response
        /* for Get request */
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
//            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
//            wr.writeBytes(json);
//            wr.flush();
//            wr.close();
            // try to get response
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == 200) {
                inputStream = urlConnection.getInputStream();
                Log.e("info",pathAbs + "/" + LICENSE_NAME);
                write(pathAbs + "/" + LICENSE_NAME, inputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static void write(String filename, InputStream in) {

        File file = new File(filename);
        if (!file.exists()) {
            if (!file.mkdirs()) {//若创建文件夹不成功f
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        File targetfile = new File(filename);
        OutputStream os = null;
        try {
            os = new FileOutputStream(targetfile);
            int ch = 0;
            while ((ch = in.read()) != -1) {
                os.write(ch);
            }
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void downLoad(String urlStr){
        OutputStream output=null;
        try {
                /*
                 * 通过URL取得HttpURLConnection
                 * 要网络连接成功，需在AndroidMainfest.xml中进行权限配置
                 * <uses-permission android:name="android.permission.INTERNET" />
                 */
            URL url=new URL(urlStr);
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            //取得inputStream，并将流中的信息写入SDCard

                /*
                 * 写前准备
                 * 1.在AndroidMainfest.xml中进行权限配置
                 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
                 * 取得写入SDCard的权限
                 * 2.取得SDCard的路径： Environment.getExternalStorageDirectory()
                 * 3.检查要保存的文件上是否已经存在
                 * 4.不存在，新建文件夹，新建文件
                 * 5.将input流中的信息写入SDCard
                 * 6.关闭流
                 */
            String SDCard=Environment.getExternalStorageDirectory()+"";
            InputStream input=conn.getInputStream();
            FileUtils.saveToSDCard(LICENSE_NAME,input);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            try {
                output.close();
                System.out.println("success");
            } catch (IOException e) {
                System.out.println("fail");
                e.printStackTrace();
            }
        }
    }
}
