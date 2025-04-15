package com.yzj.picturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 请求删除包装类
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}

