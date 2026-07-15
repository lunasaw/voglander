package io.github.lunasaw.voglander.manager.domaon.dto.task;

import java.io.Serializable;

import lombok.Data;

/** Trusted visibility scope applied independently from user-supplied query filters. */
@Data
public class BizTaskAccessScopeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Boolean global;
    private String ownerType;
    private String ownerId;
    private String organizationId;

    public static BizTaskAccessScopeDTO global() {
        BizTaskAccessScopeDTO scope = new BizTaskAccessScopeDTO();
        scope.setGlobal(true);
        return scope;
    }

    public static BizTaskAccessScopeDTO owner(String ownerType, String ownerId) {
        BizTaskAccessScopeDTO scope = new BizTaskAccessScopeDTO();
        scope.setGlobal(false);
        scope.setOwnerType(ownerType);
        scope.setOwnerId(ownerId);
        return scope;
    }

    public boolean isGlobalScope() {
        return Boolean.TRUE.equals(global);
    }
}
