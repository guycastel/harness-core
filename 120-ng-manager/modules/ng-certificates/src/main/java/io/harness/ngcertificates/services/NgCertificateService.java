/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngcertificates.services;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeInfo;
import io.harness.ngcertificates.entities.NgCertificate;
import io.harness.spec.server.ng.v1.model.CertificateDTO;

import java.io.InputStream;
import javax.annotation.Nullable;

@OwnedBy(PL)
public interface NgCertificateService {
  NgCertificate create(ScopeInfo scopeInfo, CertificateDTO certificateDTO, @Nullable InputStream uploadedInputStream);
}
