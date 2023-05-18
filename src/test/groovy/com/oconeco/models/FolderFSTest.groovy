package com.oconeco.models

import com.oconeco.analysis.FolderAnalyzer
import com.oconeco.helpers.Constants
import com.oconeco.persistence.SolrSaver
import org.apache.solr.common.SolrInputDocument
import spock.lang.Specification

import java.util.regex.Pattern

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/5/22, Friday
 * @description:
 */

class FolderFSTest extends Specification {
    Pattern ignoreFolders = Constants.DEFAULT_FOLDERNAME_PATTERNS.ignore
    Pattern ignoreFiles = Constants.DEFAULT_FILENAME_PATTERNS.ignore
    public static final int FOLDER_SIZE = 1305281

    static public final Map testFileNames = [
            ignore      : ['foo.lock', '_ignoreme.txt', 'foo.tmp', 'bar.class', 'robots.txt'],//~/([.~]*lock.*|_.*|.*\.te?mp$|.*\.class$|robots.txt)/,
            office      : ['foo.doc', 'bar.docx', 'llibre.ods', 'free.odt', 'slides.ppt', 'sheet.xls', 'sheets.xlsx'],        // ~'.*(accdb|docx?|ods|odp|odt|pptx?|rtf|txt|vsdx?|xslx?)',
            system      : ['soffice.bin', 'package.deb', 'gcc_s1_stub', 'my.lib', 'fubar.rpm'],   //~/.*(bin|deb|gcc|lib|pkg|rpm)/,
            archive     : ['foo.gz', 'win.rar', 'tarball.tar.gz', 'some.zip'],   //~/.*(arc|gz|rar|zip|tar.gz|zip)/,
            web         : ['small.htm', 'medium.html'],   //~/.*(html?)/,
            instructions: ['readme.first', 'instructions.adoc', 'important.md'],   // `'(?i).*(adoc|readme.*|md)',
            techDev     : ['prog.c', 'style.css', 'new.go', 'script.groovy', 'build.gradle', 'gradlew', 'gradlew.bar', 'custom.jar', 'hello.java', 'ticktack.js', 'tick.javascript', 'script.php', 'bash.sh', 'managed-schema'],   // `/.*(c|css|go|groovy|gradle|jar|java|javascript|js|php|schema|sh)/,
            config      : ['foo.config', 'bar.cfg', 'foo.config.bar', 'secret.pem', 'foo.properties', 'ace.xml', 'config.yaml'],   // `/.*(\.cfg|config.*|pem|properties|xml|yaml)/,
            data        : ['one.csv', 'apple.json', 'one.jsonl', 'two.jsonld', 'files.lst', 'list.tab'],   // `/.*(csv|jsonl?d?|lst|tab)/,
            media       : ['movie.avi', 'pic.jpeg', 'pic2.jpg', 'music.ogg', 'song.mp3', 'movie.mpeg', 'movie2.mpg', 'sound.wav'],   // `/.*(avi|jpe?g|ogg|mp3|mpe?g|wav)/,
    ]

    static public final Map testFolderNames = [
            ignore       : ['__snapshots__', '.cache', 'cache', '.git', '.github', 'ignore.me', 'packages', 'pkg', 'plugin', 'skins', 'svn', 'target', 'vscode'],   // `/.*(__snapshots__|.?cache.?|git|github|ignore.*|packages?|pkgs?|plugins?|skins?|svn|target|vscode)/,
            backups      : ['backup', 'bkup', 'bkups', 'old', 'timeshift'],   // `/.*(backups?|bkups?|old|timeshift)/,
            configuration: ['configs'],   // `/.*(configs)/,
            documents    : ['documents'],   // `/.*([Dd]ocuments|[Dd]esktop)/,
            downloads    : ['Downloads', 'downloads'],   // `/.*([Dd]ownloads)/,
            pictures     : ['pictures'],   // `/.*([Pp]ictures)/,
            programming  : ['java', 'jupyter', 'src', 'work'],   // `/.*(java|jupyter|src|work)/,
            system       : ['class', 'sbt', 'sbt-d-1', 'runtime'],   // `/.*(class|sbt|sbt-\d.*|runtime)/,
    ]


