/*
 * Copyright (c) 2021. Tesla Motors, Inc. All rights reserved.
 */

package com.tesla.data.quota.enforcer;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tesla.data.enforcer.UnmanagedPrefixes;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuotaEnforcerTest {

  private static final UnmanagedPrefixes UNMANAGED =
      UnmanagedPrefixes.from(Map.of("principalPrefixes", List.of("ds-")));

  private AdminClientQuotaService quotaService;
  private static final List<ConfiguredQuota> quotas = Arrays.asList(
      new ConfiguredQuota("user1", "clientA", 5000d, 4000d, 100d),
      new ConfiguredQuota("<default>", null, 6000d, 3000d, null)
  );

  @Before
  public void setup() {
    quotaService = mock(AdminClientQuotaService.class);
  }

  @Test
  public void testCreate() {
    QuotaEnforcer enforcer = new QuotaEnforcer(emptyList(), quotaService, true, false);
    enforcer.create(quotas);
    verify(quotaService).create(quotas);
  }

  @Test
  public void testDelete() {
    QuotaEnforcer enforcer = new QuotaEnforcer(emptyList(), quotaService, true, false);
    enforcer.delete(quotas);
    verify(quotaService).delete(quotas);
  }

  @Test
  public void testCreateDryRun() {
    QuotaEnforcer enforcer = new QuotaEnforcer(emptyList(), quotaService, true, true);
    enforcer.create(quotas);
    verifyNoInteractions(quotaService);
  }

  @Test
  public void testDeleteDryRun() {
    QuotaEnforcer enforcer = new QuotaEnforcer(emptyList(), quotaService, true, true);
    enforcer.delete(quotas);
    verifyNoInteractions(quotaService);
  }

  private static ConfiguredQuota quota(String principal, String client) {
    return new ConfiguredQuota(principal, client, 1000d, null, null);
  }

  @Test
  public void testUnexpectedSkipsUnmanagedPrincipals() {
    ConfiguredQuota unmanaged = quota("ds-app", null);
    ConfiguredQuota unmanagedWithClient = quota("ds-app", "clientA");
    ConfiguredQuota managed = quota("user1", null);
    // client ids are not principals
    ConfiguredQuota clientOnly = quota(null, "ds-client");
    when(quotaService.listExisting()).thenReturn(List.of(unmanaged, unmanagedWithClient, managed, clientOnly));

    QuotaEnforcer enforcer = new QuotaEnforcer(emptyList(), quotaService, UNMANAGED, true, true);

    Assert.assertEquals(Set.of(managed, clientOnly), new HashSet<>(enforcer.unexpected()));
  }

  @Test
  public void testDefaultPrincipalAlwaysManaged() {
    // the default quota applies to every principal, so it is never unmanaged
    UnmanagedPrefixes lt = UnmanagedPrefixes.from(Map.of("principalPrefixes", List.of("<")));
    ConfiguredQuota defaultQuota = quota(ConfiguredQuota.ENTITY_DEFAULT, null);
    ConfiguredQuota defaultWithClient = quota(ConfiguredQuota.ENTITY_DEFAULT, "clientA");
    when(quotaService.listExisting()).thenReturn(List.of(defaultQuota, defaultWithClient));

    QuotaEnforcer enforcer = new QuotaEnforcer(emptyList(), quotaService, lt, true, true);

    Assert.assertEquals(Set.of(defaultQuota, defaultWithClient), new HashSet<>(enforcer.unexpected()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfiguredUnmanagedPrincipalRejected() {
    new QuotaEnforcer(List.of(quota("user1", null), quota("ds-app", "clientA")), quotaService, UNMANAGED, true, true);
  }

  @Test
  public void testConfiguredDefaultAndClientQuotasAccepted() {
    List<ConfiguredQuota> configured = List.of(quota(ConfiguredQuota.ENTITY_DEFAULT, null), quota(null, "ds-client"));
    when(quotaService.listExisting()).thenReturn(emptyList());

    QuotaEnforcer enforcer = new QuotaEnforcer(configured, quotaService, UNMANAGED, true, true);

    Assert.assertEquals(configured, enforcer.absent());
  }

  @Test
  public void testDeleteUnexpectedLeavesUnmanagedQuotas() {
    ConfiguredQuota keepA = quota("a", null);
    ConfiguredQuota keepB = quota("b", null);
    ConfiguredQuota stale = quota("stale", null);
    when(quotaService.listExisting()).thenReturn(List.of(keepA, keepB, stale, quota("ds-app", null)));

    // without the unmanaged filter, 2 of 2 configured would be deleted and trip the 50% threshold
    QuotaEnforcer enforcer = new QuotaEnforcer(List.of(keepA, keepB), quotaService, UNMANAGED, false, false);
    enforcer.deleteUnexpected();

    verify(quotaService).delete(List.of(stale));
  }

  @Test
  public void testLogsEffectiveUnmanagedPrefixes() {
    // a misspelled 'unmanaged' section is silently ignored, the startup log is how an operator notices
    Logger logger = (Logger) LoggerFactory.getLogger(QuotaEnforcer.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      new QuotaEnforcer(emptyList(), quotaService, UNMANAGED, true, true);
    } finally {
      logger.detachAppender(appender);
    }
    Assert.assertTrue(appender.list.stream()
        .map(ILoggingEvent::getFormattedMessage)
        .anyMatch("Unmanaged principal prefixes: [ds-]"::equals));
  }
}
