#!/bin/bash

REPO_ROOT_DIR="$(git rev-parse --show-toplevel)"

cp "${REPO_ROOT_DIR}/git-hooks/pre-commit" "${REPO_ROOT_DIR}/.git/hooks/"
