#!/bin/bash

declare -a SUBJECTS=()

SUBJECTS=("${SUBJECTS[@]}" "commons-bcel")
SUBJECTS=("${SUBJECTS[@]}" "commons-codec")
SUBJECTS=("${SUBJECTS[@]}" "commons-compress")
SUBJECTS=("${SUBJECTS[@]}" "commons-csv")
SUBJECTS=("${SUBJECTS[@]}" "commons-digester")
SUBJECTS=("${SUBJECTS[@]}" "commons-fileupload")
SUBJECTS=("${SUBJECTS[@]}" "commons-math")

BASE_DIR=$(cd $(dirname $0)/.. && pwd)

# set up subjects
SUBJECTS_DIR=${BASE_DIR}/subjects
if [ ! -d ${SUBJECTS_DIR} ]; then
  mkdir -p ${SUBJECTS_DIR}
  cd ${SUBJECTS_DIR}
  git clone https://github.com/apache/commons-bcel
  cd commons-bcel
  git checkout 54a95d8
  cd ${SUBJECTS_DIR}
  git clone https://github.com/apache/commons-codec
  cd commons-codec
  git checkout c18b192
  cd ${SUBJECTS_DIR}
  git clone https://github.com/apache/commons-compress
  cd commons-compress
  git checkout 083dd8c
  cd ${SUBJECTS_DIR}
  git clone https://github.com/apache/commons-csv
  cd commons-csv
  git checkout edb87f3
  cd ${SUBJECTS_DIR}
  git clone https://github.com/apache/commons-digester
  cd commons-digester
  git checkout c1d0e56
  cd ${SUBJECTS_DIR}
  git clone https://github.com/apache/commons-fileupload
  cd commons-fileupload
  git checkout 422caf4
  cd ${SUBJECTS_DIR}
  git clone https://github.com/apache/commons-math
  cd commons-math
  git checkout 3488609
fi

# detect & naive-detect
for SUBJECT in "${SUBJECTS[@]}"
do
  sh/run ${SUBJECT} detect
  sh/run ${SUBJECT} naive-detect
done
