package configs


import com.oconeco.helpers.Constants

import java.nio.file.LinkOption

/**
 * this is a starter template that I (SoC) find useful.
 * hopefully there are some helpful examples and ideas here, but please customize to suit your interests (dear reader)
 * some of these options have code to override these config FILE defaults, with more explicit program arguments (e.g.  'wipeContent')
 */

solrUrl = "http://oldie:8983/solr/solr_system"
locationName = Inet4Address.localHost.getHostName()
compareExistingSolrFolderDocs = true

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
            Work = "/work"
            IdeaProjects = "${userHome}/IdeaProjects"
            GoogleDriveLocal = "${userHome}/My Drive"
        }
    }
/*
    // not implemented yet... use command line override of folders to get this
    externalDrives {
        wd4tb = "/media/sean/sean-data"
    }
*/
}


// note: all of the labels below are optional/customizable.
namePatterns {
    folders = [
            ignore : [pattern: Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE], analysis: [Constants.IGNORE]],
            content: [pattern: ~/(content)/, analysis: Constants.BASIC_BUNDLE],
            office : [pattern: ~/(?i).*(documents)/, analysis: Constants.BASIC_BUNDLE],
            techDev: [pattern: ~/.*(groovy|gradle|classes)/, analysis: Constants.BASIC_BUNDLE],
            system : [pattern: ~/(_global)/, analysis: Constants.BASIC_BUNDLE],
            techDev: [pattern: ~/(.gradle|compile|groovy|java|main|scala|src|target|test|resources|wrapper)/, analysis: Constants.SOURCE_CODE],
//            test   : [pattern: ~/(?i).*(test)/, analysis:],
            work   : [pattern: ~/(?i).*(lucidworks|oconeco|work)/, analysis: Constants.BASIC_BUNDLE],
    ]

    files = [
            ignore      : [pattern: ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/, analysis: Constants.IGNORE],
            index       : [pattern: ~/(bash.*|csv|groovy|ics|ipynb|java|lst|md|php|py|rdf|rss|scala|sh|tab|te?xt|tsv)/, analysis: Constants.BASIC_BUNDLE],
            office      : [pattern: ~/.*(accdb|docx?|ods|odp|odt|pdf|pptx?|ps|pub|rtf|txt|vsdx?|xmind|xlsx?)/, analysis: Constants.BASIC_BUNDLE],
            instructions: [pattern: ~/(?i).*(adoc|readme.*|md)/, analysis: Constants.BASIC_BUNDLE],
            techDev     : [pattern: ~/.*(c|codeStyles|css|dat|go|gradlew|groovy|gradle|iml|ipynb|jar|java|javascript|js|map|mat|php|pyi?|sav|sbt|schema|sh|ts)/, analysis: Constants.SOURCE_CODE],
            config      : [pattern: ~/.*(config.*|manifest.*|properties|xml|ya?ml)/, analysis: Constants.BASIC_BUNDLE],
            control     : [pattern: ~/.*(ignore|license.*|lock|pem)/, analysis: Constants.BASIC_BUNDLE],
            data        : [pattern: ~/.*(csv|jsonl?d?|lst|pbix|tab|tsv)/, analysis: Constants.BASIC_BUNDLE],
            media       : [pattern: ~/.*(avi|jpe?g|ogg|mp3|mpe?g|png|wav)/, analysis: Constants.BASIC_BUNDLE],
            logs        : [pattern: ~/.*(logs?\.?\d*)/, analysis: Constants.LOGS_BUNDLE],                          // attempting to allow singular or plural (option s after log, and 0 or more digits)
            archive     : [pattern: ~/.*(arc|cab|dmg|gz|jar|parquet|rar|zip|tar\.(bz2?|gz|Z)|war|zip)/, analysis: Constants.ARCHIVE_BUNDLE],
            compressed  : [pattern: ~/.*\.(bz2?|gz|z|Z)/, analysis: Constants.BASIC_BUNDLE],                       // revisit this? something better/more efficient?
            web         : [pattern: ~/.*(aspx?|cfm|html?|zhtml?)/, analysis: Constants.BASIC_BUNDLE],
            system      : [pattern: ~/.*(_SUCCESS|bat|bin|bkup|cache|class|cookies|deb|gcc|lib|\.old|pkg|rpm|#$)/, analysis: Constants.TRACK]
    ]
}


pathPatterns {
    folders = [
            work: [pattern: ~/(?i).*(lucidworks|oconeco|work)/, analysis: Constants.BASIC_BUNDLE],
    ]
    files = []
}


