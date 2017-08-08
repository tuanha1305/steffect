package sensetime.senseme.com.effects.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import sensetime.senseme.com.effects.R;
import sensetime.senseme.com.effects.view.FilterItem;
import sensetime.senseme.com.effects.view.ObjectItem;
import sensetime.senseme.com.effects.view.StickerItem;

/**
 * Created by sensetime on 16-11-16.
 */

public class FileUtils {

    //private static final String FACE_TRACK_MODEL_NAME = "face_track_3.3.0.model";
    private static final String FACE_TRACK_MODEL_NAME = "action3.8.0.model";
    private static final String FACE_ATTRIBUTE_NAME = "face_attribute_1.0.1.model";

    public static ArrayList<String> copyStickerFiles(Context context) {
        String files[] = null;
        ArrayList<String> zipfiles = new ArrayList<String>();

        try {
            files = context.getAssets().list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String folderpath = null;
        File dataDir = context.getExternalFilesDir(null);
        if (dataDir != null) {
            folderpath = dataDir.getAbsolutePath();
        }
        for (int i = 0; i < files.length; i++) {
            String str = files[i];
            if(str.indexOf(".zip") != -1){
                copyFileIfNeed(context, str);
            }
        }

        File file = new File(folderpath);
        File[] subFile = file.listFiles();

        for (int i = 0; i < subFile.length; i++) {
            // 判断是否为文件夹
            if (!subFile[i].isDirectory()) {
                String filename = subFile[i].getAbsolutePath();
                String path = subFile[i].getPath();
                // 判断是否为zip结尾
                if (filename.trim().toLowerCase().endsWith(".zip")) {
                    zipfiles.add(filename);
                }
            }
        }

        return zipfiles;
    }

    public static boolean copyFileIfNeed(Context context, String fileName) {
        String path = getFilePath(context, fileName);
        if (path != null) {
            File file = new File(path);
            if (!file.exists()) {
                //如果模型文件不存在
                try {
                    if (file.exists())
                        file.delete();

                    file.createNewFile();
                    InputStream in = context.getApplicationContext().getAssets().open(fileName);
                    if(in == null)
                    {
                        LogUtils.e("copyMode", "the src is not existed");
                        return false;
                    }
                    OutputStream out = new FileOutputStream(file);
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        out.write(buffer, 0, n);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    file.delete();
                    return false;
                }
            }
        }
        return true;
    }

    public static String getFilePath(Context context, String fileName) {
        String path = null;
        File dataDir = context.getApplicationContext().getExternalFilesDir(null);
        if (dataDir != null) {
            path = dataDir.getAbsolutePath() + File.separator + fileName;
        }
        return path;
    }

//    public static List<Bitmap> getStickerImage(Context context) {
//        List<Bitmap> stickerList = new ArrayList<>();
//
//        Bitmap icon0 = BitmapFactory.decodeResource(context.getResources(), R.drawable.close_sticker);
//        Bitmap icon1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.none);
//        Bitmap icon2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.bunny);
//        Bitmap icon3 = BitmapFactory.decodeResource(context.getResources(), R.drawable.maozi);
//        Bitmap icon4 = BitmapFactory.decodeResource(context.getResources(), R.drawable.rabbiteating);
//
//        stickerList.add(icon0);
//        stickerList.add(icon1);
//        stickerList.add(icon2);
//        stickerList.add(icon3);
//        stickerList.add(icon4);
//
//        return stickerList;
//    }

    public static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                LogUtils.e("FileUtil", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    public static void copyModelFiles(Context context) {
        copyFileIfNeed(context, FACE_TRACK_MODEL_NAME);
        copyFileIfNeed(context, FACE_ATTRIBUTE_NAME);
    }


    public static String getTrackModelPath(Context context) {
        return getFilePath(context, FACE_TRACK_MODEL_NAME);

    }

    public static String getFaceAttributeModelPath(Context context) {
        return getFilePath(context, FACE_ATTRIBUTE_NAME);
    }

    public static List<ObjectItem> getObjectList(){
        List<ObjectItem> objectList = new ArrayList<>();

        objectList.add(new ObjectItem("close", R.drawable.close_object));
        objectList.add(new ObjectItem("null", R.drawable.none));
        objectList.add(new ObjectItem("object", R.drawable.object_guide));

        return objectList;
    }

    public static ArrayList<StickerItem> getStickerFiles(Context context){
        ArrayList<StickerItem> stickerFiles = new ArrayList<StickerItem>();
        Bitmap iconClose = BitmapFactory.decodeResource(context.getResources(), R.drawable.close_sticker);
        Bitmap iconNone = BitmapFactory.decodeResource(context.getResources(), R.drawable.none);

        stickerFiles.add(new StickerItem("close", iconClose, null));
        stickerFiles.add(new StickerItem("none", iconNone, null));

        List<String> stickerModels = copyStickerZipFiles(context);
        Map<String, Bitmap> stickerIcons = copyStickerIconFiles(context);
        List<String> stickerNames = getStickerNames(context);

        for(int i = 0;i< stickerModels.size(); i++){
            if(stickerIcons.get(stickerNames.get(i)) != null)
                stickerFiles.add(new StickerItem(stickerNames.get(i), stickerIcons.get(stickerNames.get(i)), stickerModels.get(i)));
            else{
                stickerFiles.add(new StickerItem(stickerNames.get(i), iconNone, stickerModels.get(i)));
            }
        }

        return  stickerFiles;
    }

    public static List<String> copyStickerZipFiles(Context context){
        String files[] = null;
        ArrayList<String> modelFiles = new ArrayList<String>();

        try {
            files = context.getAssets().list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String folderpath = null;
        File dataDir = context.getExternalFilesDir(null);
        if (dataDir != null) {
            folderpath = dataDir.getAbsolutePath();
        }
        for (int i = 0; i < files.length; i++) {
            String str = files[i];
            if(str.indexOf(".zip") != -1){
                copyFileIfNeed(context, str);
            }
        }

        File file = new File(folderpath);
        File[] subFile = file.listFiles();

        for (int i = 0; i < subFile.length; i++) {
            // 判断是否为文件夹
            if (!subFile[i].isDirectory()) {
                String filename = subFile[i].getAbsolutePath();
                String path = subFile[i].getPath();
                // 判断是否为model结尾
                if (filename.trim().toLowerCase().endsWith(".zip")) {
                    modelFiles.add(filename);
                }
            }
        }

        return modelFiles;
    }

    public static Map<String, Bitmap> copyStickerIconFiles(Context context){
        String files[] = null;
        TreeMap<String, Bitmap> iconFiles = new TreeMap<String, Bitmap>();

        try {
            files = context.getAssets().list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String folderpath = null;
        File dataDir = context.getExternalFilesDir(null);
        if (dataDir != null) {
            folderpath = dataDir.getAbsolutePath();
        }
        for (int i = 0; i < files.length; i++) {
            String str = files[i];
            if(str.indexOf(".png") != -1){
                copyFileIfNeed(context, str);
            }
        }

        File file = new File(folderpath);
        File[] subFile = file.listFiles();

        for (int i = 0; i < subFile.length; i++) {
            // 判断是否为文件夹
            if (!subFile[i].isDirectory()) {
                String filename = subFile[i].getAbsolutePath();
                String path = subFile[i].getPath();
                // 判断是否为png结尾
                if (filename.trim().toLowerCase().endsWith(".png") && filename.indexOf("mode_") == -1) {
                    String name = subFile[i].getName();
                    iconFiles.put(getFileNameNoEx(name), BitmapFactory.decodeFile(filename));
                }
            }
        }

        return iconFiles;
    }

    public static List<String> getStickerNames(Context context){
        ArrayList<String> modelNames = new ArrayList<String>();
        String folderpath = null;
        File dataDir = context.getExternalFilesDir(null);
        if (dataDir != null) {
            folderpath = dataDir.getAbsolutePath();
        }

        File file = new File(folderpath);
        File[] subFile = file.listFiles();

        for (int i = 0; i < subFile.length; i++) {
            // 判断是否为文件夹
            if (!subFile[i].isDirectory()) {
                String filename = subFile[i].getAbsolutePath();
                // 判断是否为model结尾
                if (filename.trim().toLowerCase().endsWith(".zip") && filename.indexOf("filter") == -1) {
                    String name = subFile[i].getName();
                    modelNames.add(getFileNameNoEx(name));
                }
            }
        }

        return modelNames;
    }

    public static ArrayList<FilterItem> getFilterFiles(Context context){
        ArrayList<FilterItem> filterFiles = new ArrayList<FilterItem>();
        Bitmap iconClose = BitmapFactory.decodeResource(context.getResources(), R.drawable.close_filter);
        Bitmap iconNone = BitmapFactory.decodeResource(context.getResources(), R.drawable.none);

        filterFiles.add(new FilterItem("close", iconClose, null));
        filterFiles.add(new FilterItem("none", iconNone, null));

        List<String> filterModels = copyFilterModelFiles(context);
        Map<String, Bitmap> filterIcons = copyFilterIconFiles(context);
        List<String> filterNames = getFilterNames(context);

        for(int i = 0;i< filterModels.size(); i++){
            if(filterIcons.get(filterNames.get(i)) != null)
                filterFiles.add(new FilterItem(filterNames.get(i), filterIcons.get(filterNames.get(i)), filterModels.get(i)));
            else{
                filterFiles.add(new FilterItem(filterNames.get(i), iconNone, filterModels.get(i)));
            }
        }

        return  filterFiles;
    }

    public static List<String> copyFilterModelFiles(Context context){
        String files[] = null;
        ArrayList<String> modelFiles = new ArrayList<String>();

        try {
            files = context.getAssets().list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String folderpath = null;
        File dataDir = context.getExternalFilesDir(null);
        if (dataDir != null) {
            folderpath = dataDir.getAbsolutePath();
        }
        for (int i = 0; i < files.length; i++) {
            String str = files[i];
            if(str.indexOf(".model") != -1){
                copyFileIfNeed(context, str);
            }
        }

        File file = new File(folderpath);
        File[] subFile = file.listFiles();

        for (int i = 0; i < subFile.length; i++) {
            // 判断是否为文件夹
            if (!subFile[i].isDirectory()) {
                String filename = subFile[i].getAbsolutePath();
                String path = subFile[i].getPath();
                // 判断是否为model结尾
                if (filename.trim().toLowerCase().endsWith(".model") && filename.indexOf("filter") != -1) {
                    modelFiles.add(filename);
                }
            }
        }

        return modelFiles;
    }

    public static Map<String, Bitmap> copyFilterIconFiles(Context context){
        String files[] = null;
        TreeMap<String, Bitmap> iconFiles = new TreeMap<String, Bitmap>();

        try {
            files = context.getAssets().list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String folderpath = null;
        File dataDir = context.getExternalFilesDir(null);
        if (dataDir != null) {
            folderpath = dataDir.getAbsolutePath();
        }
        for (int i = 0; i < files.length; i++) {
            String str = files[i];
            if(str.indexOf(".png") != -1){
                copyFileIfNeed(context, str);
            }
        }

        File file = new File(folderpath);
        File[] subFile = file.listFiles();

        for (int i = 0; i < subFile.length; i++) {
            // 判断是否为文件夹
            if (!subFile[i].isDirectory()) {
                String filename = subFile[i].getAbsolutePath();
                String path = subFile[i].getPath();
                // 判断是否为png结尾
                if (filename.trim().toLowerCase().endsWith(".png") && filename.indexOf("mode_") != -1) {
                    String name = subFile[i].getName().substring(5);
                    iconFiles.put(getFileNameNoEx(name), BitmapFactory.decodeFile(filename));
                }
            }
        }

        return iconFiles;
    }

    public static List<String> getFilterNames(Context context){
        ArrayList<String> modelNames = new ArrayList<String>();
        String folderpath = null;
        File dataDir = context.getExternalFilesDir(null);
        if (dataDir != null) {
            folderpath = dataDir.getAbsolutePath();
        }

        File file = new File(folderpath);
        File[] subFile = file.listFiles();

        for (int i = 0; i < subFile.length; i++) {
            // 判断是否为文件夹
            if (!subFile[i].isDirectory()) {
                String filename = subFile[i].getAbsolutePath();
                // 判断是否为model结尾
                if (filename.trim().toLowerCase().endsWith(".model") && filename.indexOf("filter") != -1) {
                    String name = subFile[i].getName().substring(13);
                    modelNames.add(getFileNameNoEx(name));
                }
            }
        }

        return modelNames;
    }

    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

}
