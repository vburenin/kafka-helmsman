/*
 * Copyright (c) 2019. Tesla Motors, Inc. All rights reserved.
 */

package com.tesla.data.enforcer;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class EnforcerTest {

  private static final List<Dummy> dummies =
      Arrays.asList(new Dummy("a", emptyMap()), new Dummy("b", emptyMap()));
  private DummyEnforcer enforcer;

  @Test(expected = IllegalArgumentException.class)
  public void testDuplicateConfigured() {
    new DummyEnforcer(Collections.nCopies(2, dummies.get(0)), Collections::emptyList);
  }

  @Test
  public void testAbsent() {
    enforcer = new DummyEnforcer(dummies, () -> singletonList(dummies.get(0)));
    Assert.assertEquals(singletonList(dummies.get(1)), enforcer.absent());
  }

  @Test
  public void testAbsentNoneExisting() {
    enforcer = new DummyEnforcer(dummies, Collections::emptyList);
    Assert.assertEquals(dummies, enforcer.absent());
  }

  @Test
  public void testAbsentNoneConfigured() {
    enforcer = new DummyEnforcer(emptyList(), () -> dummies);
    Assert.assertTrue(enforcer.absent().isEmpty());
  }

  @Test
  public void testNoUnexpected() {
    enforcer = new DummyEnforcer(dummies, () -> dummies);
    Assert.assertTrue(enforcer.unexpected().isEmpty());
  }

  @Test
  public void testUnexpected() {
    Dummy unexpected = new Dummy("x", emptyMap());
    enforcer = new DummyEnforcer(dummies, () -> singletonList(unexpected));
    Assert.assertEquals(1, enforcer.unexpected().size());
    Assert.assertEquals(unexpected, enforcer.unexpected().get(0));
  }

  @Test(expected = IllegalStateException.class)
  public void testSafeMode() {
    enforcer = new DummyEnforcer(dummies, Collections::emptyList);
    enforcer.deleteUnexpected();
  }

  @Test
  public void testSafeModeEnforceAll() {
    Dummy unexpected = new Dummy("x", emptyMap());

    // safemode on
    enforcer = spy(new DummyEnforcer(dummies, () -> singletonList(unexpected), true));
    enforcer.enforceAll();
    verify(enforcer, times(0)).delete(anyList());

    // safemode off
    enforcer = spy(new DummyEnforcer(dummies, () -> singletonList(unexpected), false));
    enforcer.enforceAll();
    verify(enforcer, times(1)).delete(singletonList(unexpected));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyConfigSafeModeOff() {
    enforcer = new DummyEnforcer(emptyList(), Collections::emptyList, false);
  }

  @Test
  public void testUnexpectedSkipsUnmanaged() {
    Dummy managed = new Dummy("x", emptyMap());
    Dummy unmanaged = new Dummy("ext-x", emptyMap());
    enforcer = new ExtSkippingEnforcer(dummies, () -> Arrays.asList(managed, unmanaged));
    Assert.assertEquals(singletonList(managed), enforcer.unexpected());
  }

  @Test
  public void testDeleteUnexpectedLeavesUnmanaged() {
    Dummy stale = new Dummy("x", emptyMap());
    enforcer = spy(new ExtSkippingEnforcer(dummies,
        () -> Arrays.asList(stale, new Dummy("ext-x", emptyMap()), new Dummy("ext-y", emptyMap()))));
    // the deletion threshold is computed after skipping unmanaged entities, 1 of 2 configured is permissible
    enforcer.deleteUnexpected();
    verify(enforcer).delete(singletonList(stale));
  }

  @Test(expected = IllegalStateException.class)
  public void testTooManyBeingDeleted() {
    enforcer =
        new DummyEnforcer(
            dummies,
            () -> Arrays.asList(new Dummy("x", emptyMap()), new Dummy("y", emptyMap())),
            false);
    // should not be allowed as we are deleting lot of dummies!
    enforcer.deleteUnexpected();
  }

  // treats dummies prefixed with 'ext-' as owned by an external system
  private static class ExtSkippingEnforcer extends DummyEnforcer {
    ExtSkippingEnforcer(List<Dummy> configured, Supplier<Collection<Dummy>> existing) {
      super(configured, existing, false);
    }

    @Override
    protected boolean isUnmanaged(Dummy dummy) {
      return dummy.name.startsWith("ext-");
    }
  }
}
