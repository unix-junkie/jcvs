
The files in this directory are an example of using JavaScript to drive
the jCVS core. Using JavaScript, you can perform anything that jCVS can.
I will warn you, however, that these scripts are not simple. They show
you how to directly access the com.ice.cvsc package to drive the cvs
client/server protocol. Thus, you must work with the com.ice.cvsc classes,
such as CVSProject, CVSRequest, CVSClient, etc., just as the jCVS II
application must. I hope at some future time to provide a JavaScript
library that you can simply load(), and then write your scripts as if
you were invoking the command line cvs. However, that is some amount of
work, and I do not have time right now.

To use these examples, you must download the Netscape JavaScript package
named Rhino. You can get that here:

     http://www.mozilla.org/rhino/download.html

Once you have the package, extract the contents. In the contents will be
a js.jar file, which you will need to place on your classpath in order to
run JavaScript scripts. Once the js.jar file is on your classpath, change
your current working directory to the jCVS javascript directory that
contains this file and the example scripts. Check the two variables that
set the temp dir and local working directory root dir in 'gjt-common.js'.
Look for the string 'MODIFY THESE PLEASE'. Once these two variables are set
appropriately for your environment, start the JavaScript interpretter with
this command:

     java org.mozilla.javascript.tools.shell.Main gjt-co.js

This will invoke the JavaScript shell and instruct it to evaluate the
script named "gjt-co.js". This will run the script that connects to the
Giant Java Tree anonymous cvs server and download the GJT util package.
This package is small, and will be used as the basis for the other
examples scripts. Once you have run the script successfully, then you
may run any of the other scripts, as they will work with the checked
out directory.

You must be cd-ed to the javascript directory, because the load()
funtions use only a relative filename to load the included javascripts.

You can run the commands in this order if you wish:

     java org.mozilla.javascript.tools.shell.Main gjt-co.js
     java org.mozilla.javascript.tools.shell.Main gjt-list.js
     java org.mozilla.javascript.tools.shell.Main gjt-status.js


tim.
