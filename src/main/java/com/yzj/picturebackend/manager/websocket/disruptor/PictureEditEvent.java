package com.yzj.picturebackend.manager.websocket.disruptor;

import com.yzj.picturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yzj.picturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑事件
 * 事件是 Disruptor 执行的核心单位，充当了上下文容器，所有处理消息所需的数据都被封装在其中
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;
}
