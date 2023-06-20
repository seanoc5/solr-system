package configs

import com.oconeco.analysis.FileAnalyzer
import com.oconeco.analysis.FolderAnalyzer
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
folderAnalyzer = new FolderAnalyzer()
fileAnalyzer = new FileAnalyzer()
// folder names can be matches to assign tags for a given folder (name matching)
namePatterns.folders = [
        ignore : [pattern:Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE], analyzer: folderAnalyzer],
        content: [pattern:~/(content)/, analyzer: folderAnalyzer],
        office : [pattern:~/(?i).*(documents)/, analyzer: folderAnalyzer],
        techDev: [pattern:~/.*(groovy|gradle|classes)/, analyzer: folderAnalyzer],
        system : [pattern:~/(_global)/, analyzer: folderAnalyzer],
        techDev: [pattern:~/(.gradle|compile|groovy|java|main|scala|src|target|test|resources|wrapper)/, analyzer: folderAnalyzer],
        test   : [pattern:~/(?i).*(test)/, analyzer: folderAnalyzer],
        work   : [pattern:~/(?i).*(lucidworks|oconeco|work)/, analyzer: folderAnalyzer],
]

// edit and customize these entries. The namePattern name is the assigned tag, the regex pattern (`~` is the pattern operator) are the matching regexes for the label/tag
namePatterns.files {
    ignore = [pattern: ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/ , analyzer:null]
    index:[pattern: ~/(bash.*|csv|groovy|ics|ipynb|java|lst|md|php|py|rdf|rss|scala|sh|tab|te?xt|tsv)/ ,analyzer:'tika']
    office:[pattern: ~/.*(accdb|docx?|ods|odp|odt|pdf|pptx?|ps|pub|rtf|txt|vsdx?|xmind|xlsx?)/ ,analyzer: 'tika']
    instructions:[pattern: ~/(?i).*(adoc|readme.*|md)/ ,analyzer: 'tika']
    techDev:[pattern: ~/.*(c|codeStyles|css|dat|go|gradlew|groovy|gradle|iml|ipynb|jar|java|javascript|js|map|mat|php|pyi?|sav|sbt|schema|sh|ts)/ ,analyzer: 'tika']
    config:[pattern: ~/.*(config.*|manifest.*|properties|xml|ya?ml)/ ,analyzer: 'tika']
    control:[pattern: ~/.*(ignore|license.*|lock|pem)/ ,analyzer: 'tika']
    data:[pattern: ~/.*(csv|jsonl?d?|lst|pbix|tab|tsv)/ ,analyzer: 'tika']
    media:[pattern: ~/.*(avi|jpe?g|ogg|mp3|mpe?g|png|wav)/ ,analyzer: 'tika']
    logs:[pattern: ~/.*(logs?\.?\d*)/ , analyzer: 'locate']      // attempting to allow singular or plural (option s after log, and 0 or more digits)
    archive:[pattern: ~/.*(arc|cab|dmg|gz|jar|parquet|rar|zip|tar\.(bz2?|gz|Z)|war|zip)/ ,analyzer: 'locate']
    compressed:[pattern: ~/.*\.(bz2?|gz|z|Z)/ ,analyzer: 'tika']
    web:[pattern: ~/.*(aspx?|cfm|html?|zhtml?)/ ,analyzer: 'tika']
    system:[pattern: ~/.*(_SUCCESS|bat|bin|bkup|cache|class|cookies|deb|gcc|lib|\.old|pkg|rpm|#$)/ ,analyzer: 'locate']
}

//namePatterns.files = [
//        ignore      : ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/,
//        index       : ~/(bash.*|csv|groovy|ics|ipynb|java|lst|md|php|py|rdf|rss|scala|sh|tab|te?xt|tsv)/,
//        analyze     : ~/(aspx\?|cfm|docx\?|html|od.|pptx\?|pdf|ps|pub|rss|xlsx|zhtml\?)/,
//        office      : ~/.*(accdb|docx?|ods|odp|odt|pdf|pptx?|rtf|txt|vsdx?|xmind|xlsx?)/,
//        instructions: ~/(?i).*(adoc|readme.*|md)/,
//        techDev     : ~/.*(c|codeStyles|css|dat|go|gradlew|groovy|gradle|iml|ipynb|jar|java|javascript|js|map|mat|php|pyi?|sav|sbt|schema|sh|ts)/,
//        config      : ~/.*(config.*|manifest.*|properties|xml|ya?ml)/,
//        control     : ~/.*(ignore|license.*|lock|pem)/,
//        data        : ~/.*(csv|jsonl?d?|lst|pbix|tab|tsv)/,
//        media       : ~/.*(avi|jpe?g|ogg|mp3|mpe?g|png|wav)/,
//        logs        : ~/.*(logs?\.?\d*)/,       // attempting to allow singular or plural (option s after log, and 0 or more digits)
//        archive     : ~/.*(arc|cab|dmg|gz|jar|parquet|rar|zip|tar\.(bz2?|gz|Z)|war|zip)/,
//        compressed  : ~/.*\.(bz2?|gz|z|Z)/,
//        web         : ~/.*(html?)/,
//        system      : ~/.*(_SUCCESS|bat|bin|bkup|cache|class|cookies|deb|gcc|lib|\.old|pkg|rpm|#$)/,
//]

pathPatterns.folders = Constants.DEFAULT_FOLDERPATH_PATTERNS
//pathPatterns.files = Constants.DEFAULT_FILENAME_PATTERNS


