#!/bin/bash
set -eu -o pipefail

perform_reindexing(){

echo ">>STEP1: mapping $alias_name with $old_index index"


alias_old_index_status=$( curl -s --write-out %{http_code} --silent --output  --location --request POST 'http://'$es_ip':9200/_aliases?pretty' \
--header 'Content-Type: application/json' \
--data-raw '{
    "actions" : [
        { "add" : { "index" : "'$old_index'", "alias" : "'$alias_name'" } }
    ]
}')


if [[ $alias_old_index_status == 200 ]] ; then
  echo "'$old_index' index successfully map to alias $alias_name with status code 200"
else
  echo "STEP1 FAILED:$old_index index is unable to map with alias $alias_name with status code $alias_old_index_status hence exiting program....."
  exit 1
fi


echo ">>STEP2: creating '$new_index' index"

index_status_code=$( curl -s --write-out %{http_code} --silent --output --location --request PUT 'http://'$es_ip':9200/'$new_index'' \
--header 'Content-Type: application/json' \
-d @$index_req_filepath)

if [[ $index_status_code == 200 ]] ; then
  echo "'$new_index' index successfully created with status code 200"
else
  echo "STEP2 FAILED:'$new_index' index is unable to create with status code $index_status_code hence exiting program....."
  exit 1
fi

echo ">>STEP3: creating mapping of '$new_index' index\n"

mapping_status_code=$( curl -s --write-out %{http_code} --silent --output --location --request PUT 'http://'$es_ip':9200/'$new_index'/_doc/_mapping' \
--header 'Content-Type: application/json' \
-d @$mapping_req_filepath)

if [[ $mapping_status_code == 200 ]] ; then
  echo "'$new_index' index mappings successfully created with status code 200"
else
  echo "STEP3 FAILED:'$new_index' index is unable to create mappings with status code $mapping_status_code hence exiting program......"
  exit 1
fi

echo ">>STEP4: copying $data_count certificates from '$old_index' index to '$new_index' index\n"


status_code=$( curl -s --write-out %{http_code} --silent --output --location --request POST 'http://'$es_ip':9200/_reindex' \
--header 'Content-Type: application/json' \
--data-raw '{
  "source": {
    "index": "'$old_index'"
  },
  "dest": {
    "index": "'$new_index'"
  }
}') 

if [[ $status_code == 200 ]] ; then
  echo "$data_count certificates copied from '$old_index' to '$new_index' index with status code 200"
else
  echo "STEP4 FAILED:to copy certificates with status code $status_code, please manually delete the temp index. exiting the program......"
  exit 1
fi


echo ">>STEP5: deleting $alias_name with $old_index index and mapping alias with $new_index"

alias_new_index_status=$( curl -s --write-out %{http_code} --silent --output  --location --request POST 'http://'$es_ip':9200/_aliases' \
--header 'Content-Type: application/json' \
--data-raw '{
    "actions" : [
        { "add" : { "index" :  "'$new_index'", "alias" : "'$alias_name'" } },
        { "remove" : { "index" : "'$old_index'", "alias" : "'$alias_name'" }}
    ]
}')

if [[ $alias_new_index_status == 200 ]] ; then
  echo "$alias_name successfully mapped to $new_index"
else
  echo "$alias_name failed to  map with $new_indexES with status code $alias_new_index_status, exiting....."
  exit 1
fi

echo ">>STEP6: deleting previous $old_index index\n"

index_delete_status_code=$( curl -s  --write-out %{http_code} --silent --output --location --request DELETE 'http://'$es_ip':9200/'$old_index'' \
--header 'Content-Type: application/json' )


if [[ $index_delete_status_code == 200 ]] ; then
  echo "$old_index index deleted with status code 200"
else
  echo "STEP6 FAILED:to delete $old_index index with status code $index_delete_status_code exiting the program......"
  exit 1
fi

}


echo "Starting REINDEXING PROGRAM IN ELASTICSEARCH......."

es_ip=$1
old_index=$2
new_index=$3
alias_name=$4
index_req_filepath=$5
mapping_req_filepath=$6

if [ "$#" -ne 6 ]; then
    echo "PARAM INITIALIZATION FAILED, No command line arguments provided, Please provide esIp, old index, new index and alias name, index_req_filepath, mapping_req_filepath"
    exit 1
fi



echo "checking provide file existence"

[ -f "$index_req_filepath" ] || { echo "$index_req_filepath NOT FOUND" ; exit 1 ;}
[ -f "$mapping_req_filepath" ] || { echo "$mapping_req_filepath NOT FOUND" ; exit 1 ;}





echo "ES_IP GOT: $es_ip\n"
echo "OLD INDEX(NEED TO BE DELETED) GOT: $old_index\n"
echo "NEW INDEX GOT: $new_index\n"
echo "Alias GOT: $alias_name\n"
echo "index json request path got $index_req_filepath"
echo "mapping request json path $mapping_req_filepath"
echo "=======Params Initialized==========\n"
echo "NOTE: IF ANY STEP FAILED PLEASE MANUALLY DELETE THE NEW INDEX from ElasticSearch i.e $new_index."



eshealth_status_code=$( curl -s  --write-out %{http_code} --silent --output --location --request GET 'http://'$es_ip':9200' \
--header 'Content-Type: application/json' )

if [[ $eshealth_status_code == 200 ]] ; then
  echo "ELASTICSEARCH IS ALIVE"
else
  echo "ES is not alive please make sure es is up and running, exiting....."
  exit 1
fi

DATE=`date "+%Y%m%d-%H%M%S"`
backup_file_name=certregBackup$DATE.txt
echo "PERFORMING '$old_index' BACKUP, can be found in file $backup_file_name"

curl -s --location --request GET 'http://'$es_ip':9200/'$old_index'/_search?size=10000' --header 'Content-Type: application/json' --data-raw '{
   "query":{
   
   "match_all":{}
   
   }
}

' | jq '.' > $backup_file_name


data_count=$( curl -s --location --request GET 'http://'$es_ip':9200/'$old_index'/_count' --header 'Content-Type: application/json' --header 'Accept: text' | jq ."count" )
echo "continue reindexing of $data_count records"
perform_reindexing




