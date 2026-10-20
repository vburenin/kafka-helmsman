/*
 * Copyright (c) 2020. Tesla Motors, Inc. All rights reserved.
 */

package com.tesla.data.acl;

import com.tesla.data.enforcer.Enforcer;
import com.tesla.data.enforcer.UnmanagedPrefixes;

import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.utils.SecurityUtils;

import java.util.Collection;
import java.util.List;

public class AclEnforcer extends Enforcer<AclBinding> {
  private static final String WILDCARD_PRINCIPAL_NAME = "*";
  private final AclService aclService;
  private final UnmanagedPrefixes unmanaged;
  private final boolean dryRun;

  public AclEnforcer(Collection<AclBinding> configured, AclService aclService, boolean safemode, boolean dryRun) {
    this(configured, aclService, UnmanagedPrefixes.NONE, safemode, dryRun);
  }

  /**
   * The constructor.
   *
   * @param configured desired bindings, these are created even if they are unmanaged
   * @param aclService an acl service
   * @param unmanaged  prefixes of TOPIC and GROUP resources and of principals owned by an external system,
   *                   existing un-configured bindings on such resources or of such principals are never deleted
   * @param safemode   if true, risky operations (ex: deletion) are skipped
   * @param dryRun     if true, no changes are made to the cluster
   */
  public AclEnforcer(Collection<AclBinding> configured, AclService aclService, UnmanagedPrefixes unmanaged,
                     boolean safemode, boolean dryRun) {
    super(configured, aclService::listExisting, AclBinding::equals, safemode);
    this.aclService = aclService;
    this.unmanaged = unmanaged;
    this.dryRun = dryRun;
    LOG.info("Unmanaged topic prefixes: {}, group prefixes: {}, principal prefixes: {}",
        unmanaged.topicPrefixes(), unmanaged.groupPrefixes(), unmanaged.principalPrefixes());
  }

  @Override
  protected boolean isUnmanaged(AclBinding binding) {
    return isUnmanagedResource(binding.pattern()) || isUnmanagedPrincipal(binding.entry().principal());
  }

  // Bindings on the wildcard resource grant access beyond any prefix, so they are always managed.
  private boolean isUnmanagedResource(ResourcePattern pattern) {
    if (ResourcePattern.WILDCARD_RESOURCE.equals(pattern.name())) {
      return false;
    }
    switch (pattern.resourceType()) {
      case TOPIC:
        return unmanaged.isUnmanagedTopic(pattern.name());
      case GROUP:
        return unmanaged.isUnmanagedGroup(pattern.name());
      default:
        return false;
    }
  }

  // The principal type is not matched, ex: prefix 'ds-' matches 'User:ds-app' and 'Group:ds-team'. The wildcard
  // principal grants access to everyone, so it is always managed.
  private boolean isUnmanagedPrincipal(String principal) {
    if (unmanaged.principalPrefixes().isEmpty()) {
      return false;
    }
    String name = SecurityUtils.parseKafkaPrincipal(principal).getName();
    return !WILDCARD_PRINCIPAL_NAME.equals(name) && unmanaged.isUnmanagedPrincipal(name);
  }

  @Override
  protected void create(List<AclBinding> toCreate) {
    if (!dryRun) {
      aclService.create(toCreate);
    }
  }

  @Override
  protected void delete(List<AclBinding> toDelete) {
    if (!dryRun) {
      aclService.delete(toDelete);
    }
  }

}
