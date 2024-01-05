/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

terraform {
  required_providers {
    harness = {
      source = "harness/harness"
    }
  }
}
  provider "harness" {
    endpoint         = "https://harness.io"
    account_id       = "<account_id>"
    platform_api_key = "<pat.account_id...>"
  }
