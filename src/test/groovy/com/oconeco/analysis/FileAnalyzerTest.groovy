//package com.oconeco.analysis
//
//import com.oconeco.helpers.Constants
//import com.oconeco.models.FSFile
//import com.oconeco.models.FSFolder
//import org.apache.logging.log4j.core.Logger
//import spock.lang.Specification
//
//import java.util.regex.Pattern
//
///**
// * @author :    sean
// * @mailto :    seanoc5@gmail.com
// * @created :   10/9/22, Sunday
// * @description:
// */
//
//class FileAnalyzerTest extends Specification {
//    Logger log = LogManager.getLogger(this.class.name);
//    String locationName = 'spock'
//    String crawlName = 'test'
//
//    URL purl = FileAnalyzerTest.getClassLoader().getResource('./content/params.json')
////    File paramsJson = new File(FileAnalyzerTest.getClassLoader().getResource('./content/params.json').toURI())
//    File currencyXml = new File(FileAnalyzerTest.getClassLoader().getResource('./content/currency.xml').toURI())
//    File datasourcesZip = new File(FileAnalyzerTest.getClassLoader().getResource('./content/datasources.zip').toURI())
//    Pattern ignoreFiles = Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE]
//    Pattern ignoreFolders = Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE]
//    FSFolder parentFolder = new FSFolder(currencyXml.parentFile, null, locationName, crawlName)
//
//    def "Analyze Test resource files"() {
//        given:
//        File paramsJson = new File(purl.toURI())
//        FSFile paramFS = new FSFile(paramsJson, parentFolder, locationName, crawlName)
//        FSFile currencyFS = new FSFile(currencyXml, parentFolder, locationName, crawlName)
//        FSFile datasourcesFS = new FSFile(datasourcesZip, parentFolder, locationName, crawlName)
//        FileAnalyzer analyzer = new FileAnalyzer(Constants.DEFAULT_FILENAME_PATTERNS)
//
//        when:
//        def paramsAnalysis = analyzer.analyze(paramFS)
//        def currencyAnalysis = analyzer.analyze(currencyFS)
//        def datasourcesAnalysis = analyzer.analyze(datasourcesFS)
//
//        then:
//        paramsAnalysis != null
//        paramsAnalysis.size() == 1
//        paramsAnalysis[0] == 'data'
//
//        currencyAnalysis != null
//        currencyAnalysis.size() == 1
//        currencyAnalysis[0] == 'config'
//
//        datasourcesAnalysis != null
//        datasourcesAnalysis.size() == 1
//        datasourcesAnalysis[0] == 'archive'
//    }
//}
