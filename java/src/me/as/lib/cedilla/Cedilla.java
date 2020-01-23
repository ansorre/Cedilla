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


import me.as.lib.core.lang.ArrayExtras;
import me.as.lib.core.lang.ClassExtras;
import me.as.lib.core.lang.StringExtras;
import me.as.lib.core.system.FileSystemExtras;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static me.as.lib.core.lang.StringExtras.parseJson;


/**
 * Cedilla templating engine is incredibly small, quick, effective, easy!
 *
 * to insert the content of a variable:
 *
 *       template example: "this is the content of foo: §foo"
 *
 * to execute some javascript inside the template:
 *
 * one line way:
 *
 *       template example: " § // the rest till the enf of line is javascript"
 *
 * another one line way:
 *
 *       template example: " § var foo="I'm it!"; §This is foo: §foo
 *
 * the multiline way is possibile too, instead of § <javascript> § you just go with §| <javascript> |§
 *
 *       template example:
 *       "§|
 *
 *        var createdHere=
 *               'This is made now!';
 *
 *       |§ this is the content made now: §createdHere"
 *
 * expressions are easy too, you include them in §= <expression> §:
 *
 *       template example: "This is more then foo: §= foo+" and some more" §!"
 *
 *       but thanks to wonderful javascript this means you can also do things like:
 *
 *       template ->
 *       "This is more then foo: §= (function()
 *       {
 *        var res=....
 *        // do all you want !!!
 *        return res;
 *       })() §!"
 *
 * also if you provide a CedillaHelper class you can do even more.
 * For example you can load external files:
 *
 * "§= loadFile(".../path/to/external/file...", {also: "pass", what: "some keysValues", and: "Be very Happy! :-))"}); §"
 *
 * or import code from external files:
 *
 * "§ importFrom(".../path/to/external/file...", {also: "pass", what: "some keysValues", and: "Be very Happy! :-))"});
 *
 * Cedilla is the simplest, quickest, smallest yet most powerful templating engine in the entire Universe!!! Ah ah ah ah! But it's true!
 *
 */
public class Cedilla
{
 private static final ValuesProvider nokeysValues=
  new ValuesProvider() {public String[] getKeys(){return new String[]{};}public Object getValueFor(String key){return null;}};

 /**
  * Why this? You could ask! What is used for without keysValues? Well you can create them inside the template! :-) :-) :-)
  * Example:
  *
  * Cedilla.render("§|var foo='For example this is added inside!';|§I'm useful even without keysValues! §foo");
  *
  * results in:
  *
  * I'm useful even without keysValues! For example this is added inside!
  *
  */
 public static <S> S render(Object template)
 {
  return render(template, (Object)null);
 }

 public static <S> S render(Object template, CedillaHelper helper)
 {
  return render(template, (Object)null, helper);
 }

 public static <S> S render(Object template, Object keysValues)
 {
  return render(template, toValuesProvider(keysValues));
 }

 public static <S> S render(Object template, Object keysValues, CedillaHelper helper)
 {
  return render(template, toValuesProvider(keysValues), helper);
 }

 public static <S> S render(Object template, Map<String, Object> keysValues)
 {
  return render(template, toValuesProvider(keysValues));
 }

 public static <S> S render(Object template, Map<String, Object> keysValues, CedillaHelper helper)
 {
  return render(template, toValuesProvider(keysValues), helper);
 }


 public static <S> S render(Object template, String jsonString)
 {
  return render(template, toValuesProvider(jsonString));
 }

 public static <S> S render(Object template, String jsonString, CedillaHelper helper)
 {
  return render(template, toValuesProvider(jsonString), helper);
 }


 public static <S> S render(Object template, ValuesProvider keysValues)
 {
  return render(Configuration.defaultConfiguration, template, keysValues, null);
 }

 public static <S> S render(Object template, ValuesProvider keysValues, CedillaHelper helper)
 {
  return render(Configuration.defaultConfiguration, template, keysValues, helper);
 }



 // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

 public static ValuesProvider toValuesProvider(Object keysValues)
 {
  if (keysValues==null) return nokeysValues;
  if (keysValues instanceof ValuesProvider) return (ValuesProvider)keysValues;
  if (keysValues instanceof String) return toValuesProvider(parseJson((String)keysValues));
  if (keysValues instanceof String[]) return toValuesProvider(StringExtras.quickMap((String[])keysValues));

  if (keysValues instanceof Map) return
   new ValuesProvider() {
      public String[] getKeys()
      {return ArrayExtras.toArray(((Map)keysValues).keySet(), String.class);}

      public Object getValueFor(String key)
      {return ((Map)keysValues).get(key);}
     };

  HashMap<String, Object> hmkeysValues=new HashMap<>();
  Field fields[]=ClassExtras.getAllFields(keysValues.getClass());
  int t, len=ArrayExtras.length(fields);

  for (t=0;t<len;t++)
  {
   Field field=fields[t];
   hmkeysValues.put(field.getName(), ClassExtras.getFieldValue_bruteForce(keysValues, field));
  }

  return toValuesProvider(hmkeysValues);
 }


 public static <S> S render(Configuration configuration, Object template, ValuesProvider keysValues, CedillaHelper helper)
 {
  return (S)new Renderer(configuration, template, keysValues, helper).result;
 }


}
