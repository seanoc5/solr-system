# Notes on crawling process/logic

## File and Folder wrapper objects

### FolderFS
This is a wrapper that adds filtering and labeling information.
Will eventually provide ML categorization and possibly clustering wrapping/conveniece.

FolderFS has attempt (trial code) for recursive crawling in constructor. That seems muddied, so currently looking at dropping back to having the crawling only in a crawler object.


### FileFS
Similar to FolderFS

## Analysis 
### FolderAnalyzer
Helps with analyzing folders.
Config file should have a map of patterns to tag folders based on file name (and path???)

## Crawling

- crawl all folders
  - each folder:
    - analyze name/contents

### Local
