package com.oconeco.models

import com.oconeco.helpers.ArchiveUtils
import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSystemClient
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.jar.JarArchiveEntry
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.solr.common.SolrInputDocument

import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

/**
 * structure for handling information about a folder from the FS (filesystem),
 * handles both basic information from the filesystem, and analysis results from some process
 */
class FSFile extends FSObject {
    Logger log = LogManager.getLogger(this.class.name)
    public static final String TYPE = 'File'
    String extension

    Integer ARCHIVE_STATUS_SIZE = 1000
    // todo move this code out of FSFile into analyzer??
    public Pattern IGNORE_ARCHIVE_PATTERN = ~/.*(bundle|concepts|deb|dmg|exe|iso|jar|o[dt]t|pages|pkg|rpm|war)|(doc|xls|ppt)x/


    /**
     * Basic constructor
     * @param f source file to model/track
     * @param parent this may be unused... todo track value of parent object in constructor (filevisitor does not lend itself to eash tracking of parent/child)
     * @param locationName - source machine, profile, email account...
     * @param crawlName - subpartition of source location into logical naming (of content)
     */
    FSFile(File f, SavableObject parentObj, String locationName, String crawlName) {
        super(f, parentObj, locationName, crawlName)

        if (parent==null && f.parentFile){
            parent = new FSFolder(f.parentFile,null, locationName, crawlName)
        }
        type = TYPE
        groupObject = false
        size = f.size()
        dedup = buildDedupString()

        // todo -- more here -- also check FSFolder object, and analyze() method,
        extension = FilenameUtils.getExtension(f.name)
        log.debug "File(${this.toString()})"
        archive = isArchive()       // todo move this code out of FSFile into analyzer??
        if (archive) {
            log.debug "\t\tfound archive file: $this"
        }
    }


    FSFile(File f, SavableObject parent, String locationName, String crawlName, Pattern ignoreName, Pattern ignorePath=null) {
        this(f, parent, locationName, crawlName)
        if (name ==~ ignoreName) {
            log.debug "\t\tIgnore this file -- NAME:(${this.name}) matches ignore name pattern:($ignoreName)"
            ignore = true
        }
        if (ignorePath && path ==~ ignorePath) {
            log.debug "\t\tIgnore this file -- PATH:(${this.path}) matches ignore path pattern:($ignorePath)"
            ignore = true
        }
    }

    /**
     * create an index-ready solr input doc from this FSFile object
     * @return SolrInputDocument
     */
    @Override
    SolrInputDocument toPersistenceDocument() {
        SolrInputDocument sid = super.toPersistenceDocument()
        if (crawlName) {
            sid.setField(SolrSystemClient.FLD_CRAWL_NAME, crawlName)
        }

        if (owner) {
            sid.setField(SolrSystemClient.FLD_OWNER, owner)
        }

        if (extension) {
            sid.addField(SolrSystemClient.FLD_EXTENSION_SS, extension)
        }

        if (!sid.getFieldValue(SolrSystemClient.FLD_DEDUP)) {
            if (dedup) {
                sid.addField(SolrSystemClient.FLD_DEDUP, dedup)
            } else {
                log.warn "No dedup value available!!!"
                sid.addField(SolrSystemClient.FLD_DEDUP, "${this.name}:${this.size}")
            }
        } else {
            log.debug("have a dedup value: $dedup -- $this")
        }
        return sid
    }