    def "Hardcoded check of filename patterns"() {
        given:
        String key = 'ignore'
        List<String> basicFilenames = testFileNames[key]
        List<String> mixedFilenames = testFileNames[key] + testFileNames['office']
        Pattern regex = Constants.DEFAULT_FILENAME_PATTERNS[key]

        when:
        def foundBasic = basicFilenames.findAll { it =~ regex }
        def foundMixed = mixedFilenames.findAll { it =~ regex }

        then:
        foundBasic.size() == basicFilenames.size()
        foundMixed.size() == basicFilenames.size()
    }

    def "test files regex with datatable"() {
        when:
        def found = names.findAll { it =~ regex }

        then:
        found == results

        where:
        key            | names                                        | regex                                         || results
        'ignore'       | testFileNames[key]                           | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]
        'office'       | testFileNames[key]                           | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]
        'system'       | testFileNames[key]                           | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]
        'archive'      | testFileNames[key]                           | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]
        'web'          | testFileNames[key]                           | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]
        'instructions' | testFileNames[key]                           | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]
        'techDev'      | testFileNames[key]                           | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]
        'config'       | testFileNames[key]                           | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]
        'data'         | testFileNames[key]                           | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]
        'media'        | testFileNames[key]                           | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]


        'ignore'       | testFileNames[key] + testFileNames['office'] | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]
        'office'       | testFileNames[key] + testFileNames['ignore'] | Constants.DEFAULT_FILENAME_PATTERNS[key] || testFileNames[key]

    }

    def "test folders regex with datatable"() {
        when:
        def found = names.findAll { it =~ regex }

        then:
        found == results

        where:
        key             | names              | regex                                         || results
        'ignore'        | testFolderNames[key] | Constants.DEFAULT_FOLDERNAME_PATTERNS[key] || testFolderNames[key]
        'backups'       | testFolderNames[key] | Constants.DEFAULT_FOLDERNAME_PATTERNS[key] || testFolderNames[key]
        'configuration' | testFolderNames[key] | Constants.DEFAULT_FOLDERNAME_PATTERNS[key] || testFolderNames[key]
        'documents'     | testFolderNames[key] | Constants.DEFAULT_FOLDERNAME_PATTERNS[key] || testFolderNames[key]
        'downloads'     | testFolderNames[key] | Constants.DEFAULT_FOLDERNAME_PATTERNS[key] || testFolderNames[key]
        'pictures'      | testFolderNames[key] | Constants.DEFAULT_FOLDERNAME_PATTERNS[key] || testFolderNames[key]
        'programming'   | testFolderNames[key] | Constants.DEFAULT_FOLDERNAME_PATTERNS[key] || testFolderNames[key]
        'system'        | testFolderNames[key] | Constants.DEFAULT_FOLDERNAME_PATTERNS[key] || testFolderNames[key]
//        'ignore'       | testFolderNames[key] + testFolderNames['office'] | FolderAnalyzer.DEFAULT_FILENAME_PATTERNS[key] || testFolderNames[key]
//        'office'       | testFolderNames[key] + testFolderNames['ignore'] | FolderAnalyzer.DEFAULT_FILENAME_PATTERNS[key] || testFolderNames[key]
    }

    def "basic folder load"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FolderFS folder = new FolderFS(src, 1, ignoreFiles, ignoreFolders)

        then:
        folder.size == FOLDER_SIZE
        folder.countTotal == 21

        folder.filesFS.size() == 20
        folder.countFiles == 20

        folder.subDirectories.size() == 1
        folder.countSubdirs == 1
    }


    def "basic folder load with ignore file and folder patterns"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FolderFS folder = new FolderFS(src, 1, ignoreFiles, ignoreFolders)

        then:
        folder.size == FOLDER_SIZE       //642754
        folder.countTotal == 21

        folder.filesFS.size() == 20
        folder.countFiles == 20

        folder.subDirectories.size() == 1
        folder.countSubdirs == 1

        folder.ignoredDirectories.size() == 1
        folder.ignoredFolders.size() == 0
    }


    def "recursive folder load with ignore file and folder patterns"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        boolean recurse = true
        FolderFS folderFS = new FolderFS(src, 1, ignoreFiles, ignoreFolders, recurse)

        then:
        folderFS.size == FOLDER_SIZE
        folderFS.countTotal == 21

        folderFS.filesFS.size() == 20
        folderFS.countFiles == 20

        folderFS.subDirectories.size() == 1
        folderFS.countSubdirs == 1

        folderFS.ignoredDirectories.size() == 1
        folderFS.ignoredFolders.size() == 0
    }


    def "recursive folder load get all subfolders"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        boolean recurse = true
        FolderFS folderFS = new FolderFS(src, 1, ignoreFiles, ignoreFolders, recurse)
        List<FolderFS> allSubFolders = folderFS.getAllSubFolders()

        then:
        allSubFolders instanceof List<FolderFS>
        allSubFolders.size() == 3

    }


    def "basic folder load with ignore check toSolrInputDocument"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FolderFS folder = new FolderFS(src, 1, ignoreFiles, ignoreFolders)
        SolrInputDocument sid = folder.toSolrInputDocument()
        List<SolrInputDocument> sidList = folder.toSolrInputDocumentList()
        SolrInputDocument sidFolder = sidList[0]
        String folderName = sidFolder.getField('name_s').value
        SolrInputDocument sidSolrxml = sidList.find {
            it.getField('name_s').value == 'solrconfig.xml'
        }
        String xmlName = sidSolrxml.getField('name_s').value

        then:
        sid instanceof SolrInputDocument
        sid.size() == 9

        sidList instanceof List<SolrInputDocument>
        sidList.size() == 21
        sidFolder.getFieldValues('type_s')[0] == 'Folder'
        // test 'uniqify' value
        sidFolder.getField(SolrSaver.FLD_NAME_SIZE_S).value == 'content:1305281'


        sidSolrxml.getFieldValues('type_s')[0] == 'File'
        sidSolrxml.getField(SolrSaver.FLD_NAME_SIZE_S).value == 'solrconfig.xml:62263'

        xmlName == 'solrconfig.xml'
    }


