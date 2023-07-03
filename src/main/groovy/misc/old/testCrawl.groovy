package misc.old

import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.LogManager

Logger log = LogManager.getLogger(this.class.name);

List<String> ignoreDirs = [
        ".git",
        ".idea",
        ".svn",
        ".gradle",
        ".ipynb_checkpoints",
        ".scrap.*",

        "build",
        "css",
        "gradle",
        "jquery.*",
        "lib",
        "node.modules",
        "out",
        "site-packages",
        "vendor",
        "target",
        "web-app",
]
String joinedIgnore = ignoreDirs.join('\\|')
String regexIgnore = /.*\(${joinedIgnore}\)/
//String regexIgnore = /.*(css|.git|vendor)/


// todo -- fixme? seems to not work from groovy, but same command works directly in bash
String bashCommand = "find /home/sean/work/oconeco/content-analysis -iregex \"${regexIgnore}\" -prune -o -type d -print"
log.info "execute: \n\t\t[$bashCommand]"
def proc = bashCommand.execute()

String errMsg = proc.err.text
if (errMsg) {
    log.error "Process err : ${errMsg}"
} else {
    log.debug "Seemed to succeed?"
}

String results = proc.in.text
if (results){
    log.debug "Process text: ${results}"
    List<String> folders = results.split('\n')
    log.info "Folders count: ${folders.count(Http)}"
} else {
    log.warn "No results found?"
}




