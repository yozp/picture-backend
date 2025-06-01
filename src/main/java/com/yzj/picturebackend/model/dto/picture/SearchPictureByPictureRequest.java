package com.yzj.picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 以图搜图请求封装类
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
