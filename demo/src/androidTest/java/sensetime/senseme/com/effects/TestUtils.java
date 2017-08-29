package sensetime.senseme.com.effects;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class TestUtils {

        public static boolean copyFileIfNeed(String fileName,Context context) {
        String path = getFilePath(fileName,context);
        if (path != null) {
            File file = new File(path);
            if (!file.exists()) {
                try {
                    if (file.exists())
                        file.delete();
                    file.createNewFile();
                    InputStream in = context.getAssets().open(fileName);
                    if(in == null)
                    {
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

    public static String getFilePath(String fileName,Context context) {
        String path = null;
        File dataDir = context.getExternalFilesDir(null);
        if (dataDir != null) {
            path = dataDir.getAbsolutePath() + File.separator + fileName;
        }
        return path;
    }

    public static int[] getBGRAImageByte(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (image.getConfig().equals(Bitmap.Config.ARGB_8888)) {
            int[] imgData = new int[width * height];
            image.getPixels(imgData, 0, width, 0, 0, width, height);
            return imgData;

        } else {
            // do nothing
        }

        return null;
    }
}
