# Android Localizationer

This is a Android Studio/ IntelliJ IDEA plugin to localize your Android app, translate your string resources automactically.

Translate all your strings in your string resources(e.g. `strings.xml`) to your target languages automactically. Help developers localize their Android app easily, with just one click.


# Usage

Right click the strings resource file, choose 'Convert to other languages'.<br>
![img](https://raw.githubusercontent.com/JantHsueh/AndroidLocalizationer/master/screen_shot_3.png)<br>
Then check the target languages.<br>
![img](https://raw.githubusercontent.com/JantHsueh/AndroidLocalizationer/master/screen_shot_2.png) 
<br>
After clicking `OK`, the string resources will be translated and created in the correct value folder.

# Feature

* Filter the strings you don't wanna translate by adding `NAL_` prefix to the `string key`, case sensitive. Change:<br>
`<string name="flurry_id">FLURRY_ID</string>`<br>
to<br>
`<string name="NAL_flurry_id">FLURRY_ID</string>`

* Filter the strings you don't wanna translate by adding `filter rule` in plugin settings interface

* With google translate, do not need to add a secret key

* input from excel and output to excel
 
  select outputExcel checkbox，Export the selected language to excel.path is project res/values-xx.xls.
  select inputExcel checkbox,Input the selected language from excel.path is must project res/values-xx.xls.
  if select outputExcel or inputExcel, will not translate.
  
* Set client id or client secret for Microsoft Translator, in case of running out of quota. 
	* [How to get Microsoft Translator client id and client secret?](http://blogs.msdn.com/b/translation/p/gettingstarted1.aspx)
	
	![img](https://raw.githubusercontent.com/JantHsueh/AndroidLocalizationer/master/screen_shot_5.png) 
	 

More features are coming, please check [Todo](https://github.com/JantHsueh/AndroidLocalizationer#todo).

# Warning
* Currently, Android Localizationer only support translate **Chinese** to other languages
* The result may **not** meet your standards due to the Translation API that this plugin is using, so keep your string resources **as simple as possible**
* Try to avoid using special symbols


# Downloads
You can download the plugin [here](https://github.com/JantHsueh/AndroidLocalizationer/releases).

To Install the plugin, please check [IntelliJ IDEA Web Help](https://www.jetbrains.com/idea/help/installing-updating-and-uninstalling-repository-plugins.html#d1282549e185).

# ChangeLog
Version 0.1.8
* add input and output to excel
* Fix bug:Single quotation marks caused the problem. like: "I'm good man" in android display "I".fix "I'm good man" to "I\'m good man"

Version 0.1.7
* Solved the problem of translation quotes

Version 0.1.6
* With google translate, do not need to add a secret key
* Default translation source language is Chinese
* Fix install to AndroidStudio3.0 bug

Version 0.1.5
* Fix [issue #17](https://github.com/westlinkin/AndroidLocalizationer/issues/17)
* Fix [issue #1](https://github.com/westlinkin/AndroidLocalizationer/issues/1)

Version 0.1.4

* Fix [issue #13](https://github.com/westlinkin/AndroidLocalizationer/issues/13)
* Fix [issue #15](https://github.com/westlinkin/AndroidLocalizationer/issues/15)

Version 0.1.3

* Fix bug: translation fails when there are too many string resources
* Fix bug: translation fails when there are special symbols, like `€`
* Fix bug: translation fails when there are special tags, like `<u>`
* Fix Java escape problems in MS Translator

Version 0.1.2

* Add Google Translation API support. Please **NOTE** that this is a [paid service](https://cloud.google.com/translate/v2/pricing).
* Fix bug: show error when opening the translated strings.xml file

Version 0.1.1

* Fix bug: when translate to more than one language, only the first target language will be translated correctly
* Fix bug: filter rule in plugin settings cannot be filtered
* Fix bug: wrongly show 'Quota exceed' error dialog when both not running out of quota and no strings need to be translated
     

Version 0.1.0

* Add **filter rule** setting in plugin settings interface, filter strings you don't wanna translate
* Fix a possible throwable when automatically open the translated strings.xml file

Version 0.0.3

* Only show 'Convert to other languages' menu on `strings.xml` files, current only `strings.xml` file under `values` or `values-en` folders.* 
* Add an icon before 'Convert to other languages' menu* 
* Add a plugin settings interface, client id and client secret for Microsoft Translator can be set by users* 
* Popup error message when Microsoft Translator quota exceed or client id/ client secret is invalid

Version 0.0.2

* Fix string error on the popup dialog

Version 0.0.1

* Publish project


## Todo
* Multiple translation engine
* Plugin Settings
	* <del>Choose the translation engine (translation API) you wanna use
	* <del>Set translation engine (translation API)'s application key, in case of the API is runing out of quota
	* <del>Filter the `string` key that you don't wanna translate, e.g. `app_name`, `some_api_key`
* <del>Only show the `Convert to other languages` in the popup menu when right clicking the string resources, like [Google's Translation Editor](http://tools.android.com/recent/androidstudio087released) does
* Support more source languages
* Support string arrays


## License

	Copyright 2014-2016 Wesley Lin

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

    	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
