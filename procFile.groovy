import java.util.regex.Pattern
import groovy.text.SimpleTemplateEngine

// http://www.mcquilleninteractive.com/blog/
// http://www.markhneedham.com/blog/category/java/
// http://query7.com/php-jquery-todo-list-part-1
// http://snook.ca/

class Block {
    StringBuilder rawText
    String type
    String cssId
    String cssClass

    public static binding = new HashMap()

    def preSpace(str) {
        def padding = ""

        def len = str.length()
        for (def i=0; i<len; i++) {
            if (str.charAt(i) == ' ') {
                padding = padding + " "
            } else {
                break
            }
        }
        return padding
    }

    def transform() {
        def rtext = rawText
        def rlen = rawText.toString().length()
        rtext = rtext.substring(0,rlen-2)

        def rtlist = rtext.split("\n")

        def rtokens = rtlist[0].split("\\.")
        def rtoken  = rtokens[0]
        def rchar   = rtoken.charAt(0)

        def lpPos = rtoken.indexOf("(")
        def rpPos = rtoken.indexOf(")")

        if (rpPos > 0 && lpPos > 0 && (rpPos > lpPos)) {
            cssClass = rtoken.substring(lpPos+1,rpPos)
        }

        def regx = rtoken
        regx = regx.replaceAll("\\(", "\\\\(")
        regx = regx.replaceAll("\\)", "\\\\)")
        rtext = rtext.replaceFirst(regx+"\\.","")

        Pattern emptyLine = Pattern.compile("^\\n", Pattern.MULTILINE)

        //Pattern boldFace = Pattern.compile("\\*(.*)\\*", Pattern.MULTILINE)
        Pattern boldFace = Pattern.compile("\\*([\\w*\\W*\\s*]{0,}?)\\*", Pattern.MULTILINE)

        //Pattern emphasis = Pattern.compile("_(.*)_", Pattern.MULTILINE)
        Pattern emphasis = Pattern.compile("_([\\w*\\W*\\s*]{0,}?)_", Pattern.MULTILINE)

        //Pattern delText = Pattern.compile("---(.*)---", Pattern.MULTILINE)
        Pattern delText = Pattern.compile("---([\\w*\\W*\\s*]{0,}?)---", Pattern.MULTILINE)

        //Pattern insText = Pattern.compile("\\+(.*)\\+", Pattern.MULTILINE)
        Pattern insText = Pattern.compile("\\+([\\w*\\W*\\s*]{0,}?)\\+", Pattern.MULTILINE)

        //Pattern spanText = Pattern.compile("%(.*)%", Pattern.MULTILINE)
        Pattern spanText = Pattern.compile("%([\\w*\\W*\\s*]{0,}?)%", Pattern.MULTILINE)

        //Pattern linkText = Pattern.compile("\"(.*)\":(\\S+)\\s", Pattern.MULTILINE)
        Pattern linkText = Pattern.compile("\"([\\w*\\W*\\s*]{0,}?)\":(\\S+)\\s", Pattern.MULTILINE)

        Pattern imgLink = Pattern.compile("!(.*)!:(\\S+)\\s", Pattern.MULTILINE)
        //Pattern imgLink = Pattern.compile("!([\\w*\\s*]{0,}?)!:(\\S+)\\s", Pattern.MULTILINE)

        Pattern imgText = Pattern.compile("!(\\S*)!", Pattern.MULTILINE)

        //Pattern listItem = Pattern.compile("^\\s*\\*\\s*(.*)\$", Pattern.MULTILINE)
        //Pattern listItem = Pattern.compile("^\\s*=\\s*(.*\\n\\s*.*)", Pattern.MULTILINE)
        //Pattern listItem = Pattern.compile("^\\s*={1}\\s*(.*\\n\\s*.*)?", Pattern.MULTILINE)
        //Pattern listItem = Pattern.compile("^\\s*=\\s*(.*\\n\\s*.*)*", Pattern.MULTILINE)

        //Pattern ordListWrapper = Pattern.compile("^\\s*(<li>.*</li>)", Pattern.MULTILINE)
        //def ordListWrapper = ~/^\s*(<li>.*<\/li>)/

        // Problems/Issues to be addressed (in order of priority)
        //    1. regex patterns at the end of the line (done)
        //    2. Nested patterns (done to some extent, but verify)
        //    3. imgLink and imgText patterns should be generic like emphasis Pattern
        //    4. a ! sign inside of the patterns that regex's try to match  (done)
        //    5. List items in the input file cannot span multiple lines it seems. list items should be in a single line

        /*
         *  I could not figure out how best to handle order lists and unordered lists with just
         *  regexs alone. So this block of code handles ol. and ul. blocks.
         *
         *  The main challenge was to pick up list items that spanned multiple lines. If list items
         *  were on the same line, then the regex approach worked, but it was not practical to
         *  expect the input file to contain list items that way (one per line). That would have
         *  been too restrictive.
         *
         */
        if (rchar == 'u' || rchar == 'o') {
            // Create an amended list amdList from rtlist(rawtext list) where each item holds one list item
            def amdList = []
            def indx = 0
            rtlist.each { rln ->
                // string subsequence that identifies a new list item is "= " -- change this to a regex
                if (rln.contains("= ")) {
                    indx++
                    rln = rln.replaceFirst("= ","<li>")  // replace it with the corresponding html markup
                } else {
                    rln = "  " + rln  // indent by two spaces to preserve indentation similar to the input file.
                                      // '<li>' is 4 spaces, '= ' is 2 spaces, difference is 2 spaces.
                }
                // append to existing list member or simply set as value depending on whether amdList[indx] is null or not
                if (amdList[indx] != null) {
                   amdList[indx] = amdList[indx] + rln + "\n"
                } else {
                   amdList[indx] = rln + "\n"
                }
            }

            // reset the rawtext with the newly created list after removing the last carriage return in each item
            // and replacing it with </li>. This is just for pretty printing purposes. A <li> and </li> on their
            // own lines would look sort of hard on the eyes (IMO).
            indx = 0
            rtext = ""
            amdList.each { alst ->
                def lastCRindx = alst.lastIndexOf("\n")
                alst = alst.substring(0,lastCRindx)
                if (indx > 0) {
                    rtext = rtext + alst + "</li>\n"
                }
                indx++
            }
            rtext = rtext.substring(0,rtext.lastIndexOf("\n")) // remove the very last carriage return
        }


        if (rchar == 'e') {
            def amdMap = [:]
            def term = null

            rtlist.each { rln ->
                def termMatcher = rln =~ /^\s*\w*\s*\w*:$/
                if (termMatcher.matches()) {
                    //println "Definition Term : ${rln}"
                    term = rln.substring(0,rln.lastIndexOf(":"))
                } else {
                    if (amdMap[term] != null) {
                        amdMap[term] = amdMap[term] + rln + "\n"
                    } else {
                        amdMap[term] = rln + "\n"
                    }
                }
            }

            rtext = ""
            // dump key and values
            amdMap.each { k, v ->
                if (k != null) {
                    def padding = preSpace(k)
                    def key = k.trim()
                    key = padding + "<dt>${key}</dt>"
                    //println key
                    rtext = rtext + key + "\n"
                    def value = padding + "<dd>" + v.trim() + "</dd>"
                    //println value
                    rtext = rtext + value + "\n"
                }
            }
            rtext = rtext.substring(0,rtext.lastIndexOf("\n")) // remove the very last carriage return

        }

        if (rchar != 'c') {

            //def padding = preSpace(rtext)
            //rtext = rtext.replaceFirst(padding,"")
            rtext = rtext.replaceAll(emptyLine,"\n<p>")
            // Failed attempts with pure regular expressions for list items (ol and ul)
            //rtext = rtext.replaceAll(listItem, "<li>\$1</li>")
            //rtext = rtext.replaceAll(ordListWrapper, "<ol>\n\$1\n</ol>")
            //rtext.eachMatch(/.*<li>.*<\/li>.*/){ println ' ####>>>> ' + it }
            rtext = rtext.replaceAll(boldFace, "<b>\$1</b>")
            rtext = rtext.replaceAll(emphasis, "<em>\$1</em>")
            rtext = rtext.replaceAll(delText, "<del>\$1</del>")
            rtext = rtext.replaceAll(insText, "<ins>\$1</ins>")
            rtext = rtext.replaceAll(spanText, "<span>\$1</span>")
            rtext = rtext.replaceAll(linkText, "<a href=\"\$2\">\$1</a> ")
            rtext = rtext.replaceAll(imgLink, "<a href=\"\$2\"><img src=\"\$1\" alt=\"\" border=\"0\"></a> ")
            rtext = rtext.replaceAll(imgText, "<img src=\"\$1\" alt=\"\" border=\"0\"> ")
        }

        if (rchar == 'v') {
            //println "======================================================="
            //println rtext
            //println "======================================================="
            rtlist.each { rln ->
                def termMatcher = rln =~ /\s*(.*)\s*=\s*(.*)/
                if (termMatcher.matches()) {
                     def key = "" + termMatcher.group(1)
                     def value = " " + termMatcher.group(2)
                     //println termMatcher.group(1) + " " + termMatcher.group(2)
                     binding[key.trim()] = value.trim()
                }
            }
        }

        //rtlist[0] = rtlist[0].replaceFirst(regx+"\\.","")
        switch (rchar) {
            case 'h':
                type = "h"
                //return "  is a Heading block with " + rtlist.size() + " line(s) token: ${rtoken} ${cssClass}\n" + rtext
                return "<${rtoken}>"+rtext+"</${rtoken}>"
                break
            case 'p':
                type = "p"
                //return "  is a Paragraph block with " + rtlist.size() + " line(s) token: ${rtoken} ${cssClass}\n" + rtext
                return "<p>"+rtext+"</p>"
                break
            case 'd':
                type = "d"
                //return "  is a Div block with " + rtlist.size() + " line(s) token: ${rtoken} ${cssClass}\n" + rtext
                def res = (cssClass != null) ? "<div class=\"${cssClass}\">\n"+rtext+"\n</div>\n" : "<div>\n"+rtext+"\n</div>\n"
                return res
                break
            case 'u':
                type = "u"
                //return "  is a Div block with " + rtlist.size() + " line(s) token: ${rtoken} ${cssClass}\n" + rtext
                def res = (cssClass != null) ? "<ul class=\"${cssClass}\">\n"+rtext+"\n</ul>\n" : "<ul>\n"+rtext+"</ul>\n"
                return res
                break
            case 'o':
                type = "o"
                //return "  is a Div block with " + rtlist.size() + " line(s) token: ${rtoken} ${cssClass}\n" + rtext
                def res = (cssClass != null) ? "<ol class=\"${cssClass}\">\n"+rtext+"\n</ol>\n" : "<ol>\n"+rtext+"</ol>\n"
                return res
                break
            case 'c':
                type = "c"
                //return "  is a Code block with " + rtlist.size() + " line(s) token: ${rtoken} ${cssClass}\n" + rtext
                def res = (cssClass != null) ? "<pre class=\"brush:${cssClass};\">\n"+rtext+"\n</pre>\n" : "<pre>\n"+rtext+"\n</pre>\n"
                return res
                //return "<pre>\n"+rtext+"\n</pre>\n"
                break
            case 'e':
                type = "e"
                //return "  is a Div block with " + rtlist.size() + " line(s) token: ${rtoken} ${cssClass}\n" + rtext
                def res = (cssClass != null) ? "<dl class=\"${cssClass}\">\n"+rtext+"\n</dl>\n" : "<dl>\n"+rtext+"</dl>\n"
                return res
                break
            case 'v':
                type = "v"
                return ""
                break
        }

    }

}

