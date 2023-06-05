package com.oconeco.helpers

import java.util.regex.Pattern

class Constants {

    public static final String LBL_ARCHIVE = 'archive'
    public static final String LBL_CRAWL = 'crawl'
    public static final String LBL_IGNORE = 'ignore'
    public static final String LBL_UNKNOWN = 'unknown'

    public static final Map<String, Pattern> DEFAULT_FOLDERNAME_PATTERNS = [
            ignore       : ~/.*(?i)(csv|\.gradle|\.m2|\b*snapshots?\b*|\b*caches?\b*|deleteme.*|git|github|ignore.*|never_index|node_modules|packages?|pkgs?|plugins?|repository|skins?|skipme.*|svn|target|tmp|vscode)/,
            backups      : ~/.*(?i)(backups?|bkups?|old|timeshift)/,
            configuration: ~/.*(?i)(configs)/,
            documents    : ~/.*([Dd]ocuments|[Dd]esktop)/,
            downloads    : ~/.*([Dd]ownloads)/,
            pictures     : ~/.*([Pp]ictures)/,
            programming  : ~/.*(?i)(java|jupyter|src|work)/,
            system       : ~/.*(?i)(class|sbt|sbt-\d.*|runtime)/,
    ]

    public static final Map<String, Pattern> DEFAULT_FILENAME_PATTERNS = [
            ignore      : ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/,
            office      : ~/.*\.(?i)(accdb|docx?|ods|odp|odt|pptx?|rtf|vsdx?|xlsx?)/,
            system      : ~/.*(\.(?i)(bin|deb|lib|pkg|rpm)|(gcc.*))/,
            archive     : ~/.*\.(?i)(arc|gz|rar|zip|tar.gz|zip)/,
            web         : ~/.*(?i)(html?)/,
            instructions: ~'(?i).*(adoc|readme.*|md)',
            techDev     : ~/.*(?i)(c|css|go|groovy|gradle|jar|java|javascript|js|php|schema|sh)/,
            config      : ~/.*(?i)(cfg|config.*|pem|properties|xml|yaml)/,
            data        : ~/.*(?i)(csv|jsonl?d?|lst|tab)/,
            media       : ~/.*(?i)(avi|jpe?g|ogg|mp3|mpe?g|wav)/,
    ]
    public static int DEFAULT_FOLDER_SIZE = 4096     // todo -- revisit, this is just an arbitrary starting size...?

//    public static final String TOCRAWL = 'crawl'
//    public static final String TOIGNORE = 'ignore'
}
