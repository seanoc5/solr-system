# Notes on crawling process/logic -- Simple version

Basic `slocate` functionality: locate folders and files

This approach keeps track of `skip` patterns for both folders and files.

~~Get all relevannt (non-ignored) folders (type FSFolder).
?? Call fsFolder.analyze~~

- Crawler: folderPatterns, filePatterns, Config, DifferenceChecker?
  - SolrSystemClient
  - one crawler per **locationName**
  - iterate crawlNames/startFolders
    - File.traverse() -- traverse directory tree (bredth first) -- save each folder as we go
    ```
    visit folders - build FSFolder
    check for existing solr doc
    if present && in sync: skip update
    else save new or updated 
    ```
      - predir: if folder.ignore: skip tree (but save ignored folder as "ignored" for review/auditing)
      - if shouldUpdate: get all non-ignored files
      - if or extended folder/file pattern map
        - add tags
      - if analyser: 
        - add analysis
      - save all gathered objects for current folder

## First pass 
Update: switch to processing (slocate) each folder on it's own. Allow more comprehensive analysis in subsequent passes.


slocate-type processing: get basic folders and files, no content, no analysis yet (that is second pass)
- ~~Get all (non-ignored) folders.~~  porocess them one-by-one
- each folder:
  - get file objects 
    - with ignore flag if should 'ignore'
    - even ignored files will save basic info (no content, no analysis)
    - consider **skip** label for things we can confidently skip even basic tracking

The goal is to find all files of interest, and skip those that are not interesting.
This is lightweight, and focused on a first pass of filtering out 'skip' files, and saving **to solr** the name, path, size, and timestamp of the file/folder.


## ArchiveUtils


## Second Pass
Apply more broadly scoped pattern matching to determin if a given file should be:
- simple location/size/time only
- content extracted and indexed
- more fully analyzed



## Classes and Inheritence
### BaseCrawler
* provide struct



## Data Model
- SavableObject
  - FSObject (tbd)
  - FSFolder
    - parent (fsfolder)  ?? needed ??
    - children(fsfile)
  - FSFile
    - parent (fsfolder)
    - ?Archive folders/files?
      - parent (fsfile)


- Crawler
  - persistence client (Solr, CSV file,...)
  - diff checker
  - analyzer
