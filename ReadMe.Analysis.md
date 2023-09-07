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
- \_text_ field
  - all text _(with copy-field *)_,
- body 
  - unprocessed main content (ie html body. without tags stripped, or possibly tika export of xml/html formatted content)
- content
  - filtered, de-boilerplated, more analyzed (the good stuff)
- term analysis
  - shingles
    - 2-5 shingles (?)
  - outliers (unusual)
  - term halos
  - context tracking (tbd) -- add something like wordnet word-senses

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


## Supporting collections
- analysis
  - (copy) interesting documents to this collection for more advanced analysis
- vocabulary 
  - (with specific content, or 'general' context)
  - terms list, with word-senses where possible
    - filed: already been filed in system, no further processing/tagging needed (for basic setup)
    - known: seen, and recognized as needing 'filing' but pending actual processing.
    - skip: seen and determined to be inconsequential in this context
    - unknown
- concepts
  - taxonomy of concepts (and contexts?)
    - opinionated selection of concepts and structure
    - can be customized or adjusted as needed (based on stakeholder agreement)
    - 
- logs
