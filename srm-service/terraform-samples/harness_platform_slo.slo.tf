/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

resource "harness_platform_slo" "slo" {
  depends_on = [
    harness_platform_monitored_service.service_ref_environment_ref,
  ]
  org_id     = harness_platform_organization.terraform_org.id
  project_id = harness_platform_project.terraform_project.id
  identifier = "slo"
  request {
    name              = "slo"
    description       = "description"
    tags              = ["foo:bar", "bar:foo"]
    user_journey_refs = ["one", "two"]
    slo_target {
      type                  = "Calender"
      slo_target_percentage = 98
      spec                  = jsonencode({
        type = "Monthly"
        spec = {
          dayOfMonth = 6
        }
      })
    }
    type = "Simple"
    spec = jsonencode({
      monitoredServiceRef       = harness_platform_monitored_service.service_ref_environment_ref.id
      serviceLevelIndicatorType = "Availability"
      serviceLevelIndicators    = [
        {
          name       = "name"
          identifier = "slo"
          type       = "Window"
          spec       = {
            type = "Threshold"
            spec = {
              metric1        = "prometheus_metric"
              thresholdValue = 30
              thresholdType  = ">"
            }
            sliMissingDataType = "Good"
          }
        }
      ]
    })
  }
}
