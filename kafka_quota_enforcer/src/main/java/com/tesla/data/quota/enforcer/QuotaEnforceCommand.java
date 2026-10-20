/*
 * Copyright (c) 2021. Tesla Motors, Inc. All rights reserved.
 */

package com.tesla.data.quota.enforcer;

import com.tesla.data.enforcer.EnforceCommand;
import com.tesla.data.enforcer.Enforcer;
import com.tesla.data.enforcer.UnmanagedPrefixes;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;

import java.util.Map;


public class QuotaEnforceCommand extends EnforceCommand<ConfiguredQuota> {
  private AdminClient adminClient;

  public QuotaEnforceCommand(){
    // DO NOT REMOVE, this is needed by jcommander
  }

  // for testing
  public QuotaEnforceCommand(Map<String, Object> cmdConfig, String cluster) {
    this.cmdConfig = cmdConfig;
    this.cluster = cluster;
  }

  @Override
  protected Enforcer<ConfiguredQuota> initEnforcer() {
    // parse before connecting, so a bad config fails fast
    UnmanagedPrefixes unmanaged = unmanagedPrefixes();
    this.adminClient = KafkaAdminClient.create(kafkaConfig());
    return new QuotaEnforcer(configuredEntities(ConfiguredQuota.class, "quotas", "quotasFile"),
        new AdminClientQuotaService(adminClient), unmanaged, !unsafemode, dryrun);
  }

  @Override
  protected void close() {
    if (this.adminClient != null) {
      this.adminClient.close();
    }
  }
}
