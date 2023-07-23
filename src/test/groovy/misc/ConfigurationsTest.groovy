package misc

import com.oconeco.helpers.Constants
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   8/1/22, Monday
 * @description:
 */

class ConfigurationsTest extends Specification {

    def "should load basic config"() {
        given:
        String cfg = """
skip = ~/(temp|cache|delete)/
index = ~"(bash.*|csv|groovy|ics|ipynb|java|lst|md|php|py|rdf|rss|scala|sh|tab|te?xt|tsv)"
"""

        when:
        ConfigObject config = new ConfigSlurper().parse(cfg)
        def skip = config.skip
        String s = skip.toString()

        then:
        skip instanceof Pattern
        s.equals("(temp|cache|delete)")
    }

    def "test regex from config slurper"() {
        given:
        List names = ['tmp', 'temp', 'cache', 'deleted', 'myTest']
        String cfg = """
skip = ~/(te?mp|cache|delete)/
"""
        ConfigObject config = new ConfigSlurper().parse(cfg)
        def skip = config.skip

        when:
        def skipped = names.findAll {
            it =~ skip
        }
        def remaining = names - skipped

        then:
        skipped instanceof List
        skipped.size() == 4
        remaining instanceof List
        remaining == ['myTest']
    }


    def "test filtering string matches"() {
        given:
        List folderNames = ['one', '.csv', 'foo', 'bin', 'fonts', 'passme', 'goodwindows.doc']
        List<String> foldersToSkip = ['.csv', 'bin', 'fonts']

        when:
        List filtered = folderNames.findAll { foldersToSkip.contains(it) }
        List passed = folderNames.findAll { !foldersToSkip.contains(it) }

        then:
        filtered.size() == 3
        passed.size() == 4
    }

    def "test regex FOLDER names"() {
        given:
        Pattern ignoreFolders = Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE]
        List folderNames = ['/a/one/.csv/', 'bin/foo', 'c:\\fonts\\', '/home/sean/.gradle', 'c:\\Program Files', 'test/.m2', '/opt/test/pkgs']

        when:
        List ignored = []
        List notIgnored = []
        folderNames.each { String path ->
            Path foo = Paths.get(path)
            String name = foo.getFileName()
            boolean match = (name ==~ ignoreFolders)
            if(match) {
                ignored << path
            } else {
                notIgnored << path
            }
        }

        then:
        ignored.size() == 3
        notIgnored.size() == 4

        ignored.containsAll(['test/.m2', '/opt/test/pkgs'])

    }

    def "test regex FILENAMES"() {
        given:
        Pattern ignoreFiles = Constants.DEFAULT_FILENAME_PATTERNS[Constants.LBL_IGNORE]
        List filenames = ['/a/one/~temp', 'bin/foo', 'c:\\fonts\\', '/home/sean/.ssh/key.pem', 'c:\\goodwindows.doc', 'test/foo.temp', '/test/bar.tmp']

        when:
        List ignored = []
        List notIgnored = []
        filenames.each { String path ->
            Path foo = Paths.get(path)
            String name = foo.getFileName()
            boolean match = (name ==~ ignoreFiles)
            if(match) {
                ignored << path
            } else {
                notIgnored << path
            }
        }

        then:
        ignored.size() == 4
        notIgnored.size() == 3

        ignored[0] == '/a/one/~temp'
        ignored[1] == '/home/sean/.ssh/key.pem'


    }

}
