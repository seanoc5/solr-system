



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



