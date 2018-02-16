#!/bin/bash

declare -a SUBJECTS=()

SUBJECTS=("${SUBJECTS[@]}" "commons-bcel")
SUBJECTS=("${SUBJECTS[@]}" "commons-codec")
SUBJECTS=("${SUBJECTS[@]}" "commons-compress")
SUBJECTS=("${SUBJECTS[@]}" "commons-csv")
SUBJECTS=("${SUBJECTS[@]}" "commons-digester")
SUBJECTS=("${SUBJECTS[@]}" "commons-fileupload")
SUBJECTS=("${SUBJECTS[@]}" "commons-math")

BASE_DIR=$(cd $(dirname $0) && pwd)
SCC_DIR=${BASE_DIR}/SourcererCC

# set up subjects
SUBJECTS_DIR=${SCC_DIR}/subjects
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


# set up for output
cd ${SCC_DIR}
OUTPUT_DIR=${SCC_DIR}/output
if [ ! -d ${OUTPUT_DIR} ]; then
  mkdir -p ${OUTPUT_DIR}
  for SUBJECT in "${SUBJECTS[@]}"
  do
    mkdir -p ${OUTPUT_DIR}/${SUBJECT}
    touch ${OUTPUT_DIR}/${SUBJECT}/tokens.file
    touch ${OUTPUT_DIR}/${SUBJECT}/headers.file
  done
fi

# tokenize
cd ${SCC_DIR}/parser/java

for SUBJECT in "${SUBJECTS[@]}"
do
  echo ${SUBJECTS_DIR}/${SUBJECT}
  java -jar InputBuilderClassic.jar ${SUBJECTS_DIR}/${SUBJECT}/src/test/java/ \
                      ${OUTPUT_DIR}/${SUBJECT}/tokens.file ${OUTPUT_DIR}/${SUBJECT}/headers.file \
                      functions java 0 0 0 0 false false false 1
done