if (args.size() < 1) {
    println "Need a filename to process. Thanks!"
    System.exit(0)
}

List lines = new File(args[0]).readLines()

def blocks = []
def bc = 0

StringBuilder sb = null //new StringBuilder()

//lines.each { line ->
//   def str = line.trim()
//   if (str.length() == 0) {
//       bc++
//       blocks.add(new Block(rawText:sb))
//       sb = new StringBuilder()
//   } else {
//       sb.append(str+"\n")
//   }
//   //println str
//}
//blocks.add(new Block(rawText:sb))

//def bc = 0
def currbc = bc

lines.each { line ->
   //def str = line.trim()

   def str = line.replaceAll(/\s+$/,"")
   def re = /^\w{1,2}(\(\w+\))?\.\s*.*/
   def matcher = (str =~ re)
   if (matcher.matches()) {
       bc++
       if (currbc != bc && bc > 1) {
           blocks.add(new Block(rawText:sb))
           currbc = bc
       }
       sb = new StringBuilder()
       sb.append(str+"\n")
   } else {
       sb.append(str+"\n")
   }
}
blocks.add(new Block(rawText:sb))

//println "Processed ${bc+1} blocks of text"

//def ctx = new Expando()
//def lang = "language"
//ctx."${lang}" = "Groovy"
//Eval.me("def language = 'Groovy'")




def engine = new SimpleTemplateEngine()


bc = 0
def contentText = ""
blocks.each { block ->
    bc++
    //println "Block ${bc} :"
    if (block.type != "v") {
       contentText = contentText + "\n${block.transform()}"
    }
    //println "\n${block.transform()}"

    //println "${block.rawText}"
    //println "----------------------------------------------------------------------------------"
}

template = engine.createTemplate(contentText).make(Block.binding)

def content = template.toString().trim()

/**
def pageBinding = [:]
pageBinding["content"]=content
pageBinding["title"]=Block.binding["Title"]

def inclEngine = new SimpleTemplateEngine()
def fle = new File(Block.binding["pageTemplate"])
pageTemplate = inclEngine.createTemplate(fle).make(pageBinding)

println pageTemplate.toString()
**/

print "${content}"

//println "${ctx.language} is great!"
//print template.toString().trim()
