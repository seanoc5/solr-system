package configs

solrUrl = "http://oldi:8983/solr/solr_system"
sourceName = Inet4Address.localHost.getHostName()
wipeContent = false

// todo -- these name: path entries are not yet used, still reading path and name from cli args
dataSources {
    localFolders {
        MyDesktop = '/home/sean/Desktop'
        MyDocuments = '/home/sean/Documents'
        Downloads = '/home/sean/Downloads'
        MyPictures = '/home/sean/Pictures'

        SeanConfig = '/home/sean/.config'
        SeanLocal = '/home/sean/.local'
        Work = '/home/sean/work'
        Backups = '/home/sean/bkup'
//        SeanHome = '/home/sean'
        Etc = '/etc'
        Opt = '/opt'
        Var = '/var'
    }
    externalDrives {
        wd4tb = '/media/sean/sean-data'
    }
}

// note: all of the labels below are optional/customizable. You probably want to leave the `ignore`, `index`, and `analyze` labels as is, they have special meaning

// edit and customize these entries. The namePattern name is the assigned tag, the regex pattern (`~` is the pattern operator) are the matching regexes for the label/tag
namePatterns.files = [
        ignore      : ~/(|[.~]*lock.*|_.*|.*\.te?mp$|.*\.class$|robots.txt)/,
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

// folder names can be matches to assign tags for a given folder (name matching)
namePatterns.folders = [
        ignore : ~/(@angular|__snapshots__|\.bsp|\.?cache|\.csv|\.gradle|\.git|\.github|\.idea|\.settings|\.svn|\.vscode|_global|ignore.*|lib|node.modules|runtime)/,
        content: ~/(content)/,
        office : ~/(?i).*(documents)/,
        techDev: ~/.*(groovy|gradle|classes)/,
        system : ~/(_global)/,
        techDev: ~/(.gradle|compile|groovy|java|main|scala|src|target|test|resources|wrapper)/,
        test   : ~/(?i).*(test)/,
        work   : ~/(?i).*(lucidworks|oconeco|work)/,
]
