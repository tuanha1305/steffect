package com.facedemo.com.facesdkbuild.httputil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by wangdi on 2017/8/17.
 */

public class HttpUtils {
    private static final String LICENSE_NAME = "SenseME.lic";

    public static void write(String filename,InputStream in){

        File file=new File(filename);
        if(!file.exists()){
            if(!file.mkdirs()){//若创建文件夹不成功
                System.out.println("Unable to create external cache directory");
            }
        }

        File targetfile=new File(filename);
        OutputStream os=null;
        try{
            os=new FileOutputStream(targetfile);
            int ch=0;
            while((ch=in.read())!=-1){
                os.write(ch);
            }
            os.flush();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            try{
                os.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

}
