# R8 rules for the release build. Libraries ship their own consumer rules; add here only what they miss.

# Keep file and line numbers so Crashlytics can show readable stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception
