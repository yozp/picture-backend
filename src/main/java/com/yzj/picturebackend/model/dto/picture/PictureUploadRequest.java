package com.yzj.picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 接受图片上传请求参数的类
 */
@Data
public class PictureUploadRequest implements Serializable {
  
    /**  
     * 图片 id（用于修改）  
     */  
    private Long id;  
  
    private static final long serialVersionUID = 1L;  
}
