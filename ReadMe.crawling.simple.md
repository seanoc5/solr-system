# Notes on crawling process/logic -- Simple version

Basic `slocate` functionality: locate folders and files

This approach keeps track of `skip` patterns for both folders and files.

## First pass
The goal is to find all files of interest, and skip those that are not interesting.
This is lightweight, and focused on a first pass of filtering out 'skip' files, and saving **to solr** the name, path, size, and timestamp of the file/folder.


## Second Pass
Apply more broadly scoped pattern matching to determin if a given file should be:
- simple location/size/time only
- content extracted and indexed
- more fully analyzed
- 

