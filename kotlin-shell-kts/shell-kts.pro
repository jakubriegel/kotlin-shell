-target 1.8
-dontoptimize
-dontobfuscate
-dontpreverify

-keepdirectories META-INF/**

-keep class eu.jrie.jetbrains.** { *; }
-keep class kotlinx.** { *; }

-ignorewarnings
-dontnote **
-dontwarn eu.jrie.jetbrains.**
-dontwarn org.jetbrains.kotlin.**
-dontwarn org.kotlin.**
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn org.jetbrains.annotations.**
-dontwarn org.intellij.**
-dontwarn gnu.**
-dontwarn java.**
-dontwarn org.slf4j.**
-dontwarn org.zeroturnaround.**
