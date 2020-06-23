This is the script which will perform reindexing on Elasticsearch Index...

## How to run 
  - give permission to the script chmod +x reindex.sh
  - bash reindex.sh {{es_ip}} {{old_index}} {{new_index}} {{alias_name}} {{index_req_filePath}} {{mappings_req_filePath}}



## CLI ARGS DESCRIPTION
 - es_ip: Ip of ElasticSearch and port is assumed to be 9200
 - old_index: Source Index from which reindexing to be done
 - new_index: Destination Index, to which reindexing to be done
 - alias_name: Name of Alias to which new_index to be mapped and old index need to be removed.
 - index_req_filepath: .json file path which will have a request body for creating new_index.
 - mapping_req_filepath: .json file path which will have a request body for creating mappings of new_index.  
 
 
 **NOTE**: old_index will be deleted by the script. <br>
 
 
 ## Following Steps will be done by Script:

  1: Validating Input params
      a) File paths
      b) ElasticSearch health   <br>
  2: *Creating Backup file. <br>
  3: mapping alias_name with `old_index`. <br>
  4: creating indices and mapping of `new_index`. <br>
  5: Reindexing from `old_index` index to `new_index` index. <br>
  6: deleting alias with  `old_index` and mapping alias with `new_index`. <br>
  7: deleting  `old_index`. <br>


*BackUp file may not contain all the ES records(due to size limit in ES). <br>
SOURCE : https://engineering.carsguide.com.au/elasticsearch-zero-downtime-reindexing-e3a53000f0ac
