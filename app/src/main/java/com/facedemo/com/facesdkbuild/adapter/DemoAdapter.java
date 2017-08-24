package com.facedemo.com.facesdkbuild.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.facebeauty.com.beautysdk.domain.Brand;
import com.facebeauty.com.beautysdk.domain.Cosmetic;
import com.facebeauty.com.beautysdk.domain.Organ;
import com.facebeauty.com.beautysdk.view.CameraView;
import com.facedemo.com.facesdkbuild.R;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.util.List;

/**
 * Created by liupan on 17/8/15.
 */

public class DemoAdapter extends BaseAdapter {
    List<Brand> brandList;
    Context context;
    LayoutInflater layoutInflater;
    float[] color = {0.0f,0.0f,0.0f,0.0f};

    int level;  //0,1,2
    int index1;//保存选择的品牌
    int index2;//保存选择的器官
    int qiguanIndex;
    CameraView cameraView;

    public DemoAdapter(Context context, List<Brand> brandList, CameraView cameraView) {
        this.context = context;
        this.brandList = brandList;
        layoutInflater = LayoutInflater.from(context);
        this.cameraView = cameraView;
    }

    @Override
    public int getCount() {
        if (level == 0)
            return brandList == null ? 0 : brandList.size();
        else if (level == 1) {
            return (brandList.get(index1).getBrandList() == null) ? 1 : (brandList.get(index1).getBrandList().size() + 1);
        } else if (level == 2) {
            return (brandList.get(index1).getBrandList().get(index2).getCosmeticList() == null) ? 1 : (brandList.get(index1).getBrandList().get(index2).getCosmeticList().size() + 1);
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_demo, null);
            holder = new ViewHolder();
            holder.cover = (ImageView) convertView.findViewById(R.id.cover);
            holder.back = (ImageView) convertView.findViewById(R.id.back);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (level == 0) {
            Brand brand = brandList.get(position);
            ImageLoader.getInstance().displayImage(brand.getBrandLogo(), holder.cover);
            holder.back.setVisibility(View.GONE);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    level = 1;
                    index1 = position;
                    notifyDataSetChanged();
                }
            });
        } else if (level == 1) {
            //选择了一个品牌
            Brand brand = brandList.get(index1);

            if (position == 0) {
                ImageLoader.getInstance().displayImage(brand.getBrandLogo(), holder.cover);
                holder.back.setVisibility(View.VISIBLE);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        level = 0;
                        index1 = 0;
                        notifyDataSetChanged();
                    }
                });
            } else {
                Organ organ = brand.getBrandList().get(position - 1);
                ImageLoader.getInstance().displayImage(organ.getOrganLogo(), holder.cover);
                holder.back.setVisibility(View.GONE);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        level = 2;
                        index2 = position - 1;
                        notifyDataSetChanged();
                    }
                });

            }
        } else {
            //选择了一个品牌，并选择了一个器官
            final Organ organ = brandList.get(index1).getBrandList().get(index2);
            if (position == 0) {
                ImageLoader.getInstance().displayImage(organ.getOrganLogo(), holder.cover);
                holder.back.setVisibility(View.VISIBLE);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        level=1;
                        index2=0;
                        notifyDataSetChanged();
                    }
                });
            } else {
                final Cosmetic cosmetic = organ.getCosmeticList().get(position - 1);
                ImageLoader.getInstance().displayImage(cosmetic.getCosmeticLogo(), holder.cover);
                holder.back.setVisibility(View.GONE);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(organ.getOrganName().trim().equals("嘴唇")){
                            String[] moust = cosmetic.getRgbColor().split(",");
                            float r = Float.valueOf(moust[0]);
                            float g = Float.valueOf(moust[1]);
                            float b = Float.valueOf(moust[2]);
                            float k = Float.valueOf(moust[3]);
//                            cameraView.setDownMouse(r,g,b, k);
//                            cameraView.setUpMouse(r,g,b, k);
                            cameraView.setLip(r,g,b,k);

                        }
                        ImageLoader.getInstance().loadImage(cosmetic.getMaterialImage(), new ImageLoadingListener() {
                            @Override
                            public void onLoadingStarted(String s, View view) {

                            }

                            @Override
                            public void onLoadingFailed(String s, View view, FailReason failReason) {

                            }

                            @Override
                            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                                if(organ.getOrganName().trim().equals("眉毛")){
                                    //眉毛
//                                    cameraView.setLeftMeiMao(bitmap);
//                                    cameraView.setRightMeiMao(bitmap);
                                    cameraView.setEyebrow(bitmap,color);
                                    Log.i("info","眉毛");
                                }else if(organ.getOrganName().trim().equals("睫毛")){
                                    //睫毛
//                                    cameraView.setYanJieMao(bitmap);
                                    cameraView.setEyelash(bitmap,color);
                                    Log.i("info","睫毛");
                                }else if(organ.getOrganName().trim().equals("眼线")){
                                    //眼线
//                                    cameraView.setYanXian(bitmap);
                                    cameraView.setEyeliner(bitmap,color);
                                    Log.i("info","眼线");
                                }else if(organ.getOrganName().trim().equals("眼影")){
                                    //眼影
//                                    cameraView.setYanYing(bitmap);
                                    cameraView.setEyeShadow(bitmap,color);
                                    Log.i("info","眼影");
                                }else if(organ.getOrganName().trim().equals("腮红")){
                                    //腮红
//                                    cameraView.setSaihong(bitmap);
                                    cameraView.setBlush(bitmap,color);
                                    Log.i("info","腮红");
                                }
                            }

                            @Override
                            public void onLoadingCancelled(String s, View view) {

                            }
                        });


                    }
                });
            }
        }
        return convertView;
    }

    static class ViewHolder {
        ImageView cover;
        ImageView back;
    }

    /**
     * 根据原图和变长绘制圆形图片
     *
     * @param source
     * @param min
     * @return
     */
    private Bitmap createCircleImage(Bitmap source, int min)
    {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap target = Bitmap.createBitmap(min, min, Bitmap.Config.ARGB_8888);
        /**
         * 产生一个同样大小的画布
         */
        Canvas canvas = new Canvas(target);
        /**
         * 首先绘制圆形
         */
        canvas.drawCircle(min / 2, min / 2, min / 2, paint);
        /**
         * 使用SRC_IN
         */
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        /**
         * 绘制图片
         */
        canvas.drawBitmap(source, 0, 0, paint);
        return target;
    }
}
