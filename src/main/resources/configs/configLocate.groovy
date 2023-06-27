package configs

/**
 * Basic configuration file to simulate (s)locate from linux: simple file name, location, date, size tracking
 * see alsp @link configBasicAnalysis
 */
import com.oconeco.analysis.BaseAnalyzer
import com.oconeco.helpers.Constants

import java.nio.file.LinkOption

/**
 * this is a starter template that I (SoC) find useful.
 * hopefully there are some helpful examples and ideas here, but please customize to suit your interests (dear reader)
 * some of these options have code to override these config FILE defaults, with more explicit program arguments (e.g.  'wipeContent')
 */

solrUrl = "http://oldie:8983/solr/solr_system"
locationName = Inet4Address.localHost.getHostName()

// can be overriden b
wipeContent = false
userHome = System.getProperty("user.home")
// helpful to build default crawl lists for personal crawling across variety of personal machines/os-es
osName = System.getProperty("os.name")
// be default don't follow links, just focus on the 'original'
linkOption = LinkOption.NOFOLLOW_LINKS
ignoreArchivesPattern = ~/.*([jw]ar|docx|pptx|xlsx)/


// todo -- these name: path entries are not yet used, still reading path and name from cli args
dataSources {
    localFolders {
        // todo -- check if this maps across the 3 main OSes: Linux, Mac, windoze
        MyDocuments = "${userHome}/Documents"
        Downloads = "${userHome}/Downloads"
        MyDesktop = "${userHome}/Desktop"
        MyPictures = "${userHome}/Pictures"

        String bkupFolder = "${userHome}/bkup"
        if (new File(bkupFolder).exists()) {
            Backups = bkupFolder
        }

        if (osName.containsIgnoreCase('linux')) {
            Work = "${userHome}/work"
            HomeConfig = "${userHome}/.config"
            HomeLocal = "${userHome}/.local"
            Etc = "/etc"
            Opt = "/opt"
            Var = "/var"

        } else if (osName.containsIgnoreCase('windows')) {
            //  todo - this is barebones, most of my work is in linux, so this should be expanded for "normal" people/use
            Work = "/work"          // i.e. c:/work
            IdeaProjects = "${userHome}/IdeaProjects"
            GoogleDriveLocal = "${userHome}/My Drive"
        }
    }

}


// note: all of the labels below are optional/customizable.
namePatterns {
    folders = [
            ignore : [pattern: Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE], analysis: [BaseAnalyzer.IGNORE]],
            default: [pattern: ~/.*/, analysis: BaseAnalyzer.DEFAULT],
    ]

    files = [
            ignore      : [pattern: ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/, analysis: BaseAnalyzer.IGNORE],
            default: [pattern: ~/.*/, analysis: BaseAnalyzer.DEFAULT],
    ]
}

/*

pathPatterns {
    folders = [
            work: [pattern: ~/(?i).*(lucidworks|oconeco|work)/, analysis: BaseAnalyzer.BASIC_BUNDLE],
    ]
    files = []
}
*/


