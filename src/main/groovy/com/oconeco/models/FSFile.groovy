package com.oconeco.models

import com.oconeco.helpers.ArchiveUtils
import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSystemClient
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
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
    String mimeType
    String owner

    String osName
    Integer ARCHIVE_STATUS_SIZE = 1000
    public Pattern FAKE_ARCHIVE_PATTERN = ~/.*(doc|xls|ppt)x/


    FSFile(File f, SavableObject parent, String locationName, String crawlName) {
        super(f, parent, locationName, crawlName)

        type = TYPE
        groupObject = false
        size = f.size()
        dedup = buildDedupString()

        // todo -- more here -- also check FSFolder object, and analyze() method,
        extension = FilenameUtils.getExtension(f.name)
        log.debug "File(${this.toString()})"
        archive = isArchive()
        if (archive) {
            log.debug "\t\tfound archive file: $this"
        }
    }


    /**
     * create an index-ready solr input doc from this FSFile object
     * @return SolrInputDocument
     */
    @Override
    SolrInputDocument toSolrInputDocument() {
        SolrInputDocument sid = super.toSolrInputDocument()
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

    Boolean isArchive() {
        Boolean archive = null

        File f = this.thing
        if (f.exists() && f.canRead()) {
            if (f.size() > 0) {
                if (extension ==~ FAKE_ARCHIVE_PATTERN) {
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
                log.info "\t\t ------ File (${f.name}) has no size, not an archive: ${f.absolutePath}"
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
            ArchiveInputStream aiszip
            try {
                aiszip = ArchiveUtils.getArchiveInputStream(this.thing)
                if (aiszip) {
                    archiveEntries = gatherArchiveEntries(aiszip)
                } else {
                    log.warn "no valid archive input stream available, skipping gatherArchiveEntries for this (fake archive/office doc??): $this"
                }
            } catch (IOException ioe) {
                log.warn "IOException: $ioe"
            } finally {
                if (aiszip) {
                    aiszip.close()
                    log.debug "close Archive input steam: $this"
                } else {
                    log.debug "do not have a valid Archive input steam to close! this:$this"
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
        log.info "gatherArchiveEntries(ais) for FSFile: ${this.toString()}"
        if (ais) {
            if (!children) {
                log.debug "Create children list... ($this)"
                children = []
            } else {
                log.warn "\t\t$this) children prop already exists/defined?? $children"
            }
            int i = 0
            ArchiveEntry entry = null
            while ((entry = getSpecificArchEntry(ais)) != null) {
//            while ((entry = (ais instanceof JarArchiveInputStream ? ais.getNextJarEntry() : ais instanceof ZipArchiveInputStream ? ais.getNextZipEntry() : ais.getNextEntry())) != null) {

                i++
                SavableObject archObj
                if (entry.isDirectory()) {
                    archObj = new ArchFolder(entry, this, locationName, crawlName)
                    children << archObj
                    log.debug "\t\t+.+.+. ${archObj.toString()}"
                } else {
                    archObj = new ArchFile(entry, this, locationName, crawlName)
                    children << archObj
                    log.debug "\t\t+.+.+. File: ${archObj.toString()})"
                }
                if (i % ARCHIVE_STATUS_SIZE == 0) {
                    log.info "\t\t$i) (still) gathering archive entries (e.g. ${archObj}) for file ($this) "
                }
            }
        } else {
            log.warn "Invalid archive input stream: $ais, how did this happen?? -- $this"
        }
        return children
    }

    ArchiveEntry getSpecificArchEntry(ArchiveInputStream ais) {
        ArchiveEntry entry = null
        if (ais) {
            if (ais instanceof ZipArchiveInputStream) {
                entry = ais.getNextZipEntry()
                if (entry) {
                    long size = entry.size
                    log.debug "Size: $size"
                } else {
                    log.warn "no entry: $entry -- in ais: $ais"
                }
            } else if (ais instanceof JarArchiveInputStream) {
                entry = ais.getNextJarEntry()
                long size = entry.size
                log.debug "Size: $size"
            } else {
                entry = ais.getNextEntry()
                def size = entry.size
                log.debug "Size: $size"
            }
        } else {
            log.warn "no ArchiveInputStream: $ais"
        }
        return entry
    }
}
