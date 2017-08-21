#ifndef INCLUDE_STMOBILE_ST_MOBILE_HUMAN_ACTION_H_
#define INCLUDE_STMOBILE_ST_MOBILE_HUMAN_ACTION_H_

#include "st_mobile_common.h"

// 用于detect_config 配置选项,也可用来判断action动作类型
#ifndef ST_MOBILE_FACE_DETECT
#define ST_MOBILE_FACE_DETECT       0x00000001	///< 人脸检测
#endif
#ifndef ST_MOBILE_EYE_BLINK
#define ST_MOBILE_EYE_BLINK         0x00000002  ///< 眨眼
#endif
#ifndef ST_MOBILE_MOUTH_AH
#define ST_MOBILE_MOUTH_AH          0x00000004  ///< 嘴巴大张
#endif
#ifndef ST_MOBILE_HEAD_YAW
#define ST_MOBILE_HEAD_YAW          0x00000008  ///< 摇头
#endif
#ifndef ST_MOBILE_HEAD_PITCH
#define ST_MOBILE_HEAD_PITCH        0x00000010  ///< 点头
#endif
#ifndef ST_MOBILE_BROW_JUMP
#define ST_MOBILE_BROW_JUMP         0x00000020  ///< 眉毛挑动
#endif
#ifndef ST_MOBILE_HAND_GOOD
#define ST_MOBILE_HAND_GOOD         0x00000800  ///< 大拇哥 2048
#endif
#ifndef ST_MOBILE_HAND_PALM
#define ST_MOBILE_HAND_PALM         0x00001000  ///< 手掌 4096
#endif
#ifndef ST_MOBILE_HAND_LOVE
#define ST_MOBILE_HAND_LOVE         0x00004000  ///< 爱心 16384
#endif
#ifndef ST_MOBILE_HAND_HOLDUP
#define ST_MOBILE_HAND_HOLDUP       0x00008000  ///< 托手 32768
#endif
#ifndef ST_MOBILE_HAND_CONGRATULATE
#define ST_MOBILE_HAND_CONGRATULATE 0x00020000  ///< 恭贺（抱拳） 131072
#endif
#ifndef ST_MOBILE_HAND_FINGER_HEART
#define ST_MOBILE_HAND_FINGER_HEART 0x00040000  ///< 单手比爱心 262144
#endif
#ifndef ST_MOBILE_SEG_BACKGROUND
#define ST_MOBILE_SEG_BACKGROUND    0x00010000	///< 前景背景分割 65536
#endif
#ifndef ST_MOBILE_FACE_240_DETECT
#define ST_MOBILE_FACE_240_DETECT	0x01000000	///< 人脸240关键点
#endif

#define ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_DETECT    ST_MOBILE_FACE_DETECT | ST_MOBILE_EYE_BLINK | ST_MOBILE_MOUTH_AH | ST_MOBILE_HEAD_YAW |ST_MOBILE_HEAD_PITCH | ST_MOBILE_BROW_JUMP | ST_MOBILE_HAND_GOOD | ST_MOBILE_HAND_PALM | ST_MOBILE_HAND_LOVE | ST_MOBILE_HAND_HOLDUP |ST_MOBILE_HAND_CONGRATULATE | ST_MOBILE_HAND_FINGER_HEART | ST_MOBILE_SEG_BACKGROUND   ///< 全部检测,不建议使用,耗时、cpu占用率会变高,建议根据需求检测相关动作

#define ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT           10
#define ST_MOBILE_HUMAN_ACTION_MAX_HAND_COUNT           1

#ifndef INCLUDE_STMOBILE_ST_MOBILE_HAND_H_
#define INCLUDE_STMOBILE_ST_MOBILE_HAND_H_
/// hand信息及手部相关动作
typedef struct st_mobile_hand_action_t {
	st_rect_t hand;             /// 手部矩形
	st_pointi_t key_point;             /// 手部关键点
	float hand_score;                  /// 手置信度
	unsigned int hand_action;          /// 手部动作
	float hand_action_score;           /// 动作置信度
} st_mobile_hand_action_t;
#endif


