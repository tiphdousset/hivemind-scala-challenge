package com.nealmilstein.scalachallenge

final case class Review(
    asin: String,
    reviewerID: String,
    reviewerName: String,
    helpful: List[Int],
    reviewText: String,
    overall: Float,
    summary: String,
    unixReviewTime: Long
)
