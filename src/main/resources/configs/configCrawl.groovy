package configs

/**
 * configuration file to do both tracking (simple file name, location, date, size)
 * and/or parsing (tika metadata and content) depending on regex patterns
 * for groups (folders) and items (files), both name and 'path'
 */

import com.oconeco.helpers.Constants

import java.nio.file.Files
import java.nio.file.LinkOption

solrUrl = "http://localhost:8983/solr/solr_system"
locationName = Inet4Address.localHost.getHostName()

// can be overriden b
wipeContent = false
userHome = System.getProperty("user.home")
// helpful to build default crawl lists for personal crawling across variety of personal machines/os-es
osName = System.getProperty("os.name")
// be default don't follow links, just focus on the 'original'
//linkOption = LinkOption.NOFOLLOW_LINKS
//ignoreArchivesPattern = ~/.*([jw]ar|docx|pptx|xlsx)/


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
            Windows = "c:/Windows"
            ProgramFiles = "c:/Program Files"
            ProgramFilesx86 = "c:/Program Files (x86)"

        }
    }

}


// note: all of the labels below are optional/customizable.
namePatterns {
    folders = [
            (Constants.IGNORE): [pattern: Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE], analysis: [Constants.IGNORE]],          // track ignored folders, but do not descend/crawl
            (Constants.TRACK) : [pattern: null, analysis: Constants.TRACK_BUNDLE]
    ]


    files = [
            (Constants.IGNORE): [pattern: ~/(?i)([_.~]+lock.*|[#_~].*|.*\bte?mp|.*\.class)/, analysis: Constants.IGNORE_BUNDLE],
//            (Constants.IGNORE): [pattern: ~/(?i)([_.~]+lock.*|[#_~].*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/, analysis: Constants.IGNORE_BUNDLE],
            office            : [pattern: ~/.*\.(?i)(accdb|do[ct]x?|ics|ods|odp|odt|ott|pages|pdf|pptx?|rtf|sxi|vsdx?|xmind|xlsx?)/, analysis: Constants.PARSE_BUNDLE],
            system            : [pattern: ~/.*(\.(?i)(bin|bundle|cab|deb|dmg|exe|jar|lib|md5|pkg|rpm|so)|(gcc.*))/, analysis: Constants.TRACK_BUNDLE],
            web               : [pattern: ~/.*(?i)(html?)/, analysis: Constants.PARSE_BUNDLE],
            instructions      : [pattern: ~'(?i).*(adoc|readme.*|md)', analysis: Constants.PARSE_BUNDLE],
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


pathPatterns {
    folders = [
            binaries: [pattern: ~/(?i)[\/\\](s?bin)[\/\\]?/, analysis: Constants.TRACK_BUNDLE],
            work    : [pattern: ~/(?i).*[\/\\](lucidworks|oconeco|work).*/, analysis: Constants.PARSE_BUNDLE],
            system  : [pattern: ~/(?i)[\/\\](Windows|Program Files.*|\/lib(x?32|64|exec)?\b|share|include)\b.*/, analysis: Constants.TRACK_BUNDLE],
            data    : [pattern: ~/.*(\/data\/).*/, analysis: Constants.TRACK_BUNDLE],
            opt     : [pattern: ~/.*(\/opt\/?).*/, analysis: Constants.TRACK_BUNDLE],
            var     : [pattern: ~/.*(\/var\/?).*/, analysis: Constants.TRACK_BUNDLE],
            config  : [pattern: ~/(\/etc\b).*/, analysis: Constants.PARSE_BUNDLE],
            manuals  : [pattern: ~/(\/man\b).*/, analysis: Constants.PARSE_BUNDLE],
    ]
    files = []
}