/// 眼睛,眉毛,嘴唇详细检测结果
typedef struct st_mobile_face_extra_info_t {
        int eye_count;                 ///< 检测到眼睛数量
	int eyebrow_count;             ///< 检测到眉毛数量
	int lips_count;                ///< 检测到嘴唇数量
	st_pointf_t eye_left[ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT][22];       ///< 左眼关键点
	st_pointf_t eye_right[ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT][22];      ///< 右眼关键点
        st_pointf_t eyebrow_left[ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT][13];   ///< 左眉毛关键点
        st_pointf_t eyebrow_right[ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT][13];  ///< 右眉毛关键点
        st_pointf_t lips[ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT][64];           ///< 嘴唇关键点
} st_mobile_face_extra_info_t, *p_st_mobile_face_extra_info_t;



/// 检测结果
typedef struct st_mobile_human_action_t {
	st_mobile_face_action_t
	faces[ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT];   /// 检测到的人脸及动作数组
	int face_count;                                                         /// 检测到的人脸数目
	st_mobile_hand_action_t
	hands[ST_MOBILE_HUMAN_ACTION_MAX_HAND_COUNT];         /// 手部位置与动作信息
	int hand_count;                                                     /// 检测到的手的数目
	st_image_t
	background;                                              /// 前后背景分割图片信息,前景为0,背景为255,边缘部分有模糊(0-255之间),输出图像大小可以调节
	int background_result;                                                 /// 前后背景分割返回结果 0表示没有做背景分割或不成功,0x00010000表示做了背景分割
	st_mobile_face_extra_info_t *p_face_extra_info;

} st_mobile_human_action_t, *p_st_mobile_human_action_t;

/// @defgroup st_mobile_human_action
/// @brief human action detection interfaces
///
/// This set of interfaces detect human action.
///
/// @{

/// 该文件中的API不保证线程安全.多线程调用时,需要确保安全调用.例如在 create handle 没有执行完就执行 process 可能造成crash;在 process 执行过程中调用 destroy 函数可能会造成crash.

/// @brief human_action配置选项
// 使用单线程或双线程track：处理图片必须使用单线程,处理视频建议使用多线程
#ifndef ST_MOBILE_TRACKING_MULTI_THREAD
#define ST_MOBILE_TRACKING_MULTI_THREAD         0x00000000  ///< 多线程,功耗较多,卡顿较少
#endif
#ifndef ST_MOBILE_TRACKING_SINGLE_THREAD
#define ST_MOBILE_TRACKING_SINGLE_THREAD        0x00010000  ///< 单线程,功耗较少,对于性能弱的手机,会偶尔有卡顿现象
#endif
#ifndef ST_MOBILE_TRACKING_ENABLE_DEBOUNCE
#define ST_MOBILE_TRACKING_ENABLE_DEBOUNCE      0x00000010  ///< 打开人脸三维旋转角度去抖动
#endif
#ifndef ST_MOBILE_TRACKING_ENABLE_FACE_ACTION
#define ST_MOBILE_TRACKING_ENABLE_FACE_ACTION   0x00000020  ///< 检测脸部动作：张嘴、眨眼、抬眉、点头、摇头
#endif

#define ST_MOBILE_ENABLE_FACE_DETECT            0x00000040  ///< 检测人脸
#define ST_MOBILE_ENABLE_HAND_DETECT            0x00000080  ///< 检测手势
#define ST_MOBILE_ENABLE_SEGMENT_DETECT         0x00000100  ///< 检测背景分割
#define ST_MOBILE_ENABLE_FACE_240_DETECT        0x00000200  ///< 检测人脸240

/// st_mobile_human_action_create的config参数默认配置
/// 对视频进行检测推荐使用多线程
#define ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_VIDEO     ST_MOBILE_TRACKING_MULTI_THREAD | \
                                                        ST_MOBILE_TRACKING_ENABLE_FACE_ACTION | \
                                                        ST_MOBILE_TRACKING_ENABLE_DEBOUNCE | \
                                                        ST_MOBILE_ENABLE_FACE_DETECT | \
                                                        ST_MOBILE_ENABLE_HAND_DETECT | \
                                                        ST_MOBILE_ENABLE_SEGMENT_DETECT
/// 对图片进行检测只能使用单线程
#define ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_IMAGE     ST_MOBILE_TRACKING_SINGLE_THREAD | \
                                                        ST_MOBILE_ENABLE_FACE_DETECT | \
                                                        ST_MOBILE_ENABLE_HAND_DETECT | \
                                                        ST_MOBILE_ENABLE_SEGMENT_DETECT

