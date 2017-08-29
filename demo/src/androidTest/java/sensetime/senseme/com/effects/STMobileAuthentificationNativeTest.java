package sensetime.senseme.com.effects;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.sensetime.stmobile.STMobileAuthentificationNative;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class STMobileAuthentificationNativeTest {
    STMobileAuthentificationNative mSTMobileAuthentificationNative;
    private final static String LIENCSE_FILE_NAME = "SenseME.lic";

    @Test
    public void checkActiveCodeFromBuffer()  {
        Context appContext = InstrumentationRegistry.getTargetContext();

        StringBuilder sb = new StringBuilder();
        InputStreamReader isr = null;
        BufferedReader br = null;
        // 读取license文件内容
        try {
            isr = new InputStreamReader(appContext.getResources().getAssets().open(LIENCSE_FILE_NAME));
            br = new BufferedReader(isr);
            String line = null;
            while((line=br.readLine()) != null) {
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

        String activeCode = STMobileAuthentificationNative.generateActiveCodeFromBuffer(sb.toString(),sb.length());
        assertTrue(activeCode!=null);
        assertTrue(activeCode.length()>0);
        int result = STMobileAuthentificationNative.checkActiveCodeFromBuffer(sb.toString(),sb.length(),activeCode, activeCode.length());
        assertEquals(result,0);
    }

    @Test
    public void checkActiveCodeFromFile()  {
        Context appContext = InstrumentationRegistry.getTargetContext();
        TestUtils.copyFileIfNeed(LIENCSE_FILE_NAME,appContext);
        String filePath = TestUtils.getFilePath(LIENCSE_FILE_NAME,appContext);
        String activeCode = STMobileAuthentificationNative.generateActiveCode(filePath);
        assertTrue(activeCode!=null);
        assertTrue(activeCode.length()>0);
        int result = STMobileAuthentificationNative.checkActiveCode(filePath,activeCode);
        assertEquals(result,0);
    }
}