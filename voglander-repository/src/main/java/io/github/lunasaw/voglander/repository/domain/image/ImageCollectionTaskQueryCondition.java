package io.github.lunasaw.voglander.repository.domain.image;

import java.io.Serializable;
import java.util.Set;

import lombok.Data;

/** Database-ready filters and trusted scope for task/config joined reads. */
@Data
public class ImageCollectionTaskQueryCondition implements Serializable {
    private static final long serialVersionUID = 1L;
    private String taskId;
    private String taskNameLike;
    private String taskMode;
    private String state;
    private String deviceId;
    private String channelId;
    private String filterOwnerType;
    private String filterOwnerId;
    private String filterOrganizationId;
    private Boolean global;
    private String ownerType;
    private String ownerId;
    private String organizationId;
    private Set<String> allowedTaskTypes;
}
