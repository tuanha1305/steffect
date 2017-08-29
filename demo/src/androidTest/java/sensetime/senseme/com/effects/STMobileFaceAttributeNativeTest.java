package sensetime.senseme.com.effects;

import android.support.test.runner.AndroidJUnit4;

import com.sensetime.stmobile.STMobileFaceAttributeNative;

import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class STMobileFaceAttributeNativeTest {

    STMobileFaceAttributeNative mSTMobileFaceAttributeNative;
    @org.junit.Before
    public void setup()
    {
        mSTMobileFaceAttributeNative = new STMobileFaceAttributeNative();
        String modelpath = "tttt";
       int result =  mSTMobileFaceAttributeNative.createInstance(modelpath);
        assertEquals(result,0);
    }
    @org.junit.Test
    public void detect() throws Exception {
        //public native int detect(byte[] image, int format, int width, int height, STMobile106[] mobile106, STFaceAttribute[] attributes);
        int result = 0;
        assertEquals(result,0);
    }


    @org.junit.After
    public void destroyInstance() throws Exception {
        mSTMobileFaceAttributeNative.destroyInstance();
    }

}