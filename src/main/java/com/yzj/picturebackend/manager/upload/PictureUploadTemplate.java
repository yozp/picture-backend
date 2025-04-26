package com.yzj.picturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yzj.picturebackend.config.CosClientConfig;
import com.yzj.picturebackend.exception.BusinessException;
import com.yzj.picturebackend.exception.ErrorCode;
import com.yzj.picturebackend.manager.CosManager;
import com.yzj.picturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板 抽象类
 * 采用模板方法设计模式
 */
@Slf4j
public abstract class PictureUploadTemplate {  
  
    @Resource
    protected CosManager cosManager;
  
    @Resource  
    protected CosClientConfig cosClientConfig;

    /**
     * 模板方法，定义上传流程
     * 为了让模板同时兼容 MultiPartFile 和 String 类型的文件参数，
     * 直接将这两种情况统一为 Object 类型的 inputSource 输入源
     * @param inputSource
     * @param uploadPathPrefix
     * @return
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片  
        validPicture(inputSource);

        // 2. 图片上传地址  
        String uuid = RandomUtil.randomString(16);//随机生成16位字符
        //获取输入源的原始文件名
        String originFilename = getOriginFilename(inputSource);

        //String类的format()方法用于创建格式化的字符串以及连接多个字符串对象
        //FileUtil.getSuffix() 获取文件后缀名，扩展名不带“.”
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        //拼接图片完整路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
//        uploadPath=processUploadPath(uploadPath);
  
        File file = null;  
        try {  
            // 3. 创建临时文件  
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源（本地或 URL）  
            processFile(inputSource, file);  
  
            // 4. 上传图片到对象存储  
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //获取图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
  
            // 5. 封装返回结果  
            return buildResult(originFilename, file, uploadPath, imageInfo);  
        } catch (Exception e) {  
            log.error("图片上传到对象存储失败", e);  
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {  
            // 6. 清理临时文件  
            deleteTempFile(file);  
        }  
    }  
  
    /**  
     * 校验输入源（本地文件或 URL）  
     */  
    protected abstract void validPicture(Object inputSource);  
  
    /**  
     * 获取输入源的原始文件名  
     */  
    protected abstract String getOriginFilename(Object inputSource);  
  
    /**  
     * 处理输入源并生成本地临时文件  
     */  
    protected abstract void processFile(Object inputSource, File file) throws Exception;  
  
    /**  
     * 封装返回结果  
     */  
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();  
        int picWidth = imageInfo.getWidth();  
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();//图片宽高比
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);//图片地址
        uploadPictureResult.setPicName(FileUtil.getName(originFilename));//mainName() 返回主文件名
        uploadPictureResult.setPicSize(FileUtil.size(file));//计算目录或文件的总大小
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());//获取图片格式
        return uploadPictureResult;  
    }  
  
    /**  
     * 删除临时文件  
     */  
    public void deleteTempFile(File file) {  
        if (file == null) {  
            return;  
        }  
        boolean deleteResult = file.delete();  
        if (!deleteResult) {  
            log.error("file delete error, filepath = {}", file.getAbsolutePath());  
        }  
    }

//    /**
//     * 处理路径方法（暂时废弃）
//     */
//    final List<String> MUST_SUFFIX = Arrays.asList("jpeg", "jpg", "png", "webp", "gif", "bmp", "tiff", "svg");
//    public String processUploadPath(String uploadPath) {
//        // 统一转为小写处理（保证后缀大小写不敏感）
//        String lowerPath = uploadPath.toLowerCase();
//        // 遍历所有合法后缀，检查是否以.后缀结尾
//        for (String suffix : MUST_SUFFIX) {
//            // 支持带参数链接（如image.png?width=200）
//            if (lowerPath.matches(".*\\." + suffix + "($|\\?.*)")) {
//                return uploadPath;
//            }
//        }
//        return uploadPath + ".png"; // 默认补全为 PNG（兼容性最佳）
//    }

}
