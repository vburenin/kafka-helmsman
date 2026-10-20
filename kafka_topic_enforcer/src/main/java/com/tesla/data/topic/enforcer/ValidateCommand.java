/*
 * Copyright (c) 2026. Tesla Motors, Inc. All rights reserved.
 */

package com.tesla.data.topic.enforcer;

import static tesla.shade.com.google.common.base.Preconditions.checkArgument;

import com.tesla.data.enforcer.BaseCommand;
import com.tesla.data.enforcer.ClusterEntities;

import com.beust.jcommander.Parameters;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Parameters(commandDescription = "Validate config, configured topics must not match unmanaged prefixes")
public class ValidateCommand extends BaseCommand<ConfiguredTopic> {

  private static final String TOPICS_KEY = "topics";
  private static final String TOPICS_FILE_KEY = "topicsFile";
  private static final String NAME = "name";

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
    return validateEntities(TOPICS_KEY, TOPICS_FILE_KEY, topics -> TopicEnforcer.checkNoUnmanagedTopics(
        topics.stream().flatMap(ValidateCommand::names).collect(Collectors.toList()), unmanagedPrefixes()));
  }

  // names should not be overridden per cluster, but an override would still be applied, so it is checked as well
  private static Stream<String> names(Map<String, Object> topic) {
    checkArgument(topic.get(NAME) != null, "Topic without a name: %s", topic);
    return ClusterEntities.propertyValues(topic, NAME).stream().map(String::valueOf);
  }
}
