package configs
/**
 * Basic configuration file to simulate (s)locate from linux: simple file name, location, date, size tracking
 * see alsp @link configBasicAnalysis
 */

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
//    "${FSFolder.TYPE}" = [
    folders = [
            ignore : [pattern: Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE], analysis: [Constants.IGNORE]],
            test   : [pattern: ~/.*(?i)(test.*)/, analysis: Constants.TRACK_BUNDLE],
            content: [pattern: ~/.*(?i)(content)/, analysis: Constants.PARSE_BUNDLE],
            default: [pattern: '', analysis: Constants.DEFAULT],
    ]

    files = [
            ignore : [pattern: ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/, analysis: Constants.IGNORE],
            office            : [pattern: ~/.*\.(?i)(accdb|do[ct]x?|ics|ods|odp|odt|ott|pages|pdf|pptx?|rtf|sxi|vsdx?|xmind|xlsx?)/, analysis: Constants.PARSE_BUNDLE],
            system            : [pattern: ~/.*(\.(?i)(bin|bundle|cab|deb|dmg|exe|jar|lib|md5|pkg|rpm|so)|(gcc.*))/, analysis: Constants.TRACK_BUNDLE],
            web               : [pattern: ~/.*(?i)(html?)/, analysis: Constants.PARSE_BUNDLE],
            instructions      : [pattern: ~/.*(?i)(adoc|readme.*|man|md)/, analysis: Constants.PARSE_BUNDLE],
            techDev           : [pattern: ~/.*(?i)(c|css|go|groovy|gradle|java|javascript|js|php|schema|sh)/, analysis: Constants.PARSE_BUNDLE],
            config            : [pattern: ~/.*(?i)(cfg|config.*|pem|properties|xml|yaml)/, analysis: Constants.PARSE_BUNDLE],
            data              : [pattern: ~/.*(?i)(avro|bundle|csv|dat|db|jsonl?d?|lst|mdb.?|orc|parquet|tab)/, analysis: Constants.TRACK_BUNDLE],
            archives          : [pattern: ~/.*(?i)(bz2|cab|dmg|ear|img|iso|msi|rar|tar.gz|tgz|war)/, analysis: Constants.TRACK_BUNDLE],
            media             : [pattern: ~/.*(?i)(avi|bmp|gif|ico|jpe?g|kra|mp3|mpe?g|ogg|png|wav)/, analysis: Constants.PARSE_BUNDLE],
            communication     : [pattern: ~/.*(?i)(eml|vcf)/, analysis: Constants.PARSE_BUNDLE],
            logs              : [pattern: ~/.*(?i)\.log([.\d_-]+)?/, analysis: Constants.TRACK_BUNDLE],
            (Constants.TRACK) : [pattern: null, analysis: Constants.TRACK_BUNDLE],
    ]
}

// regex labeling/analysis based path rather than name (for folders and files -- assumes this is for FSObjects (folders/files)
pathPatterns {
    folders = [
            binariesFolder : [pattern: ~/(?i)[\/\\](s?bin)[\/\\]?/, analysis: Constants.TRACK_BUNDLE],
            workFolder     : [pattern: ~/(?i).*[\/\\](lucidworks|oconeco|work).*/, analysis: Constants.PARSE_BUNDLE],
            systemFolder   : [pattern: ~/(?i)[\/\\](Windows|Program Files.*|\/lib(x?32|64|exec)?\b|share|include)\b.*/, analysis: Constants.TRACK_BUNDLE],
            dataFolder     : [pattern: ~/.*(\/data\/).*/, analysis: Constants.TRACK_BUNDLE],
            downloadsFolder: [pattern: ~/.*(\/[dD]ownloads?]\/).*/, analysis: Constants.TRACK_BUNDLE],
            optFolder      : [pattern: ~/.*(\/opt\/?).*/, analysis: Constants.TRACK_BUNDLE],
            varFolder      : [pattern: ~/.*(\/var\/?).*/, analysis: Constants.TRACK_BUNDLE],
            configFolder   : [pattern: ~/(\/etc\b).*/, analysis: Constants.PARSE_BUNDLE],
            manualsFolder  : [pattern: ~/(\/man\b).*/, analysis: Constants.PARSE_BUNDLE],
    ]
    files = [
            testFiles  : [pattern: ~/(?i).*(test).*/, analysis: Constants.TRACK_BUNDLE],
            solrFiles: [pattern: ~/(?i)(managed-schema|solrconfig).*/, analysis: Constants.TRACK_BUNDLE],
            interestingFiles: [pattern: ~/(?i)(future|predictions|fuzzy|vector|license).*/, analysis: Constants.PARSE_BUNDLE],
    ]
}