    /**
     * basic test to check if this file(object) is an archive which we may want to track the contents of
     * @return boolean: yes if archive
     */
    Boolean isArchive() {
        Boolean archive = null

        File f = this.thing
        if (f.exists() && f.canRead()) {
            if (f.size() > 5) {         // todo - revisit what is the minimum size to test against, picking 5 as a starting point...
                if (extension ==~ IGNORE_ARCHIVE_PATTERN) {
                    log.debug "\t\t~~~~This($this) is a 'fake' archive, probably a single  composit (zipped) office doc"
                    archive = false
                } else {
                    try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
                        int fileSignature = raf.readInt()
                        archive = fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708 || fileSignature == 529205248 || fileSignature == 529205268

                        if (labels?.contains(Constants.LBL_ARCHIVE)) {      // todo -- wtf am I doing here....?
                            if (!archive) {
                                log.info "Should be archive: $f -> ($fileSignature) -- $archive"
                            } else {
                                log.debug "is archive: $f"
                            }
                        }
                    } catch (IOException e) {
                        log.warn "File ($f) io exception checking if we have an archive: $e"
                    }
                }
            } else {
                log.debug "\t\t ------ File (${f.name}) is too small to check (size:${this.size}), assuming not an actual archive: ${f.absolutePath}"
            }
        } else {
            log.warn "File ($f) does not exist, or cannot be read: ${f.absolutePath}"
        }
        return archive

    }

    /**
     * helper function to build list of entries from archive
     *
     * @return list of Archive*.SavableObject s
     * todo - revisit archive folder, and it's value/role (omitted currently?)
     */
    List<SavableObject> gatherArchiveEntries() {
        List<SavableObject> archiveEntries = null
        if (this.isArchive()) {
            ArchiveInputStream archiveInputStream
            try {
                archiveInputStream = ArchiveUtils.getArchiveInputStream(this.thing)
                if (archiveInputStream) {
                    if (archiveInputStream instanceof ZipArchiveInputStream || archiveInputStream instanceof JarArchiveEntry) {
                        archiveEntries = gatherZippyArchiveEntries(archiveInputStream)
                    } else {
                        archiveEntries = gatherArchiveEntries(archiveInputStream)
                    }
                } else {
                    log.warn "no valid archive input stream available, skipping gatherArchiveEntries for this (fake archive/office doc??): $this"
                }
            } catch (IOException ioe) {
                log.warn "IOException: $ioe"
            } finally {
                if (archiveInputStream) {
                    archiveInputStream.close()
                    log.debug "close Archive input steam: $this"
                } else {
                    log.info "do not have a valid Archive input steam to close! this:$this"
                }
            }
        } else {
            log.debug "Not an archive, skipping..."
        }
        return archiveEntries
    }


    /**
     * helper util to get archive entries for any of the 'handled' archive types (e.g. zip, tar,...)
     * @param ais
     * @return List of various Archive entries
     */
    List<SavableObject> gatherArchiveEntries(ArchiveInputStream ais) {
        log.info "\t\t....gatherArchiveEntries(ais) for FSFile: ${this.toString()}"
        if (ais) {
            int i = 0
            ArchiveEntry entry = null
            while ((entry = getSpecificArchEntry(ais)) != null) {
                if (entry.getSize() >= 0) {
                    log.debug "good, entry has a size: $entry"
                } else if (entry.name.endsWith('/')) {
                    log.debug "processing arch folder: $entry, size should be 0 (this is fine)"
                } else {
                    log.info "still no entry size?? $entry"
                }
                i++
                SavableObject archObj
                if (entry.isDirectory()) {
                    archObj = new ArchFolder(entry, this, locationName, crawlName)
                    childGroups << archObj
                    log.debug "\t\t+.+.+. arch folder: ${archObj.toString()}"
                } else {
                    archObj = new ArchFile(entry, this, locationName, crawlName)
                    childItems << archObj
                    log.debug "\t\t+.+.+. arch File: ${archObj.toString()})"
                }
                if (i % ARCHIVE_STATUS_SIZE == 0) {
                    log.info "\t\t$i) (still) gathering archive entries (e.g. ${archObj}) for file ($this) "
                }
            }
        } else {
            log.warn "Invalid archive input stream: $ais, how did this happen?? -- $this"
        }
        List allItems = childGroups + childItems
        return allItems
    }


    /**
     * hackish approach to deal with zip and jar arhcives have trouble with file size
     * need to get 'next' entry before current entry has a valid size
     * @param ais
     * @return list of FSObjects with minimal metadata
     */
    List<SavableObject> gatherZippyArchiveEntries(ArchiveInputStream ais) {
        log.info "\t\t....gatherZippyArchiveEntries(ais) for FSFile: ${this.toString()}"
        if (ais) {
            int i = 0
            ArchiveEntry entry = getSpecificArchEntry(ais)
            ArchiveEntry nextEntry = null
            do {
                nextEntry = getSpecificArchEntry(ais)
                long size = entry.size          // debugging, checking if we have an actual size from the entry
                if (size >= 0) {
                    log.debug "good, entry has a size: $entry"
                } else if (entry.name.endsWith('/')) {
                    log.debug "processing arch folder: $entry, size should be 0 (this is fine)"
                } else {
                    log.info "still no entry size?? $entry"
                }
                i++
                SavableObject archObj
                if (entry.isDirectory()) {
                    archObj = new ArchFolder(entry, this, locationName, crawlName)
                    childGroups << archObj
                    log.debug "\t\t+.+.+. arch folder: ${archObj.toString()}"
                } else {
                    archObj = new ArchFile(entry, this, locationName, crawlName)
                    childItems << archObj
                    log.debug "\t\t+.+.+. arch File: ${archObj.toString()})"
                }
                if (i % ARCHIVE_STATUS_SIZE == 0) {
                    log.info "\t\t$i) (still) gathering archive entries (e.g. ${archObj}) for file ($this) "
                }
                entry = nextEntry
            } while (nextEntry != null)

        } else {
            log.warn "Invalid archive input stream: $ais, how did this happen?? -- $this"
        }
        List allItems = childGroups + childItems
        return allItems
    }


    ArchiveEntry getSpecificArchEntry(ArchiveInputStream ais) {
        ArchiveEntry entry = null
        if (ais) {
            if (ais instanceof JarArchiveInputStream) {
                entry = ais.getNextJarEntry()
            } else if (ais instanceof ZipArchiveInputStream) {
                entry = ais.getNextZipEntry()
            } else {
                entry = ais.getNextEntry()
            }
            if (entry) {
                long size = entry.size
                log.debug "Size: $size"
            } else {
                log.debug "no entry: $entry -- in ais: $ais -- hit end of archive??"
            }

        } else {
            log.warn "no ArchiveInputStream: $ais"
        }
        return entry
    }
}
