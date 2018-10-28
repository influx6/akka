/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package jdocs.typed.tutorial_4;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

import java.util.Optional;

import static jdocs.typed.tutorial_4.DeviceManagerProtocol.*;
import static jdocs.typed.tutorial_4.DeviceProtocol.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DeviceTest extends JUnitSuite {

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource();

  //#device-registration-tests
  @Test
  public void testReplyToRegistrationRequests() {
    TestProbe<DeviceRegistered> probe = testKit.createTestProbe(DeviceRegistered.class);
    ActorRef<DeviceManagerMessage> managerActor = testKit.spawn(DeviceManager.behavior());

    managerActor.tell(new RequestTrackDevice("group", "device", probe.getRef()));
    DeviceRegistered registered = probe.expectMessageClass(DeviceRegistered.class);

    // registering same again should be idempotent
    managerActor.tell(new RequestTrackDevice("group", "device", probe.getRef()));
    DeviceRegistered registered2 = probe.expectMessageClass(DeviceRegistered.class);
    assertEquals(registered.device, registered2.device);

    // another deviceId
    managerActor.tell(new RequestTrackDevice("group", "device3", probe.getRef()));
    DeviceRegistered registered3 = probe.expectMessageClass(DeviceRegistered.class);
    assertNotEquals(registered.device, registered3.device);
  }

  @Test
  public void testIgnoreWrongRegistrationRequests() {

  }

  //#device-registration-tests

  //#device-read-test
  @Test
  public void testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
    TestProbe<RespondTemperature> probe = testKit.createTestProbe(RespondTemperature.class);
    ActorRef<DeviceMessage> deviceActor = testKit.spawn(Device.behavior("group", "device"));
    deviceActor.tell(new ReadTemperature(42L, probe.getRef()));
    RespondTemperature response = probe.expectMessageClass(RespondTemperature.class);
    assertEquals(42L, response.requestId);
    assertEquals(Optional.empty(), response.value);
  }
  //#device-read-test

  //#device-write-read-test
  @Test
  public void testReplyWithLatestTemperatureReading() {
    TestProbe<TemperatureRecorded> recordProbe = testKit.createTestProbe(TemperatureRecorded.class);
    TestProbe<RespondTemperature> readProbe = testKit.createTestProbe(RespondTemperature.class);
    ActorRef<DeviceMessage> deviceActor = testKit.spawn(Device.behavior("group", "device"));

    deviceActor.tell(new RecordTemperature(1L, 24.0, recordProbe.getRef()));
    assertEquals(1L, recordProbe.expectMessageClass(TemperatureRecorded.class).requestId);

    deviceActor.tell(new ReadTemperature(2L, readProbe.getRef()));
    RespondTemperature response1 = readProbe.expectMessageClass(RespondTemperature.class);
    assertEquals(2L, response1.requestId);
    assertEquals(Optional.of(24.0), response1.value);

    deviceActor.tell(new RecordTemperature(3L, 55.0, recordProbe.getRef()));
    assertEquals(3L, recordProbe.expectMessageClass(TemperatureRecorded.class).requestId);

    deviceActor.tell(new ReadTemperature(4L, readProbe.getRef()));
    RespondTemperature response2 = readProbe.expectMessageClass(RespondTemperature.class);
    assertEquals(4L, response2.requestId);
    assertEquals(Optional.of(55.0), response2.value);
  }
  //#device-write-read-test


}