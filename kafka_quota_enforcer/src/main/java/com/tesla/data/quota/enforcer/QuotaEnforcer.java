/*
 * Copyright (c) 2021. Tesla Motors, Inc. All rights reserved.
 */


package com.tesla.data.quota.enforcer;

import static tesla.shade.com.google.common.base.Preconditions.checkArgument;

import com.tesla.data.enforcer.Enforcer;
import com.tesla.data.enforcer.UnmanagedPrefixes;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class QuotaEnforcer extends Enforcer<ConfiguredQuota> {
  private final AdminClientQuotaService quotaService;
  private final UnmanagedPrefixes unmanaged;
  private final boolean dryRun;

  public QuotaEnforcer(Collection<ConfiguredQuota> configuredQuotas, AdminClientQuotaService quotaService,
                       boolean safeMode, boolean dryRun) {
    this(configuredQuotas, quotaService, UnmanagedPrefixes.NONE, safeMode, dryRun);
  }

  /**
   * The constructor.
   *
   * @param configuredQuotas desired quotas, none of them may be of an unmanaged principal
   * @param quotaService     a quota service
   * @param unmanaged        prefixes of principals owned by an external system, existing un-configured quotas of
   *                         such principals are never deleted
   * @param safeMode         if true, risky operations (ex: deletion) are skipped
   * @param dryRun           if true, no changes are made to the cluster
   */
  public QuotaEnforcer(Collection<ConfiguredQuota> configuredQuotas, AdminClientQuotaService quotaService,
                       UnmanagedPrefixes unmanaged, boolean safeMode, boolean dryRun) {
    super(configuredQuotas, quotaService::listExisting, ConfiguredQuota::equals, safeMode);
    LOG.info("Unmanaged principal prefixes: {}", unmanaged.principalPrefixes());
    checkNoUnmanagedPrincipals(
        configuredQuotas.stream().map(ConfiguredQuota::getPrincipal).collect(Collectors.toList()), unmanaged);
    this.quotaService = quotaService;
    this.unmanaged = unmanaged;
    this.dryRun = dryRun;
  }

  /**
   * Check that none of the configured quotas is of an unmanaged principal. A quota has a single value, setting it
   * here as well would fight with the external owner of the principal.
   *
   * @param principals principals of configured quotas, null for client only quotas
   * @param unmanaged  prefixes of principals owned by an external system
   * @throws IllegalArgumentException if any of the principals is unmanaged
   */
  static void checkNoUnmanagedPrincipals(Collection<String> principals, UnmanagedPrefixes unmanaged) {
    List<String> unmanagedInConfig = principals.stream()
        .filter(p -> isUnmanagedPrincipal(p, unmanaged))
        .distinct()
        .collect(Collectors.toList());
    checkArgument(unmanagedInConfig.isEmpty(), "Quotas of unmanaged principals found in config: %s",
        unmanagedInConfig);
  }

  // Client only quotas have no principal, and the default quota applies to every principal, so both are managed.
  private static boolean isUnmanagedPrincipal(String principal, UnmanagedPrefixes unmanaged) {
    return principal != null && !ConfiguredQuota.ENTITY_DEFAULT.equals(principal)
        && unmanaged.isUnmanagedPrincipal(principal);
  }

  @Override
  protected boolean isUnmanaged(ConfiguredQuota quota) {
    return isUnmanagedPrincipal(quota.getPrincipal(), unmanaged);
  }

  @Override
  protected void create(List<ConfiguredQuota> toCreate) {
    if (!dryRun) {
      quotaService.create(toCreate);
    }
  }

  @Override
  protected void delete(List<ConfiguredQuota> toDelete) {
    if (!dryRun) {
      quotaService.delete(toDelete);
    }
  }
}
