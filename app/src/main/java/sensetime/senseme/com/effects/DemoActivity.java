package sensetime.senseme.com.effects;

import android.app.Activity;
import android.os.Bundle;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import sensetime.senseme.com.effects.domain.Brand;

public class DemoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        ImageLoaderConfiguration imageLoaderConfiguration = ImageLoaderConfiguration.createDefault(this);
        ImageLoader.getInstance().init(imageLoaderConfiguration);

        HorizontalListView horizontalListView = (HorizontalListView) findViewById(R.id.listview);
        String data = openAssetsFile("makeuplist.json");

      JSONObject jsonObject =  JSON.parseObject(data);
      String dataStr =   jsonObject.getString("data");
//       JSONObject dataJsonObject=  jsonObject.getJSONObject("data");
       List<Brand> brandList =  JSON.parseArray(dataStr, Brand.class);

//        DemoAdapter adapter = new DemoAdapter(this,brandList,);

//        horizontalListView.setAdapter(adapter);
    }

    private String openAssetsFile(String filename) {
        try {
            InputStreamReader inputReader = new InputStreamReader(getResources().getAssets().open(filename));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            String Result = "";
            while ((line = bufReader.readLine()) != null)
                Result += line;
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
