# Notes on crawling process/logic -- complex version

# NOTE -- likely outdated/incorrect 
consider removing, or heavily editing 

## File and Folder wrapper objects 

**Note**: this assumes everthing is Local Filesystem

### FolderFS
This is a wrapper that adds filtering and labeling information.
Will eventually provide ML categorization and possibly clustering wrapping/conveniece.

FolderFS has attempt (trial code) for recursive crawling in constructor. That seems muddied, so currently looking at dropping back to having the crawling only in a crawler object.


### FileFS
Similar to FolderFS

### Config elements: namePatterns
Map of tags and regex patterns for:
* folders
* files

### Process & Actual Crawling

#### Overview and approach
* Crawl all the folders first
  * include simple/quick file details: name, size, modified time
  * tag folder based on "quick analysis"
  - call solr for `past state` info to inform `incremental updates` 
  - each folder:  _(WIP-- more to come here...)_
    - record folder name, "size" and actual size, time(s)
      - add folder tags based on folder name (size/date/current-ness?) 
    - record filenames, sizes, and dates (maybe permissions?)
      - add folder tags based on filename and/or extentions
      - add folder tags based on file current-ness and sizes
    - analyze 'basic' information
    - save and update/change metadata to sidecar collection for later analysis
    - **Archive files** treated like folders 
    
* Recrawl with purpose
  * two phases (first is high-value targets, second is everything else)
    - call solr for `past state` info to inform `incremental updates` 
    * Prioritize folders based on tags/meta
      * start with the most likely valuable folders (based on quick analysis/initial folder crawl)
    * add `prioritized` content extraction
    - save and update/change metadata to sidecar collection for later analysis


## Browser History
`more to come`
sqlite?

## Email
`more to come`
* gmail to start with
* imap (javamail) alternative
* 
## Calendar
`more to come`
gcal

## Messaging platforms
`more to come`
* slack
* ...

## GDrive and others
`more to come`

