/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

resource "harness_platform_monitored_service" "service_ref_environment_ref" {
  depends_on = [
    harness_platform_connector_prometheus.connectorRef
  ]

  org_id     = "terraform_org"
  project_id = "terraform_project"
  identifier = "service_ref_environment_ref"
  request {
    name            = "service_ref_environment_ref"
    type            = "Application"
    description     = "new_description_new"
    service_ref     = "service_ref"
    environment_ref = "environment_ref"
    tags            = ["foo:bar", "bar:foo"]
    health_sources {
      name       = "prometheus"
      identifier = "prometheus"
      type       = "Prometheus"
      spec       = jsonencode({
        connectorRef      = "connectorRef"
        feature           = "feature"
        metricDefinitions = [
          {
            identifier = "prometheus_metric1"
            metricName = "Prometheus Metric1"
            analysis   = {
            }
            riskProfile = {
            }
            sli = {
              enabled = true
            },
            query         = "sum(abc{identifier=\"slo-ratiobased-unsuccessfulCalls-datapattern\"})",
            groupName     = "t2",
            isManualQuery = true
          }
        ]
      })
    }
  }

}