/*
    def "folder load with toSolrInputDocument"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())

        when:
        FolderFS folder = new FolderFS(src, 1, ignoreFiles, ignoreFolders)
        List<SolrInputDocument> solrDocs = folder.toSolrInputDocumentList()

        then:
        folder.filesFS.size()== 20    //14
        solrDocs.size() == 21
        folder.subDirectories.size()==1
        folder.ignoredFolders.size()==0
        folder.ignoredDirectories.size()==1
        folder.ignoredFiles.size()==1
    }
*/


    def "load and analyze folder"() {
        given:
        File src = new File(getClass().getResource('/content').toURI())
        ConfigObject config = new ConfigSlurper().parse(getClass().getResource('/configLocate.groovy'))
        FolderAnalyzer analyzer = new FolderAnalyzer(config)
        FolderFS startFolder = new FolderFS(src, 1, ignoreFiles, ignoreFolders)

        when:
        def foo = startFolder.analyze(analyzer)
        Map assignmentGroupedFiles = startFolder.filesFS.groupBy { it.assignedTypes[0] }
        List keys = assignmentGroupedFiles.keySet().toList()

        then:
        keys.size == 8
        keys == ['techDev', 'archive', 'office', 'config', 'instructions', 'data', 'control', 'media']
    }


    def "check overlapping start folders"() {
        given:
        def dataSources = [
                localFolders: [
                        MyDocuments: '/home/sean/Documents',
                        MyDesktop  : '/home/sean/Desktop',
                        MyPictures : '/home/sean/Pictures',
                        SeanHome   : '/home/sean',
                ]
        ]

        when:
        // todo -- complete me...
        int foo = 2

        then:
        foo == 2

    }

    def "crawl user docs local folders"() {
        given:
        long start = System.currentTimeMillis()
        long maxMem = Runtime.runtime.maxMemory()
        long totalMem = Runtime.runtime.totalMemory()
        println("Starting memory: $maxMem -- $totalMem")

        File rootFolder = new File('/home/sean/')
        List<File> folders = []
        List<FolderFS> folderFSList = []

        when:
        rootFolder.eachDirRecurse {
            folders << it
            FolderFS folderFS = new FolderFS(it)

            folderFSList << folderFS
//            println(it)
        }

        long maxMem2 = Runtime.runtime.maxMemory()
        long totalMem2 = Runtime.runtime.totalMemory()
        println("after memory: $maxMem2 -- $totalMem2")
        println("difference memory: ${maxMem2 - maxMem} -- ${totalMem2 - totalMem}")

        long end = System.currentTimeMillis()
        long elapsed = end - start
        println "Elapsed time: ${elapsed}ms (${elapsed / 1000} sec)  -- ${folders.size()}"

        then:
        folders.size() > 10
    }
}
