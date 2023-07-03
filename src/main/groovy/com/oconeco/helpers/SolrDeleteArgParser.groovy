package com.oconeco.helpers


import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   7/31/22, Sunday
 * @description:
 */

/**
 * General helper class trying to standardize args specific to solr operations
 * probably can be deprecated or removed, but leaving for now
 */
class SolrDeleteArgParser {
    static final Logger log = LogManager.getLogger(this.class.name)
    public static OptionAccessor parse(String toolName, String[] args) {
        CliBuilder cli = new CliBuilder(usage: "${toolName}.groovy --config=configLocate.groovy", width: 160)
        cli.with {
            h longOpt: 'help', 'Show usage information'
            c longOpt: 'config', args: 1, required: true, argName: 'ConfigFile', 'optional configuration file to be parsed/slurped with configSlurper'
            f longOpt: 'folders', 'Parent folder(s) to crawl' , args: '+', valueSeparator: ',', required: false
            n longOpt: 'name', args:1, required: false, 'Name of crawl to be added to all solr input docs (files and folders)'
            s longOpt: 'solrUrl', args: 1, required: false, 'Solr url with protocol, host, and port (if any)'
            w longOpt: 'wipeContent', required: false, 'Clear data from previous crawl (beware--easy to really bork things with this..)'
        }

        OptionAccessor options = cli.parse(args)
        if (!options) System.exit(-1)

        if (options.help) {
            cli.usage()
            System.exit(0)
        }

        if(options.solrUrl) {
            log.info "Solr url: ${options.solrUrl}"
        } else {
            log.info "No solr url given in command line args, assuming this is set in the config file..."
        }

        return options
    }

}