/// @brief 创建人体行为检测句柄
/// @param[in] model_path 模型文件的路径,例如models/action.model
/// @param[in] config 设置创建人体行为句柄的方式,检测视频时设置为ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_CREATE,检测图片时设置为ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_IMAGE
/// @parma[out] handle 人体行为检测句柄,失败返回NULL
/// @return 成功返回ST_OK,失败返回其他错误码,错误码定义在st_mobile_common.h中,如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_human_action_create(
	const char *model_path,
	unsigned int config,
	st_handle_t *handle
);

/// @brief 释放人体行为检测句柄
/// @param[in] handle 已初始化的人体行为句柄
ST_SDK_API
void st_mobile_human_action_destroy(
	st_handle_t handle
);

/// @brief 人体行为检测
/// @param[in] handle 已初始化的人体行为句柄
/// @param[in] image 用于检测的图像数据
/// @param[in] pixel_format 用于检测的图像数据的像素格式,不支持GRAY图
/// @param[in] image_width 用于检测的图像的宽度(以像素为单位)
/// @param[in] image_height 用于检测的图像的高度(以像素为单位)
/// @param[in] image_stride 用于检测的图像的跨度(以像素为单位),即每行的字节数；目前仅支持字节对齐的padding,不支持roi
/// @param[in] orientation 图像中人脸的方向
/// @param[in] detect_config 需要检测的人体行为,例如ST_MOBILE_EYE_BLINK | ST_MOBILE_MOUTH_AH | ST_MOBILE_HAND_LOVE | ST_MOBILE_SEG_BACKGROUND
/// @param[out] p_human_action 检测到的人体行为,由用户分配内存
/// @return 成功返回ST_OK,失败返回其他错误码,错误码定义在st_mobile_common.h中,如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_human_action_detect(
	st_handle_t handle,
	const unsigned char *image,
	st_pixel_format pixel_format,
	int image_width,
	int image_height,
	int image_stride,
	st_rotate_type orientation,
	unsigned int detect_config,
	st_mobile_human_action_t *p_human_action
);

///@brief 重置,清除所有缓存信息
ST_SDK_API st_result_t
st_mobile_human_action_reset(
	st_handle_t handle
);

///@brief human_action参数类型
typedef enum  {
	// background结果中长边的长度[10,长边长度](默认长边240,短边=长边/原始图像长边*原始图像短边).值越大,背景分割的耗时越长,边缘部分效果越好.
	ST_HUMAN_ACTION_BACKGROUND_MAX_SIZE = 1,
	// 背景分割羽化程度[0,1](默认值0.35),0 完全不羽化,1羽化程度最高,在strenth较小时,羽化程度基本不变.值越大,前景与背景之间的过度边缘部分越宽.
	ST_HUMAN_ACTION_BACKGROUND_BLUR_STRENGTH = 2,
	// 设置检测到的最大人脸数目N(默认值10),持续track已检测到的N个人脸直到人脸数小于N再继续做detect.值越大,检测到的人脸数目越多,但相应耗时越长.
	ST_HUMAN_ACTION_FACELIMIT = 3,
	//  设置tracker每多少帧进行一次detect(默认值有人脸时30,无人脸时30/3=10). 值越大,cpu占用率越低, 但检测出新人脸的时间越长.
	ST_HUMAN_ACTION_DETECT_INTERVAL = 4,
	// 设置106点平滑的阈值[0.0,1.0](默认值0.5), 值越大, 点越稳定,但相应点会有滞后.
	ST_HUMAN_ACTION_SMOOTH_THRESHOLD = 5,
	// 设置head_pose去抖动的阈值[0.0,1.0](默认值0.5),值越大, pose信息的值越稳定,但相应值会有滞后.
	ST_HUMAN_ACTION_HEADPOSE_THRESHOLD = 6,
} st_human_action_type;

/// @brief 设置human_action参数
/// @param [in] handle 已初始化的human_action句柄
/// @param [in] type human_action参数关键字,例如ST_BACKGROUND_MAX_SIZE、ST_BACKGROUND_BLUR_STRENGTH
/// @param [in] value 参数取值
/// @return 成功返回ST_OK,错误则返回错误码,错误码定义在st_mobile_common.h 中,如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_human_action_setparam(
	st_handle_t handle,
	st_human_action_type type,
	float value
);

#endif  // INCLUDE_STMOBILE_ST_MOBILE_HUMAN_ACTION_H_
