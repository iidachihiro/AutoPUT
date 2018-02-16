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
OUTPUT_DIR=${SCC_DIR}/output

SCC_PROPERTIES=${SCC_DIR}/sourcerer-cc.properties
SCC_PROP_ORIGIN=$(cat ${SCC_PROPERTIES})

cd ${SCC_DIR}
for SUBJECT in "${SUBJECTS[@]}"
do
  if [ ! -e ${OUTPUT_DIR}/${SUBJECT}/dataset/tokens.file ]; then
    mkdir -p ${OUTPUT_DIR}/${SUBJECT}/dataset
    cp ${OUTPUT_DIR}/${SUBJECT}/tokens.file ${OUTPUT_DIR}/${SUBJECT}/dataset/tokens.file
  fi
  echo "QUERY_DIR_PATH=${OUTPUT_DIR}/${SUBJECT}/dataset" > ${SCC_PROPERTIES}
  echo "DATASET_DIR_PATH=${OUTPUT_DIR}/${SUBJECT}/dataset" >> ${SCC_PROPERTIES}
  cat ${BASE_DIR}/sourcerer-cc.properties.sample  >> ${SCC_PROPERTIES}
  for THRESHOLD in 4 5 6
  do
    start_time=`gdate +%s%N`
    java -jar dist/indexbased.SearchManager.jar index ${THRESHOLD} >> scc.log
    java -jar dist/indexbased.SearchManager.jar search ${THRESHOLD} >> scc.log
    end_time=`gdate +%s%N`
    elapsed_time=`expr \( ${end_time} - ${start_time} \) / 1000000`
    echo "SCC took ${elapsed_time} msec. on ${SUBJECT} given threshold ${THRESHOLD}"
  done
done
echo ${SCC_PROP_ORIGIN} > ${SCC_PROPERTIES}
