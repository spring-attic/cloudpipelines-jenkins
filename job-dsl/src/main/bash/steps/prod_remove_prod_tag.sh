#!/bin/bash

export ENVIRONMENT=prod

# shellcheck source=/dev/null
source "${WORKSPACE}"/.git/tools/src/main/bash/pipeline.sh

removeProdTag
