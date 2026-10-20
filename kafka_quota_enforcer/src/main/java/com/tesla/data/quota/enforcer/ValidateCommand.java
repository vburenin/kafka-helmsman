/*
 * Copyright (c) 2026. Tesla Motors, Inc. All rights reserved.
 */

package com.tesla.data.quota.enforcer;

import com.tesla.data.enforcer.BaseCommand;
import com.tesla.data.enforcer.ClusterEntities;

import com.beust.jcommander.Parameters;

import java.util.Map;
import java.util.stream.Collectors;

@Parameters(commandDescription = "Validate config, configured quotas must not be of unmanaged principals")
public class ValidateCommand extends BaseCommand<ConfiguredQuota> {

  private static final String QUOTAS_KEY = "quotas";
  private static final String QUOTAS_FILE_KEY = "quotasFile";
  private static final String PRINCIPAL = "principal";

  public ValidateCommand() {
    // DO NOT REMOVE, this is needed by jcommander
  }

  public ValidateCommand(Map<String, Object> cmdConfig) {
    super(cmdConfig);
  }

  @Override
  public int run() {
    int result = super.run();
    if (result != SUCCESS) {
      return result;
    }
    // a principal can be overridden per cluster, overrides are checked as well
    return validateEntities(QUOTAS_KEY, QUOTAS_FILE_KEY, quotas -> QuotaEnforcer.checkNoUnmanagedPrincipals(
        quotas.stream()
            .flatMap(quota -> ClusterEntities.propertyValues(quota, PRINCIPAL).stream())
            .map(String::valueOf)
            .collect(Collectors.toList()),
        unmanagedPrefixes()));
  }
}
