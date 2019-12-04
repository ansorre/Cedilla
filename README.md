# Cedilla

Open source templating engine for Java with the power of JavaScript. Incredibly small, quick, effective, easy! 

## Key points

 * just one dependency, get it here: [As-Libs-Core](https://github.com/ansorre/As-Libs-Core)   
 * templates are fully scriptable, uses the Java builtin JavaScript engine
 * Apache 2.0 license

## How-tos
   
  To insert the content of a variable: 
        
    this is the content of foo: §foo
 
  hence the classic Hello World example:

      String res=Cedilla.render("Hello, §what!", new String[]{"what", "World"});
      System.out.println(res); // Hello, World!

  to execute some javascript inside the template (one line way):
 
    § var foo="foo"; // the rest till the enf of line is javascript
 
  another one line way:
 
    § var foo="I'm it!"; §This is foo: §foo
 
  the multiline way is possibile too, instead of § &lt;javascript&gt; § you just go with §| &lt;javascript&gt; |§
 
    §| 
     var createdHere=
             'This is made now!';
 
    |§this is the content made now: §createdHere
 
  expressions are easy too, you include them in §= &lt;expression&gt; §:
 
    This is more then foo: §= foo+" and some more" §!
 
 This means you can also do things like:
 
    This is more then foo: §= (function()
    {
     var res="I do whatever I want in expressions too!!";
     // do all you want !!!
     return res;
    })() §!"
 
  also if you provide a CedillaHelper class you can do even more.
  For example you can load external files:
 
    §= loadFile(".../path/to/external/file...", {also: "pass", what: "some keysValues", and: "Be very Happy! :-))"}); §
 
  or import code from external files:
 
    § importFrom(".../path/to/external/file...", {also: "pass", what: "some keysValues", and: "Be very Happy! :-))"});
   
  Cedilla is also configurable, you don't like "§" and "|"? No problem, provide you own: 

    String res=Cedilla.render( 
        new Configuration()
        {{
          delimiter="%";
          multilineCodeDelimiters=new String[]{"%|", "|%"};
          expressionDelimiters=new String[]{"%=", "%"};
          internalVarPrefix="ȼ";
        }},
        "Hello, %what!", Cedilla.toValuesProvider(new String[]{"what", "World"}), null);

    System.out.println(res); // Hello, World!


Cedilla can also be used without parameters, since you can create stuff inside the template itself:

    String res=Cedilla.render(
        "§ var lastPetalIsHeLovesMe=Math.random()>=0.5; §§= "+
        "lastPetalIsHeLovesMe ? "+
        "'He loves me! 😍' : "+
        "'He loves me not! 😭' §"); // just the template, no parameters passed!

    System.out.println(res); 
    // sometimes 'He loves me! 😍', sometimes 'He loves me not! 😭'

Cedilla can be used for whatever kind of files you want, probably even for binary files (not tested). 




 
## Quick links

 * [Github project](https://github.com/ansorre/Cedilla)
