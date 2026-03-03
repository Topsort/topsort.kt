# Topsort Analytics SDK - ProGuard/R8 consumer rules
# These rules are automatically applied to consuming apps.

# Keep public API classes and their members
-keep class com.topsort.analytics.Analytics { *; }
-keep interface com.topsort.analytics.TopsortAnalytics { *; }

# Keep model classes used in public API parameters and JSON serialization.
# Property names are used in toJsonObject() / fromJsonObject() methods,
# and data class component functions must be preserved for Kotlin interop.
-keep class com.topsort.analytics.model.Placement { *; }
-keep class com.topsort.analytics.model.Entity { *; }
-keep class com.topsort.analytics.model.EntityType { *; }
-keep class com.topsort.analytics.model.Impression { *; }
-keep class com.topsort.analytics.model.Impression$Factory { *; }
-keep class com.topsort.analytics.model.Click { *; }
-keep class com.topsort.analytics.model.Click$Factory { *; }
-keep class com.topsort.analytics.model.Purchase { *; }
-keep class com.topsort.analytics.model.PurchasedItem { *; }
-keep class com.topsort.analytics.model.ImpressionEvent { *; }
-keep class com.topsort.analytics.model.ClickEvent { *; }
-keep class com.topsort.analytics.model.PurchaseEvent { *; }
-keep class com.topsort.analytics.model.Session { *; }

# Keep banner public API
-keep class com.topsort.analytics.banners.BannerView { *; }
-keep class com.topsort.analytics.banners.BannerConfig { *; }
-keep class com.topsort.analytics.banners.BannerConfig$* { *; }
-keep class com.topsort.analytics.banners.BannerResponse { *; }

# Keep auction model classes
-keep class com.topsort.analytics.model.auctions.Auction { *; }
-keep class com.topsort.analytics.model.auctions.Auction$Factory { *; }
-keep class com.topsort.analytics.model.auctions.AuctionRequest { *; }
-keep class com.topsort.analytics.model.auctions.AuctionResponse { *; }
-keep class com.topsort.analytics.model.auctions.AuctionResponse$* { *; }
-keep class com.topsort.analytics.model.auctions.AuctionError { *; }
-keep class com.topsort.analytics.model.auctions.AuctionError$* { *; }
-keep class com.topsort.analytics.model.auctions.EntityType { *; }
-keep class com.topsort.analytics.model.auctions.Device { *; }
-keep class com.topsort.analytics.model.auctions.ApiConstants { *; }

# Keep service interfaces for mock injection in tests
-keep class com.topsort.analytics.service.AuctionsHttpService { *; }
-keep class com.topsort.analytics.service.TopsortAuctionsHttpService { *; }

# Keep HttpResponse and HttpClient (public API)
-keep class com.topsort.analytics.core.HttpResponse { *; }
-keep class com.topsort.analytics.core.HttpClient { *; }
-keep class com.topsort.analytics.core.RequestFactory { *; }
-keep class com.topsort.analytics.core.RequestFactory$Companion { *; }

# Keep WorkManager worker class names (WorkManager uses reflection)
-keep class com.topsort.analytics.worker.EventEmitterWorker { *; }
-keep class com.topsort.analytics.EventPipeline$EventEmitterWorker { *; }
