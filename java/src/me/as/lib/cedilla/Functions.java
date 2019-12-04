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



public class Functions
{
 Renderer owner;


 public Functions(Renderer owner)
 {
  this.owner=owner;
 }


 public Object loadAndEvalInScope(String path)
 {
  try
  {
   return owner.scriptEngine.eval(loadFile(path));
  }
  catch (Throwable tr)
  {
   throw new RuntimeException(tr);
  }
 }


 public String loadFile(String path)
 {
  String src=owner.helper.getSourceFor(path);
  return (String)new Renderer(owner.scriptEngine, owner.configuration, src, null, owner.helper).result;
 }





}
