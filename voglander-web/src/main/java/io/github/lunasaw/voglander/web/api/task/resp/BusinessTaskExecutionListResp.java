package io.github.lunasaw.voglander.web.api.task.resp;

import java.io.Serializable;
import java.util.List;

import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskExecutionVO;
import lombok.Data;

@Data
public class BusinessTaskExecutionListResp implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long total;
    private List<BusinessTaskExecutionVO> items;
}
