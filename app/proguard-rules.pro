# Add project specific ProGuard rules here.
# Room database
-keep class com.N3looG.thingtracker.db.** { *; }
-keep class com.N3looG.thingtracker.model.** { *; }

# AdMob
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class com.google.android.gms.ads.** { *; }
