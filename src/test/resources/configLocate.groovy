
// don't process these files, don't add them to the index, assume you will never even want to find if a file exists or not.
files.skip = ~/([.~]*lock.*|_.*|.*\.te?mp$|.*\.class$|robots.txt|.*\.odb$|.*\.pos)/

// NOTE: this is the default operation: track not skip or index, no need to specify these, this is the catch-all
// track the file metdata and that it exissts, but don't extract, index, or analyze
// static Pattern FILE_REGEX_TO_TRACK = Pattern.compile('(.*json|.*csv|.*xml)')

// INDEXING file is meant to run an extraction process (i.e. tika) and then send that info to solr for indexing/persistence/searchability (analysis below goes one step further)
files.index = ~/(bash.*|csv|groovy|ics|ipynb|java|lst|md|php|py|rdf|rss|scala|sh|tab|te?xt|tsv)/
//    Pattern FILE_EXTENSIONS_TO_INDEX = Pattern.compile(/.*[._](csv|groovy|history|ics|ini|java|log|php|rdf|rss|xml)$/)

// ANALYSIS is meant to go beyond indexing, and actually do more qualitative analysis (NLP, content sectioning, tagging,...)
files.analyze = ~/(aspx\?|cfm|docx\?|html|od.|pptx\?|pdf|ps|pub|rss|xlsx|zhtml\?)/

files.namePatterns = [
        office : ~'.*(accdb|docx?|ods|odp|odt|pptx?|rtf|txt|vsdx?|xslx?)',
        instructions : ~'(?i).*(adoc|readme.*|md)',
        techDev: ~/.*(c|css|go|groovy|gradle|jar|java|javascript|js|php|schema|sh)/,
        config: ~/.*(config.*|pem|properties|xml|yaml)/,
        data   : ~/.*(csv|jsonl?d?|lst|tab)/,
        media  : ~/.*(avi|jpe?g|ogg|mp3|mpe?g|wav)/,
        system : ~/.*(bin|deb|gcc|lib|pkg|rpm)/,
        archive: ~/.*(arc|gz|rar|zip|tar.gz|zip)/,
        web    : ~/.*(html?)/,
]

folders {
    namesToSkip = [
            '__snapshots__',
            '.cache',
            '.csv',
            '.gradle',
            '.git',
            '.github',
            '.idea',
            '.settings',
            '.svn',
            '.vscode',

            'anaconda',
            'anaconda2',
            'anaconda3',
            'assets',
            'bin',
            'build',
            'cache',
            'cache2',
            'classes',
            'css',
            'fonts',
//            'gradle',
//            'hibernate',
//            'images',
            'img',
            'layouts',
            'lib',
            'libs',
            'logo',
            'logs',
            'material-ui',
            'META-INF',
            'modules',
            'old',
            'out',
            'package',
            'packages',
            'persistence',
            'pkg',
            'pkgs',
            'plugins',
            'proc',
            'skin',
            'skins',
            'spring',
            'styles',
            'svg',
            'target',
            'taglib',
            'temp',
            'templates',
//            'testCrawl',
            'test',
            'tests',
            'themes',
            'tmp',
            'vendor',
            'web-app',
            'webapp',
            'wrapper',
            'node_modules',
            '$global',
            'streams',
            '.svn',
            'prop-base',
            'httpcache',
            'professional-services',
            'react',
    ]
}
