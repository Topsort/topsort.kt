# Keep public API
-keep public class com.topsort.analytics.Analytics {
    public *;
}

# Keep public model classes
-keep public class com.topsort.analytics.model.** {
    public *;
}

# Keep public banner classes
-keep public class com.topsort.analytics.banners.** {
    public *;
}

# Keep any public interfaces
-keep public interface com.topsort.analytics.** {
    public *;
}

# Keep enums
-keepclassmembers enum com.topsort.analytics.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep ApiConstants
-keepclassmembers class com.topsort.analytics.model.auctions.ApiConstants {
    public static final *;
}
