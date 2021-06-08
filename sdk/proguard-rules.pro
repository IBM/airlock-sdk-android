-dontskipnonpubliclibraryclassmembers
-dontshrink
-dontoptimize
-printmapping build/libs/output/obfuscation.map
-keepattributes
-adaptclassstrings
-dontnote
-dontwarn
-dontwarn org.**
-dontwarn com.ibm.airlock.common.**
-dontwarn com.github.**


-keep public class com.ibm.airlock.common.** {
  public protected *;
}

-keep public class org.** {
  public protected *;
}

# Keep Android classes
-keep class ** extends android.** {
    <fields>;
    <methods>;
}


# Keep serializable classes & fields
-keep class ** extends java.io.Serializable {
    <fields>;
}

-keep public class com.weather.airlock.sdk.AirlockManager{
  public *;
}

-keep public class com.weather.airlock.sdk.AirlyticsConstants{
  public *;
}

-keepattributes Exceptions

# airlock
#-keep class  com.weather.airlock.sdk.** { *; }
#-keep class  com.ibm.airlock.common.** {  *; }


# Keep - Applications. Keep all application classes, along with their 'main'
# methods.
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# Also keep - Enumerations. Keep the special static methods that are required in
# enumeration classes.
-keepclassmembers enum  * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** e(...);
    public static *** w(...);
}

# Rhino stuff
-keep public class org.mozilla.javascript.Scriptable
-keep public class org.mozilla.javascript.Callable
-keep public class org.mozilla.javascript.ContextFactory
-keep public class org.mozilla.javascript.Script
-keep public class org.mozilla.javascript.ScriptableObject

-keep  public class org.mozilla.javascript.Context {
  public static final int FEATURE_NON_ECMA_GET_YEAR;
  public static final int FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME;
  public static final int FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER;
  public static final int FEATURE_TO_STRING_AS_SOURCE;
  public static final int FEATURE_PARENT_PROTO_PROPERTIES;
  public final void setOptimizationLevel(int);
  public final void setInstructionObserverThreshold(int);
  public static org.mozilla.javascript.Context enter();
  public final  org.mozilla.javascript.ScriptableObject initStandardObjects();
  public final java.lang.Object evaluateString(org.mozilla.javascript.Scriptable , java.lang.String , java.lang.String , int , java.lang.Object);
  public final org.mozilla.javascript.Script compileString(java.lang.String, java.lang.String, int , java.lang.Object);
  public static boolean toBoolean(java.lang.Object);
  public static java.lang.String toString(java.lang.Object);
  public static void exit();
}

-keep  public class org.mozilla.javascript.Script {
  public abstract java.lang.Object exec(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable);
}
-keep  public class org.mozilla.javascript.Scriptable {
  public void setPrototype(org.mozilla.javascript.Scriptable);
}


-keep public class org.mozilla.javascript.ContextFactory{
	public protected *;
}

-keep class org.mozilla.javascript.VMBridge_custom { *; }
-keep class org.mozilla.javascript.jdk15.VMBridge_jdk15 { *; }
-keep class org.mozilla.javascript.jdk15.VMBridge_jdk13 { *; }
-keep class org.mozilla.javascript.jdk15.VMBridge_jdk11 { *; }

-keep class org.mozilla.javascript.VMBridge { *; }


-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}