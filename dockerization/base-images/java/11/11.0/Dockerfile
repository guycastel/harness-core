# Copyright 2022 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

# UBI BASE IMAGE + JDK

# Usage: Application runtime base image for java based application, CIE agent base image
# Test image locally by running this command:
#
# $ docker build \
#     -f Dockerfile-ubi-java-base" \
#     -t <tag> \
#     .

FROM us.gcr.io/platform-205701/harness/ubi8:8.8
COPY --from=adoptopenjdk/openjdk11:jdk-11.0.19_7-ubi-minimal /opt/java/openjdk/ /opt/harness/OpenJDK11/jdk-11.0.19+7

ENV JAVA_HOME=/opt/harness/OpenJDK11/jdk-11.0.19+7
ENV PATH=$JAVA_HOME/bin:$PATH
