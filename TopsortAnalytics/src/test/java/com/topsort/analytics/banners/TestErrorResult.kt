package com.topsort.analytics.banners

/**
 * Fake implementation of coil.request.ErrorResult for testing
 */
interface ErrorResult {
    val request: ImageRequest
    val throwable: Throwable
}

/**
 * Fake implementation of coil.request.ImageRequest for testing
 */
interface ImageRequest 