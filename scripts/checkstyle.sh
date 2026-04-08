#!/bin/bash
./mvnw checkstyle:check 2>&1 | grep -E "\[WARN\]|\[ERROR\].*\.java|There is"
