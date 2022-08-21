# Solr System
## Personal search and content discovery
A project to create a personal information discovery and search tool, similar to Windows search, or Mac spotlight.

## Install Solr
Why Solr? 
Because I work with it daily, it is a great web interface to lucene, which is a great indexing and search engine.

The version of solr should be recent. I have developed with Solr 8.11.1: https://solr.apache.org/guide/8_11/
Quickstart: https://solr.apache.org/guide/solr/latest/getting-started/solr-tutorial.html
Install: https://solr.apache.org/guide/8_11/installing-solr.html

### CORS (cross origin resource sharing)
https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS

To allow a front end (like: https://github.com/mrseanr/solr-admin-pro)
the simplest approach is to allow CORS. This will allow a web app to "proxy" to a solr instance. A better (more production-ready) approach is to have a middleware layer that does the solr query & operations. 
More to come...

## Run solr-system
This software is still beta status. \
Gradle tasks are in the works. 

### Java JDK 
Install Java JDK
For linux consider sdkman.io

For windows...  
There are many options, a [google search](https://www.google.com/search?q=install+openjdk+windows) should show several approaches

MS build of openjdk is a reasonable option: https://docs.microsoft.com/en-us/java/openjdk/install

## Solr
https://solr.apache.org/guide/8_11/installing-solr.html

**Note**: avoid paths with spaces for Java applications (such as solr, solr-system)  
Consider `C:\work\solr\solr.8.11.2` rather than `C:\Program Files...`

If Windows defender prompts for access, allow it.
![Windows defender prompt](img.png)

Confirm solr is running: \
http://localhost:8983/solr/#/

Edit `solr.in.sh` or for windows `solr.in.cmd` uncomment (or add) memory setting: \
`set SOLR_JAVA_MEM=-Xms512m -Xmx4g`\

or pass memory flag `-m` on the command line:
${solr_home}/bin/solr start -c -m 4g

start solr: 
Linux/mac: `/opt/solr/solr.8.11.2/bin/solr start -c -m 4g

Windows: `c:\work\solr\solr.8.11.2\bin start -c -m 4g``

**Note:** -c tells solr to start in `cloud` mode, which is the default for any recent solr distribution.

 
### Solr-system

### Create solr collection
Via the Solr Admin ui:
http://localhost:8983/solr/#/~collections
![Create Solr Collection](other\create-solr-collection.png)

Note: solr-system uses dynamic schema fields (field type defined by suffix like `_t` for text fields, `_s` for String fields. 
String fields are for sorting and faceting but not searching (requires exact matches). Text fields perform analysis (tokenization, lower casing, stemming,...).

#### Get Solr system code
Clone the solr-system repository:
`git clone https://github.com/seanoc5/solr-system.git`

Review (or edit/create) the configuration file:
https://github.com/seanoc5/solr-system/blob/master/src/main/resources/configs/configLocate.groovy  
This config file uses regular expressions to assign tags to various filename patterns.

For a nice regex test site with a sample pattern tagging office-type documents (by matching filename extensions),   
see: https://regex101.com/r/QxfdmC/1

Run the `locate` script with TODO



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
