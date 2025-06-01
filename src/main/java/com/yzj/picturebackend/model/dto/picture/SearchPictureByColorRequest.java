package com.yzj.picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 以颜色搜图的请求封装类
 */
@Data
public class SearchPictureByColorRequest implements Serializable {

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
