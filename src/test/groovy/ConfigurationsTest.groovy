import com.oconeco.helpers.Constants
import spock.lang.Specification

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

    def "test regex vs lots of string matches"() {
        given:
        Pattern ignoreFolders = Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE]
        List folderNames = ['/a/one/.csv/foo', 'bin/foo', 'c:\\fonts\\', '/home/sean/passme', 'c:\\goodwindows.doc', 'test/.m2', '/opt/test/pkgs']

        when:
        List ignored = folderNames.findAll { it ==~ ignoreFolders }
        List notIgnored = folderNames.findAll { !(it ==~ ignoreFolders) }

        then:
        ignored.size() == 2
        notIgnored.size() == 5

        ignored[0] == 'test/.m2'
        ignored[1] == '/opt/test/pkgs'


    }

}
