package io.github.lunasaw.voglander.web.api.task.resp;

import java.io.Serializable;
import java.util.List;

import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskVO;
import lombok.Data;

@Data
public class BusinessTaskListResp implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long total;
    private List<BusinessTaskVO> items;
}
