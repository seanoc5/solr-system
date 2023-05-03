package misc.old

import groovy.cli.commons.CliBuilder

//import groovy.cli.picocli.CliBuilder
//import CommandLine$ParameterException

def defaultStringThing = 'How now little friend?'
def defaultDoubleThing = '1.09'
def defaultNumberThing = "${1550 * 4}"

def cli = new CliBuilder(header: 'Little Friend CLI', usage:'littleFriend', width: -1)
cli.st(longOpt: 'stringThing', "Some string. [defaults to '${defaultStringThing}']", args: 1, defaultValue: defaultStringThing)
cli.dt(longOpt: 'doubleThing', "Some double. [defaults to '${defaultDoubleThing}']", args: 1, defaultValue: defaultDoubleThing)
cli.nt(longOpt: 'numberThing', "Some number. [defaults to '${defaultNumberThing}']", args: 1, defaultValue: defaultNumberThing)
cli.h(longOpt: 'help', 'Usage Information')

def cliOptions = cli.parse(args)

if (!cliOptions) {
  cli.usage()
  System.exit(-1)
}

if (cliOptions.help) {
  cli.usage()
  System.exit(0)
}

