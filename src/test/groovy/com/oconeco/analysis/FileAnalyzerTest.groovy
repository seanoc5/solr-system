package com.oconeco.analysis

import com.oconeco.models.FileFS
import spock.lang.Specification

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   10/9/22, Sunday
 * @description:
 */

class FileAnalyzerTest extends Specification {
    def "Analyze Test resource files"() {
        given:
        FileAnalyzer analyzer = new FileAnalyzer()
        File folder = new File(getClass().getResource('/content').toURI())
        List<FileFS> fileFSList = FileFS.getFileFSList(folder)

        when:
        def results = analyzer.analyze(fileFSList)

        then:
        results != null
    }
}
