package com.oconeco.analysis

import com.oconeco.difference.BaseDifferenceChecker
import com.oconeco.helpers.Constants
import com.oconeco.models.FSFile
import com.oconeco.models.FSFolder
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import spock.lang.Specification

class FileSystemAnalyzerTest extends Specification {
    String locationName = 'spock'
    String crawlName = 'test'
    File contentDir = new File(getClass().getResource('/content').toURI())
    FSFolder startFSFolder = new FSFolder(contentDir, null, locationName, crawlName)
    BaseDifferenceChecker differenceChecker = new BaseDifferenceChecker()

    def 'tika content parsing'() {
        given:
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(1000 * 1000 * 50);
        FileSystemAnalyzer analyzer = new FileSystemAnalyzer(Constants.DEFAULT_FOLDERNAME_LOCATE, Constants.DEFAULT_FILENAME_PARSE, null, null, parser, handler)
        File f1 = new File(contentDir, 'doc.go')
        File f2 = new File(contentDir, 'FutureTechPredictions.doc')
        FSFile goFile = new FSFile(f1, startFSFolder, locationName, crawlName)
        FSFile docFile = new FSFile(f2, startFSFolder, locationName, crawlName)

        when:
        def goResults = analyzer.analyze(goFile)
        def docResults = analyzer.analyze(docFile)

        then:
        goFile != null
        goFile.extension == 'go'
        goFile.mimeType.containsIgnoreCase('text/plain')
        goFile.labels.size() == 1
        goFile.labels[0] == 'techDev'
        goFile.metadata.size() == 4

        docFile != null

    }


    def 'tika content parsing via analyzer'() {
        given:
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        FileSystemAnalyzer analyzer = new FileSystemAnalyzer(Constants.DEFAULT_FOLDERNAME_LOCATE, Constants.DEFAULT_FILENAME_LOCATE, null, null, parser, handler)
        File f = new File(contentDir, 'FutureTechPredictions.doc')
        FSFile fsFile = new FSFile(f, startFSFolder, locationName, crawlName)

        when:
        def results = analyzer.analyze(fsFile)

        then:
        results != null

    }

}
