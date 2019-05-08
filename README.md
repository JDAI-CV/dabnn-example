# dabnn example

[![License](https://img.shields.io/badge/license-BSD--3--Clause-blue.svg)](LICENSE) 
[![jcenter](https://img.shields.io/badge/dynamic/json.svg?label=jcenter&query=name&url=https%3A%2F%2Fapi.bintray.com%2Fpackages%2Fdaquexian566%2Fmaven%2Fdabnn%2Fversions%2F_latest)](https://bintray.com/daquexian566/maven/dabnn/_latestVersion)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/JDAI-CV/dabnn-example/pulls)

This example contains Bi-Real Net 18 with Stem Module (The network structure will be described in detail in our coming paper). 

Our package for Android has been published on jcenter. Just add

```
implementation 'me.daquexian:dabnn:replace_me_with_the_latest_version'
```

in your app's `build.gradle`'s `dependencies` section.

The latest version can be found in the following badge:

[![jcenter](https://img.shields.io/badge/dynamic/json.svg?label=jcenter&query=name&url=https%3A%2F%2Fapi.bintray.com%2Fpackages%2Fdaquexian566%2Fmaven%2Fdabnn%2Fversions%2F_latest)](https://bintray.com/daquexian566/maven/dabnn/_latestVersion)

Just try it in Android Studio :)

Note: This demo uses OpenCV to read camera, however, the app orientation can only be landscape due to [a bug of OpenCV](https://github.com/opencv/opencv/issues/4704). PRs which replace OpenCV by raw Android Camera API are welcomed.
