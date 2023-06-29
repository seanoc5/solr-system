## Process & Overview
- BASIC : Locate - track folders and files
  - quick and simple: like locatedb in linux 
- DEFAULT : Crawl folders  (daily or more -- )
  - track folder metadata
    - name
    - size
    - file count
    - folder count
    - modified date
  - track files in each folder
    - quick-assign file types by patterns _(labels?)_
    - name
    - size
    - file count
    - folder count
    - modified date
  - folder-assign based on file patterns, and: (move this to 'extra' analysis phase)
    - parent folder
    - children folders


## Thoughs and Questions
- Should FolderAnalyzer "know about" children (i.e. FileAnalyzer)?
- or should the crawler call the individual analysis for each object/type? (better flexibility? let the crawler "know" the intent/goal)

## Analysis
- Simple: 
  - name based
- Moderate:
  - name and neighbors
- Advanced
  - content, ML, LLM, NLP

### Content qualitative analysis
- text field
  - all text, irrespective
- content
  - filtered, de-boilerplated, more analyzed (the good stuff)
- term analysis
  - shingles
    - 2, 3, and 4 shingling?
  - outliers (unusual)
  - term halos

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



