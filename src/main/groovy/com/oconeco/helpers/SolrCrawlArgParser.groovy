package com.oconeco.helpers

import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor
import org.apache.log4j.Logger

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
class SolrCrawlArgParser {
    static final Logger log = Logger.getLogger(this.class.name)
//    OptionAccessor options = null
//    ConfigObject config = null


    public static groovy.cli.internal.OptionAccessor parse(String toolName, String[] args) {
        CliBuilder cli = new CliBuilder(usage: "${toolName}.groovy --config=configLocate.groovy", width: 160)
        cli.with {
            h longOpt: 'help', 'Show usage information'
            c longOpt: 'config', args: 1, required: true, argName: 'ConfigFile', 'optional configuration file/url to be parsed/slurped with configSlurper'
            f longOpt: 'folders', 'Parent folder(s) to crawl', args: '+', valueSeparator: ',', required: false
            n longOpt: 'name', args: 1, required: false, 'Name of crawl to be added to all solr input docs (files and folders)'
            s longOpt: 'solrUrl', args: 1, required: false, 'Solr url with protocol, host, and port (if any)'
            w longOpt: 'wipeContent', required: false, 'Clear data from previous crawl (beware--easy to really bork things with this..)'
        }

        OptionAccessor options = cli.parse(args)
        if (!options) System.exit(-1)

        if (options.help) {
            cli.usage()
            System.exit(0)
        }

        if (options.config) {
            String cfg = options.config
            ConfigSlurper configSlurper = new ConfigSlurper()

//            File f = new File(options.config)
//            if (f.exists()) {
//                log.info "Config file: ${f.path}"
//                config = configSlurper.parse(f.toURI().toURL())
//            } else {
//                URL cfgUrl = SolrCrawlArgParser.class.getClassLoader().getResource(options.config)
//                log.info "Config url resolved to: $cfgUrl"
////                String myAppName = 'MyFusionApp'
////                Map cliArgs = [appName: myAppName]
////                configSlurper.setBinding(cliArgs)
//                config = configSlurper.parse(cfgUrl)
//                log.warn "Cannot seem to find file: ${cfg}"
//            }

        } else {
            log.warn "No Config file found, cancelling..."
            cli.usage()
            System.exit(-1)
        }

        if (options.solrUrl) {
            log.info "Solr url: ${options.solrUrl}"
        } else {
            log.info "No solr url given in command line args, assuming this is set in the config file..."
        }
        return options
    }

}
