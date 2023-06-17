package configs

import com.oconeco.helpers.Constants

import java.nio.file.LinkOption
/**
 * this is a starter template that I (SoC) find useful.
 * hopefully there are some helpful examples and ideas here, but please customize to suit your interests (dear reader)
 * some of these options have code to override these config FILE defaults, with more explicit program arguments (e.g.  'wipeContent')
 */

solrUrl = "http://oldie:8983/solr/solr_system"
sourceName = Inet4Address.localHost.getHostName()

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
        if(new File(bkupFolder).exists()) {
            Backups = bkupFolder
        }
//        Home = "${userHome}"

        if(osName.containsIgnoreCase('linux')) {
            Work = "${userHome}/work"
            HomeConfig = "${userHome}/.config"
            HomeLocal = "${userHome}/.local"
            Etc = "/etc"
            Opt = "/opt"
            Var = "/var"

        } else if(osName.containsIgnoreCase('windows')){
            //  todo - this is barebones, most of my work is in linux, so this should be expanded for "normal" people/use
            Work = "/work"
            IdeaProjects = "${userHome}/IdeaProjects"
            GoogleDriveLocal = "${userHome}/My Drive"
        }
    }
    // not implemented yet... use command line override of folders to get this
/*
    externalDrives {
        wd4tb = "/media/sean/sean-data"
    }
*/
}

// note: all of the labels below are optional/customizable. You probably want to leave the `ignore`, `index`, and `analyze` labels as is, they have special meaning

// folder names can be matches to assign tags for a given folder (name matching)
namePatterns.folders = [
        ignore : Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE],
        content: ~/(content)/,
        office : ~/(?i).*(documents)/,
        techDev: ~/.*(groovy|gradle|classes)/,
        system : ~/(_global)/,
        techDev: ~/(.gradle|compile|groovy|java|main|scala|src|target|test|resources|wrapper)/,
        test   : ~/(?i).*(test)/,
        work   : ~/(?i).*(lucidworks|oconeco|work)/,
]

// edit and customize these entries. The namePattern name is the assigned tag, the regex pattern (`~` is the pattern operator) are the matching regexes for the label/tag
namePatterns.files = [
        ignore      : ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/,
        index       : ~/(bash.*|csv|groovy|ics|ipynb|java|lst|md|php|py|rdf|rss|scala|sh|tab|te?xt|tsv)/,
        analyze     : ~/(aspx\?|cfm|docx\?|html|od.|pptx\?|pdf|ps|pub|rss|xlsx|zhtml\?)/,
        office      : ~/.*(accdb|docx?|ods|odp|odt|pdf|pptx?|rtf|txt|vsdx?|xmind|xlsx?)/,
        instructions: ~'(?i).*(adoc|readme.*|md)',
        techDev     : ~/.*(c|codeStyles|css|dat|go|gradlew|groovy|gradle|iml|ipynb|jar|java|javascript|js|map|mat|php|pyi?|sav|sbt|schema|sh|ts)/,
        config      : ~/.*(config.*|manifest.*|properties|xml|ya?ml)/,
        control     : ~/.*(ignore|license.*|lock|pem)/,
        data        : ~/.*(csv|jsonl?d?|lst|pbix|tab|tsv)/,
        media       : ~/.*(avi|jpe?g|ogg|mp3|mpe?g|png|wav)/,
        logs        : ~/.*(logs?\.?\d*)/,       // attempting to allow singular or plural (option s after log, and 0 or more digits)
        archive     : ~/.*(arc|cab|dmg|gz|jar|parquet|rar|zip|tar\.(bz2?|gz|Z)|war|zip)/,
        compressed  : ~/.*\.(bz2?|gz|z|Z)/,
        web         : ~/.*(html?)/,
        system      : ~/.*(_SUCCESS|bat|bin|bkup|cache|class|cookies|deb|gcc|lib|\.old|pkg|rpm|#$)/,
]

