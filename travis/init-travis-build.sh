#!/bin/bash
#
# Licensed to the University Corporation for Advanced Internet Development,
# Inc. (UCAID) under one or more contributor license agreements.  See the
# NOTICE file distributed with this work for additional information regarding
# copyright ownership. The UCAID licenses this file to You under the Apache
# License, Version 2.0 (the "License"); you may not use this file except in
# compliance with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


echo -e "Building branch: ${TRAVIS_BRANCH}"
echo -e "Build directory: ${TRAVIS_BUILD_DIR}"
echo -e "Build id: ${TRAVIS_BUILD_ID}"
echo -e "Builder number: ${TRAVIS_BUILD_NUMBER}"
echo -e "Job id: ${TRAVIS_JOB_ID}"
echo -e "Job number: ${TRAVIS_JOB_NUMBER}"
echo -e "Repo slug: ${TRAVIS_REPO_SLUG}"
echo -e "OS name: ${TRAVIS_OS_NAME}"

if [ "$TRAVIS_SECURE_ENV_VARS" == "false" ]
then
  echo -e "Secure environment variables are NOT available...\n"
else
  echo -e "Secure environment variables are available...\n"
fi
