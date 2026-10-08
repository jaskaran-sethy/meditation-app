# R8 rules for the release build. The app uses no reflection or serialization, and AndroidX and
# Compose ship their own consumer rules, so the default optimize rules are all it needs.

# Keep line numbers in Play Console crash reports (the mapping file is uploaded with the bundle).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
