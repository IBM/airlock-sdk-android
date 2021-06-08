package com.weather.airlock.sdk;

public class AirlyticsConstants {

        public static final String AIRLYTICS="app.Airlytics";

        public static final String ENVIRONMENTS="analytics.Environments";
        public static final String PROVIDERS="analytics.Providers";
        public static final String EVENTS="analytics.Events";
        public static final String USER_ATTRIBUTES_FEATURE ="airlytics.User Attributes";
        public static final String ATTRIBUTE_GROUPS="analytics.User Attributes Grouping";
        public static final String LOCATION_CHANGED_FEATURE="analytics.Location changed";
        public static final String PREMIUM_FEATURE_NAME =  "ads.Ad Free";

        public static final String REST_EVENT_PROXY_NAME="REST_EVENT_PROXY";
        public static final String REST_EVENT_PROXY_HANDLER="com.weather.airlytics.providers.RestEventProxyProvider";

        public static final String EVENT_LOG_PROVIDER_NAME="EVENT_LOG";
        public static final String EVENT_LOG_PROVIDER_HANDLER="com.weather.airlytics.providers.EventLogProvider";

        public static final String DEBUG_BANNERS_PROVIDER_NAME="DEBUG_BANNERS";
        public static final String DEBUG_BANNERS_PROVIDER_HANDLER="com.weather.airlytics.providers.DebugBannersProvider";

        public static final String STREAMS_EVENT_PROXY_NAME="STREAMS_EVENTS";
        public static final String STREAMS_EVENT_PROXY_HANDLER="com.weather.airlock.sdk.airlytics.StreamsProvider";

        ///Event Names
        public static final String SESSION_START_EVENT = "session-start";
        public static final String SESSION_END_EVENT = "session-end";
        public static final String USER_ATTRIBUTES_EVENT = "user-attributes";
        public static final String STREAM_RESULTS_EVENT = "stream-results";
        public static final String APP_CRASH_EVENT = "app-crash";
        public static final String APP_LAUNCH_EVENT = "app-launch";
        public static final String NOTIFICATION_INTERACTED_EVENT = "notification-interacted";
        public static final String FIRST_TIME_LAUNCH_EVENT = "first-time-launch";


        public static final String PURCHASE_SCREEN_VIEWED_EVENT = "purchase-screen-viewed";
        public static final String PURCHASE_ATTEMPTED_EVENT = "purchase-attempted";
        public static final String PURCHASE_RENEWED_EVENT = "purchase-renewed";
        public static final String PURCHASE_EXPIRED_EVENT = "purchase-expired";
        public static final String PURCHASE_RESTORED_EVENT = "purchase-restored";
        public static final String PURCHASE_REPORT_ISSUE_EVENT = "purchase-report-issue";

        public static final String INAPP_MESSAGE_DISPLAYED_EVENT = "in-app-message-displayed";
        public static final String INAPP_MESSAGE_INTERACTED_EVENT = "messaging-campaign-interacted";
        public static final String INAPP_MESSAGE_SUPPRESSED_EVENT = "in-app-message-suppressed";

        public static final String VIDEO_PLAYED_EVENT = "video-played";
        public static final String VIDEO_ASSET_VIEWED_EVENT = "asset-viewed";
        public static final String LOCATION_VIEWED_EVENT = "location-viewed";

        public static final String DEV_ENV_NAME = "analytics.DevEnvironment";

        //attribute names
        public static final String ATTRIBUTES = "attributes";
        public static final String ATTRIBUTION = "attribution";
        public static final String PUSH_TOKEN_ATTRIBUTE = "pushToken";
        public static final String PUSH_AUTHORIZATION_ATTRIBUTE = "pushAuthorization";
        public static final String PREMIUM_ATTRIBUTE = "premium";
        public static final String PREMIUM_PRODUCT_ATTRIBUTE = "premiumProductId";

