/*
 * Copyright (c) 2026. Tesla Motors, Inc. All rights reserved.
 */

package com.tesla.data.topic.enforcer;

import com.tesla.data.enforcer.BaseCommand;
import com.tesla.data.enforcer.BaseCommand.CommandConfigConverter;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ValidateCommandTest {

  @ClassRule
  public static final TemporaryFolder TEMP_FOLDER = new TemporaryFolder();
  private static final String KAFKA = String.join("\n",
      "---",
      "kafka:",
      "  bootstrap.servers: localhost:9092");
  private static final String UNMANAGED = String.join("\n",
      "unmanaged:",
      "  topicPrefixes: [\"ext-\"]");
  private final CommandConfigConverter converter = new CommandConfigConverter();

  private int validate(String... lines) throws IOException {
    String conf = String.join("\n", lines);
    return new ValidateCommand(converter.convert(new ByteArrayInputStream(conf.getBytes(StandardCharsets.UTF_8))))
        .run();
  }

  @Test
  public void testManagedTopicsAccepted() throws IOException {
    Assert.assertEquals(BaseCommand.SUCCESS, validate(KAFKA, UNMANAGED,
        "topics:",
        "  - name: orders",
        "    partitions: 1",
        "    replicationFactor: 1"));
  }

  @Test
  public void testUnmanagedTopicRejected() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA, UNMANAGED,
        "topics:",
        "  - name: orders",
        "    partitions: 1",
        "    replicationFactor: 1",
        "  - name: ext-orders",
        "    partitions: 1",
        "    replicationFactor: 1"));
  }

  @Test
  public void testInternalTopicRejectedWithoutUnmanagedSection() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA,
        "topics:",
        "  - name: _internal",
        "    partitions: 1",
        "    replicationFactor: 1"));
  }

  @Test
  public void testConsolidatedConfigAcceptedWithoutCluster() throws IOException {
    // partitions are only set per cluster, so these entries can't be parsed into topics without a cluster
    Assert.assertEquals(BaseCommand.SUCCESS, validate(KAFKA, UNMANAGED,
        "topics:",
        "  - name: orders",
        "    replicationFactor: 1",
        "    clusters:",
        "      foo:",
        "        partitions: 1"));
  }

  @Test
  public void testUnmanagedTopicRejectedInConsolidatedConfig() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA, UNMANAGED,
        "topics:",
        "  - name: orders",
        "    partitions: 1",
        "    replicationFactor: 1",
        "    clusters:",
        "      foo: {}",
        "  - name: ext-orders",
        "    partitions: 1",
        "    replicationFactor: 1",
        "    clusters:",
        "      bar: {}"));
  }

  @Test
  public void testUnmanagedNameOverrideRejected() throws IOException {
    // names should not be overridden per cluster, but an override would still be applied
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA, UNMANAGED,
        "topics:",
        "  - name: orders",
        "    partitions: 1",
        "    replicationFactor: 1",
        "    clusters:",
        "      foo:",
        "        name: ext-orders"));
  }

  @Test
  public void testEmptyTopicsRejected() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA, "topics:"));
  }

  @Test
  public void testEmptyTopicsFileRejected() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA, "topicsFile:"));
  }

  @Test
  public void testEmptyTopicEntryRejected() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA,
        "topics:",
        "  - name: orders",
        "    partitions: 1",
        "    replicationFactor: 1",
        "  -"));
  }

  @Test
  public void testUnmanagedTopicRejectedInTopicsFile() throws IOException {
    File topicsFile = TEMP_FOLDER.newFile();
    Files.write(topicsFile.toPath(), String.join("\n",
        "---",
        "- name: ext-orders",
        "  partitions: 1",
        "  replicationFactor: 1").getBytes(StandardCharsets.UTF_8));
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA, UNMANAGED, "topicsFile: " + topicsFile.getPath()));
  }

  @Test
  public void testMissingTopicsFileRejected() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA, "topicsFile: /does/not/exist.yaml"));
  }

  @Test
  public void testTopicWithoutNameRejected() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA,
        "topics:",
        "  - partitions: 1",
        "    replicationFactor: 1"));
  }

  @Test
  public void testConfigWithoutTopicsAccepted() throws IOException {
    // ex: a config only used to dump existing topics
    Assert.assertEquals(BaseCommand.SUCCESS, validate(KAFKA, UNMANAGED));
  }

  @Test
  public void testInvalidUnmanagedSectionRejected() throws IOException {
    Assert.assertEquals(BaseCommand.FAILURE, validate(KAFKA,
        "unmanaged:",
        "  topicprefixes: [\"ext-\"]",
        "topics:",
        "  - name: orders",
        "    partitions: 1",
        "    replicationFactor: 1"));
  }
}
