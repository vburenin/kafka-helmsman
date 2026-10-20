/*
 * Copyright (c) 2026. Tesla Motors, Inc. All rights reserved.
 */

package com.tesla.data.quota.enforcer;

import com.tesla.data.enforcer.BaseCommand;
import com.tesla.data.enforcer.BaseCommand.CommandConfigConverter;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ValidateCommandTest {

  private static final String KAFKA = String.join("\n",
      "---",
      "kafka:",
      "  bootstrap.servers: localhost:9092");
  private static final String UNMANAGED = String.join("\n",
      "unmanaged:",
      "  principalPrefixes: [\"ds-\"]");
  private final CommandConfigConverter converter = new CommandConfigConverter();

  private int validate(String... lines) throws IOException {
    String conf = String.join("\n", lines);
    return new ValidateCommand(converter.convert(new ByteArrayInputStream(conf.getBytes(StandardCharsets.UTF_8))))
        .run();
  }

  @Test
  public void testManagedQuotasAccepted() throws IOException {
    Assert.assertEquals(BaseCommand.SUCCESS, validate(KAFKA, UNMANAGED,
        "quotas:",
        "  - principal: user1",
        "    producer-byte-rate: 1000",
        "  - principal: <default>",
        "    producer-byte-rate: 1000",
        "  - client: ds-client",
        "    producer-byte-rate: 1000"));
  }

  @Test
  public void testUnmanagedPrincipalRejected() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA, UNMANAGED,
        "quotas:",
        "  - principal: user1",
        "    producer-byte-rate: 1000",
        "  - principal: ds-app",
        "    client: clientA",
        "    producer-byte-rate: 1000"));
  }

  @Test
  public void testUnmanagedPrincipalOverrideRejected() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA, UNMANAGED,
        "quotas:",
        "  - principal: user1",
        "    producer-byte-rate: 1000",
        "    clusters:",
        "      foo: {}",
        "      bar:",
        "        principal: ds-app"));
  }

  @Test
  public void testEmptyQuotasRejected() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA, "quotas:"));
  }

  @Test
  public void testEmptyQuotaEntryRejected() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA,
        "quotas:",
        "  - principal: user1",
        "    producer-byte-rate: 1000",
        "  -"));
  }

  @Test
  public void testConfigWithoutQuotasAccepted() throws IOException {
    Assert.assertEquals(BaseCommand.SUCCESS, validate(KAFKA, UNMANAGED));
  }
}