        public static final String INSTALL_DATE_ATTRIBUTE = "installDate";
        public static final String INSTALL_UPDATE_ATTRIBUTE = "versionInstallDate";
        public static final String UPS_ID_ATTRIBUTE = "upsId";
        public static final String PERSONALIZED_ADS_ATTRIBUTE = "personalizedAds";
        public static final String DEV_USER_ATTRIBUTE = "devUser";
        public static final String SOURCE_ATTRIBUTE = "source";
        public static final String HIGHLIGHTED_PRODUCT_ID_ATTRIBUTE = "highlightedProductId";
        public static final String TYPE_ATTRIBUTE = "type";
        public static final String SCREEN_TYPE_ATTRIBUTE = "screenType";
        public static final String PRODUCTID_ATTRIBUTE = "productId";
        public static final String COMPLETED_ATTRIBUTE = "completed";
        public static final String ERROR_ATTRIBUTE = "error";
        public static final String USER_CANCELLED_ATTRIBUTE = "userCancelled";
        public static final String PRICE_ATTRIBUTE = "price";
        public static final String CURRENCY_ATTRIBUTE = "currency";
        public static final String EXPIRATION_DATE_ATTRIBUTE = "expirationDate";
        public static final String ACTIVE_EXPIRED_PRODUCTID_ATTRIBUTE = "activeOrExpiredProductId";
        public static final String CAMPAIGN_ID_ATTRIBUTE = "campaignId";
        public static final String CAMPAIGN_NAME_ATTRIBUTE = "campaignName";
        public static final String ID_ATTRIBUTE = "id";
        public static final String NAME_ATTRIBUTE = "name";
        public static final String CREATIVE_ATTRIBUTE = "creative";
        public static final String SYSTEM_ATTRIBUTE = "system";
        public static final String URL_ATTRIBUTE = "url";
        public static final String DURATION_ATTRIBUTE = "duration";
        public static final String LOCATION_AUTHORIZATION_ATTRIBUTE = "locationAuthorization";
        public static final String PREMIUM_START_DATE_ATTRIBUTE = "premiumStartDate";

        public static final String LOCALYTICS_SYSTEM = "localytics";

        public static final String APPSFLYER_ATTRIBUTION_STATUS = "attributionStatus";
        public static final String APPSFLYER_ATTRIBUTION_CAMPAIGN = "attributionCampaign";
        public static final String APPSFLYER_ATTRIBUTION_MEDIA_SOURCE = "attributionMediaSource";

        public static final String PREMIUM_PRODUCT_ID_ATTRIBUTE = "premiumProductId";
        public static final String PREMIUM_TRANSACTION_ID_ATTRIBUTE = "premiumTransactionId";
        public static final String PREMIUM_EXPIRATION_DATE_ATTRIBUTE = "premiumExpirationDate";
        public static final String SOURCE_ATTRIBUTE_AIRLYTICS = "airlytics";
        public static final String DEVICE_MODEL_ATTRIBUTE = "deviceModel";
        public static final String OS_VERSION_ATTRIBUTE = "osVersion";
        public static final String DEVICE_COUNTRY_ATTRIBUTE = "deviceCountry";
        public static final String DEVICE_LANGUAGE_ATTRIBUTE = "deviceLanguage";
        public static final String DEVICE_TIMEZONE_ATTRIBUTE = "deviceTimeZone";
        public static final String DEVICE_PLATFORM_ATTRIBUTE = "devicePlatform";
        public static final String THIRD_PARTY_ID_ATTRIBUTE = "thirdPartyId";
        public static final String INAPP_SOURCE = "inApp";
        public static final String DEVICE_PHONE = "phone";
        public static final String DEVICE_TABLET = "tablet";
        public static final String DEVICE_TV = "tv";
        public static final String IN_APP_AUTHORIZED = "authorized";
        public static final String IN_APP_DENIED = "denied";

        public static final String EXPERIMENT_ATTRIBUTE = "experiment";
        public static final String VARIANT_ATTRIBUTE = "variant";

        public static final String JSON_EVENT_ID = "eventId";
        public static final String JSON_EVENT_NAME = "eventName";
        public static final String JSON_EVENT_TIME = "eventTime";
        public static final String JSON_USER_ID = "userId";
        public static final String JSON_AIRLYTICS = "airlytics";
        public static final String JSON_ANALYTICS_SYSTEM = "analyticsSystem";
        public static final String JSON_NAME = "name";
        public static final String JSON_ENABLE_CLIENTSIDE_VALIDATION = "enableClientSideValidation";
        public static final String JSON_SESSION_EXPIRATION_IN_SECONDS = "sessionExpirationInSeconds";
}
