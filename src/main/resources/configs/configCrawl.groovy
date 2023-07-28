package configs

import com.oconeco.helpers.Constants

import java.util.regex.Pattern

/**
 * configuration file to do both tracking (simple file name, location, date, size)
 * and/or parsing (tika metadata and content) depending on regex patterns
 * for groups (folders) and items (files), both name and 'path'
 */
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
checkGroupSizes = false


// todo -- these name: path entries are not yet used, still reading path and name from cli args
dataSources {
    localFolders {
        // todo -- check if this maps across the 3 main OSes: Linux, Mac, windoze
        MyDocuments = "${userHome}/Documents"
        Downloads = "${userHome}/Downloads"
        MyDesktop = "${userHome}/Desktop"
        MyPictures = "${userHome}/Pictures"
        ExtPassport = "/media/sean/sean-data"

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
            UsrShare = "/usr/share"
            SBin = "/usr/sbin"
            Bin = "/bin"
            Usr = "/usr"

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


solrIgnore = '.*shard\\d+_replica.*'          // don't dive into solr index folders
folderIgnoreSystem = /RECYCLE.BIN|google-chrome|packages?|Partitions|pkgs?|snapshots?|System\b.*|te?mp|[\w_-]*trash|[._#~]*Trash.*/
folderIgnoreUser = /\.gradle.?|\.m2|assets|build|classes|[._#~-]*cache.*|deleteme.*|engine|flatpack|git|github|google-chrome|heritrix.*ignore.*|never_index|node_modules|packages?|plugins?|repo|repository|skins?|skipme.*|svn|target|vscode/
ignorePattern = Pattern.compile("($folderIgnoreSystem|$folderIgnoreUser)",  Pattern.CASE_INSENSITIVE)
println "Ignore pattern: $ignorePattern"

// note: all of the labels below are optional/customizable.
namePatterns {
    folders = [
            ignore             : [pattern: ignorePattern, analysis: [Constants.IGNORE], inherit:true],          // track ignored folders, but do not descend/crawl
            systemFolder       : [pattern: ~/[\\\/](Windows|Program Files.*|bin|include|lib(x?32|64|exec)?|share|usr|var)[\\\/]/, analysis: [Constants.TRACK_BUNDLE], inherit:true],          // track ignored folders, but do not descend/crawl
            securedSystemFolder: [pattern: ~/[\\\/](sbin)[\\\/]/, analysis: [Constants.TRACK_BUNDLE], inherit:true],          // track ignored folders, but do not descend/crawl
            logsFolder         : [pattern: ~/[\\\/](logs?)[\\\/]/, analysis: [Constants.TRACK_BUNDLE], inherit:true],          // track ignored folders, but do not descend/crawl
            backupsFolder      : [pattern: ~/[\\\/](bkup|backups?)[\\\/]/, analysis: [Constants.TRACK_BUNDLE], inherit:true],          // track ignored folders, but do not descend/crawl
            configurationFolder: [pattern: ~/[\\\/](etc|config(s|uration)?)[\\\/]/, analysis: [Constants.TRACK_BUNDLE], inherit:true],          // track ignored folders, but do not descend/crawl
            outFolder          : [pattern: ~/[\\\/](out|class(es)?|build)[\\\/]/, analysis: [Constants.TRACK_BUNDLE], inherit:true],          // track ignored folders, but do not descend/crawl
            (Constants.UNLABELED)          : [pattern: null, analysis: Constants.TRACK_BUNDLE, inherit:false]
    ]


    files = [
            (Constants.IGNORE): [pattern: ~/^(?i)([_.~]+lock.*|[#_~].*|.*\.te?mp|.*\.class)/, analysis: Constants.IGNORE_BUNDLE, inherit:true],
//            (Constants.IGNORE): [pattern: ~/(?i)([_.~]+lock.*|[#_~].*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/, analysis: Constants.IGNORE_BUNDLE],
            office            : [pattern: ~/.*\.(?i)(accdb|do[ct]x?|ics|ods|odp|odt|ott|pages|pdf|pptx?|rtf|sxi|vsdx?|xmind|xlsx?)/, analysis: Constants.PARSE_BUNDLE, inherit:true],
            system            : [pattern: ~/.*(\.(?i)(bin|bundle|cab|deb|dmg|exe|jar|lib|md5|pkg|rpm|so)|(gcc.*))/, analysis: Constants.TRACK_BUNDLE, inherit:true],
            web               : [pattern: ~/.*(?i)(html?)/, analysis: Constants.PARSE_BUNDLE, inherit:true],
            instructions      : [pattern: ~/.*(?i)(adoc|readme.*|man|md)/, analysis: Constants.PARSE_BUNDLE, inherit:true],
            techDev           : [pattern: ~/.*\.(?i)(c|css|go|groovy|gradle|java|javascript|js|php|py|schema|sh)/, analysis: Constants.PARSE_BUNDLE, inherit:true],
            config            : [pattern: ~/.*(?i)(\.cfg|config.*|\.pem|\.properties|\.xml|\.yaml)/, analysis: Constants.PARSE_BUNDLE, inherit:true],
            data              : [pattern: ~/.*\.(?i)(avro|bundle|csv|dat|db|jsonl?d?|lst|mdb.?|orc|parquet|tab)/, analysis: Constants.TRACK_BUNDLE, inherit:true],
            archives          : [pattern: ~/.*\.(?i)(bz2|cab|dmg|ear|img|iso|msi|rar|tar.gz|tgz|war)/, analysis: Constants.TRACK_BUNDLE, inherit:true],
            media             : [pattern: ~/.*\.(?i)(avi|bmp|gif|ico|jpe?g|kra|mp3|mpe?g|ogg|png|wav)/, analysis: Constants.PARSE_BUNDLE, inherit:true],
            communication     : [pattern: ~/.*\.(?i)(eml|vcf)/, analysis: Constants.PARSE_BUNDLE, inherit:true],
            logs              : [pattern: ~/.*(?i)[\\/](.*\.log([.\d_-]+))?/, analysis: Constants.TRACK_BUNDLE, inherit:true],
            (Constants.UNLABELED)         : [pattern: null, analysis: Constants.TRACK_BUNDLE, inherit:false],
    ]
}


pathPatterns {
    folders = [
            binariesFolder : [pattern: ~/(?i)[\/\\](s?bin)[\/\\]?/, analysis: Constants.TRACK_BUNDLE, inherit:true],
            workFolder     : [pattern: ~/(?i).*[\/\\](lucidworks|oconeco|work).*/, analysis: Constants.PARSE_BUNDLE, inherit:true],
            systemFolder   : [pattern: ~/(?i)[\/\\](Windows|Program Files.*|lib(x?32|64|exec)?\b|share|include)\b.*/, analysis: Constants.TRACK_BUNDLE, inherit:true],
            dataFolder     : [pattern: ~/.*(\/data\/).*/, analysis: Constants.TRACK_BUNDLE, inherit:true],
            downloadsFolder: [pattern: ~/.*(\/[dD]ownloads?]\/).*/, analysis: Constants.TRACK_BUNDLE, inherit:true],
            optFolder      : [pattern: ~/.*(\/opt\/?).*/, analysis: Constants.TRACK_BUNDLE, inherit:true],
            varFolder      : [pattern: ~/.*(\/var\/?).*/, analysis: Constants.TRACK_BUNDLE, inherit:true],
            configFolder   : [pattern: ~/.*(\/etc\/).*/, analysis: Constants.PARSE_BUNDLE, inherit:true],
            manualsFolder  : [pattern: ~/.*(?i)\/(help|info|man)\/.*/, analysis: Constants.PARSE_BUNDLE, inherit:true],
    ]
    files = []
}



