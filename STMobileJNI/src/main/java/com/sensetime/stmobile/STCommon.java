package com.sensetime.stmobile;

/**
 * 定义美颜支持的图片格式
 */
public class STCommon {

    static {
        System.loadLibrary("st_mobile");
        System.loadLibrary("stmobile_jni");
    }

    //支持的图片格式
    public final static int ST_PIX_FMT_GRAY8 = 0;   // Y    1        8bpp ( 单通道8bit灰度像素 )
    public final static int ST_PIX_FMT_YUV420P = 1; // YUV  4:2:0   12bpp ( 3通道, 一个亮度通道, 另两个为U分量和V分量通道, 所有通道都是连续的 )
    public final static int ST_PIX_FMT_NV12 = 2;    // YUV  4:2:0   12bpp ( 2通道, 一个通道是连续的亮度通道, 另一通道为UV分量交错 )
    public final static int ST_PIX_FMT_NV21 = 3;    // YUV  4:2:0   12bpp ( 2通道, 一个通道是连续的亮度通道, 另一通道为VU分量交错 )
    public final static int ST_PIX_FMT_BGRA8888 = 4;// BGRA 8:8:8:8 32bpp ( 4通道32bit BGRA 像素 )
    public final static int ST_PIX_FMT_BGR888 = 5;  // BGR  8:8:8   24bpp ( 3通道24bit BGR 像素 )
    public final static int ST_PIX_FMT_RGBA8888 = 6; // RGRA 8:8:8:8 32bpp ( 4通道32bit RGBA 像素 )

    //支持的最大人脸数量
    public final static int ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT = 10;


    //人脸跟踪的配置选项，对应STMobileHumanActionNative 初始化时的config参数，具体配置如下：
    //  使用单线程或双线程track：处理图片必须使用单线程，处理视频建议使用多线程
    public final static int ST_MOBILE_TRACKING_MULTI_THREAD = 0x00000000;  // 多线程，功耗较多，卡顿较少
    public final static int ST_MOBILE_TRACKING_SINGLE_THREAD = 0x00010000;  // 单线程，功耗较少，对于性能弱的手机，会偶尔有卡顿现象

    //检测配置选项
    public final static int ST_MOBILE_TRACKING_ENABLE_DEBOUNCE = 0x00000010;  // 打开去抖动
    public final static int ST_MOBILE_TRACKING_ENABLE_FACE_ACTION = 0x00000020; // 检测脸部动作：张嘴、眨眼、抬眉、点头、摇头
    public final static int ST_MOBILE_ENABLE_FACE_DETECT = 0x00000040;     ///< 检测人脸
    public final static int ST_MOBILE_ENABLE_HAND_DETECT = 0x00000080;     ///< 检测手势
    public final static int ST_MOBILE_ENABLE_SEGMENT_DETECT = 0x00000100;  ///< 检测背景分割
    public final static int ST_MOBILE_ENABLE_FACE_240_DETECT = 0x00000200; ///< 检测人脸240


    //贴纸JNI API返回值定义
    enum ResultCode {
        ST_OK(0),                           ///< 正常运行
        ST_E_INVALIDARG(-1),                ///< 无效参数
        ST_E_HANDLE(-2),                    ///< 句柄错误
        ST_E_OUTOFMEMORY(-3),               ///< 内存不足
        ST_E_FAIL(-4),                      ///< 内部错误
        ST_E_DELNOTFOUND(-5),               ///< 定义缺失
        ST_E_INVALID_PIXEL_FORMAT(-6),      ///< 不支持的图像格式
        ST_E_FILE_NOT_FOUND(-7),            ///< 模型文件不存在
        ST_E_INVALID_FILE_FORMAT(-8),       ///< 模型格式不正确，导致加载失败
        ST_E_FILE_EXPIRE(-9),               ///< 模型文件过期
        ST_E_INVALID_AUTH(-13),             ///< license不合法
        ST_E_INVALID_APPID(-14),            ///< 包名错误
        ST_E_AUTH_EXPIRE(-15),              ///< license过期
        ST_E_UUID_MISMATCH(-16),            ///< UUID不匹配
        ST_E_ONLINE_AUTH_CONNECT_FAIL(-17), ///< 在线验证连接失败
        ST_E_ONLINE_AUTH_TIMEOUT(-18),      ///< 在线验证超时
        ST_E_ONLINE_AUTH_INVALID(-19),      ///< 在线签发服务器端返回错误
        ST_E_LICENSE_IS_NOT_ACTIVABLE(-20), ///< license不可激活
        ST_E_ACTIVE_FAIL(-21),              ///< license激活失败
        ST_E_ACTIVE_CODE_INVALID(-22),      ///< 激活码无效
        ST_E_NO_CAPABILITY(-23),            ///< license文件没有提供这个能力
        ST_E_GET_UDID_FAIL(-24),            ///< 在线签发绑定机器码的license时，无法获取机器唯一标识
        ST_E_SOCKET_INIT_FAILED(-25),       ///< socket初始化失败
        ST_E_SOCKET_NO_VERSION_TWO_POINT_TWO(-26), ///< Windows下特定错误，没有socket2.2版本
        ST_E_GET_HOST_ERROR(-27);                  ///< 无法解析验证服务器的地址


        private final int resultCode;

        ResultCode(int resultCode) {
            this.resultCode = resultCode;
        }

        public int getResultCode() {
            return resultCode;
        }
    }

    /**
     * 进行颜色格式转换, 不建议使用关于YUV420P的转换,速度较慢
     *
     * @param inputImage   用于待转换的图像数据
     * @param outputImage  转换后的图像数据
     * @param width        用于转换的图像的宽度(以像素为单位)
     * @param height       用于转换的图像的高度(以像素为单位)
     * @param type         需要转换的颜色格式,参考st_mobile_common.h中的st_color_convert_type
     * @return 成功返回0，错误返回其他，参考STUtils.ResultCode
     */
    public static native int stColorConvert(byte[] inputImage, byte[] outputImage, int width, int height, int type);

    /**
     * 进行图片旋转
     *
     * @param inputImage   待旋转的图像数据
     * @param outputImage  旋转后的图像数据
     * @param width        用于旋转的图像的宽度(以像素为单位)
     * @param height       用于旋转的图像的高度(以像素为单位)
     * @param format       用于旋转的图像的类型
     * @param rotation     需要旋转的角度，当旋转角度为90度或270度时，交换width和height后，按照新的宽高读取outputImage数据
     * @return 成功返回0，错误返回其他，参考STUtils.ResultCode
     */
    public static native int stImageRotate(byte[] inputImage, byte[] outputImage, int width, int height, int format, int rotation);
}
