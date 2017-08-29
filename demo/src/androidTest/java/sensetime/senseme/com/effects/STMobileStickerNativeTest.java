package sensetime.senseme.com.effects;

import android.support.test.runner.AndroidJUnit4;

import com.sensetime.stmobile.STMobileStickerNative;

import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class STMobileStickerNativeTest {
    STMobileStickerNative mSTMobileStickerNative;

    @org.junit.Before
    public void setup()
    {
        mSTMobileStickerNative = new STMobileStickerNative();
        int result = mSTMobileStickerNative.createInstance("test");
        assertEquals(result,0);
    }
    @org.junit.Test
    public void setCallback() throws Exception {

    }

    @org.junit.Test
    public void item_callback() throws Exception {
    }

    @org.junit.Test
    public void processTexture() throws Exception {
        //mSTMobileStickerNative.processTexture()
    }

    @org.junit.Test
    public void processTextureAndOutputBuffer() throws Exception {

    }

    @org.junit.Test
    public void changeSticker() throws Exception {

        int result = mSTMobileStickerNative.changeSticker("test");
        assertEquals(result,0);
        result = mSTMobileStickerNative.getTriggerAction();
        assertEquals(result,0);
    }

    @org.junit.After
    public void destroyInstance() throws Exception {
        mSTMobileStickerNative.destroyInstance();
    }

}