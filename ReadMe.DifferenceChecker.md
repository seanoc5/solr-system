# Difference Checker
- Compare Source objects against pre-existing/saved solr documents
  - check for key values to be equal, 
    - ie: name, lastModifiedDate, size, and/or possibly md5/hash
  - start with folders, assume
    - name, lastmodified equal means no files have changed
    - alternatively: name and size being equal (more expensive?? but more thorough?)
  - if folders differ: 
    - walk files, find differences
    - only update different files
  - consider:
    - tracking changes in separate collection

Diff checker responsible for performing difference checking.
Diff status is the structure to 'record' those differences, along with difference messages

# DifferenceStatus
Holds source and saved things,
Holds flags about differences
Holds any relevant text messages about differences (explanations, etc)
