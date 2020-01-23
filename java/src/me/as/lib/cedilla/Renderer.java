/*
 * Copyright 2019 Antonio Sorrentini
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package me.as.lib.cedilla;


import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import me.as.lib.core.StillUnimplemented;
import me.as.lib.core.extra.JavaScriptExtras;
import me.as.lib.core.lang.ArrayExtras;
import me.as.lib.core.lang.StringExtras;

import javax.script.ScriptEngine;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import static me.as.lib.core.lang.StringExtras.replace;


class Renderer
{
 private static final String ivcKey="_ȼ_internal_Vars_Count_ȼ_";
 private static final String resultKey="_ȼ_result_ȼ_";
 private static final String functionsKey="_ȼ_functions_ȼ_";

 // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

 Object result;

 protected ValuesProvider keyValues;
 protected CedillaHelper helper;
 protected Configuration configuration;
 protected ScriptEngine scriptEngine;

 private int delimiterLen;
 private int mulDelStartLen;
 private int mulDelEndLen;
 private int expDelStartLen;
 private int expDelEndLen;

 private Object template;
 private List<CodePiece> codePieces;
 private Functions functions;



 Renderer(Configuration configuration, Object template, ValuesProvider keyValues, CedillaHelper helper)
 {
  go(true, JavaScriptExtras.newJavaScriptEngine(), configuration, template, keyValues, helper);
 }

 Renderer(ScriptEngine scriptEngine, Configuration configuration, Object template, ValuesProvider keyValues, CedillaHelper helper)
 {
  go(false, scriptEngine, configuration, template, keyValues, helper);
 }

 private void go(boolean do_addKeyValues, ScriptEngine scriptEngine, Configuration configuration, Object template, ValuesProvider keyValues, CedillaHelper helper)
 {
  this.scriptEngine=scriptEngine;
  this.configuration=configuration;
  this.template=template;
  this.keyValues=keyValues;
  this.helper=helper;

  delimiterLen=configuration.delimiter.length();
  mulDelStartLen=configuration.multilineCodeDelimiters[0].length();
  mulDelEndLen=configuration.multilineCodeDelimiters[1].length();
  expDelStartLen=configuration.expressionDelimiters[0].length();
  expDelEndLen=configuration.expressionDelimiters[1].length();

  JSObject global=(JSObject)scriptEngine.get("global");
  if (global==null)
  {
   global=(JSObject)_i_eval("new Object();");
   scriptEngine.put("global", global);
  }

  if (helper!=null) functions=new Functions(this);

  if (do_addKeyValues)
  {
   for (String key : keyValues.getKeys())
   {
    Object value=keyValues.getValueFor(key);

    scriptEngine.put(key, value);
    global.setMember(key, value);
   }
  }

  render();
 }

 private Object _i_eval(String code)
 {
  try
  {
   return scriptEngine.eval(code);
  }
  catch (Throwable tr)
  {
   throw new RuntimeException(tr);
  }
 }


 private String getCode(String stringTemplate, int pos, boolean isMultiline)
 {
  if (isMultiline)
  {
   int end=stringTemplate.indexOf(configuration.multilineCodeDelimiters[1], pos);

   if (end>=0)
    return stringTemplate.substring(pos, end+mulDelEndLen);
   else
    return stringTemplate.substring(pos);
  }
  else
  {
   StringBuilder sb=new StringBuilder();
   boolean goOn;
   char ch;

   do
   {
    if (pos<stringTemplate.length())
    {
     try
     {
      if (stringTemplate.substring(pos).startsWith(configuration.delimiter))
      {
       sb.append(configuration.delimiter);
       return sb.toString();
      }
     } catch (Throwable ignore){}

     ch=stringTemplate.charAt(pos++);

     sb.append(ch);
     goOn=!(ch=='\n' || ch=='\r');

     if (!goOn)
     {
      do
      {
       try
       {
        ch=stringTemplate.charAt(pos++);
       }
       catch (Throwable tr)
       {
        // just end of file!
        ch=0;
       }

       if (ch=='\n' || ch=='\r') sb.append(ch);
       else
       {
        break;
       }
      } while (true);
     }
    }
    else
    {
     goOn=false;
    }

   } while (goOn);

   return sb.toString();
  }
 }


 private String getIdentifier(String stringTemplate, int pos)
 {
  StringBuilder sb=new StringBuilder();
  boolean goOn=true;
  boolean inArrayIndex=false;
  char ch;
  int delimiterIdx=-1;
  int len=stringTemplate.length();

  do
  {
   if (pos==len)
   {
    if (inArrayIndex)
     throw new RuntimeException("Array opend with "+sb.toString()+" but never closed!");

    if (sb.length()==0)
     throw new RuntimeException("Empty identifier!?! File finished!");

    goOn=false;
   }

   if (goOn)
   {
    ch=stringTemplate.charAt(pos++);

    if (ch=='[')
    {
     if (sb.length()==0)
      throw new StillUnimplemented("getIdentifier with '[' before any char???");

     inArrayIndex=true;
    }

    if (inArrayIndex)
    {
     sb.append(ch);
     goOn=(ch!=']');
    }
    else
    {
     if (delimiterIdx+1==delimiterLen) goOn=false;
     else
     {
      if (ch==configuration.delimiter.charAt(delimiterIdx+1))
      {
       goOn=true;
       delimiterIdx++;
      }
      else
      {
       if (delimiterIdx>-1)
       {
        String tmp=sb.toString();
        tmp=tmp.substring(0, tmp.length()-(delimiterIdx+1));
        sb.setLength(0);
        sb.append(tmp);
        goOn=false;
       }
       else
       {
        goOn=(Character.isAlphabetic(ch) || Character.isDigit(ch) || ch=='_' || ch=='.' || ch==configuration.delimiter.charAt(0));
       }
      }
     }

     if (goOn)
     {
      sb.append(ch);
     }
    }
   }
  } while (goOn);

  return sb.toString();
 }


 private void render()
 {
  if (template instanceof String)
  {
   String stringTemplate=(String)template;
   boolean reRun;
   int idx;

   do
   {
    reRun=false;
    idx=stringTemplate.indexOf(configuration.delimiter);

    if (idx>=0)
    {
     String old=stringTemplate;
     stringTemplate=elab(stringTemplate);
     reRun=!StringExtras.areEqual(old, stringTemplate);
    }
   } while (reRun);

   result=stringTemplate;
  }
  else
  {
   throw new StillUnimplemented("At the moment Cedilla can render only string assets");
  }
 }


 private void addPreamble(StringBuilder code, boolean skip_write, boolean skip_functions)
 {
  scriptEngine.put(resultKey, new Result());
  if (!skip_write) code.append("function write(o){"+resultKey+".write(o);}\n");

  if (functions!=null)
  {
   scriptEngine.put(functionsKey, functions);

   if (!skip_functions)
   {
    code.append("function loadFile(path){return "+functionsKey+".loadFile(path);}\n");
    code.append("function importFrom(path){return "+functionsKey+".loadAndEvalInScope(path);}\n");
   }
  }

 }


 private void addPostamble(StringBuilder code)
 {
  code.append("\n"+resultKey+";");
 }



 // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

 private enum Type
 {
  text,
  code
 }

 private class CodePiece
 {
  Type type;
  String string;

  CodePiece(Type type, String string)
  {
   this.type=type;
   this.string=string;

   if (type==Type.code)
   {
    this.string=StringExtras.purgeCommentsFromCLikeLanguageSource(this.string);
   }

  }
 }


 private String preScanTransformations(String stringTemplate)
 {
  boolean reRun=false;

  do
  {
   int idx=stringTemplate.indexOf(configuration.expressionDelimiters[0]);

   reRun=idx>=0;

   if (reRun)
   {
    int eIdx=stringTemplate.indexOf(configuration.expressionDelimiters[1], idx+expDelStartLen);

    if (eIdx<0)
     throw new RuntimeException("Error! expression started at "+idx+" but never ended!");

    String sub=stringTemplate.substring(idx, eIdx+1);
    String newS=replace(sub, configuration.expressionDelimiters[0],
     configuration.multilineCodeDelimiters[0]+"write(");
    newS=newS.substring(0, newS.length()-expDelEndLen);
    newS+=");"+configuration.multilineCodeDelimiters[1];

    stringTemplate=replace(stringTemplate, sub, newS);
   }

  } while (reRun);

  return stringTemplate;
 }


 private void scanTemplate(String stringTemplate)
 {
  stringTemplate=preScanTransformations(stringTemplate);

  codePieces=new ArrayList<>();

//  ArrayList<String> varsYetDone=new ArrayList<>();
  boolean reRun;
  boolean lookForMultiline;
  int prevIdx=0;
  int idx;
  int mlIdx;

  do
  {
   reRun=false;

   mlIdx=stringTemplate.indexOf(configuration.multilineCodeDelimiters[0], prevIdx);
   idx=stringTemplate.indexOf(configuration.delimiter, prevIdx);

   if (mlIdx>=0 && (mlIdx<=idx || idx<0))
   {
    idx=mlIdx;
    lookForMultiline=true;
   }
   else
   {
    lookForMultiline=false;
   }

   if (idx>=0)
   {
    codePieces.add(new CodePiece(Type.text, stringTemplate.substring(prevIdx, idx)));
    char postDelimiterChar=stringTemplate.charAt(idx+delimiterLen);

    if (!lookForMultiline && Character.isAlphabetic(postDelimiterChar))
    {
     int pos=idx+delimiterLen;
     String key=getIdentifier(stringTemplate, pos);

     String keyToWrite=key;
     if (keyToWrite.endsWith(configuration.delimiter))
     {
      keyToWrite=keyToWrite.substring(0, keyToWrite.length()-delimiterLen);
     }

     codePieces.add(new CodePiece(Type.code, "write("+keyToWrite+");"));
     prevIdx=idx+delimiterLen+key.length();
     reRun=true;
    }
    else
    {
     int pos=lookForMultiline ? idx+mulDelStartLen : idx+delimiterLen;
     String ending=lookForMultiline ? configuration.multilineCodeDelimiters[1] : configuration.delimiter;
     String code=getCode(stringTemplate, pos, lookForMultiline);
     String okCode=code;

     if (okCode.endsWith(ending))
     {
      okCode=okCode.substring(0, okCode.length()-ending.length());
     }

     codePieces.add(new CodePiece(Type.code, okCode.trim()));
     prevIdx=pos+code.length();
     reRun=true;
    }
   }
   else
   {
    if (prevIdx<stringTemplate.length())
    {
     codePieces.add(new CodePiece(Type.text, stringTemplate.substring(prevIdx)));
    }
   }
  } while (reRun);

 }


 private int getOrCreate_internalVarsCount()
 {
  int res;
  try{ res=(int)scriptEngine.get(ivcKey); }catch (Throwable tr){ res=0; }
  return res;
 }



 private String elab(String stringTemplate)
 {
  StringBuilder code=new StringBuilder();
  int internalVarsCount=getOrCreate_internalVarsCount();

  Object prevResult=scriptEngine.get(resultKey);
  Object prevFunctions=scriptEngine.get(functionsKey);

  addPreamble(code, prevResult!=null, prevFunctions!=null);
  scanTemplate(stringTemplate);

  int t, len=ArrayExtras.length(codePieces);

  for (t=0;t<len;t++)
  {
   CodePiece cp=codePieces.get(t);

   switch (cp.type)
   {
    case code:
    {
     code.append(cp.string);
    } break;

    case text:
    {
     String varName=configuration.internalVarPrefix+(internalVarsCount++);
     scriptEngine.put(varName, cp.string);
     code.append("write(").append(varName).append(");");
    } break;
   }
  }

  addPostamble(code);
  scriptEngine.put(ivcKey, internalVarsCount);

  try
  {
   Result cr=(Result)scriptEngine.eval(code.toString());

   if (prevResult!=null) scriptEngine.put(resultKey, prevResult);
   if (prevFunctions!=null) scriptEngine.put(functionsKey, prevFunctions);

   return cr.sb.toString();
  }
  catch (Throwable tr)
  {
   System.err.println("OFFENDING CODE:\n"+code.toString());
   throw new RuntimeException(tr);
   //console.log(tr.getMessage());
   //systemErrDeepCauseStackTrace(tr);
   //log.exception(tr);
  }




 }


}
