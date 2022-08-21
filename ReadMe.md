# Solr System
## Personal search and content discovery


## Process & Overview
- Locate - track folders and files
- Full Crawl folders  (~once per week)
  - track folder metadata
    - name
    - size
    - file count
    - folder count
    - modified date
  - track files in each folder
    - quick-assign file types by patterns
    - name
    - size
    - file count
    - folder count
    - modified date
  - folder-assign based on file patterns, and: 
    - parent folder
    - children folders

## Notes:

Starting solr:
- /opt/apache/solr-8.11.1/bin/solr start -c -m 4g 
  - built-in zk, default solr home
- old:
  - #/opt/apache/solr/bin/solr start -c -m 4g -z localhost:2181
  - #/opt/apache/solr/bin/solr start -c -m 1g  -s /opt/apache/solr/data/


## Analysis

### Folders

#### Tracking data
- Folder metadata
  - recency
    - popularity
  - type
    - assignedType  (see contentTypes below)
    - taggedType
    - mlClusterType
    - mlClassificationType
    - systemFile (true/false now, but possibly type of system folder? -- if true, deprecate indexing and searching this and child files)
    - parentTypes 
    - childTypes
  - permissions (outlier?)
  - count of files
  - count of subdirs
  - depth
  - totalFileCount
- list of files
  - list of file dates - median date
  - list of file sizes - media size
  - permissions? - odd permissions
- Files by type
  - officeFontent files
  - support files
  - hidden files
  - temp files
  - links - (symbolic, hard)

- support: 
  - parentLink
  - childLink
- contentTypes
   - officeContent
   - techDev
   - data
   - media
   - system
   - archive/backup
   - web (?)




## various notes:

https://en.wikipedia.org/wiki/List_of_archive_formats

There is a nice Solr interface available at:
https://github.com/mrseanr/solr-admin-pro
It is more solr admin than user query/browsing, but still quite nice.
