package sensetime.senseme.com.effects;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.sensetime.stmobile.STCommon;
import com.sensetime.stmobile.STHumanAction;
import com.sensetime.stmobile.STMobileHumanActionNative;

import org.junit.runner.RunWith;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import sensetime.senseme.com.effects.utils.FileUtils;

import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class STMobileHumanActionNativeTest {
    STMobileHumanActionNative mSTHumanActionNative;

    @org.junit.Before
    public void testBefore() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        FileUtils.copyModelFiles(appContext);
        mSTHumanActionNative = new STMobileHumanActionNative();
        int result = mSTHumanActionNative.createInstance(FileUtils.getTrackModelPath(appContext),
                STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_VIDEO);

        assertEquals(result, 0);
    }

    @org.junit.Test
    public void humanActionDetect() throws Exception {

        Context appContext = InstrumentationRegistry.getContext();
        //InputStream input = appContext.getResources().openRawResource(com.sensetime.ststickersample.test.R.people_sample);
        InputStream input = appContext.getAssets().open("people_sample.png");
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeStream(input, new Rect(0, 0, 0, 0), options);
        Bitmap srcFaceBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int height = srcFaceBitmap.getHeight();
        int width = srcFaceBitmap.getWidth();

        ByteBuffer rgbaBuffer = ByteBuffer.allocate(height * width * 4);
//        rgbaBuffer.asCharBuffer();
        srcFaceBitmap.copyPixelsToBuffer(rgbaBuffer);
//        rgbaBuffer.rewind();

        int[] imgData = new int[width * height];
        srcFaceBitmap.getPixels(imgData, 0, width, 0, 0, width, height);
        ByteBuffer byteBuffer = ByteBuffer.allocate(imgData.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(imgData);

        STHumanAction humanAction = mSTHumanActionNative.humanActionDetect(byteBuffer.array(), STCommon.ST_PIX_FMT_RGBA8888,
                STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_DETECT, 0,
                width, height);

        assertEquals(humanAction.faceCount, 1);
    }

    @org.junit.After
    public void testAfter() throws Exception {
        mSTHumanActionNative.reset();
        mSTHumanActionNative.destroyInstance();
    }
}