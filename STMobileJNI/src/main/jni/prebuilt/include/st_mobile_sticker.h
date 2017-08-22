#ifndef INCLUDE_STMOBILE_ST_MOBILE_STIKER_H_
#define INCLUDE_STMOBILE_ST_MOBILE_STIKER_H_

#include "st_mobile_common.h"
#include "st_mobile_human_action.h"
/// 该文件中的API不保证线程安全.多线程调用时,需要确保安全调用.例如在 create handle 没有执行完就执行 process 可能造成crash;在 process 执行过程中调用 destroy 函数可能会造成crash.

/// @defgroup st_mobile_sticker
/// @brief sticker for image interfaces
///
/// This set of interfaces sticker.
///

/// @brief 创建贴纸句柄
/// @param[in] zip_path 输入的素材包路径
/// @parma[out] handle 贴纸句柄,失败返回NULL
/// @return 成功返回ST_OK,失败返回其他错误码,错误码定义在st_mobile_common.h中,如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_sticker_create(
	const char* zip_path,
	st_handle_t *handle
);

/// @brief 更换素材包,删除原有素材包
/// @parma[in] handle 已初始化的贴纸句柄
/// @param[in] zip_path 待更换的素材包文件夹
/// @return 成功返回ST_OK,失败返回其他错误码,错误码定义在st_mobile_common.h中,如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_sticker_change_package(
	st_handle_t handle,
	const char* zip_path
);

/// @brief 获取触发动作类型
/// @parma[in] handle 已初始化的贴纸句柄
/// @param[out] action 返回的触发动作,每一位分别代表该位对应状态是否是触发动作,对应状态详见st_mobile_common.h中,如ST_MOBILE_EYE_BLINK等
/// @return 成功返回ST_OK,失败返回其他错误码,错误码定义在st_mobile_common.h中,如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_sticker_get_trigger_action(
	st_handle_t handle,
	unsigned int *action
);

/// @brief 设置贴纸素材图像所占用的最大内存
/// @parma[in] handle 已初始化的贴纸句柄
/// @param[in] img_mem 贴纸素材图像所占用的最大内存（MB）,默认150MB,素材过大时,循环加载,降低内存； 贴纸较小时,全部加载,降低cpu
/// @return 成功返回ST_OK,失败返回其他错误码,错误码定义在st_mobile_common.h中,如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_sticker_set_max_imgmem(
	st_handle_t handle,
	float img_mem
);

/// 素材渲染状态
typedef enum {
	ST_MATERIAL_BEGIN = 0,     ///< 开始渲染素材
	ST_MATERIAL_PROCESS = 1,   ///< 素材渲染中
	ST_MATERIAL_END = 2         ///< 素材未被渲染
} st_material_status;

/// 素材渲染状态回调函数
/// @param[in] material_name 素材文件夹名称
/// @param[in] status 素材渲染状态,详见st_material_status定义
typedef void(*item_action)(const char* material_name,
			   st_material_status status);


/// @brief 对OpenGL ES 中的纹理进行贴纸处理,必须在opengl环境中运行,仅支持RGBA图像格式
/// @param[in]textureid_src 输入textureid
/// @param[in] image_width 图像宽度
/// @param[in] image_height 图像高度
/// @param[in] rotate 人脸朝向
/// @param[in] need_mirror 传入图像与显示图像是否是镜像关系
/// @param[in] human_action 动作,包含106点、face动作
/// @param[in] callback 素材渲染回调函数,由用户定义
/// @param[in]textureid_dst 输出textureid
ST_SDK_API st_result_t
st_mobile_sticker_process_texture(
	st_handle_t handle,
	unsigned int textureid_src, int image_width, int image_height,
	st_rotate_type rotate, bool need_mirror,
	p_st_mobile_human_action_t human_action,
	item_action callback,
	unsigned int textureid_dst
);

/// @brief 对OpenGL ES 中的纹理进行贴纸处理并转成buffer输出,必须在opengl环境中运行,仅支持RGBA图像格式的texture
/// @param[in] textureid_src 输入textureid
/// @param[in] image_width 图像宽度
/// @param[in] image_height 图像高度
/// @param[in] rotate 人脸朝向
/// @param[in] need_mirror 传入图像与显示图像是否是镜像关系
/// @param[in] human_action 动作,包含106点、face动作
/// @param[in] callback 素材渲染回调函数,由用户定义
/// @param[in] textureid_dst 输出textureid
/// @param[out] img_out 输出图像数据数组,需要用户分配内存,如果是null,不输出buffer
/// @param[in] fmt_out 输出图片的类型,支持NV21,BGR,BGRA,NV12,RGBA格式.
ST_SDK_API st_result_t
st_mobile_sticker_process_and_output_texture(
	st_handle_t handle,
	unsigned int textureid_src, int image_width, int image_height,
	st_rotate_type rotate, bool need_mirror,
	p_st_mobile_human_action_t human_action,
	item_action callback,
	unsigned int textureid_dst,
	unsigned char* img_out, st_pixel_format fmt_out
);


/// @brief 等待素材加载完毕后再渲染，用于希望等待模型加载完毕再渲染的场景，比如单帧或较短视频的3D绘制等
/// @parma[in] handle 已初始化的贴纸句柄
/// @param[in] wait 是否等待素材加载完毕后再渲染
/// @return 成功返回ST_OK,失败返回其他错误码,错误码定义在st_mobile_common.h中,如ST_E_FAIL等
ST_SDK_API st_result_t
st_mobile_sticker_set_waiting_material_loaded(
	st_handle_t handle,
	bool wait
);

/// @brief 释放贴纸句柄
ST_SDK_API void
st_mobile_sticker_destroy(
	st_handle_t handle
);


#endif  // INCLUDE_STMOBILE_ST_MOBILE_STIKER_H_
