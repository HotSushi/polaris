/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.polaris.core.entity;

import java.util.List;
import org.apache.polaris.core.PolarisDefaultDiagServiceImpl;
import org.apache.polaris.core.persistence.ResolvedPolarisEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PolarisBaseEntity} focusing on correct value preservation through the Builder,
 * especially during JSON deserialization scenarios.
 */
class PolarisBaseEntityTest {

  /**
   * Regression test for: Builder constructor must preserve grantRecordsVersion=0 rather than
   * silently converting it to 1. A stored entity with grantRecordsVersion=0 (e.g. a newly-created
   * federated principal role that has never had grants modified) was being deserialized with
   * grantRecordsVersion=1, causing a grants_version_going_backward crash in ResolvedPolarisEntity
   * when the top-level grantsVersion in the same response remained 0.
   */
  @Test
  void builderPreservesGrantRecordsVersionZero() {
    PolarisBaseEntity entity =
        new PolarisBaseEntity.Builder()
            .catalogId(0L)
            .id(1161160115267L)
            .typeCode(PolarisEntityType.PRINCIPAL_ROLE.getCode())
            .subTypeCode(PolarisEntitySubType.NULL_SUBTYPE.getCode())
            .parentId(0L)
            .name("ZEROCOPY$89E38813C1DF4162A5AC34ABD6CFF736")
            .entityVersion(1)
            .grantRecordsVersion(0)
            .build();

    Assertions.assertThat(entity.getGrantRecordsVersion())
        .as("Builder must preserve grantRecordsVersion=0 without converting to 1")
        .isEqualTo(0);
  }

  /**
   * Verifies that ResolvedPolarisEntity can be successfully constructed when entity
   * grantRecordsVersion and the provided grantsVersion are both 0. This was previously crashing
   * with grants_version_going_backward because the Builder silently converted the entity's version
   * from 0 to 1 while the top-level grantsVersion remained 0.
   */
  @Test
  void resolvedPolarisEntitySucceedsWhenGrantsVersionIsZero() {
    PolarisBaseEntity entity =
        new PolarisBaseEntity.Builder()
            .catalogId(0L)
            .id(1161160115267L)
            .typeCode(PolarisEntityType.PRINCIPAL_ROLE.getCode())
            .subTypeCode(PolarisEntitySubType.NULL_SUBTYPE.getCode())
            .parentId(0L)
            .name("ZEROCOPY$89E38813C1DF4162A5AC34ABD6CFF736")
            .entityVersion(1)
            .grantRecordsVersion(0)
            .build();

    var diagnostics = new PolarisDefaultDiagServiceImpl();

    // Should not throw grants_version_going_backward when entity.grantRecordsVersion == grantsVersion == 0
    Assertions.assertThatNoException()
        .isThrownBy(
            () -> new ResolvedPolarisEntity(diagnostics, entity, List.of(), 0));
  }

  /** Verifies that Builder preserves non-zero grantRecordsVersion values unchanged. */
  @Test
  void builderPreservesNonZeroGrantRecordsVersion() {
    PolarisBaseEntity entity =
        new PolarisBaseEntity.Builder()
            .catalogId(0L)
            .id(42L)
            .typeCode(PolarisEntityType.PRINCIPAL_ROLE.getCode())
            .subTypeCode(PolarisEntitySubType.NULL_SUBTYPE.getCode())
            .parentId(0L)
            .name("MY_ROLE")
            .entityVersion(1)
            .grantRecordsVersion(5)
            .build();

    Assertions.assertThat(entity.getGrantRecordsVersion()).isEqualTo(5);
  }

  /**
   * Verifies that withGrantRecordsVersion() round-trips correctly through zero, since it also uses
   * the Builder internally.
   */
  @Test
  void withGrantRecordsVersionPreservesZero() {
    PolarisBaseEntity original =
        new PolarisBaseEntity.Builder()
            .catalogId(0L)
            .id(42L)
            .typeCode(PolarisEntityType.PRINCIPAL_ROLE.getCode())
            .subTypeCode(PolarisEntitySubType.NULL_SUBTYPE.getCode())
            .parentId(0L)
            .name("MY_ROLE")
            .entityVersion(1)
            .grantRecordsVersion(3)
            .build();

    PolarisBaseEntity patched = original.withGrantRecordsVersion(0);

    Assertions.assertThat(patched.getGrantRecordsVersion())
        .as("withGrantRecordsVersion(0) must set the version to 0")
        .isEqualTo(0);
  }
}
