package com.topsort.analytics.core

import com.topsort.analytics.model.auctions.ApiConstants

@Deprecated("Use ApiConstants instead")
object ServiceSettings {
    var baseApiUrl: String
        get() = ApiConstants.baseApiUrl
        set(value) { ApiConstants.baseApiUrl = value }
}
